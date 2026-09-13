package com.godfrey.tamilbible;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

/** கலைக் காட்சியகம் — browse curated Biblical art by scene (online images). */
public class GalleryActivity extends Activity {

    private static final int MODE_SCENES = 0, MODE_SCENE_GRID = 1, MODE_ALL = 2;
    private int mode = MODE_SCENES;
    private String curScene;
    private TextView tabScenes, tabAll, status;
    private ListView list;
    private GridView grid;
    private MediaData md;
    private boolean dark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        Ui.edgeToEdge(this);
        dark = Ui.dark(this);
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (mode == MODE_SCENE_GRID) { mode = MODE_SCENES; fill(); }
            else finish();
        });
        tabScenes = findViewById(R.id.tab_scenes);
        tabAll = findViewById(R.id.tab_all);
        status = findViewById(R.id.tv_status);
        list = findViewById(R.id.list);
        grid = findViewById(R.id.grid);
        list.setOnItemClickListener((p, v, pos, id) -> {
            curScene = md.scenes().get(pos);
            mode = MODE_SCENE_GRID;
            fill();
        });
        grid.setOnItemClickListener((p, v, pos, id) -> {
            MediaData.Item it = currentItems().get(pos);
            Intent i = new Intent(this, ArtViewerActivity.class);
            i.putExtra("id", it.id);
            startActivity(i);
        });
        tabScenes.setOnClickListener(v -> { mode = MODE_SCENES; fill(); });
        tabAll.setOnClickListener(v -> { mode = MODE_ALL; fill(); });
        status.setText(ImageNet.online(this)
                ? getString(R.string.media_online)
                : getString(R.string.media_offline));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (md == null) MediaData.get(this, m -> { md = m; fill(); });
        else fill();
    }

    private List<MediaData.Item> currentItems() {
        if (mode == MODE_ALL) return md.all();
        return md.forScene(curScene == null ? "" : curScene);
    }

    private void fill() {
        if (md == null) return;
        tabScenes.setTextColor(mode == MODE_ALL ? Ui.attr(this, android.R.attr.textColorSecondary) : 0xFFF57F17);
        tabAll.setTextColor(mode == MODE_ALL ? 0xFFF57F17 : Ui.attr(this, android.R.attr.textColorSecondary));
        ((TextView) findViewById(R.id.tv_count)).setText(getString(R.string.media_count_fmt, md.all().size()));
        TextView title = findViewById(R.id.tv_title);
        boolean gridMode = mode != MODE_SCENES;
        list.setVisibility(gridMode ? View.GONE : View.VISIBLE);
        grid.setVisibility(gridMode ? View.VISIBLE : View.GONE);
        if (mode == MODE_SCENES) {
            title.setText(getString(R.string.media_scenes));
            list.setAdapter(new SceneAdapter());
        } else if (mode == MODE_SCENE_GRID) {
            title.setText(MediaData.sceneTaLabel(curScene, md.forScene(curScene)));
            grid.setNumColumns(2);
            grid.setAdapter(new ArtAdapter());
        } else {
            title.setText(getString(R.string.media_all));
            grid.setNumColumns(3);
            grid.setAdapter(new ArtAdapter());
        }
    }

    private class SceneAdapter extends BaseAdapter {
        public int getCount() { return md.scenes().size(); }
        public Object getItem(int p) { return md.scenes().get(p); }
        public long getItemId(int p) { return p; }

        public View getView(int pos, View cv, ViewGroup parent) {
            String scene = md.scenes().get(pos);
            List<MediaData.Item> items = md.forScene(scene);
            LinearLayout row = new LinearLayout(GalleryActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_list_item);
            row.setPadding(dp(16), dp(12), dp(12), dp(12));

            TextView name = new TextView(GalleryActivity.this);
            name.setText(MediaData.sceneTaLabel(scene, items));
            name.setTextSize(16);
            name.setTypeface(Typeface.SERIF);
            name.setTextColor(dark ? 0xFFE8E3DC : 0xFF26221E);
            name.setPadding(0, 0, dp(8), 0);
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView count = new TextView(GalleryActivity.this);
            count.setText(String.valueOf(items.size()));
            count.setTextSize(13);
            count.setTextColor(dark ? 0xFF8B8378 : 0xFF9C9086);
            row.addView(count);
            return row;
        }
    }

    private class ArtAdapter extends BaseAdapter {
        private final List<MediaData.Item> items = currentItems();

        public int getCount() { return items.size(); }
        public Object getItem(int p) { return items.get(p); }
        public long getItemId(int p) { return p; }

        public View getView(int pos, View cv, ViewGroup parent) {
            MediaData.Item it = items.get(pos);
            LinearLayout cell = new LinearLayout(GalleryActivity.this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setBackgroundResource(R.drawable.bg_list_item);
            cell.setPadding(dp(6), dp(6), dp(6), dp(6));

            ImageView iv = new ImageView(GalleryActivity.this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackgroundColor(dark ? 0xFF242220 : 0xFFEFE9E0);
            ImageNet.load(GalleryActivity.this, it.img, iv, R.drawable.bg_list_item);
            cell.addView(iv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(mode == MODE_ALL ? 110 : 130)));

            TextView tv = new TextView(GalleryActivity.this);
            String label = it.taTitle == null || it.taTitle.isEmpty() ? it.title : it.taTitle;
            tv.setText(label);
            tv.setTextSize(12);
            tv.setTypeface(Typeface.SERIF);
            tv.setMaxLines(1);
            tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tv.setTextColor(dark ? 0xFFE8E3DC : 0xFF26221E);
            tv.setPadding(dp(4), dp(6), 0, dp(4));
            cell.addView(tv);
            return cell;
        }
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
