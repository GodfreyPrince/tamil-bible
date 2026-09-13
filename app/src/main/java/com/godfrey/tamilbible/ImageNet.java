package com.godfrey.tamilbible;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Online image loading with a small memory cache; optional persistent storage. */
public final class ImageNet {
    private static final ExecutorService POOL = Executors.newFixedThreadPool(6);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int MEM_MAX = 48;
    private static final Map<String, Bitmap> MEM = new LinkedHashMap<String, Bitmap>(32, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, Bitmap> e) { return size() > MEM_MAX; }
    };
    private static final Map<String, Boolean> INFLIGHT = new ConcurrentHashMap<>();
    private static File cacheDir, keepDir;

    public static boolean online(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    /** When "store" is on, images land in the private Pictures dir and persist. */
    public static File targetFile(Context c, String url, boolean store) {
        if (cacheDir == null) cacheDir = new File(c.getCacheDir(), "media");
        if (keepDir == null) keepDir = new File(c.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "art");
        File d = store ? keepDir : cacheDir;
        if (!d.exists()) d.mkdirs();
        return new File(d, sha(url) + ".img");
    }

    public static boolean saved(Context c, String url) {
        File a = targetFile(c, url, false);
        File b = targetFile(c, url, true);
        return (a.exists() && a.length() > 5000) || (b.exists() && b.length() > 5000);
    }

    public static void load(Context c, String url, ImageView iv, int ph) {
        Bitmap mem = MEM.get(url);
        if (mem != null) { iv.setImageBitmap(mem); return; }
        iv.setImageResource(ph);
        final boolean store = Ui.prefs().getBoolean("store_media", false);
        final File f = targetFile(c, url, store);
        if (!f.exists() || f.length() < 5000) {
            if (!online(c)) { iv.setImageResource(ph); return; }
            if (INFLIGHT.putIfAbsent(url, true) != null) return;
            POOL.execute(() -> {
                try {
                    HttpURLConnection hc = (HttpURLConnection) new URL(url).openConnection();
                    hc.setConnectTimeout(15000);
                    hc.setReadTimeout(30000);
                    hc.setRequestProperty("User-Agent", "TamilBibleApp/1.0");
                    InputStream in = hc.getInputStream();
                    FileOutputStream out = new FileOutputStream(f);
                    byte[] buf = new byte[16384];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    out.close(); in.close();
                } catch (Exception ignored) {
                } finally {
                    INFLIGHT.remove(url);
                }
                Bitmap bm = decode(f);
                if (bm != null) MEM.put(url, bm);
                MAIN.post(() -> {
                    if (bm != null && iv.isAttachedToWindow()) iv.setImageBitmap(bm);
                });
            });
            return;
        }
        POOL.execute(() -> {
            Bitmap bm = decode(f);
            if (bm != null) MEM.put(url, bm);
            MAIN.post(() -> {
                if (bm != null && iv.isAttachedToWindow()) iv.setImageBitmap(bm);
            });
        });
    }

    /** Downloads (if needed) and returns a local file for sharing; null when offline/failed. */
    public static File fetchLocal(Context c, String url) {
        boolean store = Ui.prefs().getBoolean("store_media", false);
        File f = targetFile(c, url, store);
        if (f.exists() && f.length() > 5000) return f;
        if (!online(c)) return null;
        try {
            HttpURLConnection hc = (HttpURLConnection) new URL(url).openConnection();
            hc.setConnectTimeout(15000);
            hc.setReadTimeout(30000);
            hc.setRequestProperty("User-Agent", "TamilBibleApp/1.0");
            InputStream in = hc.getInputStream();
            FileOutputStream out = new FileOutputStream(f);
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.close(); in.close();
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap decode(File f) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getPath(), o);
            o = new BitmapFactory.Options();
            o.inSampleSize = Math.max(1, Math.max(o.outWidth, o.outHeight) / 1400);
            return BitmapFactory.decodeFile(f.getPath(), o);
        } catch (Exception e) {
            return null;
        }
    }

    private static String sha(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }
}
