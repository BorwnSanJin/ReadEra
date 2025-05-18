package com.example.readera.Dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.example.readera.BookInfo;
import com.example.readera.Enum.CoverDataType;
import com.example.readera.database.BookDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class BookDao {
    private final BookDatabaseHelper dbHelper;

    public BookDao(Context context){
        dbHelper = new BookDatabaseHelper(context);
    }

    public long addBook(BookInfo bookInfo){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BookDatabaseHelper.COLUMN_TITLE, bookInfo.title());
        values.put(BookDatabaseHelper.COLUMN_URI, bookInfo.uri().toString());
        values.put(BookDatabaseHelper.COLUMN_COVER_DATA, bookInfo.coverData());
        values.put(BookDatabaseHelper.COLUMN_COVER_DATA_TYPE, bookInfo.coverDataType() != null ? bookInfo.coverDataType().toString() : null);
        values.put(BookDatabaseHelper.COLUMN_IS_READ, bookInfo.isRead() ? 1 : 0);
        values.put(BookDatabaseHelper.COLUMN_IS_UNREAD, bookInfo.isUnread() ? 1 : 0);
        values.put(BookDatabaseHelper.COLUMN_IS_FAVORITE, bookInfo.isFavorite() ? 1 : 0);

        long id = db.insert(BookDatabaseHelper.TABLE_BOOKS, null, values);
        db.close();
        Log.d("BookDao", "Added book: " + bookInfo.title() + " with ID: " + id);
        return id;
    }
    public List<BookInfo> getAllBooks() {
        List<BookInfo> bookList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(BookDatabaseHelper.TABLE_BOOKS,
                null, // Select all columns
                null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
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
                        Log.e("BookDao", "Unknown CoverDataType: " + coverDataTypeString);
                    }
                }

                boolean isRead = cursor.getInt(cursor.getColumnIndexOrThrow(BookDatabaseHelper.COLUMN_IS_READ)) == 1;
                boolean isUnread = cursor.getInt(cursor.getColumnIndexOrThrow(BookDatabaseHelper.COLUMN_IS_UNREAD)) == 1;
                boolean isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(BookDatabaseHelper.COLUMN_IS_FAVORITE)) == 1;

                bookList.add(new BookInfo(title, uri,coverData,coverDataType, isRead, isUnread, isFavorite));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return bookList;
    }

    public void updateBookReadStatus(String title, boolean isRead) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BookDatabaseHelper.COLUMN_IS_READ, isRead ? 1 : 0);
        int rowsAffected = db.update(BookDatabaseHelper.TABLE_BOOKS, values,
                BookDatabaseHelper.COLUMN_TITLE + " = ?",
                new String[]{title});
        db.close();
        Log.d("BookDao", "Updated read status for " + title + ", rows affected: " + rowsAffected);
    }

    public void updateBookUnreadStatus(String title, boolean isUnread) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BookDatabaseHelper.COLUMN_IS_UNREAD, isUnread ? 1 : 0);
        int rowsAffected = db.update(BookDatabaseHelper.TABLE_BOOKS, values,
                BookDatabaseHelper.COLUMN_TITLE + " = ?",
                new String[]{title});
        db.close();
        Log.d("BookDao", "Updated unread status for " + title + ", rows affected: " + rowsAffected);
    }

    public void updateBookFavoriteStatus(String title, boolean isFavorite) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BookDatabaseHelper.COLUMN_IS_FAVORITE, isFavorite ? 1 : 0);
        int rowsAffected = db.update(BookDatabaseHelper.TABLE_BOOKS, values,
                BookDatabaseHelper.COLUMN_TITLE + " = ?",
                new String[]{title});
        db.close();
        Log.d("BookDao", "Updated favorite status for " + title + ", rows affected: " + rowsAffected);
    }


    // 根据书籍标题删除书籍
    public int deleteBook(String title) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = db.delete(BookDatabaseHelper.TABLE_BOOKS,
                BookDatabaseHelper.COLUMN_TITLE + " = ?",
                new String[]{title});
        db.close();
        Log.d("BookDao", "Deleted book: " + title + ", rows affected: " + rowsAffected);
        return rowsAffected;
    }

    // 根据书籍 URI 删除书籍
    public int deleteBook(Uri uri) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = db.delete(BookDatabaseHelper.TABLE_BOOKS,
                BookDatabaseHelper.COLUMN_URI + " = ?",
                new String[]{uri.toString()});
        db.close();
        Log.d("BookDao", "Deleted book with URI: " + uri.toString() + ", rows affected: " + rowsAffected);
        return rowsAffected;
    }

}
