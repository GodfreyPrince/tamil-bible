package com.godfrey.tamilbible;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class LibraryActivity extends Activity {

    private static final String[] TABS = {"bookmarks", "highlights", "notes"};
    private int tab = 0;
    private List<int[]> rows;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        Ui.edgeToEdge(this);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.tab_bookmarks).setOnClickListener(v -> { tab = 0; fill(); });
        findViewById(R.id.tab_highlights).setOnClickListener(v -> { tab = 1; fill(); });
        findViewById(R.id.tab_notes).setOnClickListener(v -> { tab = 2; fill(); });
    }

    @Override
    protected void onResume() {
        super.onResume();
        App.get().whenReady(this::fill);
    }

    private void fill() {
        BibleData bd = App.current();
        TextView bm = findViewById(R.id.tab_bookmarks), hl = findViewById(R.id.tab_highlights), nt = findViewById(R.id.tab_notes);
        bm.setTextColor(tab == 0 ? 0xFFF57F17 : Ui.attr(this, android.R.attr.textColorSecondary));
        hl.setTextColor(tab == 1 ? 0xFFF57F17 : Ui.attr(this, android.R.attr.textColorSecondary));
        nt.setTextColor(tab == 2 ? 0xFFF57F17 : Ui.attr(this, android.R.attr.textColorSecondary));

        rows = tab == 0 ? Db.get().listBookmarks() : tab == 1 ? Db.get().listHighlights() : Db.get().listNotes();

        ListView list = findViewById(R.id.list);
        ArrayAdapter<Object> ad = new ArrayAdapter<Object>(this, R.layout.item_result) {
            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                View v = cv;
                if (v == null) v = getLayoutInflater().inflate(R.layout.item_result, parent, false);
                int[] r = rows.get(pos);
                ((TextView) v.findViewById(R.id.tv_ref)).setText(Ui.ref(bd, r[0], r[1], r[2]));
                String body = bd.text(r[0], r[1], r[2]);
                if (body.length() > 120) body = body.substring(0, 120) + "\u2026";
                ((TextView) v.findViewById(R.id.tv_text)).setText(body);
                return v;
            }
        };
        if (rows.isEmpty()) {
            String msg = tab == 0 ? getString(R.string.empty_bookmarks)
                    : tab == 1 ? getString(R.string.empty_highlights) : getString(R.string.empty_notes);
            ArrayAdapter<String> empty = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                    new String[]{msg});
            TextView first = (TextView) empty.getView(0, null, list);
            first.setPadding(40, 60, 40, 0);
            list.setAdapter(empty);
            list.setOnItemClickListener(null);
            list.setOnItemLongClickListener(null);
            return;
        }
        for (int i = 0; i < rows.size(); i++) ad.add(new Object());
        list.setAdapter(ad);
        list.setOnItemClickListener((p, v, pos, id) -> {
            int[] r = rows.get(pos);
            Intent i = new Intent(this, ReaderActivity.class);
            i.putExtra("book", r[0]);
            i.putExtra("chapter", r[1]);
            i.putExtra("verse", r[2]);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
        });
        list.setOnItemLongClickListener((p, v, pos, id) -> {
            int[] r = rows.get(pos);
            String kind = getString(tab == 0 ? R.string.bookmark : tab == 1 ? R.string.highlight : R.string.note);
            new AlertDialog.Builder(LibraryActivity.this)
                    .setTitle(Ui.ref(bd, r[0], r[1], r[2]))
                    .setMessage(getString(R.string.remove_item, kind))
                    .setPositiveButton(R.string.delete, (d, w) -> {
                        Db.get().deleteRow(TABS[tab], r[0], r[1], r[2]);
                        Toast.makeText(this, R.string.removed, Toast.LENGTH_SHORT).show();
                        fill();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        });
    }
}
