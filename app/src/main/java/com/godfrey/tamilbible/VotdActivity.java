package com.godfrey.tamilbible;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class VotdActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loading);
        App.get().whenReady(() -> {
            setContentView(R.layout.activity_votd);
            Ui.edgeToEdge(this);
            int[] r = App.current().verseOfTheDay();
            BibleData bd = App.current();
            ((TextView) findViewById(R.id.tv_ref)).setText(Ui.ref(bd, r[0], r[1], r[2]));
            ((TextView) findViewById(R.id.tv_text)).setText("\u201C" + bd.text(r[0], r[1], r[2]) + "\u201D");
            ((Button) findViewById(R.id.btn_read)).setOnClickListener(v -> {
                Intent i = new Intent(this, ReaderActivity.class);
                i.putExtra("book", r[0]);
                i.putExtra("chapter", r[1]);
                i.putExtra("verse", r[2]);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
            });
        });
    }
}
