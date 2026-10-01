"""Mark a needed symbol version as weak in an ELF64 little-endian shared object.

LWJGL 2's liblwjgl64.so (2015) requires libjawt.so to provide version
SUNWprivate_1.1. Modern JDK builds define versions for libjawt but not that one,
so glibc refuses to load the library. A weak version need is only a warning.

usage: weaken_verneed.py FILE VERSION
"""
import struct
import sys

VER_FLG_WEAK = 0x2
SHT_GNU_VERNEED = 0x6FFFFFFE

path, wanted = sys.argv[1], sys.argv[2].encode()
data = bytearray(open(path, "rb").read())
assert data[:4] == b"\x7fELF" and data[4] == 2 and data[5] == 1, "need ELF64 LE"
shoff, = struct.unpack_from("<Q", data, 0x28)
shentsize, shnum = struct.unpack_from("<HH", data, 0x3A)
sections = [struct.unpack_from("<IIQQQQIIQQ", data, shoff + i * shentsize) for i in range(shnum)]
patched = 0
for sh in sections:
    if sh[1] != SHT_GNU_VERNEED:
        continue
    strtab = sections[sh[6]][4]  # sh_link -> .dynstr, its file offset
    off = sh[4]
    for _ in range(sh[7]):  # sh_info = number of Verneed entries
        _, cnt, _, aux, nxt = struct.unpack_from("<HHIII", data, off)
        a = off + aux
        for _ in range(cnt):
            _, flags, _, name, anext = struct.unpack_from("<IHHII", data, a)
            end = data.index(b"\0", strtab + name)
            if bytes(data[strtab + name:end]) == wanted:
                struct.pack_into("<H", data, a + 4, flags | VER_FLG_WEAK)
                patched += 1
            a += anext
        off += nxt
if not patched:
    sys.exit("version %s not found in %s" % (wanted.decode(), path))
open(path, "wb").write(data)
