package com.example.readera.Dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.example.readera.model.BookInfo;
import com.example.readera.Enum.CoverDataType;
import com.example.readera.database.BookDatabaseHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BookDao {
    private static final String TAG = "BookDao"; // 定义TAG常量
    private final BookDatabaseHelper dbHelper;

    public BookDao(Context context){
        dbHelper = new BookDatabaseHelper(context);
    }

    public long addBook(BookInfo bookInfo){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BookDatabaseHelper.COLUMN_TITLE, bookInfo.getTitle());
        values.put(BookDatabaseHelper.COLUMN_URI, bookInfo.getUri().toString());
        values.put(BookDatabaseHelper.COLUMN_COVER_DATA, bookInfo.coverData());
        values.put(BookDatabaseHelper.COLUMN_COVER_DATA_TYPE, bookInfo.coverDataType() != null ? bookInfo.coverDataType().toString() : null);
        values.put(BookDatabaseHelper.COLUMN_IS_READ, bookInfo.isRead() ? 1 : 0);
        values.put(BookDatabaseHelper.COLUMN_IS_UNREAD, bookInfo.isUnread() ? 1 : 0);
        values.put(BookDatabaseHelper.COLUMN_IS_FAVORITE, bookInfo.isFavorite() ? 1 : 0);
        values.put(BookDatabaseHelper.COLUMN_FILE_SIZE, bookInfo.getFileSize());
        values.put(BookDatabaseHelper.COLUMN_LAST_MODIFIED, bookInfo.getLastModified());
        values.put(BookDatabaseHelper.COLUMN_FILE_HASH, bookInfo.getFileHash()); // 存储文件哈希值


        long id = db.insert(BookDatabaseHelper.TABLE_BOOKS, null, values);
        db.close();
        Log.d(TAG, "Added book: " + bookInfo.getTitle() + " with ID: " + id);
        return id;
    }

    /**
     * 从 Cursor 中提取 BookInfo 对象，这是共享的辅助方法。
     */
    private BookInfo extractBookInfoFromCursor(Cursor cursor) {
        String title = cursor.getString(cursor.getColumnIndexOrThrow(BookDatabaseHelper.COLUMN_TITLE));
        String uriString = cursor.getString(cursor.getColumnIndexOrThrow(BookDatabaseHelper.COLUMN_URI));
        Uri uri = Uri.parse(uriString);
        String coverData = cursor.getString(cursor.getColumnIndexOrThrow(BookDatabaseHelper.COLUMN_COVER_DATA));
        String coverDataTypeString = cursor.getString(cursor.getColumnIndexOrThrow(BookDatabaseHelper.COLUMN_COVER_DATA_TYPE));
        CoverDataType coverDataType = null;
        if (coverDataTypeString != null) {
            try {
                coverDataType = CoverDataType.valueOf(coverDataTypeString);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Unknown CoverDataType: " + coverDataTypeString);
                // 可以在这里设置一个默认值，例如 coverDataType = CoverDataType.TEXT;
            }
        }

        boolean isRead = cursor.getInt(cursor.getColumnIndexOrThrow(BookDatabaseHelper.COLUMN_IS_READ)) == 1;
        boolean isUnread = cursor.getInt(cursor.getColumnIndexOrThrow(BookDatabaseHelper.COLUMN_IS_UNREAD)) == 1;
        boolean isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(BookDatabaseHelper.COLUMN_IS_FAVORITE)) == 1;
        long fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(BookDatabaseHelper.COLUMN_FILE_SIZE));
        long lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(BookDatabaseHelper.COLUMN_LAST_MODIFIED));
        String fileHash = cursor.getString(cursor.getColumnIndexOrThrow(BookDatabaseHelper.COLUMN_FILE_HASH));

        return new BookInfo(title, uri, coverData, coverDataType, isRead, isUnread, isFavorite, fileSize, lastModified, fileHash);
    }

    /**
     * 通用的查询书籍列表的方法
     * @param selection 查询条件，例如 "COLUMN_IS_READ = ?"
     * @param selectionArgs 查询条件的参数
     * @return 符合条件的 BookInfo 列表
     */
    private List<BookInfo> getBooksBySelection(String selection, String[] selectionArgs) {
        List<BookInfo> bookList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null; // 初始化为 null，确保 finally 块中能安全关闭

        String[] projection = { // 定义所有需要查询的列
                BookDatabaseHelper.COLUMN_TITLE,
                BookDatabaseHelper.COLUMN_URI,
                BookDatabaseHelper.COLUMN_COVER_DATA,
                BookDatabaseHelper.COLUMN_COVER_DATA_TYPE,
                BookDatabaseHelper.COLUMN_IS_READ,
                BookDatabaseHelper.COLUMN_IS_UNREAD,
                BookDatabaseHelper.COLUMN_IS_FAVORITE,
                BookDatabaseHelper.COLUMN_FILE_SIZE,
                BookDatabaseHelper.COLUMN_LAST_MODIFIED,
                BookDatabaseHelper.COLUMN_FILE_HASH
        };

        try {
            cursor = db.query(
                    BookDatabaseHelper.TABLE_BOOKS,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    bookList.add(extractBookInfoFromCursor(cursor)); // 调用辅助方法
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying books: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return bookList;
    }

    public List<BookInfo> getAllBooks() {
        return getBooksBySelection(null, null); // 查询所有书籍
    }

    public List<BookInfo> getAllUnreadBooks() {
       return getBooksBySelection(BookDatabaseHelper.COLUMN_IS_UNREAD + " = ?", new String[]{"1"});
    }
    public List<BookInfo> getAllFavoriteBooks() {
        return getBooksBySelection(BookDatabaseHelper.COLUMN_IS_FAVORITE + " = ?", new String[]{"1"});
    }
    public List<BookInfo> getAllReadBooks() {
        return getBooksBySelection(BookDatabaseHelper.COLUMN_IS_READ + " = ?", new String[]{"1"});
    }

    public void updateBookReadStatus(String title, boolean isRead) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BookDatabaseHelper.COLUMN_IS_READ, isRead ? 1 : 0);
        int rowsAffected = db.update(BookDatabaseHelper.TABLE_BOOKS, values,
                BookDatabaseHelper.COLUMN_TITLE + " = ?",
                new String[]{title});
        db.close();
        Log.d(TAG, "Updated read status for " + title + ", rows affected: " + rowsAffected);
    }

    public void updateBookUnreadStatus(String title, boolean isUnread) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BookDatabaseHelper.COLUMN_IS_UNREAD, isUnread ? 1 : 0);
        int rowsAffected = db.update(BookDatabaseHelper.TABLE_BOOKS, values,
                BookDatabaseHelper.COLUMN_TITLE + " = ?",
                new String[]{title});
        db.close();
        Log.d(TAG, "Updated unread status for " + title + ", rows affected: " + rowsAffected);
    }

    public void updateBookFavoriteStatus(String title, boolean isFavorite) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BookDatabaseHelper.COLUMN_IS_FAVORITE, isFavorite ? 1 : 0);
        int rowsAffected = db.update(BookDatabaseHelper.TABLE_BOOKS, values,
                BookDatabaseHelper.COLUMN_TITLE + " = ?",
                new String[]{title});
        db.close();
        Log.d(TAG, "Updated favorite status for " + title + ", rows affected: " + rowsAffected);
    }

    // 根据书籍标题删除书籍
    public int deleteBook(String title) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = db.delete(BookDatabaseHelper.TABLE_BOOKS,
                BookDatabaseHelper.COLUMN_TITLE + " = ?",
                new String[]{title});
        db.close();
        Log.d(TAG, "Deleted book: " + title + ", rows affected: " + rowsAffected);
        return rowsAffected;
    }

    // 根据书籍 URI 删除书籍
    public int deleteBook(Uri uri) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = db.delete(BookDatabaseHelper.TABLE_BOOKS,
                BookDatabaseHelper.COLUMN_URI + " = ?",
                new String[]{uri.toString()});
        db.close();
        Log.d(TAG, "Deleted book with URI: " + uri.toString() + ", rows affected: " + rowsAffected);
        return rowsAffected;
    }

    public boolean isBookExists(BookInfo  bookInfo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // 优先根据文件哈希值判断是否存在
        if (bookInfo.getFileHash() != null && !bookInfo.getFileHash().isEmpty()) {
            Cursor cursor = null;
            try {
                cursor = db.query(
                        BookDatabaseHelper.TABLE_BOOKS,
                        new String[]{BookDatabaseHelper.COLUMN_FILE_HASH},
                        BookDatabaseHelper.COLUMN_FILE_HASH + " = ?",
                        new String[]{bookInfo.getFileHash()},
                        null, null, null
                );
                if (cursor != null && cursor.getCount() > 0) {
                    return true;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        // 如果哈希值为空或者不匹配，则比较文件大小和最后修改时间
        Cursor uriCursor = null;
        try {
            uriCursor = db.query(
                    BookDatabaseHelper.TABLE_BOOKS,
                    new String[]{BookDatabaseHelper.COLUMN_URI},
                    BookDatabaseHelper.COLUMN_URI + "=?",
                    new String[]{bookInfo.getUri().toString()},
                    null, null, null
            );
            if (uriCursor != null && uriCursor.getCount() > 0) {
                return true;
            }
        } finally {
            if (uriCursor != null) {
                uriCursor.close();
            }
        }
        // 如果URI也不匹配，最后根据文件大小和修改时间判断（作为辅助，冲突风险较高）
        Cursor metadataCursor = null;
        try {
            // 最后比较 URI
            metadataCursor = db.query(
                    BookDatabaseHelper.TABLE_BOOKS,
                    new String[]{BookDatabaseHelper.COLUMN_URI, BookDatabaseHelper.COLUMN_FILE_SIZE, BookDatabaseHelper.COLUMN_LAST_MODIFIED},
                    BookDatabaseHelper.COLUMN_FILE_SIZE + " = ? AND " + BookDatabaseHelper.COLUMN_LAST_MODIFIED + " = ?",
                    new String[]{String.valueOf(bookInfo.getFileSize()), String.valueOf(bookInfo.getLastModified())},
                    null, null, null
            );

            return metadataCursor != null && metadataCursor.getCount() > 0;
        } finally {
            if (metadataCursor != null) {
                metadataCursor.close();
            }
            db.close(); // 确保在所有判断路径结束后关闭数据库
        }
    }
}
