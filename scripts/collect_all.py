# -*- coding: utf-8 -*-
"""Fast Commons category-tree scraper (plain page GETs, parallel, cached)."""
import os, re, sys, json, time, subprocess, urllib.parse
import concurrent.futures as cf

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) TamilBibleArtCurator/1.0 (personal dev project)"
BASE = "https://commons.wikimedia.org/wiki/"
CACHE = os.path.expanduser(r"~\.zcode\workspace\default\tamil_build\media\html")
OUT = os.path.expanduser(r"~\.zcode\workspace\default\tamil_build\media")
os.makedirs(CACHE, exist_ok=True)

def fetch(url, key):
    p = os.path.join(CACHE, re.sub(r"[^A-Za-z0-9_]", "_", key) + ".html")
    if os.path.exists(p) and os.path.getsize(p) > 1000:
        return open(p, encoding="utf-8", errors="replace").read()
    for a in range(3):
        out = subprocess.run(["curl", "-s", "--max-time", "40", "-A", UA, url],
                             capture_output=True, text=True, errors="replace").stdout
        if len(out) > 1000:
            open(p, "w", encoding="utf-8").write(out)
            return out
        time.sleep(2 * (a + 1))
    return ""

def parse_cat_page(cat):
    """One category page -> (files, subcats, next_page_url). Sections only."""
    url = BASE + "Category:" + urllib.parse.quote(cat.replace(" ", "_"))
    h = fetch(url, "cat_" + cat)
    files, subcats, nxt = set(), [], None
    m = re.search(r'id="mw-subcategories".*?(?=<div id="mw-pages"|$)', h, re.S)
    if m:
        subcats = re.findall(r'href="/wiki/Category:([^"]+)"', m.group(0))
    m2 = re.search(r'id="mw-pages"(.*)$', h, re.S)
    if m2:
        body = m2.group(1)
        body = body.split('<div id="catlinks"')[0]
        files = set(urllib.parse.unquote(x) for x in re.findall(r'href="/wiki/File:([^"]+)"', body))
        nm = re.search(r'href="(/w/index\.php\?[^"]*pagefrom=[^"]+)"', body)
        if nm:
            nxt = "https://commons.wikimedia.org" + nm.group(1).replace("&amp;", "&")
    subcats = [urllib.parse.unquote(s).replace("_", " ") for s in subcats]
    junk = {"Categories", "CommonsRoot", "Media needing categories", "Hidden categories",
            "Wikipedia categories", "Template tracking", "Disambiguation categories"}
    subcats = [s for s in subcats if s not in junk and "Media needing" not in s
               and "Hidden" not in s and s.split(":")[0] not in ("Wikipedia", "Template", "Portal", "Help", "User")]
    return files, subcats, nxt

def crawl(root, depth, pool, visited):
    if root in visited or depth < 0:
        return
    visited.add(root)
    files, subcats, nxt = parse_cat_page(root)
    pool.setdefault(root, set()).update(files)
    while nxt:                       # pagination for >200 files
        h = fetch(nxt, "page_" + root + "_" + nxt[-60:])
        body = h
        files = set(urllib.parse.unquote(x) for x in re.findall(r'href="/wiki/File:([^"]+)"', body))
        files = {f for f in files if not f.startswith("Category")}
        pool[root].update(files)
        nm = re.search(r'href="(/w/index\.php\?[^"]*pagefrom=[^"]+)"', body)
        nxt = ("https://commons.wikimedia.org" + nm.group(1).replace("&amp;", "&")) if nm else None
    print(f"  [{len(pool[root]):4d} files] {root} | subcats: {subcats[:6]}", flush=True)
    if depth > 0 and subcats:
        with cf.ThreadPoolExecutor(10) as ex:
            list(ex.map(lambda s: crawl(s, depth - 1, pool, visited), subcats))

def main():
    roots = json.load(open(os.path.join(OUT, "roots.json"), encoding="utf-8"))
    pool = {}
    visited = set()
    for root, depth in roots.items():
        print("== ROOT:", root, flush=True)
        crawl(root, depth, pool, visited)
    json.dump({k: sorted(v) for k, v in pool.items()},
              open(os.path.join(OUT, "raw_catalog.json"), "w", encoding="utf-8"), indent=1)
    total = sum(len(v) for v in pool.values())
    uniq = len(set(f for v in pool.values() for f in v))
    print(f"DONE categories={len(pool)} total={total} unique={uniq}")

if __name__ == "__main__":
    main()
