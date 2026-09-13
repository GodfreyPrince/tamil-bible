package com.godfrey.tamilbible;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.JsonReader;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** வேத வரைபடங்கள் — classic Biblical maps (online images, curators' picks). */
public class MapsActivity extends Activity {

    private static class MItem {
        String id, title, taTitle, taDesc, url;
    }

    private final List<MItem> maps = new ArrayList<>();
    private GridView grid;
    private TextView status;
    private boolean dark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Ui.edgeToEdge(this);
        dark = Ui.dark(this);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        grid = findViewById(R.id.grid);
        status = findViewById(R.id.tv_status);
        status.setText(ImageNet.online(this)
                ? getString(R.string.media_online) : getString(R.string.media_offline));
        grid.setOnItemClickListener((p, v, pos, id) -> {
            MItem m = maps.get(pos);
            Intent i = new Intent(this, MapViewerActivity.class);
            i.putExtra("title", m.title);
            i.putExtra("url", m.url);
            startActivity(i);
        });
        new Thread(() -> {
            try (InputStream in = getAssets().open("media_maps.json")) {
                JsonReader r = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                r.beginObject();
                while (r.hasNext()) {
                    if (!"maps".equals(r.nextName())) { r.skipValue(); continue; }
                    r.beginArray();
                    while (r.hasNext()) {
                        MItem m = new MItem();
                        r.beginObject();
                        while (r.hasNext()) {
                            String k = r.nextName();
                            if ("id".equals(k)) m.id = r.nextString();
                            else if ("title".equals(k)) m.title = r.nextString();
                            else if ("url".equals(k)) m.url = r.nextString();
                            else if ("ta".equals(k)) {
                                r.beginObject();
                                while (r.hasNext()) {
                                    String tk = r.nextName();
                                    if ("title".equals(tk)) m.taTitle = r.nextString();
                                    else if ("desc".equals(tk)) m.taDesc = r.nextString();
                                    else r.skipValue();
                                }
                                r.endObject();
                            } else r.skipValue();
                        }
                        r.endObject();
                        maps.add(m);
                    }
                    r.endArray();
                }
                r.endObject();
            } catch (Exception e) {
                android.util.Log.e("TamilBible", "maps manifest", e);
            }
            runOnUiThread(this::fill);
        }, "maps-load").start();
    }

    private void fill() {
        grid.setAdapter(new BaseAdapter() {
            public int getCount() { return maps.size(); }
            public Object getItem(int p) { return maps.get(p); }
            public long getItemId(int p) { return p; }
            public View getView(int pos, View cv, ViewGroup parent) {
                MItem m = maps.get(pos);
                LinearLayout cell = new LinearLayout(MapsActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setBackgroundResource(R.drawable.bg_list_item);
                int pad = Math.round(6 * getResources().getDisplayMetrics().density);
                cell.setPadding(pad, pad, pad, pad);
                ImageView iv = new ImageView(MapsActivity.this);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iv.setBackgroundColor(dark ? 0xFF242220 : 0xFFEFE9E0);
                ImageNet.load(MapsActivity.this, m.url, iv, R.drawable.bg_list_item);
                cell.addView(iv, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Math.round(150 * getResources().getDisplayMetrics().density)));
                TextView tv = new TextView(MapsActivity.this);
                tv.setText(m.title);
                tv.setTextSize(11);
                tv.setMaxLines(2);
                tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
                tv.setTextColor(dark ? 0xFFE8E3DC : 0xFF26221E);
                tv.setPadding(pad, pad, 0, 0);
                cell.addView(tv);
                return cell;
            }
        });
        ((TextView) findViewById(R.id.tv_count)).setText(
                getString(R.string.media_count_fmt, maps.size()));
    }
}
