# -*- coding: utf-8 -*-
"""Fetch real map categories (with pagination) + throttled 960px download -> maps_meta.json"""
import os, re, json, time, hashlib, subprocess, urllib.parse, urllib.request
import concurrent.futures as cf

BASE = os.path.expanduser(r"~\.zcode\workspace\default\tamil_build\media")
CACHE = os.path.join(BASE, "html")
MAPS_DL = os.path.join(BASE, "maps")
os.makedirs(MAPS_DL, exist_ok=True)
UA = {"User-Agent": "TamilBibleArtBot/1.0 (https://github.com/GodfreyPrince/tamil-bible-art; contact: godfreyprince@users.noreply.github.com)"}
TITLES_JSON = os.path.join(CACHE, "map_titles.json")

CATS = ["The Holy Land in Geography and in History (1899) by MACCOUN",
        "Old maps of ancient Israel",
        "Old maps of Palestine in the time of Jesus",
        "Tanakh maps", "Torah maps",
        "Maps of the Tribes of Israel",
        "Old maps of the stations of the Exodus",
        "Maps of Saint Paul's journeys"]

def fetch(cat, nxt=None):
    url = ("https://commons.wikimedia.org/wiki/Category:"
           + urllib.parse.quote(cat.replace(" ", "_"))
           + (("?" + nxt) if nxt else ""))
    return subprocess.run(["curl", "-s", "--max-time", "40", "-A", UA["User-Agent"], url],
                          capture_output=True, text=True, errors="replace").stdout

def parse(h):
    body = h.split('<div id="catlinks"')[0]
    files = set(urllib.parse.unquote(x).replace("_", " ")
                for x in re.findall(r'href="/wiki/File:([^"]+)"', body))
    nm = re.search(r'href="(/w/index\.php\?[^"]*pagefrom=[^"]+)"', h)
    nxt = nm.group(1).split("?")[1].replace("&amp;", "&") if nm else None
    return files, nxt

def sf_thumb(title, w=960):
    name = title.replace("_", " ")
    h = hashlib.md5(name.encode("utf-8")).hexdigest()
    stem, ext = os.path.splitext(name)
    el = ext.lower()
    pre = (f"https://upload.wikimedia.org/wikipedia/commons/thumb/{h[0]}/{h[:2]}/"
           + urllib.parse.quote(name) + "/")
    if el in (".tif", ".tiff"):
        return pre + f"lossy-page1-{w}px-" + urllib.parse.quote(stem) + ".jpg"
    if el == ".gif":
        return pre + f"{w}px-" + urllib.parse.quote(name) + ".png"
    return pre + f"{w}px-" + urllib.parse.quote(name)

def grab(item):
    t, c = item
    lid = "MP" + hashlib.md5(t.encode()).hexdigest()[:6].upper()
    p = os.path.join(MAPS_DL, lid + ".jpg")
    if os.path.exists(p) and os.path.getsize(p) > 5000:
        return (lid, t, c, p, True)
    for attempt in range(6):
        try:
            req = urllib.request.Request(sf_thumb(t), headers=UA)
            with urllib.request.urlopen(req, timeout=45) as r:
                d = r.read(40_000_000)
            if len(d) > 4000:
                open(p, "wb").write(d)
                time.sleep(0.6)
                return (lid, t, c, p, True)
        except Exception as e:
            time.sleep(8 if "429" in str(e) else 2)
    return (lid, t, c, p, False)

def main():
    if os.path.exists(TITLES_JSON):
        grand = json.load(open(TITLES_JSON, encoding="utf-8"))
    else:
        grand = {}
        for cat in CATS:
            files, nxt = set(), None
            for _ in range(5):
                h = fetch(cat, nxt)
                if len(h) < 1000:
                    break
                f, nxt = parse(h)
                files |= f
                if not nxt:
                    break
                time.sleep(1.5)
            grand[cat] = sorted(x for x in files
                                if re.search(r"\.(jpe?g|png|tif|tiff)$", x, re.I))
            print(f"[{len(grand[cat]):3d}] {cat}", flush=True)
        json.dump(grand, open(TITLES_JSON, "w", encoding="utf-8"))
    titles = []
    for c, fl in grand.items():
        titles += [(t, c) for t in fl]
    print("total map files:", len(titles), flush=True)
    meta, ok = {}, 0
    with cf.ThreadPoolExecutor(3) as ex:
        for lid, t, c, p, good in ex.map(grab, titles):
            if good:
                ok += 1
                meta[lid] = {"t": t, "cat": c, "p": p}
                if ok % 40 == 0:
                    print(f"ok={ok}/{len(titles)}", flush=True)
                    json.dump(meta, open(os.path.join(BASE, "maps_meta.json"), "w",
                                         encoding="utf-8"), indent=0)
    json.dump(meta, open(os.path.join(BASE, "maps_meta.json"), "w", encoding="utf-8"), indent=0)
    print(f"DONE ok={ok}/{len(titles)} usable={len(meta)}", flush=True)

if __name__ == "__main__":
    main()
