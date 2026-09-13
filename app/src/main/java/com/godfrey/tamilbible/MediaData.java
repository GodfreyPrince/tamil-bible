package com.godfrey.tamilbible;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Curated Biblical art loaded from assets/media_manifest.json (metadata offline, images online). */
public class MediaData {
    public static class Item {
        public String id, title, artist, license, source, srcPage, img, era;
        public String taTitle, taDesc, taEra;
        public int year = -1;
        public List<String> scenes = new ArrayList<>();
        public List<int[]> refs = new ArrayList<>();   // {book, chapter}
        public List<String> refLabels = new ArrayList<>();
    }

    public interface Ready { void onReady(MediaData md); }

    private static volatile MediaData inst;
    private final List<Item> all = new ArrayList<>();
    private final HashMap<String, List<Item>> byScene = new HashMap<>();
    private final HashMap<Long, List<Item>> byChapter = new HashMap<>();
    private final List<String> sceneOrder = new ArrayList<>();

    public static void get(Context c, Ready cb) {
        if (inst != null) { cb.onReady(inst); return; }
        new Thread(() -> {
            try {
                MediaData md = new MediaData();
                md.load(c.getAssets().open("media_manifest.json"));
                inst = md;
            } catch (Exception e) {
                android.util.Log.e("TamilBible", "media manifest failed", e);
            }
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(() -> cb.onReady(inst));
        }, "media-load").start();
    }

    public List<Item> all() { return all; }

    public List<Item> forScene(String s) {
        List<Item> l = byScene.get(s);
        return l == null ? new ArrayList<>() : l;
    }

    /** Art linked to a bible chapter (any of its refs). */
    public List<Item> forChapter(int book, int chapter) {
        List<Item> l = byChapter.get(book * 1000L + chapter);
        return l == null ? new ArrayList<>() : l;
    }

    /** Scene slugs in stable first-seen order. */
    public List<String> scenes() { return sceneOrder; }

    public static String sceneTaLabel(String slug, List<Item> items) {
        for (Item it : items)
            if (it.scenes.contains(slug) && it.taTitle != null && !it.taTitle.isEmpty())
                return it.taTitle;
        return slug;
    }

    private static String opt(JSONObject o, String k) {
        return o.isNull(k) ? null : o.optString(k, null);
    }

    private void load(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        in.close();
        JSONObject root = new JSONObject(bo.toString("UTF-8"));
        JSONArray art = root.getJSONArray("art");
        for (int i = 0; i < art.length(); i++) {
            JSONObject o = art.getJSONObject(i);
            Item it = new Item();
            it.id = opt(o, "id");
            it.title = opt(o, "title");
            it.artist = opt(o, "artist");
            it.license = opt(o, "license");
            it.source = opt(o, "source");
            it.srcPage = opt(o, "srcPage");
            it.img = opt(o, "img");
            it.era = opt(o, "era");
            it.year = o.isNull("year") ? -1 : o.optInt("year", -1);
            JSONArray sc = o.optJSONArray("scenes");
            if (sc != null)
                for (int j = 0; j < sc.length(); j++) {
                    String s = sc.optString(j, null);
                    if (s == null) continue;
                    it.scenes.add(s);
                    List<Item> l = byScene.get(s);
                    if (l == null) { byScene.put(s, l = new ArrayList<>()); sceneOrder.add(s); }
                    l.add(it);
                }
            JSONObject ta = o.optJSONObject("ta");
            if (ta != null) {
                it.taTitle = opt(ta, "title");
                it.taDesc = opt(ta, "desc");
                it.taEra = opt(ta, "era");
            }
            JSONArray rf = o.optJSONArray("refs");
            if (rf != null)
                for (int j = 0; j < rf.length(); j++) {
                    JSONObject ro = rf.getJSONObject(j);
                    int b = ro.optInt("book", -1), c = ro.optInt("ch", -1);
                    String lab = opt(ro, "ref");
                    if (b < 0 || c <= 0) continue;
                    it.refs.add(new int[]{b, c});
                    it.refLabels.add(lab == null ? "" : lab);
                    long key = b * 1000L + c;
                    List<Item> l = byChapter.get(key);
                    if (l == null) byChapter.put(key, l = new ArrayList<>());
                    l.add(it);
                }
            all.add(it);
        }
    }
}
