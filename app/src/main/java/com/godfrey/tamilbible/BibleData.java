package com.godfrey.tamilbible;

import android.content.Context;
import android.util.JsonReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** One full Tamil Bible text (66 books, 1189 chapters) loaded from an assets JSON. */
public class BibleData {
    public final String id, label, shortLabel;
    public final String[] names;          // 66 canonical book names
    public final String[] shortNames;     // abbreviations for dense grids
    public final boolean[] nt;            // true if New Testament
    public final String[][][] verses;     // book -> chapter -> verse text ("" = numbering gap)
    public final int[] chapterCount;
    private final HashMap<String, Integer> nameIdx = new HashMap<>();
    private final HashMap<String, HashMap<String, String>> psalmTitles;

    /** Verse-of-the-day refs as {book, chapter, verse} — 146 hand-picked verses. */
    private static final int[][] VOTD = {
        {0,1,1},{0,1,27},{1,14,14},{1,20,12},{4,6,5},
        {4,31,6},{5,1,9},{8,16,7},{15,8,10},{17,19,25},
        {18,1,1},{18,3,5},{18,16,8},{18,19,1},{18,23,1},
        {18,23,4},{18,27,1},{18,27,14},{18,34,8},{18,37,4},
        {18,46,1},{18,46,10},{18,51,10},{18,56,3},{18,100,4},
        {18,118,24},{18,119,105},{18,121,1},{18,121,2},{18,139,14},
        {19,3,5},{19,3,6},{19,16,3},{19,18,10},{20,3,1},
        {22,6,8},{22,26,4},{22,40,31},{22,41,10},{22,53,5},
        {22,55,6},{23,29,11},{23,33,3},{24,3,22},{24,3,23},
        {25,36,26},{26,12,3},{32,6,8},{37,4,6},{38,3,6},
        {39,5,16},{39,6,33},{39,6,34},{39,11,28},{39,11,29},
        {39,22,37},{39,22,39},{39,28,19},{40,16,15},{41,1,37},
        {41,6,31},{41,6,38},{41,9,23},{41,15,7},{41,24,49},
        {42,1,1},{42,1,14},{42,3,3},{42,3,16},{42,3,17},
        {42,8,12},{42,8,32},{42,10,10},{42,11,25},{42,14,6},
        {42,14,27},{42,15,5},{42,16,33},{43,1,8},{43,16,31},
        {44,1,16},{44,3,23},{44,5,8},{44,6,23},{44,8,1},
        {44,8,28},{44,8,31},{44,10,9},{44,10,17},{44,12,1},
        {44,12,2},{45,10,13},{45,13,4},{45,13,13},{45,15,58},
        {46,5,17},{46,5,21},{46,12,9},{47,2,20},{47,5,22},
        {48,2,8},{48,2,9},{48,4,32},{48,6,10},{49,4,6},
        {49,4,7},{49,4,13},{49,4,19},{50,3,2},{50,3,23},
        {51,4,3},{51,5,16},{51,5,17},{51,5,18},{52,3,3},
        {53,4,12},{54,1,7},{54,3,16},{55,2,11},{56,1,6},
        {57,4,12},{57,4,16},{57,11,1},{57,12,1},{57,12,2},
        {57,13,8},{58,1,2},{58,1,5},{58,1,22},{58,4,7},
        {58,5,16},{59,2,9},{59,3,15},{59,4,8},{59,5,7},
        {60,1,3},{60,3,9},{61,1,9},{61,2,15},{61,3,1},
        {61,4,8},{61,4,19},{61,5,13},{64,1,24},{65,3,20},
        {65,21,4}
    };

    private BibleData(String id, String label, String shortLabel, String[] names, String[] shortNames,
                      boolean[] nt, String[][][] verses, int[] chapterCount,
                      HashMap<String, HashMap<String, String>> psalmTitles) {
        this.id = id; this.label = label; this.shortLabel = shortLabel;
        this.names = names; this.shortNames = shortNames; this.nt = nt;
        this.verses = verses; this.chapterCount = chapterCount;
        this.psalmTitles = psalmTitles;
        for (int i = 0; i < names.length; i++) nameIdx.put(names[i], i);
    }

    public int bookCount() { return names.length; }

    public String text(int book, int chapter, int verse) {
        return verses[book][chapter - 1][verse - 1];
    }

    public int verseCount(int book, int chapter) { return verses[book][chapter - 1].length; }

    /** Psalm superscription (or Psalm 119 acrostic header) printed above a verse, or null. */
    public String psalmTitle(int chapter, int verse) {
        HashMap<String, String> m = psalmTitles.get(String.valueOf(chapter));
        return m == null ? null : m.get(String.valueOf(verse));
    }

    public int[] verseOfTheDay() {
        int[] r = VOTD[(int) (System.currentTimeMillis() / 86400000L) % VOTD.length];
        // step forward if a translation numbers that verse differently ("" gap)
        for (int i = 0; i < 5; i++) {
            String t = verses[r[0]][r[1] - 1][r[2] - 1];
            if (t != null && !t.isEmpty()) break;
            r[2] = r[2] + 1;
            if (r[2] > verses[r[0]][r[1] - 1].length) { r[1] = Math.min(r[1] + 1, chapterCount[r[0]]); r[2] = 1; }
        }
        return r;
    }

    public List<int[]> search(String query, int limit) {
        String q = query.trim().toLowerCase();
        List<int[]> out = new ArrayList<>();
        if (q.isEmpty()) return out;
        for (int b = 0; b < verses.length && out.size() < limit; b++)
            for (int c = 0; c < verses[b].length && out.size() < limit; c++)
                for (int v = 0; v < verses[b][c].length && out.size() < limit; v++)
                    if (!verses[b][c][v].isEmpty() && verses[b][c][v].toLowerCase().contains(q))
                        out.add(new int[]{b, c + 1, v + 1});
        return out;
    }

    public static BibleData load(Context ctx, String asset, String id, String label, String shortLabel) throws Exception {
        try (InputStream in = ctx.getAssets().open(asset)) {
            return load(in, id, label, shortLabel);
        }
    }

    public static BibleData load(InputStream in, String id, String label, String shortLabel) throws Exception {
        try (JsonReader r = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String[] names = null, shorts = null;
            boolean[] nt = null;
            String[][][] verses = null;
            HashMap<String, HashMap<String, String>> titles = new HashMap<>();
            r.beginObject();
            while (r.hasNext()) {
                String key = r.nextName();
                if ("books".equals(key)) {
                    List<String> nList = new ArrayList<>(), sList = new ArrayList<>();
                    List<Boolean> tList = new ArrayList<>();
                    List<String[][]> bList = new ArrayList<>();
                    r.beginArray();
                    while (r.hasNext()) {
                        String name = null, shortN = null; boolean isNt = false; String[][] chs = null;
                        r.beginObject();
                        while (r.hasNext()) {
                            String k = r.nextName();
                            switch (k) {
                                case "n": name = r.nextString(); break;
                                case "s": shortN = r.nextString(); break;
                                case "t": isNt = r.nextInt() == 1; break;
                                case "c":
                                    List<String[]> chsL = new ArrayList<>();
                                    r.beginArray();
                                    while (r.hasNext()) {
                                        List<String> vsL = new ArrayList<>();
                                        r.beginArray();
                                        while (r.hasNext()) vsL.add(r.nextString());
                                        r.endArray();
                                        chsL.add(vsL.toArray(new String[0]));
                                    }
                                    r.endArray();
                                    chs = chsL.toArray(new String[0][]);
                                    break;
                                default: r.skipValue();
                            }
                        }
                        r.endObject();
                        nList.add(name); sList.add(shortN == null || shortN.isEmpty() ? name : shortN);
                        tList.add(isNt); bList.add(chs);
                    }
                    r.endArray();
                    names = nList.toArray(new String[0]);
                    shorts = sList.toArray(new String[0]);
                    nt = new boolean[tList.size()];
                    for (int i = 0; i < tList.size(); i++) nt[i] = tList.get(i);
                    verses = bList.toArray(new String[0][][]);
                } else if ("ptitles".equals(key)) {
                    r.beginObject();
                    while (r.hasNext()) {
                        String ch = r.nextName();
                        HashMap<String, String> m = new HashMap<>();
                        r.beginObject();
                        while (r.hasNext()) m.put(r.nextName(), r.nextString());
                        r.endObject();
                        titles.put(ch, m);
                    }
                    r.endObject();
                } else r.skipValue();
            }
            r.endObject();
            int[] cc = new int[verses.length];
            for (int i = 0; i < verses.length; i++) cc[i] = verses[i].length;
            return new BibleData(id, label, shortLabel, names, shorts, nt, verses, cc, titles);
        }
    }
}
