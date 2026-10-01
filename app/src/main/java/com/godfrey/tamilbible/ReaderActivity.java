package com.godfrey.tamilbible;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
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

import java.util.HashMap;
import java.util.List;

public class ReaderActivity extends BaseActivity {

    private int book, chapter;
    private ScrollView scroll;
    private LinearLayout container;
    private TextView tvRef, tvPsalmTitle, btnVersion, btnMark;
    private final HashMap<Integer, TextView> verseViews = new HashMap<>();
    private int focusVerse = -1;

    private int selectedVerse = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String appliedTheme;
    private boolean chapterRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        appliedTheme = Ui.prefs().getString("theme", "system");
        super.onCreate(savedInstanceState);
        if (Ui.keepOn()) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.loading);

        book = getIntent().getIntExtra("book", Ui.prefs().getInt("last_b", 0));
        chapter = getIntent().getIntExtra("chapter", Ui.prefs().getInt("last_c", 1));
        focusVerse = getIntent().getIntExtra("verse", -1);

        App.get().whenReady(() -> { setContentView(R.layout.activity_reader); Ui.edgeToEdge(this); bind(); render(); });
    }

    private void bind() {
        scroll = findViewById(R.id.scroll);
        container = findViewById(R.id.verse_container);
        tvRef = findViewById(R.id.tv_ref);
        tvPsalmTitle = findViewById(R.id.tv_psalm_title);
        btnVersion = findViewById(R.id.btn_version);
        btnMark = findViewById(R.id.btn_mark);
        tvRef.setOnClickListener(v -> openBookPicker());
        btnVersion.setOnClickListener(v -> toggleVersion());
        findViewById(R.id.btn_search).setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        findViewById(R.id.btn_votd).setOnClickListener(v ->
                startActivity(new Intent(this, VotdActivity.class)));
        findViewById(R.id.btn_overflow).setOnClickListener(v -> openMenu());
        findViewById(R.id.btn_prev).setOnClickListener(v -> gotoChapter(book, chapter - 1));
        findViewById(R.id.btn_next).setOnClickListener(v -> gotoChapter(book, chapter + 1));
        btnMark.setOnClickListener(v -> toggleRead(true));
    }

    private void toggleVersion() {
        App.selectVersion(App.other().id);
        BibleData to = App.current();
        // chapter numbering is identical across the two texts, so stay in place
        btnVersion.setText(to.shortLabel);
        render();
        Toast.makeText(this, getString(R.string.version_switched, to.label), Toast.LENGTH_SHORT).show();
    }

    private void toggleRead(boolean toast) {
        chapterRead = !chapterRead;
        Db.get().setChapterRead(book, chapter, chapterRead);
        if (toast) {
            if (chapterRead)
                Toast.makeText(this, getString(R.string.chapter_done_toast, Db.get().readCount()), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, R.string.unmarked_toast, Toast.LENGTH_SHORT).show();
        }
        render();
    }

    private void openMenu() {
        android.widget.PopupMenu pm = new android.widget.PopupMenu(this, findViewById(R.id.btn_overflow));
        pm.getMenuInflater().inflate(R.menu.menu_reader, pm.getMenu());
        pm.getMenu().findItem(R.id.menu_mark).setTitle(
                chapterRead ? getString(R.string.unmark_read) : getString(R.string.mark_read));
        pm.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_books) openBookPicker();
            else if (id == R.id.menu_search) startActivity(new Intent(this, SearchActivity.class));
            else if (id == R.id.menu_votd) startActivity(new Intent(this, VotdActivity.class));
            else if (id == R.id.menu_plans) startActivity(new Intent(this, PlansActivity.class));
            else if (id == R.id.menu_progress) startActivity(new Intent(this, ProgressActivity.class));
            else if (id == R.id.menu_gallery) startActivity(new Intent(this, GalleryActivity.class));
            else if (id == R.id.menu_maps) startActivity(new Intent(this, MapsActivity.class));
            else if (id == R.id.menu_library) startActivity(new Intent(this, LibraryActivity.class));
            else if (id == R.id.menu_settings) startActivity(new Intent(this, SettingsActivity.class));
            else if (id == R.id.menu_mark) toggleRead(true);
            return true;
        });
        pm.show();
    }

    private void openBookPicker() {
        Intent i = new Intent(this, BookPickerActivity.class);
        i.putExtra("book", book);
        startActivityForResult(i, 1);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 1 && res == RESULT_OK && data != null) {
            book = data.getIntExtra("book", book);
            chapter = data.getIntExtra("chapter", chapter);
            focusVerse = data.getIntExtra("verse", -1);
            render();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String now = Ui.prefs().getString("theme", "system");
        if (!now.equals(appliedTheme)) { appliedTheme = now; recreate(); return; }
        if (container != null) {
            int y = scroll.getScrollY();
            render();
            scroll.post(() -> scroll.scrollTo(0, y));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Ui.prefs().edit().putInt("last_b", book).putInt("last_c", chapter).apply();
    }

    private void gotoChapter(int b, int c) {
        selectedVerse = -1;
        if (c < 1) {
            if (b == 0) { toast(getString(R.string.first_chapter)); return; }
            b--; c = App.current().chapterCount[b];
        }
        if (c > App.current().chapterCount[b]) {
            if (b == 65) { toast(getString(R.string.last_chapter)); return; }
            b++; c = 1;
        }
        book = b; chapter = c; focusVerse = -1;
        render();
        scroll.post(() -> scroll.smoothScrollTo(0, 0));
    }

    private void render() {
        BibleData bd = App.current();
        if (chapter > bd.chapterCount[book]) chapter = bd.chapterCount[book];
        chapterRead = Db.get().isChapterRead(book, chapter);
        btnVersion.setText(bd.shortLabel);
        tvRef.setText(Ui.chapterRef(bd, book, chapter));
        int fontSp = Ui.fontSp();
        int textCol = Ui.attr(this, android.R.attr.textColorPrimary);
        paintMarkButton();

        container.removeAllViews();
        verseViews.clear();
        tvPsalmTitle.setVisibility(View.GONE);

        for (int v = 1; v <= bd.verseCount(book, chapter); v++) {
            String body = bd.text(book, chapter, v);
            if (body.isEmpty()) continue;   // verse-numbering gap in this translation
            String title = (book == 18) ? bd.psalmTitle(chapter, v) : null;
            if (title != null) {
                TextView t = new TextView(this);
                t.setText(title + ".");
                t.setTextSize(14);
                t.setTypeface(Typeface.SERIF, Typeface.ITALIC);
                t.setTextColor(Ui.attr(this, android.R.attr.textColorSecondary));
                t.setPadding(0, dp(14), 0, dp(2));
                container.addView(t);
            }
            SpannableStringBuilder sb = new SpannableStringBuilder(String.valueOf(v) + "  " + body);
            sb.setSpan(new SuperscriptSpan(), 0, String.valueOf(v).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new RelativeSizeSpan(0.7f), 0, String.valueOf(v).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new StyleSpan(Typeface.BOLD), 0, String.valueOf(v).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(0xFFF57F17), 0, String.valueOf(v).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            String hl = Db.get().getHighlight(book, chapter, v);
            if (hl != null) sb.setSpan(new BackgroundColorSpan(Ui.hlBg(this, hl)), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (Db.get().isBookmarked(book, chapter, v))
                sb.setSpan(new UnderlineSpan(), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (v == selectedVerse)
                sb.setSpan(new UnderlineSpan(), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (Db.get().getNote(book, chapter, v) != null) {
                int at = sb.length();
                sb.append(" \u270E");
                sb.setSpan(new ForegroundColorSpan(0xFFF57F17), at, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new RelativeSizeSpan(0.75f), at, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            TextView tv = new TextView(this);
            tv.setText(sb);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSp);
            tv.setTypeface(Typeface.SERIF);
            tv.setLineSpacing(0, 1.25f);
            tv.setTextColor(textCol);
            tv.setPadding(0, dp(6), 0, dp(6));
            final int vv = v;
            tv.setOnClickListener(x -> {
                if (selectedVerse == vv) {
                    selectedVerse = -1;
                    render();
                    return;
                }
                selectedVerse = vv;
                render();
                showVerseActions(vv);
            });
            container.addView(tv);
            verseViews.put(v, tv);
        }

        container.addView(buildTrackerCard());
        final int b0 = book, c0 = chapter;
        MediaData.get(this, md -> {
            if (isFinishing() || md == null) return;
            List<MediaData.Item> arts = md.forChapter(b0, c0);
            if (!arts.isEmpty()) runOnUiThread(() -> {
                if (book == b0 && chapter == c0 && !isFinishing()) addArtStrip(arts);
            });
        });

        if (focusVerse > 0) {
            TextView tv = verseViews.get(focusVerse);
            if (tv != null) {
                scroll.post(() -> {
                    scroll.smoothScrollTo(0, Math.max(0, tv.getTop() - dp(70)));
                    tv.setBackgroundColor(0x33FFB300);
                    handler.postDelayed(() -> {
                        String h = Db.get().getHighlight(book, chapter, focusVerse);
                        tv.setBackgroundColor(h == null ? Color.TRANSPARENT : Ui.hlBg(this, h));
                    }, 1400);
                });
                focusVerse = -1;
            }
        }
    }

    /** Horizontal strip of curated art linked to this chapter. */
    private void addArtStrip(java.util.List<MediaData.Item> arts) {
        TextView h = new TextView(this);
        h.setText(R.string.media_in_chapter);
        h.setTextSize(13);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        h.setTextColor(0xFFE8A13D);
        h.setPadding(0, dp(26), 0, dp(8));
        container.addView(h);

        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        boolean dark = Ui.dark(this);
        int size = dp(120);
        for (final MediaData.Item it : arts) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackgroundColor(dark ? 0xFF242220 : 0xFFEFE9E0);
            ImageNet.load(this, it.img, iv, R.drawable.bg_list_item);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(0, 0, dp(8), 0);
            iv.setLayoutParams(lp);
            iv.setOnClickListener(v -> {
                Intent i = new Intent(this, ArtViewerActivity.class);
                i.putExtra("id", it.id);
                startActivity(i);
            });
            strip.addView(iv);
        }
        container.addView(strip);
    }

    /** Rounded end-of-chapter card marking the chapter as read / already completed. */
    private View buildTrackerCard() {
        boolean dark = Ui.dark(this);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(18), dp(14), dp(18), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(24));
        if (chapterRead) {
            bg.setColor(dark ? 0x26E8A13D : 0x1AE8A13D);
        } else {
            bg.setColor(dark ? 0x1FFFFFFF : 0xFFFFFFFF);
            bg.setStroke(dp(1), dark ? 0x33FFFFFF : 0x14E8A13D);
        }
        card.setBackground(bg);
        card.setOnClickListener(v -> toggleRead(true));

        ImageView check = new ImageView(this);
        check.setImageResource(R.drawable.ic_check);
        check.setColorFilter(chapterRead ? 0xFFE8A13D
                : (dark ? 0x66FFFFFF : 0x556D6259));
        check.setPadding(dp(2), dp(2), dp(2), dp(2));
        card.addView(check, new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView tv = new TextView(this);
        tv.setText(chapterRead ? R.string.completed_state : R.string.completed_card);
        tv.setTextSize(14);
        tv.setTypeface(chapterRead ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tv.setTextColor(chapterRead ? 0xFFE8A13D
                : (dark ? 0xFFA8A099 : 0xFF6D6259));
        tv.setPadding(dp(10), 0, 0, 0);
        card.addView(tv);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(22), 0, dp(4));
        card.setLayoutParams(lp);
        return card;
    }

    private void paintMarkButton() {
        boolean dark = Ui.dark(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        if (chapterRead) {
            bg.setColor(0xFFE8A13D);
            btnMark.setTextColor(0xFF3E2723);
        } else {
            bg.setColor(Color.TRANSPARENT);
            bg.setStroke(dp(2), 0x88FFFFFF);
            btnMark.setTextColor(0xB3FFFFFF);
        }
        btnMark.setBackground(bg);
        btnMark.setText("\u2713");
    }

    private void showVerseActions(int v) {
        BibleData bd = App.current();
        String ref = Ui.ref(bd, book, chapter, v);
        boolean bookmarked = Db.get().isBookmarked(book, chapter, v);
        String note = Db.get().getNote(book, chapter, v);
        String current = Db.get().getHighlight(book, chapter, v);
        boolean dark = Ui.dark(this);
        int sheetBg = dark ? 0xFF242220 : 0xFFFFFFFF;
        int labelCol = dark ? 0xFFA8A099 : 0xFF6D6259;
        int primaryCol = dark ? 0xFFE8E3DC : 0xFF26221E;

        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        float cr = dp(26);
        bg.setCornerRadii(new float[]{cr, cr, cr, cr, 0, 0, 0, 0});
        bg.setColor(sheetBg);
        sheet.setBackground(bg);
        sheet.setPadding(dp(20), dp(10), dp(20), dp(22));

        View handle = new View(this);
        GradientDrawable hg = new GradientDrawable();
        hg.setColor(dark ? 0x55FFFFFF : 0x22000000);
        hg.setCornerRadius(dp(2));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(44), dp(4));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        handle.setLayoutParams(hlp);
        handle.setBackground(hg);
        sheet.addView(handle);

        final AlertDialog[] holder = new AlertDialog[1];

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, dp(14), 0, 0);
        TextView title = new TextView(this);
        title.setText(ref);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(primaryCol);
        head.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = new TextView(this);
        close.setText("\u2715");
        close.setTextSize(16);
        close.setTextColor(labelCol);
        close.setPadding(dp(14), dp(6), dp(4), dp(6));
        close.setOnClickListener(x -> { if (holder[0] != null) holder[0].dismiss(); });
        head.addView(close);
        sheet.addView(head);

        TextView snippet = new TextView(this);
        snippet.setText(bd.text(book, chapter, v));
        snippet.setTextSize(14);
        snippet.setTypeface(Typeface.SERIF);
        snippet.setMaxLines(4);
        snippet.setEllipsize(TextUtils.TruncateAt.END);
        snippet.setLineSpacing(0, 1.2f);
        snippet.setTextColor(labelCol);
        snippet.setPadding(0, dp(6), 0, dp(14));
        sheet.addView(snippet);

        TextView hlLabel = new TextView(this);
        hlLabel.setText(R.string.highlight);
        hlLabel.setTextSize(11);
        hlLabel.setLetterSpacing(0.12f);
        hlLabel.setTextColor(labelCol);
        sheet.addView(hlLabel);

        LinearLayout colors = new LinearLayout(this);
        colors.setOrientation(LinearLayout.HORIZONTAL);
        colors.setGravity(Gravity.CENTER_VERTICAL);
        colors.setPadding(0, dp(10), 0, dp(4));
        for (int i = 0; i < Ui.HL_KEYS.length; i++) {
            View dot = new View(this);
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(dark ? Ui.HL_BG_DARK[i] : Ui.HL_BG[i]);
            if (Ui.HL_KEYS[i].equals(current)) gd.setStroke(dp(2), dark ? 0xFFFFFFFF : 0xFF3E2723);
            dot.setBackground(gd);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(36), dp(36));
            lp.setMargins(dp(6), 0, dp(6), 0);
            dot.setLayoutParams(lp);
            final String key = Ui.HL_KEYS[i];
            dot.setOnClickListener(x -> {
                selectedVerse = -1;
                Db.get().setHighlight(book, chapter, v, key.equals(current) ? null : key);
                render();
                if (holder[0] != null) holder[0].dismiss();
            });
            colors.addView(dot);
        }
        TextView clearHl = new TextView(this);
        clearHl.setText(current == null ? R.string.highlight_none : R.string.highlight_clear);
        clearHl.setTextSize(13);
        clearHl.setTextColor(labelCol);
        clearHl.setPadding(dp(16), 0, 0, 0);
        clearHl.setOnClickListener(x -> {
            selectedVerse = -1;
            Db.get().setHighlight(book, chapter, v, null);
            render();
            if (holder[0] != null) holder[0].dismiss();
        });
        colors.addView(clearHl);
        sheet.addView(colors);

        TextView actLabel = new TextView(this);
        actLabel.setText(R.string.actions);
        actLabel.setTextSize(11);
        actLabel.setLetterSpacing(0.12f);
        actLabel.setTextColor(labelCol);
        actLabel.setPadding(0, dp(12), 0, 0);
        sheet.addView(actLabel);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(10), 0, 0);
        actions.addView(actionBtn(R.drawable.ic_bookmark, 0xFFE8A13D, bookmarked ? getString(R.string.saved) : getString(R.string.bookmark), bookmarked,
                x -> {
                    selectedVerse = -1;
                    Db.get().toggleBookmark(book, chapter, v);
                    render();
                    if (holder[0] != null) holder[0].dismiss();
                }));
        actions.addView(actionBtn(R.drawable.ic_pencil, 0xFF5B8DEF, note == null ? getString(R.string.note) : getString(R.string.edit_note), note != null,
                x -> {
                    if (holder[0] != null) holder[0].dismiss();
                    showNoteEditor(v);
                }));
        actions.addView(actionBtn(R.drawable.ic_copy, 0xFF4CAF7D, getString(R.string.copy), false, x -> {
            selectedVerse = -1;
            render();
            Ui.copy(this, Ui.shareText(bd, book, chapter, v));
            toast(getString(R.string.verse_copied));
            if (holder[0] != null) holder[0].dismiss();
        }));
        actions.addView(actionBtn(R.drawable.ic_share, 0xFF9B6BD3, getString(R.string.share), false, x -> {
            selectedVerse = -1;
            render();
            if (holder[0] != null) holder[0].dismiss();
            Ui.share(this, Ui.shareText(bd, book, chapter, v));
        }));
        actions.addView(actionBtn(R.drawable.ic_book, 0xFFE8A13D, getString(R.string.study), false, x -> {
            selectedVerse = -1;
            if (holder[0] != null) holder[0].dismiss();
            Intent si = new Intent(this, StudyActivity.class);
            si.putExtra("book", book);
            si.putExtra("chapter", chapter);
            si.putExtra("verse", v);
            startActivity(si);
        }));
        sheet.addView(actions);

        final AlertDialog dlg = new AlertDialog.Builder(this).create();
        dlg.setView(sheet);
        dlg.show();
        android.view.Window w = dlg.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        w.setGravity(Gravity.BOTTOM);
        w.setDimAmount(0.45f);
        // on API 35+ the dialog window is edge-to-edge too: lift the sheet above the nav bar
        if (android.os.Build.VERSION.SDK_INT >= 35) {
            w.getDecorView().setOnApplyWindowInsetsListener((vw, insets) -> {
                android.graphics.Insets bars =
                        insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                sheet.setPadding(dp(20), dp(10), dp(20), dp(22) + bars.bottom);
                return insets;
            });
        }
        holder[0] = dlg;
    }

    private View actionBtn(int icon, int tint, String label, boolean filled, View.OnClickListener l) {
        boolean dark = Ui.dark(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setOnClickListener(l);
        FrameLayout wrap = new FrameLayout(this);
        GradientDrawable cg = new GradientDrawable();
        cg.setShape(GradientDrawable.OVAL);
        cg.setColor(filled ? tint : ((0x2E << 24) | (tint & 0x00FFFFFF)));
        wrap.setBackground(cg);
        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setColorFilter(filled ? 0xFFFFFFFF : tint);
        iv.setPadding(dp(13), dp(13), dp(13), dp(13));
        wrap.addView(iv, new FrameLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(dp(52), dp(52));
        wlp.gravity = Gravity.CENTER_HORIZONTAL;
        col.addView(wrap, wlp);
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(11);
        tv.setTextColor(dark ? 0xFFA8A099 : 0xFF6D6259);
        tv.setPadding(0, dp(6), 0, 0);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setMaxLines(1);
        col.addView(tv, new LinearLayout.LayoutParams(dp(84), LinearLayout.LayoutParams.WRAP_CONTENT));
        return col;
    }

    private void showNoteEditor(int v) {
        String existing = Db.get().getNote(book, chapter, v);
        final EditText et = new EditText(this);
        et.setText(existing == null ? "" : existing);
        et.setMinLines(3);
        et.setGravity(Gravity.TOP);
        new AlertDialog.Builder(this)
                .setTitle(Ui.ref(App.current(), book, chapter, v))
                .setView(et)
                .setPositiveButton(R.string.done, (d, w) -> {
                    selectedVerse = -1;
                    Db.get().setNote(book, chapter, v, et.getText().toString());
                    render();
                })
                .setNeutralButton(R.string.delete, (d, w) -> {
                    selectedVerse = -1;
                    Db.get().setNote(book, chapter, v, null);
                    render();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
