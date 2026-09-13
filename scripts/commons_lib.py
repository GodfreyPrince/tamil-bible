# -*- coding: utf-8 -*-
"""Rate-limited Wikimedia Commons collector: category trees -> file metadata (no downloads)."""
import json, os, time, urllib.request, urllib.parse

API = "https://commons.wikimedia.org/w/api.php"
UA = {"User-Agent": "TamilBibleArtCurator/1.0 (https://example.local; personal project)"}
CACHE = os.path.expanduser(r"~\.zcode\workspace\default\tamil_build\media\api")
os.makedirs(CACHE, exist_ok=True)

_last = [0.0]

def get(params, cache_key=None, tries=4):
    if cache_key:
        p = os.path.join(CACHE, cache_key + ".json")
        if os.path.exists(p):
            return json.load(open(p, encoding="utf-8"))
    params = dict(params, format="json")
    for attempt in range(tries):
        gap = time.time() - _last[0]
        if gap < 0.8:
            time.sleep(0.8 - gap)
        try:
            req = urllib.request.Request(API + "?" + urllib.parse.urlencode(params), headers=UA)
            with urllib.request.urlopen(req, timeout=40) as r:
                d = json.load(r)
            _last[0] = time.time()
            if "error" in d:
                raise RuntimeError(d["error"].get("info", str(d["error"])))
            if cache_key:
                json.dump(d, open(os.path.join(CACHE, cache_key + ".json"), "w", encoding="utf-8"))
            return d
        except Exception as e:
            msg = str(e)
            if "429" in msg or "Too Many" in msg:
                time.sleep(6 * (attempt + 1))
                continue
            raise
    raise RuntimeError("api failed: " + str(params))

def cat_members(cat, cmtype):
    """All members of a category (file|subcat), following continuations."""
    out, cont = [], {}
    while True:
        d = get({"action": "query", "list": "categorymembers", "cmtitle": "Category:" + cat,
                 "cmtype": cmtype, "cmlimit": "500", **cont})
        out += [m["title"] for m in d["query"]["categorymembers"]]
        if "continue" in d:
            cont = d["continue"]
        else:
            return out

def subcats_with_size(cat):
    d = get({"action": "query", "list": "categorymembers", "cmtitle": "Category:" + cat,
             "cmtype": "subcat", "cmlimit": "500", "cmprop": "title|size"})
    return [(m["title"].replace("Category:", ""), m.get("size", 0))
            for m in d["query"]["categorymembers"]]

def tree_files(root_cat, max_depth=2):
    """Files in root_cat and its subcats (depth-limited). Returns {subcat: [files]}."""
    result = {}
    result[""] = cat_members(root_cat, "file")
    if max_depth > 0:
        for name, _sz in subcats_with_size(root_cat):
            name = name.replace("Category:", "")
            sub = tree_files(name, max_depth - 1)
            for k, v in sub.items():
                result[(name + "/" + k).strip("/")] = v
    return result

def fileinfo(titles):
    """Batched imageinfo for up to 50 titles."""
    out = []
    for i in range(0, len(titles), 50):
        chunk = titles[i:i + 50]
        d = get({"action": "query", "titles": "|".join(chunk), "prop": "imageinfo",
                 "iiprop": "url|size|mime|extmetadata", "iiurlwidth": "1024"},
                cache_key="info_%04d" % abs(hash(tuple(chunk)) % 10**6))
        for pid, page in d["query"]["pages"].items():
            if "imageinfo" not in page:
                continue
            ii = page["imageinfo"][0]
            em = ii.get("extmetadata", {})
            def emv(k):
                v = em.get(k, {}).get("value", "")
                import re
                return re.sub(r"<[^>]+>", "", v).strip()
            out.append({
                "title": page["title"],
                "w": ii.get("width"), "h": ii.get("height"),
                "mime": ii.get("mime"),
                "page": ii.get("descriptionurl", ""),
                "thumb": ii.get("thumburl"),
                "url": ii.get("url"),
                "artist": emv("Artist"), "license": emv("LicenseShortName"),
                "year": emv("DateTimeOriginal")[:40],
                "desc": emv("ImageDescription")[:300],
            })
    return out
