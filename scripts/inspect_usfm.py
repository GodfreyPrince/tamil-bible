import re
txt = open(r"C:\Users\Godfrey Prince\.zcode\workspace\default\tamil_build\ta_irv\45-ACT.usfm", encoding="utf-8-sig").read()
allv = re.findall(r"\\v (\d+)", txt)
print("total \\v tags in ACT:", len(allv))
ch2 = txt.split("\\c 2\n")[1].split("\\c 3")[0]
print("ACT ch2 \\v tags:", re.findall(r"\\v (\d+)", ch2))
i = ch2.find("\\v 34")
print("before v34:", repr(ch2[max(0,i-200):i+80]))
# lines that contain \v but do NOT start with it
bad = [l for l in ch2.split("\n") if "\\v" in l and not l.lstrip().startswith("\\v")]
print("lines with mid-line \\v:", len(bad))
for l in bad[:5]:
    print(repr(l[:160]))
