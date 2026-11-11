#!/usr/bin/env python3
"""Draws the launcher icon of the music app ("Music": the blue speaker) and writes it as an ADAPTIVE icon (white background + the speaker as the foreground).

WHY (TASKS BW1, Tj 2026-11-11): the music app must show the speaker icon of the "ATG music player" he already has. Three web searches found no copy of
that icon online, so it is REDRAWN from Tj's own screenshots (his drawer, where the icon is 39 px wide on Launcher's white plate, and the Files app).
Nothing of the screenshots is stored in the repo: only the NUMBERS measured from them (the table below) and the result of this script.
WHY ADAPTIVE (CI run #554, the real drawer on the emulator): Launcher3 wraps a LEGACY icon on its white plate and shrinks it, so a round legacy icon can never be
bigger than about 64 % of the plate (ours came out at 62 %); his speaker is 71.5 % of the plate (39.34 of 55 px). An adaptive icon is not shrunk, so the size is exact:
the 108 dp foreground layer shows its middle 72 dp (= the plate), the speaker is R_OUT / 55 of that window wide (FOREGROUND_RADIUS) and the background layer is pure white.

HOW the numbers were measured (research/MUSIC_ICON_2026-11-11.md): the speaker is a round driver, 39.5 px wide in the 600x1024 drawer. Mean colour per
radius (r, in drawer pixels, centre = the fitted centre of the thin black ring) separately for the top and the bottom of the picture:
    cone  = mean(r) + dev(r) * (dy / r)      (dy > 0 = below the centre: the cone is lit from below: top dark, bottom light, sides in between)
    bezel = mean(r) + dev(r) * (-dy / r)     (the grey ring is lighter on top)
    dust cap = 42 + 60 * (up / 4.75)         (a glossy dome: lighter on top, black at the bottom), 4.75 px radius, a dark edge ring around it
    thin black ring at r = 16.1 px (about 1.1 px wide), outer edge of the bezel at r = 19.7 px.
Run:  python3 -I tools/make_music_icon.py            -> writes eleven/src/main/res/mipmap-*/ic_launcher_foreground.png (108 dp layer: 108 / 162 / 216 / 324 / 432 px),
          mipmap-anydpi/ic_launcher.xml and values/ic_launcher_background.xml (white)
      python3 -I tools/make_music_icon.py --compare <drawer screenshot, 600x1024> <centre x> <centre y> [<out.png>]
          -> draws the speaker at the drawer's own size on a white plate and prints the difference to the screenshot (the fit check of BW1; the screenshot stays outside the repo)
      python3 -I tools/make_music_icon.py --drawer-sim <out.png> [<plate px, default 52>]
          -> a synthetic drawer: the written foreground cut to the plate window on a white plate, on grey (self-test input of tools/music_icon_check.py, tools/test_music_icon_check.sh)
"""
import math
import os
import sys

import numpy as np

R_OUT = 19.67  # outer radius of the artwork in drawer pixels (the unit of every table below)

# MEASURED (drawer pixels, centre of the speaker = the origin, f = dy / r: +1 straight below the centre, -1 straight above, 0 at the sides).
# Fit check (the --compare mode): mean absolute difference to the screenshot 4.8 levels per channel over the 1400 pixels of the speaker.
# Per radius the colour of every pixel is a straight line in f (least squares over all angles, 0.5 px bins; the residual is 2 to 5 levels).
# THE CONE: colour = M(r) + D(r) * f with the SAME D for red, green and blue: the speaker is lit from below by a white glow (lighter at the bottom, darker at the top).
CONE_R = [5.8, 6.2, 6.8, 7.2, 7.8, 8.2, 8.8, 9.2, 9.8, 10.2, 10.8, 11.2, 11.8, 12.2, 12.8, 13.2, 13.8, 14.2, 14.8, 15.4]
CONE_M = [  # mean colour (R, G, B)
    (98, 118, 163), (103, 124, 174), (99, 120, 168), (95, 117, 167), (92, 115, 167), (91, 115, 170), (89, 113, 171), (87, 114, 174),
    (85, 112, 175), (83, 111, 176), (80, 108, 174), (77, 106, 172), (73, 101, 166), (69, 97, 160), (63, 89, 149), (59, 84, 140),
    (52, 73, 124), (48, 69, 116), (41, 60, 102), (24, 34, 58)]
CONE_D = [63, 69, 69, 67, 65, 63, 60, 56, 52, 49, 45, 42, 38, 36, 32, 31, 26, 26, 22, 14]   # glow amplitude, the same in all three channels
# THE BEZEL (the grey ring): colour = M(r) + D(r) * f, grey, lighter at the top (D < 0)
BEZEL_R = [16.7, 17.2, 17.8, 18.2, 18.8, 19.3, 19.7]
BEZEL_M = [70, 90, 93, 97, 105, 125, 140]
BEZEL_D = [-8, -18, -33, -34, -39, -39, -30]
BLACK_R0, BLACK_R1 = 15.54, 16.70      # the thin black ring
BLACK = (1, 1, 3)
CAP_R = 4.03                          # the dust cap: a glossy dome, 38.3 + 11.8 * (height above the centre in px), black at the bottom
CAP_BASE, CAP_GAIN = 38.3, 11.8
CAP_EDGE_R = 5.3                      # the dark edge ring around the dome ends here
CAP_EDGE = (19, 19, 23)
def shade(x, y):
    """RGB (floats 0..255) and coverage (0 or 1) of the speaker at points (x, y) in drawer pixels, centre = (0, 0), y down. No anti-aliasing here (the caller supersamples)."""
    r = np.hypot(x, y)
    f = y / np.maximum(r, 1e-6)                    # +1 below the centre, -1 above
    alpha = (r <= R_OUT).astype(float)
    rgb = np.zeros(x.shape + (3,), float)
    # bezel (grey ring)
    g = np.interp(r, BEZEL_R, BEZEL_M) + np.interp(r, BEZEL_R, BEZEL_D) * f
    rgb = np.where((r >= BLACK_R1)[..., None], np.stack([g, g, g], axis=-1), rgb)
    # cone (blue, lit from below)
    m = np.stack([np.interp(r, CONE_R, [c[k] for c in CONE_M]) for k in range(3)], axis=-1)
    cone = np.clip(m + (np.interp(r, CONE_R, CONE_D) * f)[..., None], 0, 255)
    rgb = np.where(((r >= CAP_EDGE_R) & (r < BLACK_R0))[..., None], cone, rgb)
    # the thin black ring
    rgb = np.where(((r >= BLACK_R0) & (r < BLACK_R1))[..., None], np.array(BLACK, float), rgb)
    # the dust cap
    v = np.clip(CAP_BASE - CAP_GAIN * y, 8, 92)
    rgb = np.where((r < CAP_R)[..., None], np.stack([v, v, v], axis=-1), rgb)
    rgb = np.where(((r >= CAP_R) & (r < CAP_EDGE_R))[..., None], np.array(CAP_EDGE, float), rgb)
    return rgb, alpha


def render(size, radius_px, cx=None, cy=None, ssaa=8, background=None):
    """size x size picture; the speaker has outer radius radius_px (output pixels) and is centred at (cx, cy) (default: the middle).
    background None = transparent PNG (RGBA), else an (R, G, B) tuple the speaker is composited on (the comparison with a screenshot)."""
    cx = size / 2 if cx is None else cx
    cy = size / 2 if cy is None else cy
    scale = R_OUT / radius_px                       # drawer pixels per output pixel
    n = size * ssaa
    ys, xs = np.mgrid[0:n, 0:n]
    px = ((xs + 0.5) / ssaa - cx) * scale
    py = ((ys + 0.5) / ssaa - cy) * scale
    rgb, a = shade(px, py)
    # box-filter down to the output size (premultiplied so the edge colour is not dragged towards black)
    pre = rgb * a[..., None]
    pre = pre.reshape(size, ssaa, size, ssaa, 3).mean(axis=(1, 3))
    aa = a.reshape(size, ssaa, size, ssaa).mean(axis=(1, 3))
    if background is not None:
        bg = np.array(background, float)
        out = pre + bg * (1 - aa[..., None])
        return np.clip(np.rint(out), 0, 255).astype(np.uint8)
    col = np.where(aa[..., None] > 0, pre / np.maximum(aa[..., None], 1e-6), 0)
    rgba = np.concatenate([np.clip(np.rint(col), 0, 255), np.rint(aa[..., None] * 255)], axis=-1)
    return rgba.astype(np.uint8)


def write_icons(res_dir):
    from PIL import Image
    sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for dens, px in sizes.items():
        d = os.path.join(res_dir, "mipmap-" + dens)
        os.makedirs(d, exist_ok=True)
        # the artwork fills 92 % of the canvas (the usual legacy launcher icon proportion); Launcher3 normalises the visible size itself
        img = render(px, px * 0.46, ssaa=8 if px >= 96 else 12)
        Image.fromarray(img, "RGBA").save(os.path.join(d, "ic_launcher.png"), optimize=True)
        print("wrote", os.path.join(d, "ic_launcher.png"), px)


def compare(shot, cx, cy, out=None):
    """Fit check: draw the speaker as the drawer shows it (white plate around it) and print the difference to the screenshot region."""
    from PIL import Image
    ref = np.asarray(Image.open(shot).convert("RGB")).astype(float)
    x0, y0 = int(cx) - 26, int(cy) - 26
    size = 53
    tile = ref[y0:y0 + size, x0:x0 + size]
    mine = render(size, R_OUT, cx - x0, cy - y0, ssaa=8, background=(255, 255, 255)).astype(float)
    yy, xx = np.mgrid[0:size, 0:size]
    inside = np.hypot(xx + 0.5 - (cx - x0), yy + 0.5 - (cy - y0)) <= R_OUT + 1.5
    d = np.abs(mine - tile)
    print("pixels compared: %d" % inside.sum())
    print("mean abs difference per channel (0..255): R %.1f  G %.1f  B %.1f" % tuple(d[inside].mean(axis=0)))
    print("95th percentile of the difference: %.0f" % np.percentile(d[inside], 95))
    for name, (lo, hi) in {"cap r<5": (0, 5), "cone 5-15.6": (5, 15.6), "black ring": (15.6, 16.65), "bezel 16.65-19.7": (16.65, 19.7)}.items():
        rr = np.hypot(xx + 0.5 - (cx - x0), yy + 0.5 - (cy - y0))
        m = (rr >= lo) & (rr < hi)
        print("  %-18s mean abs difference %.1f (n=%d)" % (name, d[m].mean(), m.sum()))
    if out:
        both = np.concatenate([tile, mine, np.clip(d * 3, 0, 255)], axis=1).astype(np.uint8)
        Image.fromarray(both).resize((both.shape[1] * 6, both.shape[0] * 6), Image.NEAREST).save(out)
        print("side by side (screenshot | drawn | difference x3):", out)


if __name__ == "__main__":
    if len(sys.argv) >= 5 and sys.argv[1] == "--compare":
        compare(sys.argv[2], float(sys.argv[3]), float(sys.argv[4]), sys.argv[5] if len(sys.argv) > 5 else None)
    elif len(sys.argv) == 1:
        here = os.path.dirname(os.path.abspath(__file__))
        write_icons(os.path.join(here, "..", "eleven", "src", "main", "res"))
    else:
        print(__doc__)
        sys.exit(2)
