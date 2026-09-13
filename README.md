# தமிழ் பைபிள் — Tamil Bible (Offline) for Android

A free, fully offline Tamil Bible app with **three Tamil texts**, a **reading tracker**,
and an **online art gallery** of 2,000+ reviewed public-domain Biblical artworks plus
228 Biblical maps. No ads, no accounts, no tracking — the app never uses the internet
except to load the optional art/map images.

## Features
- **Three Tamil Bible texts**, switchable in one tap from the reader:
  - **பழைய** — the classic old BSI Tamil text (public domain, 31,102 verses)
  - **BSI** — திருவிவிலியம் 2012 ecumenical translation
  - **IRV** — Indian Revised Version (simplified Tamil)
- **Chapter reading tracker** — ✓ button per chapter, completed-card in the reader,
  per-book progress rings, full progress screen (overall ring + OT/NT bars), persisted
  in a local database and shared across versions.
- **Book/chapter picker** with OT/NT tabs and per-book rings.
- **Tap any verse**: 5-colour highlights, bookmarks, private notes, copy, share.
- **Full-Bible search**, Verse of the Day (146 hand-picked verses, day-rotating).
- **6 reading plans** with day-by-day progress (John 21, Psalms 30, Proverbs 31,
  Life of Jesus 30, NT 90, Whole Bible 365).
- **கலைக் காட்சியகம் (Art Gallery)** — 2,077 public-domain artworks mapped to Bible
  chapters, browsable by scene, with Tamil titles/descriptions. Images load online
  from the holding museums; a switch keeps them stored on the phone.
- **வேத வரைபடங்கள் (Maps)** — 228 reviewed high-res Biblical maps (tribes, Exodus
  routes, Paul's journeys, Jerusalem plans, temple plans…).
- **Reader art strip** — chapters with linked art show thumbnails at the chapter's end.
- Light/Dark/Follow-system theme, text size 14–30sp, keep-screen-on.
- **API 36 (Android 16) edge-to-edge**; runs on Android 7.0+ (minSdk 24).

## APK
`Tamil-Bible-Offline-v1.0.apk` (5.4 MB, signed) — see Releases, or build it yourself:

```bash
bash build.sh
```

The build is a no-Gradle pipeline: **aapt2 → javac → d8 → zipalign → apksigner**.
It needs JDK 17, Android build-tools (aapt2/d8/zipalign/apksigner), and
`android.jar` from platform android-36. Paths at the top of `build.sh` point at a
bundled toolchain (`tools/`, git-ignored) — adjust `BT` and `PLAT` to your SDK, e.g.
`BT=$ANDROID_HOME/build-tools/34.0.0`, `PLAT` pointing at `platforms/android-36`.
The signing keystore is intentionally **not** in this repo — generate one:

```bash
keytool -genkeypair -keystore keystore.jks -alias bible -keyalg RSA -keysize 2048 \
  -validity 10950 -storepass <yourpass> -keypass <yourpass> \
  -dname "CN=Your Name, OU=Personal, O=You, C=US"
```

## Texts & sources
| Text | Source | License |
| --- | --- | --- |
| பழைய (old BSI Tamil) | digitized public-domain text (godlytalias/Bible-Database) | public domain |
| திருவிவிலியம் 2012 | digitized Tamil Ecumenical Bible (jayarathina/Tamil-Bible-Database) | Unlicense |
| IRV Tamil | Indian Revised Version USFM (Bridge Connectivity Solutions) | CC BY-SA 4.0 |

Asset-conversion scripts live in `scripts/` (`make_old_asset.py`, `make_bsi_asset.py`,
`make_tamil_asset.py`). The artworks/maps index and pipeline are in the companion
repo [tamil-bible-art](https://github.com/GodfreyPrince/tamil-bible-art) — the app
ships that manifest in `assets/` and loads images online only.

## Data & privacy
Highlights, bookmarks, notes, plan progress and read-chapters live in a local SQLite
database on the phone. Nothing ever leaves the device.

## License
App code: MIT. Bible texts carry the licenses listed above (all free/open).
Artworks keep their original open licenses (PDM-1.0 / CC0 / CC-BY, per the manifest).
