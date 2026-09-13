package com.godfrey.tamilbible;

import java.util.ArrayList;
import java.util.List;

/** Built-in reading plans (chapter-level portions). */
public class PlanEngine {
    public static class Plan {
        public final String id, title, desc;
        public final int days;
        public final int[][][] portions; // day -> portions -> {book, chapter}
        public Plan(String id, String title, String desc, int[][][] portions) {
            this.id = id; this.title = title; this.desc = desc;
            this.days = portions.length; this.portions = portions;
        }
    }

    public static List<Plan> all(BibleData bd) {
        List<Plan> out = new ArrayList<>();
        out.add(new Plan("john21", "யோவான் நற்செய்தி — 21 நாட்கள்",
                "இயேசுவின் வாழ்க்கை வரலாறு — நாள் ஒன்றுக்கு ஒரு அத்தியாயம்", spread(bd, 42, 1, 21, 1)));
        out.add(new Plan("psalms30", "திருப்பாடல்கள் — 30 நாட்கள்",
                "நாள் ஒன்றுக்கு ஐந்து பாடல்கள் — வேண்டுதலும் ஸ்தோத்திரமும்", spread(bd, 18, 1, 150, 5)));
        out.add(new Plan("proverbs31", "நீதிமொழிகள் — 31 நாட்கள்",
                "நாள் ஒன்றுக்கு ஒரு அத்தியாய ஞானம்", spread(bd, 19, 1, 31, 1)));
        out.add(new Plan("life30", "இயேசுவின் வாழ்க்கை — 30 நாட்கள்",
                "பிறப்பு முதல் உயிர்த்தெழுதல் வரை 30 முக்கிய அத்தியாயங்கள்", lifeOfJesus(bd)));
        out.add(new Plan("nt90", "புதிய ஏற்பாடு — 90 நாட்கள்",
                "மொத்தம் 260 அத்தியாயங்கள் சீரான வேகத்தில்", paced(bd, 39, 65, 90)));
        out.add(new Plan("bible365", "முழு பைபிள் — ஒரு வருடத்தில்",
                "ஆதியாகமம் முதல் வெளிப்படுத்துதல் வரை 365 நாட்களில்", paced(bd, 0, 65, 365)));
        return out;
    }

    private static int[][][] spread(BibleData bd, int book, int from, int to, int perDay) {
        List<int[][]> days = new ArrayList<>();
        List<int[]> cur = new ArrayList<>();
        for (int c = from; c <= to; c++) {
            cur.add(new int[]{book, c});
            if (cur.size() == perDay) { days.add(cur.toArray(new int[0][])); cur = new ArrayList<>(); }
        }
        if (!cur.isEmpty()) days.add(cur.toArray(new int[0][]));
        return days.toArray(new int[0][][]);
    }

    private static int[][][] lifeOfJesus(BibleData bd) {
        int[] chapters = {41, 41, 39, 40, 42, 41, 39, 39, 39, 41, 41, 42, 39, 41, 41, 42, 42, 40, 41, 42, 42, 42, 42, 42, 41, 42, 41, 42, 42, 39};
        List<int[][]> days = new ArrayList<>();
        for (int c : chapters) days.add(new int[][]{{c, 1}});
        return days.toArray(new int[0][][]);
    }

    /** Evenly pace chapters [fromBook..toBook] over days, weighting by verse count. */
    private static int[][][] paced(BibleData bd, int fromBook, int toBook, int days) {
        List<int[]> chs = new ArrayList<>();
        int totalVerses = 0;
        for (int b = fromBook; b <= toBook; b++)
            for (int c = 1; c <= bd.chapterCount[b]; c++) {
                chs.add(new int[]{b, c});
                totalVerses += bd.verseCount(b, c);
            }
        List<int[][]> out = new ArrayList<>();
        int idx = 0, cum = 0;
        for (int d = 1; d <= days && idx < chs.size(); d++) {
            int boundary = (int) Math.round((double) totalVerses * d / days);
            boolean last = (d == days);
            List<int[]> day = new ArrayList<>();
            while (idx < chs.size() && (day.isEmpty() || cum < boundary || last)) {
                int[] ch = chs.get(idx);
                day.add(ch);
                cum += bd.verseCount(ch[0], ch[1]);
                idx++;
            }
            if (!day.isEmpty()) out.add(day.toArray(new int[0][]));
        }
        return out.toArray(new int[0][][]);
    }

    public static String label(BibleData bd, int[][] portion) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < portion.length; i++) {
            if (i > 0) sb.append("; ");
            int b = portion[i][0], c = portion[i][1];
            int run = c;
            while (i + 1 < portion.length && portion[i + 1][0] == b && portion[i + 1][1] == run + 1) {
                i++; run++;
            }
            sb.append(bd.names[b]).append(" ").append(c);
            if (run > c) sb.append("\u2013").append(run);
        }
        return sb.toString();
    }
}
