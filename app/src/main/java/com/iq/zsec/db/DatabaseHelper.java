package com.iq.zsec.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite-backed file metadata store.
 * Singleton — safe to access from both Activities and DocumentsProvider
 * (same process, serialized by SQLite's WAL mode).
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "zsec_vault.db";
    private static final int    DB_VERSION = 1;

    // Table & columns
    static final String TABLE   = "protected_files";
    static final String C_ID    = "id";
    static final String C_NAME  = "file_name";
    static final String C_STORE = "stored_name";
    static final String C_MIME  = "mime_type";
    static final String C_SIZE  = "file_size";
    static final String C_DATE  = "date_added";

    private static final String SQL_CREATE =
	"CREATE TABLE " + TABLE + " (" +
	C_ID    + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
	C_NAME  + " TEXT NOT NULL, "                     +
	C_STORE + " TEXT NOT NULL UNIQUE, "              +
	C_MIME  + " TEXT DEFAULT 'application/octet-stream', " +
	C_SIZE  + " INTEGER DEFAULT 0, "                 +
	C_DATE  + " INTEGER NOT NULL"                    +
	");";

    private static volatile DatabaseHelper instance;

    public static DatabaseHelper getInstance(Context ctx) {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper(ctx.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) { db.execSQL(SQL_CREATE); }

    @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    // ── CRUD ─────────────────────────────────────────────────────

    public long insertFile(FileRecord r) {
        ContentValues v = new ContentValues();
        v.put(C_NAME,  r.getFileName());
        v.put(C_STORE, r.getStoredName());
        v.put(C_MIME,  r.getMimeType());
        v.put(C_SIZE,  r.getFileSize());
        v.put(C_DATE,  r.getDateAdded());
        return getWritableDatabase().insertOrThrow(TABLE, null, v);
    }

    public boolean deleteFile(String storedName) {
        int rows = getWritableDatabase()
            .delete(TABLE, C_STORE + "=?", new String[]{storedName});
        return rows > 0;
    }

    public List<FileRecord> getAllFiles() {
        List<FileRecord> list = new ArrayList<>();
        Cursor c = getReadableDatabase()
            .query(TABLE, null, null, null, null, null, C_DATE + " DESC");
        if (c != null) {
            while (c.moveToNext()) list.add(fromCursor(c));
            c.close();
        }
        return list;
    }

    public FileRecord getByStoredName(String storedName) {
        Cursor c = getReadableDatabase()
            .query(TABLE, null, C_STORE + "=?", new String[]{storedName},
                   null, null, null, "1");
        if (c != null && c.moveToFirst()) {
            FileRecord r = fromCursor(c);
            c.close();
            return r;
        }
        return null;
    }

    private FileRecord fromCursor(Cursor c) {
        FileRecord r = new FileRecord();
        r.setId(        c.getLong  (c.getColumnIndex(C_ID)));
        r.setFileName(  c.getString(c.getColumnIndex(C_NAME)));
        r.setStoredName(c.getString(c.getColumnIndex(C_STORE)));
        r.setMimeType(  c.getString(c.getColumnIndex(C_MIME)));
        r.setFileSize(  c.getLong  (c.getColumnIndex(C_SIZE)));
        r.setDateAdded( c.getLong  (c.getColumnIndex(C_DATE)));
        return r;
    }
}
