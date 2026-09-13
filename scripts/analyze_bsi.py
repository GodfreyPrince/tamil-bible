import re, os, collections
base = os.path.expanduser(r"~\.zcode\workspace\default\tamil_build\bsi")
txt = open(os.path.join(base, "t_verses.sql"), encoding="utf-8").read()
ids = re.findall(r"\((\d{8}),\s*'", txt)
print("verse rows:", len(ids))
codes = collections.Counter(id[:2] for id in ids)
print("books with data:", len(codes), "min/max book:", min(codes), max(codes))
# special enclosed markers inside verse text
chars = collections.Counter()
for m in re.findall(r"[\u249c-\u24e9\u3251-\u325f\u32b1-\u32bf]", txt):
    chars[m] += 1
print("enclosed chars:", dict(chars))
# check verse-number continuity per book: collect all (b,c,v)
bible = collections.defaultdict(set)
for i in ids:
    b, c, v = int(i[:2]), int(i[2:5]), int(i[5:])
    bible[(b, c)].add(v)
gaps = 0
for (b, c), vs in sorted(bible.items()):
    mx = max(vs)
    missing = [x for x in range(1, mx + 1) if x not in vs]
    if missing:
        gaps += len(missing)
        print("GAP book", b, "ch", c, "missing", missing[:6])
print("total gaps:", gaps)
print("chapters:", len(bible))
# headers file
h = open(os.path.join(base, "t_verseheaders.sql"), encoding="utf-8").read()
print("=== verseheaders sample ===")
print(h[:800].replace("\r", ""))
bk = open(os.path.join(base, "t_bookkey.sql"), encoding="utf-8").read()
rows = re.findall(r"\((\d+),\s*'([A-Z0-9]+)',\s*'([^']*)'", bk)
print("bookkey rows:", len(rows))
print(rows[:8])
