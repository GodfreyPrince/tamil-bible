package com.godfrey.tamilbible;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class PlanDetailActivity extends BaseActivity {

    private PlanEngine.Plan plan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_detail);
        Ui.edgeToEdge(this);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        App.get().whenReady(() -> {
            String id = getIntent().getStringExtra("plan");
            for (PlanEngine.Plan p : PlanEngine.all(App.current())) if (p.id.equals(id)) plan = p;
            if (plan == null) { finish(); return; }
            ((TextView) findViewById(R.id.tv_title)).setText(plan.title + "\n" +
                    getString(R.string.days_complete_fmt, Db.get().doneCount(plan.id, plan.days), plan.days));
            fill();
        });
    }

    private void fill() {
        ListView list = findViewById(R.id.list);
        ArrayAdapter<Object> ad = new ArrayAdapter<Object>(this, R.layout.item_day) {
            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                View v = cv;
                if (v == null) v = getLayoutInflater().inflate(R.layout.item_day, parent, false);
                final int day = pos + 1;
                ((TextView) v.findViewById(R.id.tv_day)).setText(getString(R.string.day_fmt, day));
                ((TextView) v.findViewById(R.id.tv_reading)).setText(
                        PlanEngine.label(App.current(), plan.portions[pos]));
                CheckBox cb = v.findViewById(R.id.cb_done);
                cb.setOnCheckedChangeListener(null);
                cb.setChecked(Db.get().isDayDone(plan.id, day));
                cb.setOnCheckedChangeListener((b, checked) -> {
                    Db.get().setDayDone(plan.id, day, checked);
                    ((TextView) findViewById(R.id.tv_title)).setText(plan.title + "\n" +
                            getString(R.string.days_complete_fmt, Db.get().doneCount(plan.id, plan.days), plan.days));
                });
                v.setOnClickListener(x -> {
                    int[] first = plan.portions[pos][0];
                    Intent i = new Intent(PlanDetailActivity.this, ReaderActivity.class);
                    i.putExtra("book", first[0]);
                    i.putExtra("chapter", first[1]);
                    startActivity(i);
                });
                return v;
            }
        };
        for (int d = 0; d < plan.days; d++) ad.add(new Object());
        list.setAdapter(ad);
    }
}
