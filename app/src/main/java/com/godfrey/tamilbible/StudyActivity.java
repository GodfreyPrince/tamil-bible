package com.godfrey.tamilbible;

import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

/** Verse study screen — Tamil notes, Q&A, word alignment, dictionary, theology.
 *  Data comes from the offline pack (if downloaded) or the study server. */
public class StudyActivity extends BaseActivity {

    private int book, chapter, verse;
    private LinearLayout body;
    private TextView tvSource;
    private boolean dark;
    private String loadedFrom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        super.onCreate(savedInstanceState);
        dark = Ui.dark(this);
        book = getIntent().getIntExtra("book", 0);
        chapter = getIntent().getIntExtra("chapter", 1);
        verse = getIntent().getIntExtra("verse", 1);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.attr(this, android.R.attr.colorPrimary));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(4), 0, dp(12), 0);
        bar.setMinimumHeight(dp(56));
        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_back);
        back.setPadding(dp(10), dp(10), dp(10), dp(10));
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView title = new TextView(this);
        title.setText(Ui.ref(App.current(), book, chapter, verse));
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Ui.attr(this, R.color.toolbar_text));
        title.setPadding(dp(8), 0, 0, 0);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView overflow = new TextView(this);
        overflow.setText("\u22EE");
        overflow.setTextSize(20);
        overflow.setTextColor(Ui.attr(this, R.color.toolbar_text));
        overflow.setPadding(dp(12), dp(4), dp(12), dp(4));
        overflow.setOnClickListener(v -> showOverflow());
        bar.addView(overflow);
        root.addView(bar);

        tvSource = new TextView(this);
        tvSource.setTextSize(12);
        tvSource.setTextColor(Ui.attr(this, android.R.attr.textColorSecondary));
        tvSource.setPadding(dp(20), dp(4), dp(20), dp(8));
        root.addView(tvSource);

        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(4), dp(20), dp(30));
        scroll.addView(body);
        scroll.setBackground(new android.graphics.drawable.GradientDrawable());
        scroll.setBackgroundColor(Ui.attr(this, android.R.attr.windowBackground));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        Ui.edgeToEdge(this);

        load();
    }

    private void load() {
        body.removeAllViews();
        body.addView(loading());
        new Thread(() -> {
            JSONObject payload = null;
            String from = null;
            String fetchError = null;
            if (Study.hasPack(this)) {
                try {
                    payload = loadFromPack();
                    from = getString(R.string.study_source_pack);
                } catch (Exception e) {
                    android.util.Log.e("Study", "pack load failed", e);
                }
            }
            if (payload == null && !Study.serverUrl().isEmpty()) {
                try {
                    payload = Study.getJson(Study.trim(Study.serverUrl())
                            + "/api/verse?ref=" + Study.ref(book, chapter, verse));
                    from = getString(R.string.study_source_server);
                } catch (Exception e) {
                    android.util.Log.e("Study", "verse fetch failed", e);
                    fetchError = String.valueOf(e);
                }
            }
            final JSONObject data = payload;
            final String src = from;
            final String err = fetchError == null ? "" : fetchError;
            runOnUiThread(() -> {
                if (isFinishing()) return;
                if (data == null) {
                    loadedFrom = null;
                    if (err.contains("401")) tvSource.setText(R.string.study_key_rejected);
                    else if (err.contains("429")) tvSource.setText(R.string.study_rate);
                    else tvSource.setText(R.string.study_source_none);
                    renderError();
                } else {
                    loadedFrom = src;
                    tvSource.setText(src);
                    render(data);
                }
            });
        }).start();
    }

    /** Build the same payload shape as the server from the offline pack. */
    private org.json.JSONObject loadFromPack() throws Exception {
        String code = Study.CODES[book];
        org.json.JSONObject out = new org.json.JSONObject();
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(
                Study.packFile(this).getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
            org.json.JSONArray words = new org.json.JSONArray();
            java.util.List<String> strongs = new java.util.ArrayList<>();
            Cursor c = db.rawQuery(
                "SELECT a.tamil_word, a.strongs, a.lemma, g.gloss_tamil FROM ta_word_alignment a " +
                "LEFT JOIN ta_gloss g ON g.strongs = a.strongs " +
                "WHERE a.book=? AND a.chapter=? AND a.verse=? AND a.strongs != '' ORDER BY a.seq",
                new String[]{code, String.valueOf(chapter), String.valueOf(verse)});
            while (c.moveToNext()) {
                org.json.JSONObject w = new org.json.JSONObject();
                w.put("tamil_word", c.getString(0));
                w.put("strongs", c.getString(1));
                w.put("lemma", c.getString(2) == null ? "" : c.getString(2));
                w.put("gloss_tamil", c.getString(3) == null ? "" : c.getString(3));
                words.put(w);
                if (!strongs.contains(c.getString(1))) strongs.add(c.getString(1));
            }
            c.close();
            out.put("words", words);

            org.json.JSONArray notes = new org.json.JSONArray();
            c = db.rawQuery(
                "SELECT phrase, body FROM ta_notes WHERE book=? AND chapter=? AND verse=? ORDER BY id",
                new String[]{code, String.valueOf(chapter), String.valueOf(verse)});
            while (c.moveToNext()) {
                org.json.JSONObject n = new org.json.JSONObject();
                n.put("phrase", c.getString(0));
                n.put("body", c.getString(1));
                notes.put(n);
            }
            c.close();
            out.put("notes", notes);

            org.json.JSONArray questions = new org.json.JSONArray();
            c = db.rawQuery(
                "SELECT question, answer FROM ta_questions WHERE book=? AND chapter=? AND verse=? ORDER BY id",
                new String[]{code, String.valueOf(chapter), String.valueOf(verse)});
            while (c.moveToNext()) {
                org.json.JSONObject q = new org.json.JSONObject();
                q.put("question", c.getString(0));
                q.put("answer", c.getString(1));
                questions.put(q);
            }
            c.close();
            out.put("questions", questions);

            org.json.JSONArray dict = new org.json.JSONArray();
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (String sNum : strongs) {
                Cursor d = db.rawQuery(
                    "SELECT title, body FROM ta_dictionary WHERE strongs LIKE ? LIMIT 3",
                    new String[]{"%\"" + sNum + "\"%"});
                while (d.moveToNext()) {
                    String title = d.getString(0);
                    if (seen.add(title)) {
                        org.json.JSONObject a = new org.json.JSONObject();
                        a.put("title", title);
                        a.put("body", d.getString(1));
                        dict.put(a);
                    }
                }
                d.close();
                if (dict.length() >= 6) break;
            }
            out.put("dictionary", dict);

            Cursor i = db.rawQuery(
                "SELECT heading, paras FROM book_intros WHERE book=? LIMIT 1", new String[]{code});
            if (i.moveToFirst()) {
                org.json.JSONObject in = new org.json.JSONObject();
                in.put("heading", i.getString(0) == null ? "" : i.getString(0));
                in.put("paras", i.getString(1) == null ? "" : i.getString(1));
                out.put("intro", in);
            }
            i.close();

            org.json.JSONArray theo = new org.json.JSONArray();
            c = db.rawQuery(
                "SELECT tc.title_ta, tc.summary_ta, tc.detail_ta, tc.source_author FROM theology_verse_index vi " +
                "JOIN theology_content tc ON tc.id = vi.content_id " +
                "WHERE vi.book=? AND vi.chapter=? AND vi.verse=? ORDER BY vi.relevance DESC LIMIT 5",
                new String[]{code, String.valueOf(chapter), String.valueOf(verse)});
            while (c.moveToNext()) {
                org.json.JSONObject t = new org.json.JSONObject();
                t.put("title_ta", c.getString(0));
                t.put("summary_ta", c.getString(1) == null ? "" : c.getString(1));
                t.put("detail_ta", c.getString(2) == null ? "" : c.getString(2));
                t.put("source_author", c.getString(3) == null ? "" : c.getString(3));
                theo.put(t);
            }
            c.close();
            out.put("theology", theo);

            org.json.JSONArray ane = new org.json.JSONArray();
            c = db.rawQuery(
                "SELECT ae.title, ae.title_ta, ae.summary_ta FROM ane_book_mappings m " +
                "JOIN ane_entries ae ON ae.id = m.entry_id WHERE m.book=? AND " +
                "(m.chapter_start IS NULL OR ? >= m.chapter_start) AND " +
                "(m.chapter_end IS NULL OR ? <= m.chapter_end) LIMIT 3",
                new String[]{code, String.valueOf(chapter), String.valueOf(chapter)});
            while (c.moveToNext()) {
                org.json.JSONObject a = new org.json.JSONObject();
                a.put("title", c.getString(0));
                a.put("title_ta", c.getString(1) == null ? "" : c.getString(1));
                a.put("summary_ta", c.getString(2) == null ? "" : c.getString(2));
                ane.put(a);
            }
            c.close();
            out.put("ane", ane);
        }
        return out;
    }

    private View loading() {
        TextView t = new TextView(this);
        t.setText(R.string.loading);
        t.setTextSize(14);
        t.setTextColor(Ui.attr(this, android.R.attr.textColorSecondary));
        t.setPadding(0, dp(30), 0, 0);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private void renderError() {
        body.removeAllViews();
        TextView t = new TextView(this);
        t.setText(R.string.study_none);
        t.setTextSize(14);
        t.setTextColor(Ui.attr(this, android.R.attr.textColorSecondary));
        t.setPadding(0, dp(24), 0, 0);
        body.addView(t);
        body.addView(linkBtn(getString(R.string.study_download_btn), v -> downloadPack()));
    }

    private void render(JSONObject d) {
        body.removeAllViews();
        boolean any = false;
        any |= renderWords(d);
        any |= renderArray(d, "notes", R.string.study_notes, "phrase", "body");
        any |= renderArray(d, "questions", R.string.study_questions, "question", "answer");
        any |= renderDict(d);
        any |= renderIntro(d);
        any |= renderTaArticles(d, "theology", R.string.study_theology, "title_ta", "summary_ta", "detail_ta", "source_author");
        any |= renderTaArticles(d, "ane", R.string.study_ane, "title_ta", "summary_ta", null, null);
        if (!any) {
            TextView t = new TextView(this);
            t.setText(R.string.study_none_verse);
            t.setTextSize(14);
            t.setTextColor(Ui.attr(this, android.R.attr.textColorSecondary));
            t.setPadding(0, dp(24), 0, 0);
            body.addView(t);
            if (loadedFrom != null && loadedFrom.equals(getString(R.string.study_source_server)))
                body.addView(linkBtn(getString(R.string.study_download_btn), v -> downloadPack()));
        }
    }

    // ---------- section renderers ----------

    private boolean renderWords(JSONObject d) {
        JSONArray arr = d.optJSONArray("words");
        if (arr == null || arr.length() == 0) return false;
        sectionTitle(getString(R.string.study_words));
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject w = arr.optJSONObject(i);
            if (w == null) continue;
            String strongs = w.optString("strongs", "");
            if (strongs.isEmpty()) continue;
            String gloss = w.optString("gloss_tamil", "");
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));
            TextView word = new TextView(this);
            word.setText(w.optString("tamil_word", "") + "  ·  " + w.optString("lemma", ""));
            word.setTextSize(15);
            word.setTypeface(Typeface.SERIF);
            word.setTextColor(Ui.attr(this, android.R.attr.textColorPrimary));
            word.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(word);
            TextView g = new TextView(this);
            g.setText(gloss);
            g.setTextSize(13);
            g.setTextColor(0xFFE8A13D);
            row.addView(g);
            final String s = strongs, tw = w.optString("tamil_word", ""), lm = w.optString("lemma", "");
            row.setOnClickListener(v -> showWord(s, tw, lm));
            wrap.addView(row);
        }
        card(wrap);
        return true;
    }

    private boolean renderArray(JSONObject d, String key, int titleRes, String headKey, String bodyKey) {
        JSONArray arr = d.optJSONArray(key);
        if (arr == null || arr.length() == 0) return false;
        sectionTitle(getString(titleRes));
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String head = o.optString(headKey, "");
            String bodyText = o.optString(bodyKey, "").replace(" \n\n ", "\n\n");
            if (!head.isEmpty()) {
                TextView h = new TextView(this);
                h.setText(head);
                h.setTextSize(14);
                h.setTypeface(Typeface.DEFAULT_BOLD);
                h.setTextColor(0xFFE8A13D);
                h.setPadding(0, dp(8), 0, dp(2));
                wrap.addView(h);
            }
            TextView b = new TextView(this);
            b.setText(bodyText);
            b.setTextSize(14);
            b.setTypeface(Typeface.SERIF);
            b.setLineSpacing(0, 1.25f);
            b.setTextColor(Ui.attr(this, android.R.attr.textColorPrimary));
            wrap.addView(b);
        }
        card(wrap);
        return true;
    }

    private boolean renderDict(JSONObject d) {
        JSONArray arr = d.optJSONArray("dictionary");
        if (arr == null || arr.length() == 0) return false;
        sectionTitle(getString(R.string.study_dictionary));
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            TextView h = new TextView(this);
            h.setText(o.optString("title", ""));
            h.setTextSize(14);
            h.setTypeface(Typeface.DEFAULT_BOLD);
            h.setTextColor(0xFFE8A13D);
            h.setPadding(0, dp(8), 0, dp(2));
            wrap.addView(h);
            TextView b = new TextView(this);
            b.setText(o.optString("body", "").replace(" \n\n ", "\n\n"));
            b.setTextSize(14);
            b.setTypeface(Typeface.SERIF);
            b.setLineSpacing(0, 1.25f);
            b.setTextColor(Ui.attr(this, android.R.attr.textColorPrimary));
            wrap.addView(b);
        }
        card(wrap);
        return true;
    }

    private boolean renderIntro(JSONObject d) {
        JSONObject in = d.optJSONObject("intro");
        if (in == null) return false;
        String head = in.optString("heading", "");
        String paras = in.optString("paras", "");
        String text = !head.isEmpty() ? head + "\n\n" + paras : paras;
        if (text.trim().isEmpty()) return false;
        sectionTitle(getString(R.string.study_intro));
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextSize(14);
        b.setTypeface(Typeface.SERIF);
        b.setLineSpacing(0, 1.25f);
        b.setTextColor(Ui.attr(this, android.R.attr.textColorPrimary));
        wrap.addView(b);
        card(wrap);
        return true;
    }

    /** theology / ane articles: Tamil-first fields. */
    private boolean renderTaArticles(JSONObject d, String key, int titleRes,
                                     String tKey, String sKey, String dKey, String authorKey) {
        JSONArray arr = d.optJSONArray(key);
        if (arr == null || arr.length() == 0) return false;
        sectionTitle(getString(titleRes));
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String t = o.optString(tKey, "");
            if (t.isEmpty()) t = o.optString("title", "");
            if (t.isEmpty()) continue;
            TextView h = new TextView(this);
            h.setText(t);
            h.setTextSize(14);
            h.setTypeface(Typeface.DEFAULT_BOLD);
            h.setTextColor(0xFFE8A13D);
            h.setPadding(0, dp(8), 0, dp(2));
            wrap.addView(h);
            String author = authorKey == null ? "" : o.optString(authorKey, "");
            if (!author.isEmpty()) {
                TextView a = new TextView(this);
                a.setText(author);
                a.setTextSize(12);
                a.setTextColor(Ui.attr(this, android.R.attr.textColorSecondary));
                wrap.addView(a);
            }
            String s = o.optString(sKey, "");
            String det = dKey == null ? "" : o.optString(dKey, "");
            String all = (s + (det.isEmpty() ? "" : "\n\n" + det)).trim();
            if (!all.isEmpty()) {
                TextView b = new TextView(this);
                b.setText(all);
                b.setTextSize(14);
                b.setTypeface(Typeface.SERIF);
                b.setLineSpacing(0, 1.25f);
                b.setTextColor(Ui.attr(this, android.R.attr.textColorPrimary));
                wrap.addView(b);
            }
        }
        card(wrap);
        return true;
    }

    // ---------- word detail ----------

    private void showWord(String strongs, String tamilWord, String lemma) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(20), dp(14), dp(20), dp(10));
        TextView head = new TextView(this);
        head.setText(tamilWord + (lemma.isEmpty() ? "" : "  ·  " + lemma));
        head.setTextSize(17);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        head.setTextColor(Ui.attr(this, android.R.attr.textColorPrimary));
        wrap.addView(head);
        TextView code = new TextView(this);
        code.setText(strongs);
        code.setTextSize(12);
        code.setTextColor(Ui.attr(this, android.R.attr.textColorSecondary));
        wrap.addView(code);
        TextView status = new TextView(this);
        status.setText(R.string.loading);
        status.setTextSize(13);
        status.setTextColor(Ui.attr(this, android.R.attr.textColorSecondary));
        status.setPadding(0, dp(10), 0, 0);
        wrap.addView(status);

        final AlertDialog[] holder = new AlertDialog[1];
        AlertDialog dlg = new AlertDialog.Builder(this).create();
        dlg.setView(wrap);
        dlg.show();
        holder[0] = dlg;

        new Thread(() -> {
            String gloss = null;
            String renderings = null;
            String def = null;
            // offline pack first
            if (Study.hasPack(this)) {
                try (SQLiteDatabase pdb = SQLiteDatabase.openDatabase(
                        Study.packFile(this).getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                    Cursor c = pdb.rawQuery(
                            "SELECT gloss_tamil, renderings FROM ta_gloss WHERE strongs=?",
                            new String[]{strongs});
                    if (c.moveToFirst()) {
                        gloss = c.getString(0);
                        renderings = c.getString(1);
                    }
                    c.close();
                } catch (Exception ignored) { }
            }
            if (gloss == null && !Study.serverUrl().isEmpty()) {
                try {
                    JSONObject w = Study.getJson(Study.trim(Study.serverUrl())
                            + "/api/word?strongs=" + strongs);
                    gloss = w.optString("gloss_tamil", null);
                    JSONArray r = w.optJSONArray("renderings");
                    if (r != null) renderings = r.toString();
                    def = w.optString("short_definition", null);
                } catch (Exception ignored) { }
            }
            final String fGloss = gloss, fRend = renderings, fDef = def;
            runOnUiThread(() -> {
                if (isFinishing() || holder[0] == null || !holder[0].isShowing()) return;
                status.setVisibility(View.GONE);
                if (fDef != null && !fDef.isEmpty()) {
                    TextView d = new TextView(this);
                    d.setText(fDef);
                    d.setTextSize(13);
                    d.setTextColor(Ui.attr(this, android.R.attr.textColorSecondary));
                    d.setPadding(0, dp(8), 0, 0);
                    wrap.addView(d);
                }
                if (fGloss != null && !fGloss.isEmpty()) {
                    TextView g = new TextView(this);
                    g.setText(getString(R.string.study_gloss) + " " + fGloss);
                    g.setTextSize(15);
                    g.setTypeface(Typeface.DEFAULT_BOLD);
                    g.setTextColor(0xFFE8A13D);
                    g.setPadding(0, dp(8), 0, 0);
                    wrap.addView(g);
                }
                if (fRend != null && !fRend.equals("[]") && !fRend.isEmpty()) {
                    TextView r = new TextView(this);
                    r.setText(getString(R.string.study_renderings) + " " +
                            fRend.replace("[\"", "").replace("\"]", "").replace("\",\"", " · ").replace("\\\"", "\""));
                    r.setTextSize(13);
                    r.setTextColor(Ui.attr(this, android.R.attr.textColorSecondary));
                    r.setPadding(0, dp(6), 0, dp(8));
                    wrap.addView(r);
                }
            });
        }).start();
    }

    // ---------- overflow / pack ----------

    private void showOverflow() {
        String[] items;
        if (Study.hasPack(this)) {
            items = new String[]{getString(R.string.study_delete_pack), getString(R.string.study_server_url)};
        } else {
            items = new String[]{getString(R.string.study_download_btn), getString(R.string.study_server_url)};
        }
        new AlertDialog.Builder(this)
                .setItems(items, (dlg, which) -> {
                    if (which == 0) {
                        if (Study.hasPack(this)) {
                            Study.deletePack(this);
                            toast(getString(R.string.study_pack_deleted));
                            load();
                        } else downloadPack();
                    } else editUrl();
                }).show();
    }

    private void downloadPack() {
        if (Study.serverUrl().isEmpty()) {
            editUrl();
            return;
        }
        toast(getString(R.string.study_downloading));
        new Thread(() -> {
            try {
                Study.downloadPack(this, Study.serverUrl());
                runOnUiThread(() -> {
                    toast(getString(R.string.study_downloaded));
                    load();
                });
            } catch (Exception e) {
                final String em = String.valueOf(e);
                runOnUiThread(() -> toast(getString(em.contains("401") ? R.string.study_key_rejected
                        : em.contains("429") ? R.string.study_rate : R.string.study_error)));
            }
        }).start();
    }

    private void editUrl() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(6), pad, 0);
        final EditText etUrl = new EditText(this);
        etUrl.setText(Study.serverUrl());
        etUrl.setHint(R.string.study_url_hint);
        etUrl.setSingleLine(true);
        box.addView(etUrl);
        final EditText etKey = new EditText(this);
        etKey.setText(Study.apiKey());
        etKey.setHint(R.string.study_key_hint);
        etKey.setSingleLine(true);
        box.addView(etKey);
        new AlertDialog.Builder(this)
                .setTitle(R.string.study_server_url)
                .setView(box)
                .setPositiveButton(R.string.done, (d, w) -> {
                    Ui.prefs().edit()
                            .putString("study_url", Study.trim(etUrl.getText().toString()))
                            .putString("study_key", etKey.getText().toString().trim())
                            .apply();
                    toast(getString(R.string.study_server_set));
                    load();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ---------- shared ui bits ----------

    private void sectionTitle(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setLetterSpacing(0.12f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(0xFFE8A13D);
        t.setPadding(0, dp(18), 0, 0);
        body.addView(t);
    }

    private void card(View content) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(dark ? 0xFF242220 : 0xFFFFFFFF);
        c.setBackground(bg);
        c.setPadding(dp(14), dp(6), dp(14), dp(10));
        c.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(8), 0, 0);
        c.setLayoutParams(lp);
        body.addView(c);
    }

    private View linkBtn(String label, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(14);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(0xFF5B8DEF);
        t.setPadding(0, dp(18), 0, 0);
        t.setOnClickListener(l);
        return t;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
