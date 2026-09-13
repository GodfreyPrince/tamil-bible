import re, os
txt = open(r"C:\Users\Godfrey Prince\.zcode\workspace\default\tamil_build\ta_irv\44-JHN.usfm", encoding="utf-8-sig").read()
i = txt.find("\\v 16", txt.find("\\c 3"))
snip = txt[i:i+400]

def strip_zaln(txt):
    txt = re.sub(r"\\zaln(?:-[se])?\s*\|[^\\]*\\\*", "", txt)
    txt = re.sub(r"\\zaln(?:-[se])?\\\*|\\zaln\\\*", "", txt)
    return txt

s = strip_zaln(snip)
print("STRIPPED:", repr(s))
pos = 0
while pos < len(s):
    at = s.find("\\", pos)
    if at < 0: break
    m = re.match(r"\\([a-z0-9]+)(\*?)", s[at:])
    if not m:
        pos = at + 1; continue
    nxt = s.find("\\", at + m.end())
    body = s[at + m.end(): nxt if nxt >= 0 else len(s)]
    print("TAG", repr(m.group(1)), "star", repr(m.group(2)), "BODY", repr(body[:80]))
    pos = nxt if nxt >= 0 else len(s)
