# -*- coding: utf-8 -*-
"""Drill into specific Commons categories."""
import sys
sys.path.insert(0, r"C:\Users\Godfrey Prince\.zcode\workspace\default\TamilBible\scripts")
from explore_commons import find_categories, members, subcats, get

print("=" * 12, "Holman Bible (1890 ed.) subcats")
for name, size in subcats("Holman Bible (1890 ed.)", 100):
    print(f"  [{size:5d}] {name}")

print("=" * 12, "Holman files (top-level)")
files = members("Holman Bible (1890 ed.)", "file", 20)
for f in files:
    print("  ", f)

print("=" * 12, "Doré bible cats")
for prefix in ["Gustave Doré's illustrations", "Gustave Doré illustrations"]:
    try:
        for name, size in find_categories(prefix, 40):
            print(f"  [{size:5d}] Category:{name}")
    except Exception as e:
        print("  ERR", e)

print("=" * 12, "Tissot cats")
for name, size in find_categories("James Tissot", 40):
    print(f"  [{size:5d}] Category:{name}")

print("=" * 12, "Bible illustration / map cats")
for prefix in ["Bible illustrations", "Biblical illustrations", "Biblical maps", "Bible maps",
               "Maps of biblical", "Biblical history", "Bible atlas"]:
    for name, size in find_categories(prefix, 25):
        print(f"  [{size:5d}] Category:{name}")
