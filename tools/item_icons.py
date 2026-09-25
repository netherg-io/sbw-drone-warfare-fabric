#!/usr/bin/env python3
"""Generates the mod's own item icons as plain PNGs, stdlib only (zlib + struct).

Each icon is a 16x16 grid of single-character pixel codes mapped through a
palette, so the shipped PNG bytes are fully reproducible from this file.
Run: python3 tools/item_icons.py
"""
import struct
import sys
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEXTURES = ROOT / "src/fabric/resources/assets/sbwdroneconfig/textures/item"
MOD_ICON = ROOT / "src/fabric/resources/assets/sbwdroneconfig/icon.png"

TRANSPARENT = (0, 0, 0, 0)

PALETTE = {
    "k": (26, 26, 26, 255),    # frame / outline
    "K": (51, 51, 51, 255),    # body panel
    "g": (117, 117, 117, 255),  # motor hub
    "s": (176, 176, 176, 255),  # prop blur
    "w": (240, 240, 240, 255),  # fibre white
    "r": (176, 32, 32, 255),   # warhead red
    "o": (255, 140, 0, 255),   # warhead highlight
    "c": (35, 52, 84, 255),    # camera lens
    "b": (33, 58, 33, 255),    # jammer body
    "B": (48, 82, 48, 255),    # jammer body edge highlight
    "e": (18, 18, 18, 255),    # jammer screen
    "L": (90, 220, 90, 255),   # jammer LED
}

# 5" FPV quad, seen from above: X frame, 4 props, red/orange warhead nub at the nose (top).
CUBED_FPV_DRONE = [
    "..ggg......ggg..",
    ".ggggg....ggggg.",
    "ggkksgg..ggkksgg",
    "ggkkkgg..gkkksgg",
    "ggskkkgorkkkssgg",
    ".gggkkkrrkkgggg.",
    "..gggkKccKkggg..",
    ".....kKkkK......",
    "....kkKkkK......",
    "..ggkkKKKKkggg..",
    ".ggkkkk..kkkggg.",
    "ggkkkgg..gkkksgg",
    "ggkksgg..ggkksgg",
    "ggsssgg..ggsssgg",
    ".ggggg....ggggg.",
    "..ggg......ggg..",
]

# Same quad, plus a white fibre spool on its back and a trailing line off the bottom edge.
FIBRE_FPV_DRONE = [
    "..ggg......ggg..",
    ".ggggg....ggggg.",
    "ggkksgg..ggkksgg",
    "ggkkkgg..gkkksgg",
    "ggskkkgorkkkssgg",
    ".gggkkkrrkkgggg.",
    "..gggkKccKkggg..",
    ".....kKkkK......",
    "....kkKkkK......",
    "..ggkkKwwKkggg..",
    ".ggkkkwggwkkggg.",
    "ggkkkggwwgkkksgg",
    "ggkksggw.ggkksgg",
    "ggsssggw.ggsssgg",
    ".ggggg.w..ggggg.",
    "..ggg..w...ggg..",
]

# Handheld jammer: boxy dark-green body, 4 antennas, LED + screen.
SIGNAL_JAMMER = [
    ".......k.k......",
    ".....k.k.k.k....",
    ".....k.k.k.k....",
    ".....k.k.k.k....",
    ".....k.k.k.k....",
    ".....k.k.k.k....",
    "....BkBkBkBk....",
    "....Bbbbbbbb....",
    "....BbLLeebb....",
    "....Bbeeeebb....",
    "....Bbbbbbbb....",
    "....Bbbbbbbb....",
    "....Bbkkkkbb....",
    "....Bbkkkkbb....",
    "....Bbkkkkbb....",
    "................",
]

ICONS = {
    "cubed_fpv_drone.png": CUBED_FPV_DRONE,
    "fibre_fpv_drone.png": FIBRE_FPV_DRONE,
    "signal_jammer.png": SIGNAL_JAMMER,
}


def write_png(path: Path, pixels: list[str]) -> None:
    size = len(pixels)
    assert all(len(row) == size for row in pixels), path

    def rgba(ch: str) -> tuple[int, int, int, int]:
        return PALETTE.get(ch, TRANSPARENT)

    raw = bytearray()
    for row in pixels:
        raw.append(0)  # scanline filter: None
        for ch in row:
            raw.extend(rgba(ch))

    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data))

    ihdr = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)  # 8-bit RGBA, no interlace
    idat = zlib.compress(bytes(raw), 9)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def upscale(pixels: list[str], factor: int) -> list[str]:
    return ["".join(ch * factor for ch in row) for row in pixels for _ in range(factor)]


def main() -> None:
    for name, pixels in ICONS.items():
        write_png(TEXTURES / name, pixels)
    # Mod icon: the FPV quad art, nearest-neighbour upscaled 16x to 128x128.
    write_png(MOD_ICON, upscale(CUBED_FPV_DRONE, 8))
    for p in [*(TEXTURES / n for n in ICONS), MOD_ICON]:
        print("wrote", p.relative_to(ROOT))


if __name__ == "__main__":
    sys.exit(main())
