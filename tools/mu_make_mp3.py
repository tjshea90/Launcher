#!/usr/bin/env python3
"""mu_make_mp3.py <out.mp3> <title> <artist> <album> <RRGGBB>

Writes a short SILENT mp3 (about 15 s) whose ID3v2.3 tag carries a FLAT-COLOUR png cover. Used by the emulator section `mu` (TASKS BV1e): the music app (Eleven)
must never show album art, so the test song has a cover of one loud colour (magenta) and every screenshot of the app is searched for that colour.
No binary goes into the repo; the song is made when the section runs. Pure standard library (no Pillow on the CI runner).
"""
import struct
import sys
import zlib


def png_flat(rgb, w=300, h=300):
    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)
    raw = (b"\x00" + bytes(rgb) * w) * h
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def syncsafe(n):
    return bytes([(n >> 21) & 0x7F, (n >> 14) & 0x7F, (n >> 7) & 0x7F, n & 0x7F])


def frame(fid, body):
    return fid + struct.pack(">I", len(body)) + b"\x00\x00" + body   # ID3v2.3: plain big-endian size, no flags


def text_frame(fid, text):
    return frame(fid, b"\x00" + text.encode("latin-1"))   # encoding 0 = ISO-8859-1 (v2.3 has no UTF-8)


def build(title, artist, album, rgb, frames=600):
    apic = frame(b"APIC", b"\x00" + b"image/png\x00" + b"\x03" + b"\x00" + png_flat(rgb))   # type 3 = front cover, empty description
    body = (text_frame(b"TIT2", title) + text_frame(b"TPE1", artist) + text_frame(b"TALB", album)
            + text_frame(b"TPE2", artist) + text_frame(b"TRCK", "1") + text_frame(b"TYER", "2020") + apic)
    tag = b"ID3\x03\x00\x00" + syncsafe(len(body)) + body
    # MPEG-1 Layer III, 128 kbit/s, 44.1 kHz, stereo, no padding: 417 bytes per frame; the zeroed side info and main data decode to silence
    silent = b"\xff\xfb\x90\x00" + b"\x00" * 413
    return tag + silent * frames


def main(argv):
    if len(argv) != 6 or len(argv[5]) != 6:
        sys.stderr.write(__doc__)
        return 2
    out, title, artist, album, hexcol = argv[1:6]
    rgb = tuple(int(hexcol[i:i + 2], 16) for i in (0, 2, 4))
    with open(out, "wb") as f:
        f.write(build(title, artist, album, rgb))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
