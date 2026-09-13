# -*- coding: utf-8 -*-
"""Extract Biblical maps from cached Commons category HTML, download thumbs, emit maps list."""
import os, re, json, hashlib, urllib.parse, urllib.request, time
import concurrent.futures as cf

BASE = os.path.expanduser(r"~\.zcode\workspace\default\tamil_build\media")
CACHE = os.path.join(BASE, "html")
MAPS_DL = os.path.join(BASE, "maps")
os.makedirs(MAPS_DL, exist_ok=True)
UA = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) TamilBibleArtCurator/1.0"}

# extra targeted map categories (fetch now)
EXTRA_CATS = [
    "Maps of the Twelve Tribes of Israel", "Maps of Paul's missionary journeys",
    "Maps of the Exodus", "Bible atlas", "Historical maps of the Holy Land",
    "Ancient Near East maps", "Maps of Jerusalem in the Bible",
]

def sf_thumb(title, w=900):
    """upload.wikimedia thumb URL via MD5 scheme."""
    name = urllib.parse.unquote(title)
    h = hashlib.md5(name.encode("utf-8")).hexdigest()
    stem, ext = os.path.splitext(name)
    el = ext.lower()
    tb = f"https://upload.wikimedia.org/wikipedia/commons/thumb/{h[0]}/{h[:2]}/"
    if el in (".jpg", ".jpeg", ".png"):
        return tb + urllib.parse.quote(name) + f"/{w}px-" + urllib.parse.quote(name)
    return tb + urllib.parse.quote(name) + f"/{w}px-" + urllib.parse.quote(stem) + ".jpg"

def parse_cached(cat):
    p = os.path.join(CACHE, "cat_" + re.sub(r"[^A-Za-z0-9_]", "_", cat) + ".html")
    if not os.path.exists(p):
        return []
    h = open(p, encoding="utf-8", errors="replace").read()
    m = re.search(r'id="mw-pages"(.*)$', h, re.S)
    if not m:
        return []
    body = m.group(1).split('<div id="catlinks"')[0]
    return sorted(set(urllib.parse.unquote(x) for x in re.findall(r'href="/wiki/File:([^"]+)"', body)))

def fetch_cat(cat):
    url = "https://commons.wikimedia.org/wiki/Category:" + urllib.parse.quote(cat.replace(" ", "_"))
    fn = "cat_" + re.sub(r"[^A-Za-z0-9_]", "_", cat) + ".html"
    p = os.path.join(CACHE, fn)
    if os.path.exists(p):
        return
    try:
        req = urllib.request.Request(url, headers=UA)
        with urllib.request.urlopen(req, timeout=40) as r:
            h = r.read(3_000_000).decode("utf-8", "replace")
        if len(h) > 1000:
            open(p, "w", encoding="utf-8").write(h)
    except Exception as e:
        print("fetch fail", cat, e)

def grab(it):
    p, urls = it
    if os.path.exists(p) and os.path.getsize(p) > 5000:
        return True
    for u in urls:
        try:
            req = urllib.request.Request(u, headers=UA)
            with urllib.request.urlopen(req, timeout=60) as r:
                data = r.read(40_000_000)
            if len(data) > 5000:
                open(p, "wb").write(data)
                return True
        except Exception:
            time.sleep(1)
    return False

def main():
    titles = set(parse_cached("Biblical maps"))
    print("cached Biblical maps:", len(titles))
    for c in EXTRA_CATS:
        fetch_cat(c)
        got = parse_cached(c)
        titles.update(got)
        print(f"{c}: +{len(got)}")
    titles = {t for t in titles if re.search(r"\.(jpe?g|png|tif|tiff)$", t, re.I)}
    print("total map files:", len(titles))
    queue = []
    meta = {}
    for t in sorted(titles):
        lid = "MP" + hashlib.md5(t.encode()).hexdigest()[:6].upper()
        p = os.path.join(MAPS_DL, lid + ".jpg")
        meta[lid] = {"t": t, "p": p}
        urls = [sf_thumb(t)]
        queue.append((p, urls))
    ok = 0
    with cf.ThreadPoolExecutor(10) as ex:
        for r in ex.map(grab, queue):
            ok += 1 if r else 0
    good = {k: v for k, v in meta.items() if os.path.exists(v["p"]) and os.path.getsize(v["p"]) > 5000}
    json.dump(good, open(os.path.join(BASE, "maps_meta.json"), "w", encoding="utf-8"), indent=0)
    print(f"downloaded ok={ok}/{len(queue)} usable={len(good)}")

if __name__ == "__main__":
    main()
