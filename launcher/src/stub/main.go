// Single-file launcher for the survival-patched Minecraft Alpha 1.1.2_01 client
// or server (selected at build time with -ldflags "-X main.mode=server").
//
// The payload zip (Java runtime, game jars, natives, sounds) is embedded in the
// binary. On first run it is unpacked into the user cache directory, keyed by
// the payload hash, so later runs start immediately and an updated executable
// never reuses stale files.
package main

import (
	"archive/zip"
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"os"
	"os/exec"
	"os/signal"
	"path/filepath"
	"runtime"
	"strings"
)

// mode is "client", "client-test" (client with debug keys such as N for
// noclip) or "server"; set with -ldflags "-X main.mode=server".
var mode = "client"

func main() {
	if err := run(); err != nil {
		fail(err)
	}
}

func run() error {
	sum := sha256.Sum256(payload)
	cache, err := os.UserCacheDir()
	if err != nil {
		return fmt.Errorf("no cache directory: %w", err)
	}
	root := filepath.Join(cache, "alphar", hex.EncodeToString(sum[:])[:16])
	if _, err := os.Stat(filepath.Join(root, ".complete")); err != nil {
		if err := unpack(root); err != nil {
			return fmt.Errorf("unpacking game files: %w", err)
		}
	}

	java := filepath.Join(root, "runtime", "bin", "java")
	classpath := filepath.Join(root, "lib") + string(os.PathSeparator) + "*"
	var args []string
	dir := root
	userArgs, console := takeFlag(os.Args[1:], "-c")
	if mode == "server" {
		// Console program; the world and config files live in the directory it
		// is started from, just like running the server jar.
		if runtime.GOOS == "windows" {
			java += ".exe"
		}
		args = []string{"-Xms1G", "-Xmx1G", "-cp", classpath, "net.minecraft.server.MinecraftServer"}
		if dir, err = os.Getwd(); err != nil {
			return err
		}
		// Ctrl+C reaches the server directly; stay alive until it has exited.
		signal.Notify(make(chan os.Signal, 1), os.Interrupt)
	} else {
		args = []string{"-Xmx1G", "-Dalphar.home=" + root, "-cp", classpath, "AlpharLauncher"}
		if mode == "client-test" {
			args = append([]string{"-Dalphar.debug=true"}, args...)
		}
		if !console {
			// Default: detach from the terminal and log to <game dir>/client.log.
			if runtime.GOOS == "windows" {
				java = filepath.Join(root, "runtime", "bin", "javaw.exe")
			}
			return startDetached(java, append(args, userArgs...), dir)
		}
		// -c: stay attached and print the game's log to this console.
		if runtime.GOOS == "windows" {
			java += ".exe"
			attachParentConsole()
		}
	}
	cmd := exec.Command(java, append(args, userArgs...)...)
	cmd.Dir = dir
	cmd.Stdin, cmd.Stdout, cmd.Stderr = os.Stdin, os.Stdout, os.Stderr
	if err := cmd.Run(); err != nil {
		if exit, ok := err.(*exec.ExitError); ok {
			os.Exit(exit.ExitCode())
		}
		return fmt.Errorf("starting Java: %w", err)
	}
	return nil
}

// takeFlag removes every occurrence of flag from args and reports whether it
// was present.
func takeFlag(args []string, flag string) ([]string, bool) {
	var rest []string
	found := false
	for _, a := range args {
		if a == flag {
			found = true
		} else {
			rest = append(rest, a)
		}
	}
	return rest, found
}

// gameDir mirrors AlpharLauncher.gameDirectory: %APPDATA%\.alphar on Windows,
// ~/.alphar elsewhere.
func gameDir() string {
	if appData := os.Getenv("APPDATA"); runtime.GOOS == "windows" && appData != "" {
		return filepath.Join(appData, ".alphar")
	}
	home, _ := os.UserHomeDir()
	return filepath.Join(home, ".alphar")
}

// startDetached launches the game in its own session with output going to
// client.log in the game directory, and returns without waiting for it.
func startDetached(java string, args []string, dir string) error {
	logDir := gameDir()
	if err := os.MkdirAll(logDir, 0o755); err != nil {
		return err
	}
	logFile, err := os.Create(filepath.Join(logDir, "client.log"))
	if err != nil {
		return err
	}
	defer logFile.Close()
	cmd := exec.Command(java, args...)
	cmd.Dir = dir
	cmd.Stdout, cmd.Stderr = logFile, logFile
	cmd.SysProcAttr = detachedAttr()
	if err := cmd.Start(); err != nil {
		return fmt.Errorf("starting Java: %w", err)
	}
	return cmd.Process.Release()
}

// unpack extracts the payload into a temporary sibling directory and renames
// it into place, so an interrupted first run never leaves a half-written tree.
func unpack(root string) error {
	if err := os.MkdirAll(filepath.Dir(root), 0o755); err != nil {
		return err
	}
	tmp, err := os.MkdirTemp(filepath.Dir(root), ".unpack-")
	if err != nil {
		return err
	}
	defer os.RemoveAll(tmp)

	zr, err := zip.NewReader(bytes.NewReader(payload), int64(len(payload)))
	if err != nil {
		return err
	}
	for _, f := range zr.File {
		dest := filepath.Join(tmp, filepath.FromSlash(f.Name))
		if !strings.HasPrefix(dest, tmp+string(os.PathSeparator)) {
			return fmt.Errorf("bad path in payload: %s", f.Name)
		}
		if f.FileInfo().IsDir() {
			if err := os.MkdirAll(dest, 0o755); err != nil {
				return err
			}
			continue
		}
		if err := os.MkdirAll(filepath.Dir(dest), 0o755); err != nil {
			return err
		}
		mode := os.FileMode(0o644)
		if f.Mode()&0o111 != 0 {
			mode = 0o755
		}
		if err := extract(f, dest, mode); err != nil {
			return err
		}
	}
	if err := os.WriteFile(filepath.Join(tmp, ".complete"), nil, 0o644); err != nil {
		return err
	}

	os.RemoveAll(root)
	return os.Rename(tmp, root)
}

func extract(f *zip.File, dest string, mode os.FileMode) error {
	src, err := f.Open()
	if err != nil {
		return err
	}
	defer src.Close()
	out, err := os.OpenFile(dest, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, mode)
	if err != nil {
		return err
	}
	if _, err := io.Copy(out, src); err != nil {
		out.Close()
		return err
	}
	return out.Close()
}
