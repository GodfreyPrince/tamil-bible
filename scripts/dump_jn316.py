import os
txt = open(r"C:\Users\Godfrey Prince\.zcode\workspace\default\tamil_build\ta_irv\44-JHN.usfm", encoding="utf-8-sig").read()
i = txt.find("\\v 16", txt.find("\\c 3"))
print(repr(txt[i:i+900]))
