"""把最终图标按多个尺寸渲染成一张预览图（纯 Python，不依赖 Pillow）。
用法：python scripts/icon_preview.py [输出路径]
默认输出 artifacts/screenshots/00-icon.png
"""

import zlib, struct, math, sys

SS = 4
UNIT = 108.0
BG_A = (0xF4, 0x82, 0x5A)
BG_B = (0xCC, 0x45, 0x26)
CREAM = (0xFD, 0xF6, 0xEE)
PAPER = (247, 244, 239)

# —— 最终几何（与 res/drawable/ic_launcher_foreground.xml 一致）——
SUN = (54.0, 47.0, 14.5)
GLOW = (54.0, 47.0, 24.5, 0.09)
RAYS = [(-90, 18.0, 24.0, 5.2), (-138, 18.0, 24.0, 5.2), (-42, 18.0, 24.0, 5.2)]
BAR = (30.0, 68.8, 48.0, 6.6, 3.3)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def dist_seg(px, py, x1, y1, x2, y2):
    dx, dy = x2 - x1, y2 - y1
    L2 = dx * dx + dy * dy
    if L2 == 0:
        return math.hypot(px - x1, py - y1)
    t = max(0.0, min(1.0, ((px - x1) * dx + (py - y1) * dy) / L2))
    return math.hypot(px - (x1 + t * dx), py - (y1 + t * dy))


def render(size):
    out = []
    cx, cy, cr = SUN
    for yy in range(size):
        for xx in range(size):
            acc = [0, 0, 0]
            for sy in range(SS):
                for sx in range(SS):
                    u = (xx + (sx + 0.5) / SS) / size * UNIT
                    v = (yy + (sy + 0.5) / SS) / size * UNIT

                    t = (u + v) / (2 * UNIT)
                    col = lerp(BG_A, BG_B, max(0.0, min(1.0, t)))

                    gx, gy, gr, ga = GLOW
                    if (u - gx) ** 2 + (v - gy) ** 2 <= gr * gr:
                        col = lerp(col, CREAM, ga)

                    for ang, r1, r2, w in RAYS:
                        a = math.radians(ang)
                        x1 = cx + r1 * math.cos(a)
                        y1 = cy + r1 * math.sin(a)
                        x2 = cx + r2 * math.cos(a)
                        y2 = cy + r2 * math.sin(a)
                        if dist_seg(u, v, x1, y1, x2, y2) <= w / 2:  # 圆头端帽由线段距离自然产生
                            col = CREAM

                    if (u - cx) ** 2 + (v - cy) ** 2 <= cr * cr:
                        col = CREAM

                    bx, by, bw, bh, brad = BAR
                    if dist_seg(u, v, bx + brad, by + bh / 2, bx + bw - brad, by + bh / 2) <= bh / 2:
                        col = CREAM

                    acc[0] += col[0]
                    acc[1] += col[1]
                    acc[2] += col[2]
            n = SS * SS
            out.append((acc[0] // n, acc[1] // n, acc[2] // n))
    return out


def circle_mask(pixels, size):
    out = []
    c = size / 2.0
    r = size * (36.0 / 108.0)
    for y in range(size):
        for x in range(size):
            if ((x + 0.5 - c) ** 2 + (y + 0.5 - c) ** 2) ** 0.5 <= r:
                out.append(pixels[y * size + x])
            else:
                out.append(PAPER)
    return out


def write_png(path, w, h, rows):
    raw = b"".join(b"\x00" + bytes(row) for row in rows)
    comp = zlib.compress(raw, 9)

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", comp)
    png += chunk(b"IEND", b"")
    open(path, "wb").write(png)


def paste(dst, dw, src, sw, ox, oy):
    for y in range(sw):
        for x in range(sw):
            i = ((oy + y) * dw + (ox + x)) * 3
            p = src[y * sw + x]
            dst[i], dst[i + 1], dst[i + 2] = p


SIZES = [240, 120, 72, 48, 32]
GAP = 26
PAD = 30
W = sum(SIZES) + GAP * (len(SIZES) - 1) + PAD * 2
H = max(SIZES) + PAD * 2
canvas = [0] * (W * H * 3)
for i in range(W * H):
    canvas[i * 3], canvas[i * 3 + 1], canvas[i * 3 + 2] = PAPER

x = PAD
for s in SIZES:
    img = circle_mask(render(s), s)
    paste(canvas, W, img, s, x, (H - s) // 2)
    x += s + GAP

rows = []
for y in range(H):
    row = []
    for x in range(W):
        i = (y * W + x) * 3
        row.extend(canvas[i:i + 3])
    rows.append(row)

out_path = sys.argv[1] if len(sys.argv) > 1 else "artifacts/screenshots/00-icon.png"
write_png(out_path, W, H, rows)
print("ok", W, H, out_path)
