package com.godfrey.tamilbible;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

/** Reading progress: overall ring, OT/NT totals, and a ring for every book. */
public class ProgressActivity extends BaseActivity {

    private static final int TOTAL_CHAPTERS = 1189;

    private RingView bigRing;
    private TextView tvPercent, tvTotal, tvOt, tvNt;
    private LinearLayout bookList;
    private Set<Long> readSet;
    private int done, otDone, ntDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);
        Ui.edgeToEdge(this);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_reset).setOnClickListener(v -> confirmReset());
        bigRing = findViewById(R.id.ring_big);
        tvPercent = findViewById(R.id.tv_percent);
        tvTotal = findViewById(R.id.tv_total);
        tvOt = findViewById(R.id.tv_ot);
        tvNt = findViewById(R.id.tv_nt);
        bookList = findViewById(R.id.book_list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        App.get().whenReady(this::fill);
    }

    private void fill() {
        BibleData bd = App.current();
        readSet = Db.get().readSet();
        done = Db.get().readCount();
        otDone = ntDone = 0;
        for (int b = 0; b < 66; b++) {
            int n = countRead(b, bd.chapterCount[b]);
            if (b < 39) otDone += n; else ntDone += n;
        }
        ((TextView) findViewById(R.id.tv_version)).setText(bd.label);

        float frac = TOTAL_CHAPTERS == 0 ? 0 : (float) done / TOTAL_CHAPTERS;
        bigRing.set(frac, 0xFFE8A13D, Ui.dark(this) ? 0x33FFFFFF : 0x22E8A13D, true);
        tvPercent.setText(Math.round(frac * 100) + "%");
        tvTotal.setText(getString(R.string.chapters_progress, done, TOTAL_CHAPTERS));

        int otTotal = 929, ntTotal = 260;
        tvOt.setText(getString(R.string.book_progress_fmt, otDone, otTotal)
                + "  ·  " + getString(R.string.old_testament));
        tvNt.setText(getString(R.string.book_progress_fmt, ntDone, ntTotal)
                + "  ·  " + getString(R.string.new_testament));
        setBar(R.id.track_ot, R.id.fill_ot, otDone, otTotal);
        setBar(R.id.track_nt, R.id.fill_nt, ntDone, ntTotal);

        bookList.removeAllViews();
        addHeader(getString(R.string.old_testament));
        for (int b = 0; b < 39; b++) bookList.addView(bookRow(bd, b));
        addHeader(getString(R.string.new_testament));
        for (int b = 39; b < 66; b++) bookList.addView(bookRow(bd, b));
    }

    private void setBar(int trackId, int fillId, int done, int total) {
        View track = findViewById(trackId);
        View inner = findViewById(fillId);
        track.post(() -> inner.setLayoutParams(new FrameLayout.LayoutParams(
                Math.round(track.getWidth() * (total == 0 ? 0 : (float) done / total)),
                ViewGroup.LayoutParams.MATCH_PARENT)));
    }

    private void addHeader(String text) {
        TextView h = new TextView(this);
        h.setText(text);
        h.setTextSize(13);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        h.setAllCaps(false);
        h.setTextColor(0xFFE8A13D);
        h.setPadding(dp(4), dp(18), 0, dp(6));
        bookList.addView(h);
    }

    private View bookRow(BibleData bd, int b) {
        boolean dark = Ui.dark(this);
        int total = bd.chapterCount[b];
        int n = countRead(b, total);
        boolean complete = n >= total && total > 0;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_list_item);
        row.setPadding(dp(14), dp(8), dp(12), dp(8));

        RingView ring = new RingView(this);
        ring.set(total == 0 ? 0 : (float) n / total, 0xFFE8A13D,
                dark ? 0x33FFFFFF : 0x22E8A13D, true);
        row.addView(ring, new LinearLayout.LayoutParams(dp(26), dp(26)));

        TextView name = new TextView(this);
        name.setText(bd.names[b]);
        name.setTextSize(14);
        name.setTypeface(Typeface.SERIF, complete ? Typeface.BOLD : Typeface.NORMAL);
        name.setTextColor(complete ? 0xFFE8A13D : (dark ? 0xFFE8E3DC : 0xFF26221E));
        name.setPadding(dp(12), 0, dp(6), 0);
        row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView count = new TextView(this);
        count.setText(getString(R.string.book_progress_fmt, n, total));
        count.setTextSize(11);
        count.setTextColor(dark ? 0xFF8B8378 : 0xFF9C9086);
        row.addView(count);

        row.setOnClickListener(v -> {
            android.content.Intent i = new android.content.Intent(this, ReaderActivity.class);
            i.putExtra("book", b);
            i.putExtra("chapter", 1);
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
        });
        return row;
    }

    private int countRead(int b, int chapters) {
        int n = 0;
        for (int c = 1; c <= chapters; c++) if (readSet.contains(BookPickerActivity.chKey(b, c))) n++;
        return n;
    }

    private void confirmReset() {
        if (Db.get().readCount() == 0) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_all)
                .setMessage(R.string.reset_confirm)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    Db.get().clearChaptersRead();
                    Toast.makeText(this, R.string.removed, Toast.LENGTH_SHORT).show();
                    fill();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
