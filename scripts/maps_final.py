# -*- coding: utf-8 -*-
"""Throttled map downloader: 960px MD5 thumbs, 2 workers, patient 429 backoff."""
import os, re, json, time, hashlib, urllib.parse, urllib.request
import concurrent.futures as cf

BASE = os.path.expanduser(r"~\.zcode\workspace\default\tamil_build\media")
CACHE = os.path.join(BASE, "html")
MAPS_DL = os.path.join(BASE, "maps")
os.makedirs(MAPS_DL, exist_ok=True)
UA = {"User-Agent": "TamilBibleArtBot/1.0 (https://github.com/GodfreyPrince/tamil-bible-art; contact: godfreyprince@users.noreply.github.com)"}

def sf_thumb(title, w=960):
    name = urllib.parse.unquote(title).replace("_", " ")
    h = hashlib.md5(name.encode("utf-8")).hexdigest()
    tb = f"https://upload.wikimedia.org/wikipedia/commons/thumb/{h[0]}/{h[:2]}/"
    return tb + urllib.parse.quote(name) + f"/{w}px-" + urllib.parse.quote(name)

def collect():
    titles = {}
    for fn in os.listdir(CACHE):
        if not fn.startswith("map_") or "Uses_of" in fn:
            continue
        h = open(os.path.join(CACHE, fn), encoding="utf-8", errors="replace").read()
        m2 = re.search(r'id="mw-pages"(.*)$', h, re.S)
        if not m2:
            continue
        body = m2.group(1).split('<div id="catlinks"')[0]
        cat = fn[4:-5]
        for x in re.findall(r'href="/wiki/File:([^"]+)"', body):
            t = urllib.parse.unquote(x).replace("_", " ")
            if re.search(r"\.(jpe?g|png|gif|tif|tiff)$", t, re.I):
                titles[t] = cat
    return titles

def grab(item):
    t, c = item
    lid = "MP" + hashlib.md5(t.encode()).hexdigest()[:6].upper()
    p = os.path.join(MAPS_DL, lid + ".jpg")
    if os.path.exists(p) and os.path.getsize(p) > 5000:
        return (lid, t, c, p, True)
    u = sf_thumb(t)
    for attempt in range(6):
        try:
            req = urllib.request.Request(u, headers=UA)
            with urllib.request.urlopen(req, timeout=45) as r:
                d = r.read(40_000_000)
            if len(d) > 4000:
                open(p, "wb").write(d)
                time.sleep(0.8)
                return (lid, t, c, p, True)
        except Exception as e:
            if "429" in str(e):
                time.sleep(8 + 8 * attempt)
            else:
                time.sleep(2)
    return (lid, t, c, p, False)

def main():
    titles = collect()
    print("titles:", len(titles), flush=True)
    meta, ok = {}, 0
    with cf.ThreadPoolExecutor(2) as ex:
        for lid, t, c, p, good in ex.map(grab, sorted(titles.items())):
            if good:
                ok += 1
                meta[lid] = {"t": t, "cat": c, "p": p}
                if ok % 25 == 0:
                    print(f"ok={ok}/{len(titles)}", flush=True)
    json.dump(meta, open(os.path.join(BASE, "maps_meta.json"), "w", encoding="utf-8"), indent=0)
    print(f"DONE ok={ok}/{len(titles)} usable={len(meta)}", flush=True)

if __name__ == "__main__":
    main()
