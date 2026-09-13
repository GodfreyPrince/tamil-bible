# -*- coding: utf-8 -*-
"""Explore Wikimedia Commons categories for Bible art collections (metadata only)."""
import json, urllib.request, urllib.parse, sys

API = "https://commons.wikimedia.org/w/api.php"
UA = {"User-Agent": "TamilBibleCurator/1.0 (personal dev; contact: local)"}

def get(params):
    params = dict(params, format="json")
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)

def find_categories(prefix, limit=30):
    d = get({"action": "query", "list": "allcategories", "acprefix": prefix,
             "aclimit": limit, "acprop": "size"})
    return [(c["*"], c["size"]) for c in d["query"]["allcategories"]]

def members(cat, cmtype="file", limit=10):
    d = get({"action": "query", "list": "categorymembers", "cmtitle": "Category:" + cat,
             "cmtype": cmtype, "cmlimit": limit})
    return [m["title"] for m in d["query"]["categorymembers"]]

def subcats(cat, limit=50):
    d = get({"action": "query", "list": "categorymembers", "cmtitle": "Category:" + cat,
             "cmtype": "subcat", "cmlimit": limit})
    return [(m["title"], m.get("size", "?")) for m in d["query"]["categorymembers"]]

if __name__ == "__main__":
    for prefix in ["Holman", "Gustave Dor", "James Tissot", "Biblical maps", "Bible atlas",
                   "Illustrations from the Holy Bible", "Bible illustrations"]:
        print("=" * 10, prefix)
        try:
            for name, size in find_categories(prefix):
                print(f"  [{size:5d}] Category:{name}")
        except Exception as e:
            print("  ERR", e)
