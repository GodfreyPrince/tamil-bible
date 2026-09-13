import json, re, os
d = json.load(open(os.path.expanduser(r"~\.zcode\workspace\default\TamilBible\app\src\main\assets\tamil.json"), encoding="utf-8"))
books = d["books"]
# locate padded gap verses
for bi, b in enumerate(books):
    for ci, ch in enumerate(b["c"]):
        empt = [i + 1 for i, t in enumerate(ch) if t == ""]
        if empt:
            print("GAP:", b["n"], "ch", ci + 1, "verses", empt)
# kjv gaps for comparison
k = json.load(open(os.path.expanduser(r"~\.zcode\workspace\default\KJVBible\app\src\main\assets\kjv.json"), encoding="utf-8"))
kb = {b["n"]: len(b["c"]) for b in k["books"]}
# VOTD extraction restricted to the VOTD_REFS array
src = open(os.path.expanduser(r"~\.zcode\workspace\default\KJVBible\app\src\main\java\com\godfrey\kjvbible\BibleData.java"), encoding="utf-8").read()
block = src.split("VOTD_REFS = {")[1].split("};")[0]
refs = re.findall(r'"([^"]+ \d+:\d+)"', block)
print("VOTD refs:", len(refs))
en = ["Genesis","Exodus","Leviticus","Numbers","Deuteronomy","Joshua","Judges","Ruth","1 Samuel",
      "2 Samuel","1 Kings","2 Kings","1 Chronicles","2 Chronicles","Ezra","Nehemiah","Esther","Job",
      "Psalm","Proverbs","Ecclesiastes","Song of Solomon","Isaiah","Jeremiah","Lamentations","Ezekiel",
      "Daniel","Hosea","Joel","Amos","Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah","Haggai",
      "Zechariah","Malachi","Matthew","Mark","Luke","John","Acts","Romans","1 Corinthians",
      "2 Corinthians","Galatians","Ephesians","Philippians","Colossians","1 Thessalonians",
      "2 Thessalonians","1 Timothy","2 Timothy","Titus","Philemon","Hebrews","James","1 Peter",
      "2 Peter","1 John","2 John","3 John","Jude","Revelation"]
en_idx = {n: i for i, n in enumerate(en)}
bad = 0
entries = []
for ref in refs:
    book, rest = ref.rsplit(" ", 1)
    c, v = rest.split(":")
    b, c, v = en_idx[book], int(c), int(v)
    ch = books[b]["c"][c - 1]
    if v > len(ch) or ch[v - 1] == "":
        print("MISSING in Tamil:", ref)
        bad += 1
    entries.append((b, c, v))
rows = ",".join("{" + f"{b},{c},{v}" + "}" for b, c, v in entries)
out = os.path.expanduser(r"~\.zcode\workspace\default\TamilBible\scripts\votd_table.txt")
open(out, "w", encoding="utf-8").write("\n".join(rows[i:i+96].rstrip(",") + "\n" for i in range(0, len(rows), 96)))
print("missing refs:", bad)
# sample text sanity
print("Gen1:1:", books[0]["c"][0][0][:60])
print("Jn3:16:", books[42]["c"][2][15][:60])
print("Ps23 title:", d["ptitles"].get("23", {}).get("1"))
print("Ps119 aleph:", d["ptitles"].get("119", {}).get("1"))
print("Ps23:1:", books[18]["c"][22][0][:60])
print("Mt5 char sample:", books[39]["c"][4][2][:50])
