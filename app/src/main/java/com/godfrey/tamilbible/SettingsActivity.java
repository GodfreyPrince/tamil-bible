package com.godfrey.tamilbible;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.LinearLayout;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class SettingsActivity extends BaseActivity {

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

        // app language
        String lang = Ui.prefs().getString("app_lang", "system");
        ((RadioButton) findViewById(R.id.rb_lang_ta)).setChecked("ta".equals(lang));
        ((RadioButton) findViewById(R.id.rb_lang_en)).setChecked("en".equals(lang));
        ((RadioButton) findViewById(R.id.rb_lang_sys)).setChecked("system".equals(lang));
        findViewById(R.id.rb_lang_ta).setOnClickListener(v -> setLang("ta"));
        findViewById(R.id.rb_lang_en).setOnClickListener(v -> setLang("en"));
        findViewById(R.id.rb_lang_sys).setOnClickListener(v -> setLang("system"));

        // study server URL
        TextView tvUrl = findViewById(R.id.tv_study_url);
        String url = Ui.prefs().getString("study_url", "");
        tvUrl.setText(url.isEmpty() ? getString(R.string.study_url_unset) : url);
        tvUrl.setOnClickListener(v -> {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            int pad = Math.round(20 * getResources().getDisplayMetrics().density);
            box.setPadding(pad, pad / 4, pad, 0);
            final EditText et = new EditText(this);
            et.setHint(R.string.study_url_hint);
            et.setText(Ui.prefs().getString("study_url", ""));
            et.setSingleLine(true);
            box.addView(et);
            final EditText etKey = new EditText(this);
            etKey.setHint(R.string.study_key_hint);
            etKey.setText(Ui.prefs().getString("study_key", ""));
            etKey.setSingleLine(true);
            box.addView(etKey);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.study_server_url)
                    .setView(box)
                    .setPositiveButton(R.string.done, (d, w) -> {
                        String u = et.getText().toString().trim();
                        if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
                        Ui.prefs().edit().putString("study_url", u)
                                .putString("study_key", etKey.getText().toString().trim()).apply();
                        tvUrl.setText(u.isEmpty() ? getString(R.string.study_url_unset) : u);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private void setLang(String l) {
        Ui.prefs().edit().putString("app_lang", l).apply();
        recreate();
    }

    private void setTheme(String t) {
        Ui.prefs().edit().putString("theme", t).apply();
        recreate();
    }
}
