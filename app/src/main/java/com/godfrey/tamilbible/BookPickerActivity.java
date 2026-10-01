package com.godfrey.tamilbible;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BookPickerActivity extends BaseActivity {

    private boolean showNT = false;
    private int pickingBook = -1;
    private TextView btnVersion;
    private Set<Long> readSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_picker);
        Ui.edgeToEdge(this);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_ot).setOnClickListener(v -> { showNT = false; pickingBook = -1; fill(); });
        findViewById(R.id.btn_nt).setOnClickListener(v -> { showNT = true; pickingBook = -1; fill(); });
        btnVersion = findViewById(R.id.btn_version);
        btnVersion.setOnClickListener(v -> {
            App.selectVersion(App.other().id);
            fill();
        });
        findViewById(R.id.btn_progress).setOnClickListener(v ->
                startActivity(new Intent(this, ProgressActivity.class)));

        GridView grid = findViewById(R.id.grid);
        grid.setOnItemClickListener((parent, view, pos, id) -> {
            if (App.current() == null) return;
            if (pickingBook < 0) {
                pickingBook = bookList().get(pos);
                fill();
            } else {
                Intent i = new Intent();
                i.putExtra("book", pickingBook);
                i.putExtra("chapter", pos + 1);
                setResult(RESULT_OK, i);
                finish();
            }
        });
        fill();
    }

    private List<Integer> bookList() {
        BibleData bd = App.current();
        List<Integer> out = new ArrayList<>();
        for (int b = 0; b < bd.bookCount(); b++) if (bd.nt[b] == showNT) out.add(b);
        return out;
    }

    private void fill() {
        BibleData bd = App.current();
        if (bd == null) return;
        readSet = Db.get().readSet();
        btnVersion.setText(bd.shortLabel);
        TextView title = findViewById(R.id.tv_title);
        ((TextView) findViewById(R.id.btn_ot)).setTextColor(
                showNT ? Ui.attr(this, android.R.attr.textColorSecondary) : 0xFFF57F17);
        ((TextView) findViewById(R.id.btn_nt)).setTextColor(
                showNT ? 0xFFF57F17 : Ui.attr(this, android.R.attr.textColorSecondary));

        GridView grid = findViewById(R.id.grid);
        if (pickingBook < 0) {
            title.setText(getString(R.string.books));
            grid.setNumColumns(2);
            grid.setAdapter(new BookAdapter(bookList()));
        } else {
            title.setText(bd.names[pickingBook]);
            grid.setNumColumns(4);
            grid.setAdapter(new ChapterAdapter(pickingBook));
        }
    }

    static long chKey(int b, int c) { return b * 10000L + c; }

    private int countRead(int b, int chapters) {
        int n = 0;
        for (int c = 1; c <= chapters; c++) if (readSet.contains(chKey(b, c))) n++;
        return n;
    }

    private class BookAdapter extends BaseAdapter {
        private final List<Integer> books;

        BookAdapter(List<Integer> books) { this.books = books; }

        public int getCount() { return books.size(); }
        public Object getItem(int p) { return books.get(p); }
        public long getItemId(int p) { return p; }

        public View getView(int pos, View convertView, ViewGroup parent) {
            BibleData bd = App.current();
            int b = books.get(pos);
            boolean dark = Ui.dark(BookPickerActivity.this);
            boolean complete = countRead(b, bd.chapterCount[b]) >= bd.chapterCount[b];

            LinearLayout row = new LinearLayout(BookPickerActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_list_item);
            row.setPadding(dp(14), dp(10), dp(12), dp(10));

            RingView ring = new RingView(BookPickerActivity.this);
            int total = bd.chapterCount[b];
            int done = countRead(b, total);
            ring.set(total == 0 ? 0 : (float) done / total, 0xFFE8A13D,
                    dark ? 0x33FFFFFF : 0x22E8A13D, true);
            row.addView(ring, new LinearLayout.LayoutParams(dp(28), dp(28)));

            TextView name = new TextView(BookPickerActivity.this);
            name.setText(bd.names[b]);
            name.setTextSize(15);
            name.setTypeface(Typeface.SERIF, complete ? Typeface.BOLD : Typeface.NORMAL);
            name.setTextColor(complete ? 0xFFE8A13D : (dark ? 0xFFE8E3DC : 0xFF26221E));
            name.setPadding(dp(12), 0, dp(6), 0);
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView count = new TextView(BookPickerActivity.this);
            count.setText(getString(R.string.book_progress_fmt, done, total));
            count.setTextSize(11);
            count.setTextColor(dark ? 0xFF8B8378 : 0xFF9C9086);
            row.addView(count);
            return row;
        }
    }

    private class ChapterAdapter extends BaseAdapter {
        private final int book;

        ChapterAdapter(int book) { this.book = book; }

        public int getCount() { return App.current().chapterCount[book]; }
        public Object getItem(int p) { return p + 1; }
        public long getItemId(int p) { return p; }

        public View getView(int pos, View convertView, ViewGroup parent) {
            int ch = pos + 1;
            boolean read = readSet.contains(chKey(book, ch));
            boolean dark = Ui.dark(BookPickerActivity.this);

            FrameLayout cell = new FrameLayout(BookPickerActivity.this);
            int pad = dp(5);
            cell.setPadding(pad, pad, pad, pad);

            TextView num = new TextView(BookPickerActivity.this);
            num.setText(String.valueOf(ch));
            num.setTextSize(15);
            num.setGravity(Gravity.CENTER);
            num.setTypeface(read ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            num.setTextColor(read ? 0xFF3E2723 : (dark ? 0xFFE8E3DC : 0xFF26221E));
            cell.addView(num, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            if (read) {
                View circle = new View(BookPickerActivity.this);
                GradientDrawable g = new GradientDrawable();
                g.setShape(GradientDrawable.OVAL);
                g.setColor(0xFFE8A13D);
                circle.setBackground(g);
                cell.addView(circle, 0, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            return cell;
        }
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
