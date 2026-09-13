package com.godfrey.tamilbible;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

public class Ui {
    public static final String[] HL_KEYS = {"y", "g", "b", "p", "o"};
    public static final int[] HL_COLORS = {
            0xFFF9A825,
            0xFF66BB6A,
            0xFF42A5F5,
            0xFFEC407A,
            0xFFFF7043
    };
    // softer background tints applied to verse text (light theme)
    public static final int[] HL_BG = {
            0xFFFFF59D, 0xFFA5D6A7, 0xFF90CAF9, 0xFFF48FB1, 0xFFFFAB91
    };
    // translucent tints that keep light text readable in dark theme
    public static final int[] HL_BG_DARK = {
            0x66FBC02D, 0x6655BB6A, 0x6642A5F5, 0x66EC407A, 0x66FF7043
    };

    public static boolean dark(Activity a) {
        String t = prefs().getString("theme", "system");
        if ("dark".equals(t)) return true;
        if ("light".equals(t)) return false;
        int m = a.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return m == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    public static int hlBg(Activity a, String key) {
        for (int i = 0; i < HL_KEYS.length; i++)
            if (HL_KEYS[i].equals(key)) return dark(a) ? HL_BG_DARK[i] : HL_BG[i];
        return Color.TRANSPARENT;
    }

    public static SharedPreferences prefs() {
        return App.get().getSharedPreferences("prefs", Context.MODE_PRIVATE);
    }

    public static void applyTheme(Activity a) {
        String t = prefs().getString("theme", "system");
        if ("light".equals(t)) a.setTheme(R.style.AppTheme);
        else if ("dark".equals(t)) a.setTheme(R.style.AppThemeDark);
        else a.setTheme(R.style.AppThemeSystem);
    }

    /**
     * API 36 edge-to-edge: the window draws behind the system bars, so the activity
     * root is padded by the system-bar + display-cutout insets. Root backgrounds are
     * the toolbar colour, so the bars blend with the header/footer. No-op before API 30
     * where the theme colours the bars the classic way.
     */
    public static void edgeToEdge(final Activity a) {
        if (Build.VERSION.SDK_INT < 30) return;
        if (Build.VERSION.SDK_INT >= 29)
            a.getWindow().setNavigationBarContrastEnforced(false);
        final ViewGroup content = a.findViewById(android.R.id.content);
        View decor = a.getWindow().getDecorView();
        decor.setOnApplyWindowInsetsListener((v, insets) -> {
            applyInsets(a, insets, content);
            return WindowInsets.CONSUMED;
        });
        WindowInsets wi = decor.getRootWindowInsets();
        if (wi != null) applyInsets(a, wi, content);
    }

    private static void applyInsets(Activity a, WindowInsets insets, ViewGroup content) {
        if (insets == null || content == null) return;
        android.graphics.Insets bars = insets.getInsets(
                WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        View root = content.getChildAt(0);
        if (root != null) root.setPadding(bars.left, bars.top, bars.right, bars.bottom);
    }

    public static int attr(Activity a, int attr) {
        TypedValue tv = new TypedValue();
        a.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    public static int fontSp() { return prefs().getInt("font_sp", 17); }

    public static boolean keepOn() { return prefs().getBoolean("keep_on", false); }

    public static String ref(BibleData bd, int b, int c, int v) {
        return bd.names[b] + " " + c + ":" + v;
    }

    public static String chapterRef(BibleData bd, int b, int c) {
        return bd.names[b] + " " + c;
    }

    public static String shareText(BibleData bd, int b, int c, int v) {
        return "\u201C" + bd.text(b, c, v) + "\u201D \u2014 " + ref(bd, b, c, v)
                + " (" + bd.shortLabel + ")";
    }

    public static void copy(Context ctx, String text) {
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("verse", text));
    }

    public static void share(Context ctx, String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        ctx.startActivity(Intent.createChooser(i, "Share verse"));
    }
}
