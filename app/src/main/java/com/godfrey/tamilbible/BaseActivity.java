package com.godfrey.tamilbible;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

/** Applies the user's app-language choice (Tamil / English / device) to every
 *  activity by wrapping the base context with the chosen locale. */
public class BaseActivity extends Activity {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(wrap(base));
    }

    static Context wrap(Context base) {
        String lang = base.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .getString("app_lang", "system");
        if ("system".equals(lang)) return base;
        Locale loc = new Locale("ta".equals(lang) ? "ta" : "en");
        Locale.setDefault(loc);
        Configuration cfg = new Configuration(base.getResources().getConfiguration());
        cfg.setLocale(loc);
        cfg.setLocales(new android.os.LocaleList(loc));
        return base.createConfigurationContext(cfg);
    }
}
