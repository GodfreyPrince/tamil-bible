# -*- coding: utf-8 -*-
"""Fetch Biblical maps category tree (2 levels, page GETs with pacing) then download."""
import os, re, json, time, subprocess, urllib.parse, hashlib
import concurrent.futures as cf

BASE = os.path.expanduser(r"~\.zcode\workspace\default\tamil_build\media")
CACHE = os.path.join(BASE, "html")
MAPS_DL = os.path.join(BASE, "maps")
os.makedirs(MAPS_DL, exist_ok=True)
UA = "TamilBibleArtBot/1.0 (https://github.com/GodfreyPrince/tamil-bible-art; contact: godfreyprince@users.noreply.github.com)"

def slug(c):
    return re.sub(r"[^A-Za-z0-9_]", "_", c)

def get_cat(cat):
    p = os.path.join(CACHE, "map_" + slug(cat) + ".html")
    if os.path.exists(p) and os.path.getsize(p) > 1000:
        return open(p, encoding="utf-8", errors="replace").read()
    time.sleep(1.5)
    url = "https://commons.wikimedia.org/wiki/Category:" + urllib.parse.quote(cat.replace(" ", "_"))
    out = subprocess.run(["curl", "-s", "--max-time", "40", "-A", UA, url],
                         capture_output=True, text=True, errors="replace").stdout
    if len(out) > 1000:
        open(p, "w", encoding="utf-8").write(out)
        return out
    return ""

def parse(h):
    subs, files = [], set()
    m = re.search(r'id="mw-subcategories"(.*?)(?=<div id="mw-pages"|$)', h, re.S)
    if m:
        subs = [urllib.parse.unquote(s).replace("_", " ")
                for s in re.findall(r'href="/wiki/Category:([^"]+)"', m.group(1))]
    m2 = re.search(r'id="mw-pages"(.*)$', h, re.S)
    if m2:
        body = m2.group(1).split('<div id="catlinks"')[0]
        files = set(urllib.parse.unquote(x) for x in re.findall(r'href="/wiki/File:([^"]+)"', body))
    return subs, files

def sf_thumb(title, w=960):
    name = urllib.parse.unquote(title).replace("_", " ")
    h = hashlib.md5(name.encode("utf-8")).hexdigest()
    stem, ext = os.path.splitext(name)
    tb = f"https://upload.wikimedia.org/wikipedia/commons/thumb/{h[0]}/{h[:2]}/"
    if ext.lower() in (".jpg", ".jpeg", ".png"):
        return tb + urllib.parse.quote(name) + f"/{w}px-" + urllib.parse.quote(name)
    return tb + urllib.parse.quote(name) + f"/{w}px-" + urllib.parse.quote(stem) + ".jpg"

def main():
    root = "Biblical maps"
    h = get_cat(root)
    subs, files = parse(h)
    subs = [s for s in subs if not re.search(r"Wikipedia|Template|Hidden|needing", s, re.I)]
    print("root files:", len(files), "| subcats:", len(subs))
    allfiles = {t: root for t in files}
    for s in subs:
        h = get_cat(s)
        _s, f2 = parse(h)
        for t in f2:
            allfiles[t] = s
        print(f"  [{len(f2):3d}] {s}")
    allfiles = {t: c for t, c in allfiles.items() if re.search(r"\.(jpe?g|png|tif|tiff)$", t, re.I)}
    print("total:", len(allfiles))
    meta = {}
    def grab(item):
        t, c = item
        lid = "MP" + hashlib.md5(t.encode()).hexdigest()[:6].upper()
        p = os.path.join(MAPS_DL, lid + ".jpg")
        if os.path.exists(p) and os.path.getsize(p) > 5000:
            return (t, c, p, True)
        for _ in range(2):
            try:
                req = urllib.request.Request(sf_thumb(t), headers={"User-Agent": UA})
                with urllib.request.urlopen(req, timeout=60) as r:
                    data = r.read(40_000_000)
                if len(data) > 5000:
                    open(p, "wb").write(data)
                    return (t, c, p, True)
            except Exception:
                time.sleep(1)
        return (t, c, p, False)
    import urllib.request
    ok = 0
    with cf.ThreadPoolExecutor(8) as ex:
        for t, c, p, good in ex.map(grab, sorted(allfiles.items())):
            if good:
                ok += 1
                meta["MP" + hashlib.md5(t.encode()).hexdigest()[:6].upper()] = {
                    "t": t, "cat": c, "p": p}
    json.dump(meta, open(os.path.join(BASE, "maps_meta.json"), "w", encoding="utf-8"), indent=0)
    print(f"downloaded ok={ok}/{len(allfiles)} usable={len(meta)}")

if __name__ == "__main__":
    main()
