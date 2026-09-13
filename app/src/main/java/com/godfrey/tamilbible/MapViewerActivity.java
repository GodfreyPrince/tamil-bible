package com.godfrey.tamilbible;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/** Full-screen map viewer with zoom-friendly large image. */
public class MapViewerActivity extends Activity {

    private String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_viewer);
        Ui.edgeToEdge(this);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        title = getIntent().getStringExtra("title");
        String url = getIntent().getStringExtra("url");
        ImageView iv = findViewById(R.id.iv_map);
        ImageNet.load(this, url, iv, R.drawable.bg_list_item);
        ((TextView) findViewById(R.id.tv_title)).setText(title);
    }
}
