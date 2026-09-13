package com.godfrey.tamilbible;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

/** Full-screen art viewer: Tamil title/description, save-to-device, source link, share. */
public class ArtViewerActivity extends Activity {

    private MediaData.Item it;
    private TextView btnSave;
    private boolean dark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        super.onCreate(savedInstanceState);
        dark = Ui.dark(this);
        String id = getIntent().getStringExtra("id");
        MediaData.get(this, md -> {
            if (md == null) { finish(); return; }
            for (MediaData.Item x : md.all())
                if (x.id.equals(id)) { it = x; break; }
            if (it == null) { finish(); return; }
            setContentView(R.layout.activity_art_viewer);
            Ui.edgeToEdge(this);
            bind();
        });
    }

    private void bind() {
        ScrollView scroll = findViewById(R.id.scroll);
        scroll.setBackgroundColor(dark ? 0xFF141311 : 0xFFFAF6F0);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ImageView big = findViewById(R.id.iv_art);
        big.setBackgroundColor(dark ? 0xFF242220 : 0xFFEFE9E0);
        ImageNet.load(this, it.img, big, R.drawable.bg_list_item);

        TextView title = findViewById(R.id.tv_title);
        String t = it.taTitle == null || it.taTitle.isEmpty() ? it.title : it.taTitle;
        title.setText(t);

        StringBuilder meta = new StringBuilder();
        if (it.artist != null && !it.artist.isEmpty()) meta.append(it.artist);
        if (it.year > 0) meta.append(meta.length() > 0 ? ", " : "").append(it.year);
        if (it.taEra != null && !it.taEra.isEmpty()) meta.append(" · ").append(it.taEra);
        ((TextView) findViewById(R.id.tv_meta)).setText(meta.toString());

        TextView desc = findViewById(R.id.tv_desc);
        desc.setText(it.taDesc == null || it.taDesc.isEmpty() ? it.title : it.taDesc);
        if (it.srcPage != null && it.srcPage.startsWith("http")) {
            TextView src = findViewById(R.id.tv_src);
            src.setText(getString(R.string.media_source_fmt,
                    it.source == null ? "" : it.source) + " · " + (it.license == null ? "" : it.license));
            src.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(it.srcPage))));
        }

        LinearLayout refsBox = findViewById(R.id.refs_box);
        for (int i = 0; i < it.refs.size() && i < 8; i++) {
            int[] r = it.refs.get(i);
            TextView chip = new TextView(this);
            String label = it.refLabels.size() > i && !it.refLabels.get(i).isEmpty()
                    ? it.refLabels.get(i) : r[0] + " " + r[1];
            chip.setText(label);
            chip.setTextSize(12);
            chip.setTypeface(Typeface.SERIF);
            chip.setTextColor(0xFF7A2F21);
            chip.setBackgroundResource(R.drawable.bg_version_chip);
            chip.setPadding(dp(12), dp(5), dp(12), dp(5));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(4), dp(8), 0);
            refsBox.addView(chip, lp);
            final int ch = r[1], bk = r[0];
            chip.setOnClickListener(v -> {
                Intent ri = new Intent(this, ReaderActivity.class);
                ri.putExtra("book", bk);
                ri.putExtra("chapter", ch);
                ri.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(ri);
                finish();
            });
        }

        btnSave = findViewById(R.id.btn_save);
        paintSave();
        btnSave.setOnClickListener(v -> {
            boolean store = Ui.prefs().getBoolean("store_media", false);
            Ui.prefs().edit().putBoolean("store_media", !store).apply();
            paintSave();
            if (!store) {
                File f = ImageNet.fetchLocal(this, it.img);
                Toast.makeText(this, f != null ? R.string.media_saved : R.string.media_need_online,
                        Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_share).setOnClickListener(v -> {
            File f = ImageNet.fetchLocal(this, it.img);
            if (f == null) {
                Toast.makeText(this, R.string.media_need_online, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("text/plain");
            s.putExtra(Intent.EXTRA_TEXT, t + " — " + it.artist + " (" + it.year + ")\n" + it.srcPage);
            startActivity(Intent.createChooser(s, getString(R.string.share)));
        });
    }

    private void paintSave() {
        boolean store = Ui.prefs().getBoolean("store_media", false);
        btnSave.setText(store ? R.string.media_keep_on : R.string.media_keep_off);
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
