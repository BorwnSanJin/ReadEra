package com.example.readera.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
public class BookDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "books.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_BOOKS = "books";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_URI = "uri";
    public static final String COLUMN_COVER_DATA = "cover_data";
    public static final String COLUMN_COVER_DATA_TYPE = "cover_data_type";
    public static final String COLUMN_IS_READ = "is_read";       // 已读状态
    public static final String COLUMN_IS_UNREAD = "is_unread";   // 未读状态
    public static final String COLUMN_IS_FAVORITE = "is_favorite"; // 收藏状态

    private static final String SQL_CREATE_BOOKS =
            "CREATE TABLE " + TABLE_BOOKS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_TITLE + " TEXT NOT NULL," +
                    COLUMN_URI + " TEXT NOT NULL UNIQUE,"+
                    COLUMN_COVER_DATA + " TEXT," +
                    COLUMN_COVER_DATA_TYPE + " TEXT,"+
                    COLUMN_IS_READ + " INTEGER DEFAULT 0," +   // 默认未读
                    COLUMN_IS_UNREAD + " INTEGER DEFAULT 0," +   // 默认是未读
                    COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0);"; // 默认未收藏
            ;


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
//        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKS);
//        onCreate(db);
        // 当数据库版本升级时，添加新的列
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_BOOKS + " ADD COLUMN " + COLUMN_IS_READ + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_BOOKS + " ADD COLUMN " + COLUMN_IS_UNREAD + " INTEGER DEFAULT 1");
            db.execSQL("ALTER TABLE " + TABLE_BOOKS + " ADD COLUMN " + COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0");
        }

    }
}
