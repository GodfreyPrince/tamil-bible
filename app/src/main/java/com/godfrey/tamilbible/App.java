package com.godfrey.tamilbible;

import android.app.Application;

public class App extends Application {
    private static App inst;
    public static volatile BibleData old;   // பழைய தமிழ் வேதாகமம் (classic BSI text)
    public static volatile BibleData bsi;   // திருவிவிலியம் (BSI ecumenical, 2012)
    public static volatile BibleData irv;   // Indian Revised Version (simplified Tamil)
    private static final String[] CYCLE = {"old", "bsi", "irv"};
    private final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

    public static App get() { return inst; }

    @Override
    public void onCreate() {
        super.onCreate();
        inst = this;
        new Thread(() -> {
            try {
                old = BibleData.load(this, "tamil_old.json", "old",
                        getString(R.string.version_old_full), getString(R.string.version_old_short));
                bsi = BibleData.load(this, "tamil_bsi.json", "bsi",
                        getString(R.string.version_bsi_full), getString(R.string.version_bsi_short));
                irv = BibleData.load(this, "tamil.json", "irv",
                        getString(R.string.version_irv_full), getString(R.string.version_irv_short));
            } catch (Exception e) {
                android.util.Log.e("TamilBible", "Failed to load Bible", e);
            }
            latch.countDown();
        }, "bible-load").start();
    }

    public boolean ready() { return old != null && bsi != null && irv != null; }

    /** Bible text of the currently selected version. */
    public static BibleData current() {
        String v = Ui.prefs().getString("version", "old");
        if ("bsi".equals(v)) return bsi;
        if ("irv".equals(v)) return irv;
        return old;
    }

    /** Next version in the toggle cycle. */
    public static BibleData other() {
        String v = Ui.prefs().getString("version", "old");
        int i = "bsi".equals(v) ? 1 : "irv".equals(v) ? 2 : 0;
        String next = CYCLE[(i + 1) % CYCLE.length];
        return "bsi".equals(next) ? bsi : "irv".equals(next) ? irv : old;
    }

    public static void selectVersion(String id) {
        Ui.prefs().edit().putString("version", id).apply();
    }

    public void whenReady(Runnable r) {
        if (ready()) { r.run(); return; }
        new Thread(() -> {
            try { latch.await(); } catch (InterruptedException ignored) {}
            if (ready())
                new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
        }, "await-bible").start();
    }
}
