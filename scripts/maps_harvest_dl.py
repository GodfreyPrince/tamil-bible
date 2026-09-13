# -*- coding: utf-8 -*-
"""Download harvested map URLs at 960px from thumb host; filter obvious junk."""
import os, re, time, json, hashlib, urllib.request, urllib.parse
import concurrent.futures as cf

BASE = os.path.expanduser(r"~\.zcode\workspace\default\tamil_build\media")
MAPS_DL = os.path.join(BASE, "maps")
UA = {"User-Agent": "TamilBibleArtBot/1.0 (https://github.com/GodfreyPrince/tamil-bible-art; contact: godfreyprince@users.noreply.github.com)"}

urls = [u for u in open(os.path.join(BASE, "map_urls.txt"), encoding="utf-8").read().splitlines() if u.strip()]
# drop pdf/djvu book covers and known non-map photos
keep = []
for u in urls:
    if re.search(r"\.pdf|\.djvu|page1-", u, re.I):
        continue
    if re.search(r"restaurant|beach|street|Aerial|aerial|geograph|pub|portrait|Selfie|DSCF|IMG_", u, re.I):
        continue
    keep.append(u)
print("kept:", len(keep), "of", len(urls), flush=True)

def grab(u):
    lid = "MP" + hashlib.md5(u.encode()).hexdigest()[:6].upper()
    p = os.path.join(MAPS_DL, lid + ".jpg")
    if os.path.exists(p) and os.path.getsize(p) > 4000:
        return (lid, u, p, True)
    try:
        req = urllib.request.Request(u, headers=UA)
        with urllib.request.urlopen(req, timeout=60) as r:
            d = r.read(40_000_000)
        if len(d) > 4000:
            open(p, "wb").write(d)
            return (lid, u, p, True)
    except Exception:
        pass
    return (lid, u, p, False)

meta, ok = {}, 0
with cf.ThreadPoolExecutor(8) as ex:
    for lid, u, p, good in ex.map(grab, keep):
        if good:
            ok += 1
            meta[lid] = {"u": u, "p": p}
        if ok and ok % 100 == 0:
            print("ok=", ok, flush=True)
json.dump(meta, open(os.path.join(BASE, "maps_meta2.json"), "w", encoding="utf-8"), indent=0)
print(f"DONE ok={ok}/{len(keep)}", flush=True)
