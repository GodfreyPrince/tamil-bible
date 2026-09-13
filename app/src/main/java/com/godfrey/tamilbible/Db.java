package com.godfrey.tamilbible;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** All user data stays on the phone. Canonical order: book/chapter/verse indexes. */
public class Db extends SQLiteOpenHelper {
    private static Db inst;

    public static synchronized Db get() {
        if (inst == null) inst = new Db(App.get());
        return inst;
    }

    private Db(Context c) { super(c, "userdata.db", null, 2); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE bookmarks(book INTEGER,chapter INTEGER,verse INTEGER,created INTEGER,PRIMARY KEY(book,chapter,verse))");
        db.execSQL("CREATE TABLE highlights(book INTEGER,chapter INTEGER,verse INTEGER,color TEXT,PRIMARY KEY(book,chapter,verse))");
        db.execSQL("CREATE TABLE notes(book INTEGER,chapter INTEGER,verse INTEGER,text TEXT,updated INTEGER,PRIMARY KEY(book,chapter,verse))");
        db.execSQL("CREATE TABLE plan_progress(plan TEXT,day INTEGER,done INTEGER,PRIMARY KEY(plan,day))");
        db.execSQL("CREATE TABLE chapters_read(book INTEGER,chapter INTEGER,created INTEGER,PRIMARY KEY(book,chapter))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) {
        if (o < 2)
            db.execSQL("CREATE TABLE IF NOT EXISTS chapters_read(book INTEGER,chapter INTEGER,created INTEGER,PRIMARY KEY(book,chapter))");
    }

    // ---- chapter tracker (shared across translations) ----
    private static long ck(int b, int c) { return b * 10000L + c; }

    public boolean isChapterRead(int b, int c) {
        Cursor cur = getReadableDatabase().rawQuery(
                "SELECT 1 FROM chapters_read WHERE book=? AND chapter=?",
                new String[]{String.valueOf(b), String.valueOf(c)});
        boolean x = cur.moveToFirst();
        cur.close();
        return x;
    }

    public void setChapterRead(int b, int c, boolean read) {
        SQLiteDatabase db = getWritableDatabase();
        if (read) {
            ContentValues cv = new ContentValues();
            cv.put("book", b); cv.put("chapter", c); cv.put("created", System.currentTimeMillis());
            db.insertWithOnConflict("chapters_read", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } else {
            db.delete("chapters_read", "book=? AND chapter=?",
                    new String[]{String.valueOf(b), String.valueOf(c)});
        }
    }

    /** Keys (book*10000+chapter) of every read chapter — one query for grids and stats. */
    public Set<Long> readSet() {
        Set<Long> out = new HashSet<>();
        Cursor cur = getReadableDatabase().rawQuery("SELECT book,chapter FROM chapters_read", null);
        while (cur.moveToNext()) out.add(ck(cur.getInt(0), cur.getInt(1)));
        cur.close();
        return out;
    }

    public int readCount() {
        Cursor cur = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM chapters_read", null);
        int n = cur.moveToFirst() ? cur.getInt(0) : 0;
        cur.close();
        return n;
    }

    public void clearChaptersRead() {
        getWritableDatabase().delete("chapters_read", null, null);
    }

    private static long key(int b, int c, int v) { return b * 1000000L + c * 1000L + v; }

    // ---- bookmarks ----
    public boolean isBookmarked(int b, int c, int v) {
        Cursor cur = getReadableDatabase().rawQuery("SELECT 1 FROM bookmarks WHERE book=? AND chapter=? AND verse=?",
                new String[]{String.valueOf(b), String.valueOf(c), String.valueOf(v)});
        boolean x = cur.moveToFirst();
        cur.close();
        return x;
    }

    public void toggleBookmark(int b, int c, int v) {
        if (isBookmarked(b, c, v))
            getWritableDatabase().delete("bookmarks", "book=? AND chapter=? AND verse=?",
                    new String[]{String.valueOf(b), String.valueOf(c), String.valueOf(v)});
        else {
            ContentValues cv = new ContentValues();
            cv.put("book", b); cv.put("chapter", c); cv.put("verse", v);
            cv.put("created", System.currentTimeMillis());
            getWritableDatabase().insert("bookmarks", null, cv);
        }
    }

    // ---- highlights ----
    public String getHighlight(int b, int c, int v) {
        Cursor cur = getReadableDatabase().rawQuery("SELECT color FROM highlights WHERE book=? AND chapter=? AND verse=?",
                new String[]{String.valueOf(b), String.valueOf(c), String.valueOf(v)});
        String col = cur.moveToFirst() ? cur.getString(0) : null;
        cur.close();
        return col;
    }

    public void setHighlight(int b, int c, int v, String color) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("highlights", "book=? AND chapter=? AND verse=?",
                new String[]{String.valueOf(b), String.valueOf(c), String.valueOf(v)});
        if (color != null) {
            ContentValues cv = new ContentValues();
            cv.put("book", b); cv.put("chapter", c); cv.put("verse", v); cv.put("color", color);
            db.insert("highlights", null, cv);
        }
    }

    // ---- notes ----
    public String getNote(int b, int c, int v) {
        Cursor cur = getReadableDatabase().rawQuery("SELECT text FROM notes WHERE book=? AND chapter=? AND verse=?",
                new String[]{String.valueOf(b), String.valueOf(c), String.valueOf(v)});
        String t = cur.moveToFirst() ? cur.getString(0) : null;
        cur.close();
        return t;
    }

    public void setNote(int b, int c, int v, String text) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("notes", "book=? AND chapter=? AND verse=?",
                new String[]{String.valueOf(b), String.valueOf(c), String.valueOf(v)});
        if (text != null && !text.trim().isEmpty()) {
            ContentValues cv = new ContentValues();
            cv.put("book", b); cv.put("chapter", c); cv.put("verse", v);
            cv.put("text", text.trim()); cv.put("updated", System.currentTimeMillis());
            db.insert("notes", null, cv);
        }
    }

    // ---- reading plans ----
    public boolean isDayDone(String plan, int day) {
        Cursor cur = getReadableDatabase().rawQuery("SELECT done FROM plan_progress WHERE plan=? AND day=?",
                new String[]{plan, String.valueOf(day)});
        boolean x = cur.moveToFirst() && cur.getInt(0) == 1;
        cur.close();
        return x;
    }

    public void setDayDone(String plan, int day, boolean done) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("plan", plan); cv.put("day", day); cv.put("done", done ? 1 : 0);
        db.insertWithOnConflict("plan_progress", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int doneCount(String plan, int days) {
        int n = 0;
        for (int d = 1; d <= days; d++) if (isDayDone(plan, d)) n++;
        return n;
    }

    // ---- library lists ----
    public List<int[]> listBookmarks() {
        return list("bookmarks");
    }

    public List<int[]> listHighlights() {
        return list("highlights");
    }

    public List<int[]> listNotes() {
        return list("notes");
    }

    private List<int[]> list(String table) {
        List<int[]> out = new ArrayList<>();
        Cursor cur = getReadableDatabase().rawQuery("SELECT book,chapter,verse FROM " + table + " ORDER BY book,chapter,verse", null);
        while (cur.moveToNext()) out.add(new int[]{cur.getInt(0), cur.getInt(1), cur.getInt(2)});
        cur.close();
        return out;
    }

    public void deleteRow(String table, int b, int c, int v) {
        getWritableDatabase().delete(table, "book=? AND chapter=? AND verse=?",
                new String[]{String.valueOf(b), String.valueOf(c), String.valueOf(v)});
    }
}
