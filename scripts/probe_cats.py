# -*- coding: utf-8 -*-
"""Probe candidate Commons categories via plain page GETs (no API). Returns file counts."""
import subprocess, re, sys, urllib.parse, concurrent.futures as cf

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) TamilBibleArtCurator/1.0 (personal dev project)"
CANDIDATES = [
    # Dore
    "Biblical illustrations by Gustave Doré",
    "La Grande Bible de Tours",
    "Bible illustrations by Gustave Doré",
    "Gustave Doré's illustrations for the Bible",
    # Tissot
    "James Tissot paintings of the Life of Christ",
    "The Life of Christ by James Tissot",
    "James Tissot paintings of the Old Testament",
    "Paintings by James Tissot",
    # Holman + other 19c illustrated bibles
    "Holman Bible (1890 ed.)",
    # Maps
    "Biblical maps",
    "Maps of the Bible",
    "Maps of the Holy Land",
    "Maps of the Twelve Tribes of Israel",
    "Maps of Paul's missionary journeys",
    # Key events (NT)
    "Paintings of the Nativity of Jesus",
    "Paintings of the Annunciation",
    "Paintings of the Adoration of the Magi",
    "Paintings of the Baptism of Jesus",
    "Paintings of the Last Supper",
    "Paintings of the Crucifixion of Christ",
    "Paintings of the Resurrection of Christ",
    "Paintings of the Transfiguration of Christ",
    "Paintings of the Ascension of Christ",
    "Paintings of the Sermon on the Mount",
    "Paintings of the Parables of Jesus",
    "Paintings of the Miracles of Jesus",
    "Paintings of the Passion of Jesus Christ",
    "Paintings of the Entombment of Christ",
    # Key events / figures (OT)
    "Paintings of the Creation",
    "Paintings of Adam and Eve",
    "Paintings of the Deluge",
    "Paintings of Abraham",
    "Paintings of Moses",
    "Paintings of David",
    "Paintings of Elijah",
    "Paintings of Daniel",
    "Paintings of Job",
    "Paintings of Jonah",
    "Paintings of Esther",
    "Paintings of Judith",
    "Paintings of the Prophet Jeremiah",
    "Paintings of the Prophet Isaiah",
]

def probe(name):
    url = "https://commons.wikimedia.org/wiki/Category:" + urllib.parse.quote(name.replace(" ", "_"))
    try:
        out = subprocess.run(["curl", "-s", "-A", UA, "--max-time", "30", url],
                             capture_output=True, text=True, errors="replace").stdout
        files = set(re.findall(r'href="/wiki/File:([^"]+)"', out))
        nextp = re.findall(r'href="[^"]*pagefrom=([^"&]+)', out)
        return (name, len(files), len(nextp), bool(out))
    except Exception as e:
        return (name, -1, 0, str(e)[:60])

with cf.ThreadPoolExecutor(8) as ex:
    for name, n, nxt, ok in ex.map(probe, CANDIDATES):
        print(f"{n:5d} files | nextpages={nxt} | {name}" + ("" if ok else "  [NO PAGE]"))
