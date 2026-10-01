#!/bin/bash
# Build Tamil Bible APK without Gradle: aapt2 -> javac -> d8 -> package -> align -> sign
# Targets Android 16 (API 36); runs on Android 7.0 (API 24) and above.
set -e
cd "$(dirname "$0")"

BT="../KJVBible/tools/bt/android-14"
PLAT="tools/plat/android-36"
AJ="$PLAT/android.jar"
OUT="Tamil-Bible-Offline-v1.0.apk"

# keystore (copied from the KJV app — same developer identity)
if [ ! -f keystore.jks ]; then
  keytool -genkeypair -v -keystore keystore.jks -alias bible -keyalg RSA -keysize 2048 \
    -validity 10950 -storepass kjvbible2026 -keypass kjvbible2026 \
    -dname "CN=Godfrey Prince, OU=Personal, O=Godfrey Prince, C=UG"
fi

rm -rf build
mkdir -p build/gen build/obj

echo "== aapt2 compile =="
"$BT/aapt2.exe" compile --dir app/src/main/res -o build/res.zip

echo "== aapt2 link =="
"$BT/aapt2.exe" link -o build/base.apk -I "$AJ" \
  --manifest app/src/main/AndroidManifest.xml \
  -A app/src/main/assets \
  --java build/gen \
  --min-sdk-version 24 --target-sdk-version 36 \
  --version-code 2 --version-name 1.1 \
  --auto-add-overlay build/res.zip

echo "== javac =="
find build/gen app/src/main/java -name "*.java" > build/sources.txt
if ! javac --release 8 -encoding UTF-8 -cp "$AJ" -d build/obj @build/sources.txt 2> build/javac.log; then
  grep -v "^warning" build/javac.log | head -40 || true
  exit 1
fi
grep -v "^warning" build/javac.log || true
test -f build/obj/com/godfrey/tamilbible/App.class

echo "== d8 =="
jar cf build/classes.jar -C build/obj .
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --release --lib "$AJ" --min-api 24 --output build build/classes.jar

echo "== package =="
python -c "
import zipfile
with zipfile.ZipFile('build/base.apk','a') as z:
    z.write('build/classes.dex','classes.dex')
"
"$BT/zipalign.exe" -f 4 build/base.apk build/aligned.apk

echo "== sign =="
java -jar "$BT/lib/apksigner.jar" sign --ks keystore.jks --ks-pass pass:kjvbible2026 \
  --ks-key-alias bible --out "$OUT" build/aligned.apk

echo "== verify =="
java -jar "$BT/lib/apksigner.jar" verify --print-certs "$OUT" | head -5
"$BT/aapt2.exe" dump badging "$OUT" | head -8
ls -la "$OUT"
echo "DONE: $OUT"
