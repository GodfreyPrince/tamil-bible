# -*- coding: utf-8 -*-
"""Continuous pipeline: cached category HTML -> thumbs (parallel) -> labeled contact sheets.
Runs until crawler finishes + everything downloaded + sheets built. Sheets listed in sheets/index.txt
with lines: <sheet_path>\t<id1> <url-escaped-file-title>|<id2> ... (ids = review labels)."""
import os, re, sys, json, time, subprocess, urllib.parse, urllib.request
import concurrent.futures as cf
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.expanduser(r"~\.zcode\workspace\default\tamil_build\media")
CACHE = os.path.join(OUT, "html")
THUMBS = os.path.join(OUT, "thumbs")
SHEETS = os.path.join(OUT, "sheets")
STATE = os.path.join(OUT, "state.json")
os.makedirs(THUMBS, exist_ok=True); os.makedirs(SHEETS, exist_ok=True)

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) TamilBibleArtCurator/1.0"

PREFIX = [("dore", "DOR"), ("tissot", "TIS"), ("holman", "HOL"), ("maps", "MAP"),
          ("crucifixion", "CRU"), ("last supper", "LST"), ("adoration", "MAG"),
          ("sermon", "SRM"), ("abraham", "ABM"), ("moses", "MOS"), ("elijah", "ELI"),
          ("daniel", "DAN"), ("jonah", "JON"), ("ascension", "ASC")]

def prefix_for(cat):
    low = cat.lower()
    for k, p in PREFIX:
        if k in low:
            return p
    return "GEN"

def slug(t):
    return re.sub(r"[^A-Za-z0-9]+", "_", t[:110]).strip("_")

def load_state():
    try: return json.load(open(STATE, encoding="utf-8"))
    except Exception: return {"done": {}, "sheets": {}}

def save_state(s):
    json.dump(s, open(STATE, "w", encoding="utf-8"))

def scan_cached():
    """category -> file titles, from every cached category page HTML."""
    cats = {}
    for fn in os.listdir(CACHE):
        if not fn.endswith(".html") or fn.startswith("page_"):
            continue
        h = open(os.path.join(CACHE, fn), encoding="utf-8", errors="replace").read()
        m2 = re.search(r'id="mw-pages"(.*)$', h, re.S)
        if not m2:
            continue
        body = m2.group(1).split('<div id="catlinks"')[0]
        files = set(urllib.parse.unquote(x) for x in re.findall(r'href="/wiki/File:([^"]+)"', body))
        files = {f for f in files if not f.startswith("Category") and not f.startswith("Template")}
        cat = urllib.parse.unquote(fn[4:-5]).replace("_", " ")
        if files:
            cats.setdefault(cat, set()).update(files)
    return cats

def download(items, workers=14):
    """items: list of (local_path, url). Returns count downloaded."""
    def grab(it):
        p, u = it
        if os.path.exists(p) and os.path.getsize(p) > 8000:
            return True
        try:
            req = urllib.request.Request(u, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=30) as r, open(p, "wb") as f:
                f.write(r.read())
            return os.path.getsize(p) > 8000
        except Exception:
            return False
    with cf.ThreadPoolExecutor(workers) as ex:
        return sum(1 for ok in ex.map(grab, items) if ok)

def thumb_url(title, width=420):
    return ("https://commons.wikimedia.org/wiki/Special:FilePath/"
            + urllib.parse.quote(title) + "?width=" + str(width))

def build_sheet(ids_tiles, path, cols=8, tile=200):
    rows = (len(ids_tiles) + cols - 1) // cols
    W, H = cols * (tile + 6) + 6, rows * (tile + 24) + 6
    img = Image.new("RGB", (W, H), (18, 18, 18))
    d = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("arial.ttf", 15)
    except Exception:
        font = ImageFont.load_default()
    for i, (mid, p) in enumerate(ids_tiles):
        r, c = divmod(i, cols)
        x, y = 6 + c * (tile + 6), 6 + r * (tile + 24)
        try:
            t = Image.open(p).convert("RGB")
            t.thumbnail((tile, tile))
            img.paste(t, (x + (tile - t.width) // 2, y + (tile - t.height) // 2))
        except Exception:
            d.rectangle([x, y, x + tile, y + tile], fill=(80, 0, 0))
        d.text((x + 2, y + tile + 2), mid, fill=(255, 220, 120), font=font)
    img.save(path, quality=82)
    return path

def main():
    st = load_state()
    seen_files = set()
    counters = {}
    while True:
        cats = scan_cached()
        todo = []
        for cat, files in cats.items():
            pre = prefix_for(cat)
            for f in files:
                if f in seen_files:
                    continue
                seen_files.add(f)
                mid = counters.get(pre, 0) + 1
                counters[pre] = mid
                lid = f"{pre}{mid:04d}"
                local = os.path.join(THUMBS, lid + "_" + slug(f) + ".jpg")
                st["done"][lid] = {"t": f, "cat": cat, "p": local}
        newdl = []
        for lid, meta in st["done"].items():
            p = meta["p"]
            if not (os.path.exists(p) and os.path.getsize(p) > 8000):
                newdl.append((p, thumb_url(meta["t"])))
        if newdl:
            download(newdl)
        # group downloaded tiles by prefix -> sheets of 48
        pending = {}
        for lid, meta in st["done"].items():
            if lid in st["sheets"]:
                continue
            p = meta["p"]
            if os.path.exists(p) and os.path.getsize(p) > 8000:
                pending.setdefault(lid[:3], []).append((lid, p))
        made = 0
        for pre, tiles in sorted(pending.items()):
            while len(tiles) >= 48:
                batch = tiles[:48]; tiles = tiles[48:]
                made += 1
                sp = os.path.join(SHEETS, f"sheet_{pre}_{made:03d}.jpg")
                build_sheet(batch, sp)
                st["sheets"].update({b[0]: os.path.basename(sp) for b in batch})
                with open(os.path.join(SHEETS, "index.txt"), "a", encoding="utf-8") as fh:
                    fh.write(sp + "\n")
                print("SHEET", sp, flush=True)
        save_state(st)
        cat_done = os.path.exists(os.path.join(OUT, "raw_catalog.json"))
        if not newdl and not pending and cat_done:
            print("PIPELINE DRAINED", flush=True)
            return
        time.sleep(6)

if __name__ == "__main__":
    main()
