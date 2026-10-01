#!/usr/bin/env bash
# Builds single-file client and server executables for Linux and Windows (x86_64).
#
# Inputs:  ../survival/client_survival.jar, ../survival/minecraft_server_survival.jar
#          and the RetroMCP workspace (../retromcp)
# Usage:   ./build.sh <version>        e.g. ./build.sh b1.5
# Outputs: ../survival/linux/<version>/   alphar-{client,client-test,server}-linux + linux-<version>.zip
#          ../survival/windows/<version>/ alphar-{client,client-test,server}-windows.exe + windows-<version>.zip
#          (the zips hold only the client and server, for sharing)
#
# Needs: go, curl, zip, unzip, python3. Java 21 JDKs for both platforms are
# downloaded into .cache/ on first use (jlink builds the bundled runtimes).
set -euo pipefail

VERSION="${1:-}"
if [ -z "$VERSION" ]; then
	echo "usage: $0 <version>   (e.g. $0 b1.5)" >&2
	exit 1
fi

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
MCP="$ROOT/retromcp"
CLIENT="$ROOT/survival/client_survival.jar"
SERVER="$ROOT/survival/minecraft_server_survival.jar"
CACHE="$HERE/.cache"
STAGE="$HERE/.stage"
OUT="$HERE/.out"
rm -rf "$OUT" && mkdir -p "$OUT"
DIST="$ROOT/survival"
if [ -e "$DIST/linux/$VERSION" ] || [ -e "$DIST/windows/$VERSION" ]; then
	echo "$VERSION already exists in $DIST; pick a new version name" >&2
	exit 1
fi
JDK_VER="21.0.12.1_1"
JDK_TAG="jdk-21.0.12.1%2B1"
CLIENT_MODULES="java.base,java.desktop,java.logging,jdk.unsupported,jdk.crypto.ec"
SERVER_MODULES="java.base,java.desktop,java.logging"
MCLIBS="https://libraries.minecraft.net"

mkdir -p "$CACHE"
cd "$CACHE"
if [ ! -x jdk-linux/bin/jlink ]; then
	curl -fL -o jdk-linux.tar.gz "https://github.com/adoptium/temurin21-binaries/releases/download/$JDK_TAG/OpenJDK21U-jdk_x64_linux_hotspot_$JDK_VER.tar.gz"
	mkdir -p jdk-linux && tar xzf jdk-linux.tar.gz -C jdk-linux --strip-components=1
fi
if [ ! -d jdk-windows/jmods ]; then
	curl -fL -o jdk-windows.zip "https://github.com/adoptium/temurin21-binaries/releases/download/$JDK_TAG/OpenJDK21U-jdk_x64_windows_hotspot_$JDK_VER.zip"
	rm -rf jdk-windows jdk-windows-tmp && mkdir -p jdk-windows
	unzip -q jdk-windows.zip -d jdk-windows-tmp && mv jdk-windows-tmp/*/* jdk-windows/ && rm -rf jdk-windows-tmp
fi
[ -f lwjgl-natives-windows.jar ] || curl -fL -o lwjgl-natives-windows.jar "$MCLIBS/org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-windows.jar"
[ -f jinput-natives-windows.jar ] || curl -fL -o jinput-natives-windows.jar "$MCLIBS/net/java/jinput/jinput-platform/2.0.5/jinput-platform-2.0.5-natives-windows.jar"
JDK="$CACHE/jdk-linux"

# Launcher (username/server dialog), compiled against the launchwrapper.
LW="$(find "$MCP/libraries" -name 'launchwrapper-*.jar' ! -name '*sources*' | head -n1)"
rm -rf "$STAGE" && mkdir -p "$STAGE/classes"
"$JDK/bin/javac" --release 21 -nowarn -cp "$LW" -d "$STAGE/classes" "$HERE/src/java/AlpharLauncher.java"
"$JDK/bin/jar" --create --file "$STAGE/alphar-launcher.jar" -C "$STAGE/classes" .

# Zip a staged directory with Unix permissions preserved (keeps the runtime executable).
zipdir() {
	python3 - "$1" "$2" <<'PY'
import os, sys, zipfile
src, dst = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for base, dirs, files in os.walk(src):
        dirs.sort()
        for name in sorted(files):
            path = os.path.join(base, name)
            info = zipfile.ZipInfo.from_file(path, os.path.relpath(path, src))
            info.compress_type = zipfile.ZIP_DEFLATED
            with open(path, "rb") as f:
                z.writestr(info, f.read())
PY
}

runtime_for() { # os modules dest
	"$JDK/bin/jlink" --module-path "$CACHE/jdk-$1/jmods" --add-modules "$2" \
		--strip-debug --no-header-files --no-man-pages --compress=zip-9 --output "$3"
}

# Build the Go stub for both platforms around payload-{linux,windows}.zip.
stub() { # kind windows-ldflags
	(cd "$HERE/src/stub" &&
		CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -trimpath -ldflags "-s -w -X main.mode=$1" -o "$OUT/alphar-$1-linux" . &&
		CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build -trimpath -ldflags "-s -w -X main.mode=$1 $2" -o "$OUT/alphar-$1-windows.exe" .)
}

# --- client ---
for os in linux windows; do
	dir="$STAGE/client-$os"
	mkdir -p "$dir/lib" "$dir/natives"
	runtime_for "$os" "$CLIENT_MODULES" "$dir/runtime"
	cp "$CLIENT" "$dir/lib/minecraft.jar"
	cp "$STAGE/alphar-launcher.jar" "$dir/lib/"
	find "$MCP/libraries" -name '*.jar' ! -name '*sources*' ! -name '*natives*' -exec cp {} "$dir/lib/" \;
	cp -r "$MCP/jars/assets" "$dir/assets"
	if [ "$os" = linux ]; then
		cp "$MCP/libraries/natives/"*64.so "$dir/natives/"
		# LWJGL 2 (2015) asks for JAWT_GetAWT@SUNWprivate_1.1, which modern JDK
		# builds no longer export; make that version requirement non-fatal.
		python3 "$HERE/src/weaken_verneed.py" "$dir/natives/liblwjgl64.so" SUNWprivate_1.1
	else
		unzip -q -j -o "$CACHE/lwjgl-natives-windows.jar" '*.dll' -d "$dir/natives"
		unzip -q -j -o "$CACHE/jinput-natives-windows.jar" '*.dll' -d "$dir/natives"
	fi
	zipdir "$dir" "$HERE/src/stub/payload-$os.zip"
done
stub client "-H=windowsgui"
stub client-test "-H=windowsgui"

# --- server (console program, runs in the current directory) ---
for os in linux windows; do
	dir="$STAGE/server-$os"
	mkdir -p "$dir/lib"
	runtime_for "$os" "$SERVER_MODULES" "$dir/runtime"
	cp "$SERVER" "$dir/lib/minecraft_server.jar"
	zipdir "$dir" "$HERE/src/stub/payload-$os.zip"
done
stub server ""

rm -f "$HERE/src/stub/payload-linux.zip" "$HERE/src/stub/payload-windows.zip"
rm -rf "$STAGE"

# Arrange into survival/<os>/<version>/ with a shareable zip (client + server).
for os in linux windows; do
	ext=""
	[ "$os" = windows ] && ext=".exe"
	dest="$DIST/$os/$VERSION"
	mkdir -p "$dest" "$OUT/zip/$os"
	for kind in client client-test server; do
		mv "$OUT/alphar-$kind-$os$ext" "$dest/"
	done
	cp -p "$dest/alphar-client-$os$ext" "$dest/alphar-server-$os$ext" "$OUT/zip/$os/"
	(cd "$OUT/zip" && rm -f "$dest/$os-$VERSION.zip" && zip -q -r -9 "$dest/$os-$VERSION.zip" "$os")
	rm -rf "$OUT/zip/$os"
done
rm -rf "$OUT"
ls -lh "$DIST/linux/$VERSION" "$DIST/windows/$VERSION"
