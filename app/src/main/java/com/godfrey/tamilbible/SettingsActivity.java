package com.godfrey.tamilbible;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Ui.edgeToEdge(this);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        SeekBar sb = findViewById(R.id.sb_font);
        TextView tv = findViewById(R.id.tv_font_val);
        sb.setProgress(Ui.fontSp());
        tv.setText(getString(R.string.font_size) + ": " + Ui.fontSp() + "sp — " + getString(R.string.font_hint));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int val, boolean fromUser) {
                if (!fromUser) return;
                Ui.prefs().edit().putInt("font_sp", val).apply();
                tv.setText(getString(R.string.font_size) + ": " + val + "sp — " + getString(R.string.font_hint));
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });

        String theme = Ui.prefs().getString("theme", "system");
        ((RadioButton) findViewById(R.id.rb_light)).setChecked("light".equals(theme));
        ((RadioButton) findViewById(R.id.rb_dark)).setChecked("dark".equals(theme));
        ((RadioButton) findViewById(R.id.rb_system)).setChecked("system".equals(theme));
        findViewById(R.id.rb_light).setOnClickListener(v -> setTheme("light"));
        findViewById(R.id.rb_dark).setOnClickListener(v -> setTheme("dark"));
        findViewById(R.id.rb_system).setOnClickListener(v -> setTheme("system"));

        CheckBox keep = findViewById(R.id.cb_keep_on);
        keep.setChecked(Ui.keepOn());
        keep.setOnCheckedChangeListener((b, checked) -> Ui.prefs().edit().putBoolean("keep_on", checked).apply());

        CheckBox store = findViewById(R.id.cb_store_media);
        store.setChecked(Ui.prefs().getBoolean("store_media", false));
        store.setOnCheckedChangeListener((b, checked) ->
                Ui.prefs().edit().putBoolean("store_media", checked).apply());
    }

    private void setTheme(String t) {
        Ui.prefs().edit().putString("theme", t).apply();
        recreate();
    }
}
