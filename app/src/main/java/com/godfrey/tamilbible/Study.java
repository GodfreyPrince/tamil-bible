package com.godfrey.tamilbible;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Study-layer helpers: book codes, server URL, offline pack management, HTTP. */
public final class Study {

    /** 66 canonical book codes in app book order (index 0 = Genesis). */
    public static final String[] CODES = {
        "Gen","Exo","Lev","Num","Deu","Jos","Jdg","Rth","1Sa","2Sa","1Ki","2Ki",
        "1Ch","2Ch","Ezr","Neh","Est","Job","Psa","Pro","Ecc","Sng","Isa","Jer",
        "Lam","Ezk","Dan","Hos","Jol","Amo","Oba","Jon","Mic","Nam","Hab","Zep",
        "Hag","Zec","Mal","Mat","Mrk","Luk","Jhn","Act","Rom","1Co","2Co","Gal",
        "Eph","Phl","Col","1Th","2Th","1Ti","2Ti","Tit","Phm","Heb","Jas","1Pe",
        "2Pe","1Jn","2Jn","3Jn","Jud","Rev"
    };

    private Study() { }

    public static String ref(int book, int chapter, int verse) {
        if (book < 0 || book >= CODES.length) return null;
        return CODES[book] + "." + chapter + "." + verse;
    }

    public static String serverUrl() {
        return Ui.prefs().getString("study_url", "");
    }

    /** Shared key baked into the app so users never enter anything.
     *  Rotating: change the server's study_key.txt, then ship an app update
     *  (or set a per-device override in Settings). */
    public static final String DEFAULT_KEY = "2df82bc813e9460630c6216d8012205d";

    public static String apiKey() {
        String k = Ui.prefs().getString("study_key", "");
        return k.isEmpty() ? DEFAULT_KEY : k;
    }

    public static File packFile(Context ctx) {
        return new File(ctx.getFilesDir(), "tamil_study.db");
    }

    public static boolean hasPack(Context ctx) {
        return packFile(ctx).length() > 0;
    }

    /** Synchronous HTTP GET returning the parsed JSON body. */
    public static JSONObject getJson(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(6000);
        c.setReadTimeout(20000);
        String key = apiKey();
        if (!key.isEmpty()) c.setRequestProperty("X-Study-Key", key);
        int code = c.getResponseCode();
        if (code != 200) throw new Exception("HTTP " + code);
        InputStream in = c.getInputStream();
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) sb.append(new String(buf, 0, n, "UTF-8"));
        in.close();
        return new JSONObject(sb.toString());
    }

    /** Synchronous HTTP HEAD/GET of /api/health — returns pack size or -1. */
    public static long probePackSize(String base) throws Exception {
        JSONObject h = getJson(trim(base) + "/api/health");
        return h.optLong("pack", -1);
    }

    /** Download the study pack zip and extract tamil_study.db into filesDir. */
    public static void downloadPack(Context ctx, String base) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(trim(base) + "/api/pack").openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(60000);
        String key = apiKey();
        if (!key.isEmpty()) c.setRequestProperty("X-Study-Key", key);
        int code = c.getResponseCode();
        if (code != 200) throw new Exception("HTTP " + code);
        File tmp = new File(ctx.getCacheDir(), "pack.zip");
        FileOutputStream out = new FileOutputStream(tmp);
        InputStream in = c.getInputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        out.close();
        ZipInputStream z = new ZipInputStream(new java.io.FileInputStream(tmp));
        ZipEntry e;
        while ((e = z.getNextEntry()) != null) {
            if (e.getName().equals("tamil_study.db")) {
                FileOutputStream db = new FileOutputStream(packFile(ctx));
                while ((n = z.read(buf)) > 0) db.write(buf, 0, n);
                db.close();
                break;
            }
        }
        z.close();
        tmp.delete();
    }

    public static void deletePack(Context ctx) {
        packFile(ctx).delete();
    }

    public static String trim(String url) {
        url = url == null ? "" : url.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }
}
