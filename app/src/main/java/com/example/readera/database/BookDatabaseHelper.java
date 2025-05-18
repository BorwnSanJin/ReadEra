package com.example.readera.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
public class BookDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "books.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_BOOKS = "books";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_URI = "uri";
    public static final String COLUMN_COVER_DATA = "cover_data";
    public static final String COLUMN_COVER_DATA_TYPE = "cover_data_type";

    private static final String SQL_CREATE_BOOKS =
            "CREATE TABLE " + TABLE_BOOKS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_TITLE + " TEXT NOT NULL," +
                    COLUMN_URI + " TEXT NOT NULL UNIQUE,"+
                    COLUMN_COVER_DATA + " TEXT," +
                    COLUMN_COVER_DATA_TYPE + " TEXT);";


    public BookDatabaseHelper( Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_BOOKS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 当数据库版本升级时，你需要执行相应的更新操作
        // 这里简单地删除旧表并创建新表，实际应用中需要更谨慎地处理用户数据
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKS);
        onCreate(db);
    }
}
