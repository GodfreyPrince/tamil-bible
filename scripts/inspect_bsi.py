import re, os
base = os.path.expanduser(r"~\.zcode\workspace\default\tamil_build\bsi")
for fn in ("t_verses.sql", "t_bookkey.sql"):
    p = os.path.join(base, fn)
    txt = open(p, encoding="utf-8", errors="replace").read()
    print("=" * 20, fn, len(txt), "chars")
    print(txt[:1200].replace("\r", ""))
    print("---- inserts:", txt.count("INSERT"))
