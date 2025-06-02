package com.example.readera.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
public class BookDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "books.db"; // 数据库文件名
    private static final int DATABASE_VERSION = 4; // 数据库版本号

    public static final String TABLE_BOOKS = "books"; // 书籍信息表名
    public static final String COLUMN_ID = "_id"; // 表的主键ID，通常为自增长
    public static final String COLUMN_TITLE = "title"; // 书籍标题
    public static final String COLUMN_URI = "uri"; // 书籍文件在设备上的URI (统一资源标识符)
    public static final String COLUMN_COVER_DATA = "cover_data"; // 书籍封面图片的数据（例如：图片二进制数据）
    public static final String COLUMN_COVER_DATA_TYPE = "cover_data_type"; // 书籍封面图片数据的类型（例如：PNG, JPEG）
    public static final String COLUMN_IS_READ = "is_read";       // 书籍已读状态标识（例如：0为未读，1为已读）
    public static final String COLUMN_IS_UNREAD = "is_unread";   // 书籍未读状态标识（通常与is_read互斥，0为已读，1为未读）
    public static final String COLUMN_IS_FAVORITE = "is_favorite"; // 书籍收藏状态标识（例如：0为未收藏，1为已收藏）
    public static final String COLUMN_FILE_SIZE = "file_size"; // 书籍文件的大小，单位通常为字节
    public static final String COLUMN_LAST_MODIFIED = "last_modified"; // 书籍文件最后修改的时间戳
    public static final String COLUMN_FILE_HASH = "file_hash"; // 书籍文件的哈希值，用于文件完整性校验或去重

    private static final String SQL_CREATE_BOOKS =
            "CREATE TABLE " + TABLE_BOOKS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_TITLE + " TEXT NOT NULL," +
                    COLUMN_URI + " TEXT NOT NULL UNIQUE,"+
                    COLUMN_COVER_DATA + " TEXT," +
                    COLUMN_COVER_DATA_TYPE + " TEXT,"+
                    COLUMN_IS_READ + " INTEGER DEFAULT 0," +   // 默认未读
                    COLUMN_IS_UNREAD + " INTEGER DEFAULT 0," +   // 默认是未读
                    COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0,"+ // 默认未收藏
                    COLUMN_FILE_SIZE + " INTEGER DEFAULT -1," +
                    COLUMN_LAST_MODIFIED + " INTEGER DEFAULT -1," +
                    COLUMN_FILE_HASH + " TEXT);";
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
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKS);
        onCreate(db);
    }
}
