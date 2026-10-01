package com.godfrey.tamilbible;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends BaseActivity {

    private final List<int[]> results = new ArrayList<>();
    private ArrayAdapter<Object> adapter;
    private Runnable pending;
    private final SearchHandler h = new SearchHandler();

    private static class SearchHandler extends android.os.Handler { }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Ui.edgeToEdge(this);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        EditText q = findViewById(R.id.et_query);
        ListView list = findViewById(R.id.list);
        adapter = new ArrayAdapter<Object>(this, R.layout.item_result, R.id.tv_ref, (List<Object>) (List<?>) new ArrayList<String>()) {
            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                View v = super.getView(pos, cv, parent);
                TextView ref = v.findViewById(R.id.tv_ref);
                TextView text = v.findViewById(R.id.tv_text);
                if (pos < results.size() && App.get().ready()) {
                    BibleData bd = App.current();
                    int[] r = results.get(pos);
                    ref.setText(Ui.ref(bd, r[0], r[1], r[2]));
                    String body = bd.text(r[0], r[1], r[2]);
                    SpannableString ss = new SpannableString(body);
                    String query = q.getText().toString().trim().toLowerCase();
                    int from = 0, at;
                    String low = body.toLowerCase();
                    while ((at = low.indexOf(query, from)) >= 0) {
                        ss.setSpan(new StyleSpan(Typeface.BOLD), at, at + query.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        from = at + query.length();
                    }
                    text.setText(ss);
                }
                return v;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            if (pos >= results.size()) return;
            int[] r = results.get(pos);
            Intent i = new Intent(this, ReaderActivity.class);
            i.putExtra("book", r[0]);
            i.putExtra("chapter", r[1]);
            i.putExtra("verse", r[2]);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
        });
        q.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                if (pending != null) h.removeCallbacks(pending);
                pending = SearchActivity.this::runSearch;
                h.postDelayed(pending, 300);
            }
        });
        q.requestFocus();
    }

    private void runSearch() {
        EditText q = findViewById(R.id.et_query);
        String query = q.getText().toString();
        if (!App.get().ready()) return;
        new Thread(() -> {
            List<int[]> r = App.current().search(query, 400);
            runOnUiThread(() -> {
                results.clear();
                results.addAll(r);
                adapter.clear();
                for (int i = 0; i < r.size(); i++) adapter.add("");
                adapter.notifyDataSetChanged();
                if (r.isEmpty() && !query.trim().isEmpty()) {
                    Toast.makeText(this, R.string.no_results, Toast.LENGTH_SHORT).show();
                }
            });
        }, "search").start();
    }
}
