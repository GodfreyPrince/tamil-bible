package com.godfrey.tamilbible;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class PlansActivity extends BaseActivity {

    private List<PlanEngine.Plan> plans;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plans);
        Ui.edgeToEdge(this);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        ListView list = findViewById(R.id.list);

        App.get().whenReady(() -> {
            plans = PlanEngine.all(App.current());
            ArrayAdapter<PlanEngine.Plan> ad = new ArrayAdapter<PlanEngine.Plan>(this, R.layout.item_plan, plans) {
                private final int[] ICON_COLORS = {0xFF7A2F21, 0xFFB36A2E, 0xFF8C6D3F, 0xFF3E5C50, 0xFF5E5148, 0xFF7A2F21};
                @Override
                public View getView(int pos, View cv, ViewGroup parent) {
                    View v = cv;
                    if (v == null) v = getLayoutInflater().inflate(R.layout.item_plan, parent, false);
                    PlanEngine.Plan p = plans.get(pos);
                    FrameLayout icon = v.findViewById(R.id.plan_icon);
                    android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
                    g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                    g.setColor(ICON_COLORS[pos % ICON_COLORS.length]);
                    icon.setBackground(g);
                    ((TextView) v.findViewById(R.id.tv_name)).setText(p.title);
                    ((TextView) v.findViewById(R.id.tv_desc)).setText(p.desc);
                    ProgressBar pb = v.findViewById(R.id.progress);
                    pb.setMax(p.days);
                    pb.setProgress(Db.get().doneCount(p.id, p.days));
                    return v;
                }
            };
            list.setAdapter(ad);
            list.setOnItemClickListener((parent, view, pos, id) -> {
                Intent i = new Intent(PlansActivity.this, PlanDetailActivity.class);
                i.putExtra("plan", plans.get(pos).id);
                startActivity(i);
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ListView list = findViewById(R.id.list);
        if (plans != null) {
            plans = PlanEngine.all(App.current());
            ((ArrayAdapter) list.getAdapter()).notifyDataSetChanged();
        }
    }
}
