"""Builds the extra-armor side panel texture from the vanilla inventory texture.

The panel is drawn over the inventory's top-left corner and grows to the left, one 18 px column per
extra armor set, so the GUI looks like it grew extra slot columns right next to the vanilla armor column.
The texture holds three pieces the game puts together:
  u 0..6   left edge (rounded corners, like the inventory's own left edge)
  u 7..24  a middle column: 4 slot frames, plain top and bottom edge
  u 25..42 the column that joins the inventory: covers its 7 px left border and has the inner corner

usage: python generate_panel.py <vanilla inventory.png> <vanilla creative tab_inventory.png> <out dir>
"""
import sys
from PIL import Image

B = 18          # one column
H = 86          # 4 slot frames (rows 7..78), 4 px gap, 3 px bottom edge

K, Wh, G, D = (0, 0, 0, 255), (255, 255, 255, 255), (198, 198, 198, 255), (85, 85, 85, 255)


def column(px, join):
    """One 18 px column, taken from the vanilla armor column (inventory x 7..24)."""
    out = Image.new('RGBA', (B, H), (0, 0, 0, 0))
    o = out.load()
    for y in range(H):
        for x in range(B):
            if y <= 6:
                o[x, y] = px[30, y]            # plain top edge
            elif y <= 78:
                o[x, y] = px[7 + x, y]          # slot frames
            elif y <= 82:
                o[x, y] = G
            elif join and x >= B - 7:
                o[x, y] = G                     # inventory interior below the bump
            else:
                o[x, y] = px[30, y + 80]        # plain bottom edge (inventory rows 163..165)
    if join:
        # inner corner where the bump's bottom edge meets the inventory's left edge (column x = 11
        # is the inventory's x 0: K at the edge, highlight right of it)
        j = B - 7
        o[j, 83], o[j + 1, 83], o[j + 2, 83] = D, D, G
        o[j, 84], o[j + 1, 84], o[j + 2, 84] = D, G, G
        o[j, 85], o[j + 1, 85], o[j + 2, 85] = K, Wh, Wh
    return out


def edge(px):
    """The left edge: the inventory's own left border rows, with its rounded corners."""
    out = Image.new('RGBA', (7, H), (0, 0, 0, 0))
    o = out.load()
    for y in range(H):
        for x in range(7):
            o[x, y] = px[x, y + 80] if y >= 83 else px[x, y]
    return out


def assemble(pieces, sets):
    e, mid, join = pieces
    img = Image.new('RGBA', (7 + B * sets, H), (0, 0, 0, 0))
    img.alpha_composite(e, (0, 0))
    for c in range(sets):
        img.alpha_composite(join if c == sets - 1 else mid, (7 + B * c, 0))
    return img


def preview(base, panel, path, scale=4):
    full = Image.new('RGBA', (base.width + panel.width - 7, base.height), (40, 40, 40, 255))
    full.alpha_composite(base, (panel.width - 7, 0))
    full.alpha_composite(panel, (0, 0))
    full.resize((full.width * scale, full.height * scale), Image.NEAREST).save(path)


if __name__ == '__main__':
    inv = Image.open(sys.argv[1]).convert('RGBA')
    creative = Image.open(sys.argv[2]).convert('RGBA')
    out_dir = sys.argv[3]
    px = inv.load()
    pieces = (edge(px), column(px, False), column(px, True))
    sheet = Image.new('RGBA', (64, 128), (0, 0, 0, 0))
    sheet.alpha_composite(pieces[0], (0, 0))
    sheet.alpha_composite(pieces[1], (7, 0))
    sheet.alpha_composite(pieces[2], (25, 0))
    sheet.save(out_dir + '/extra_armor_panel.png')
    preview(inv.crop((0, 0, 176, 166)), assemble(pieces, 1), out_dir + '/preview_survival.png')
    preview(inv.crop((0, 0, 176, 166)), assemble(pieces, 3), out_dir + '/preview_survival_3.png')
    preview(creative.crop((0, 0, 195, 136)), assemble(pieces, 1), out_dir + '/preview_creative.png')
