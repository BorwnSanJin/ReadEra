package com.example.readera.model;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.readera.Enum.CoverDataType;

public class BookInfo {
    private String title;
    private Uri uri;
    private String coverData;
    private CoverDataType coverDataType;
    private boolean isRead;
    private boolean isUnread;
    private boolean isFavorite;
    private long fileSize;//文件大小
    private long lastModified;//最后修改

    private String fileHash; // 文件hash

    public BookInfo(String title, Uri uri, String coverData, CoverDataType coverDataType) {
        this(title, uri, coverData, coverDataType, false, false, false, -1, -1, null); // 默认状态：未读
    }

    public BookInfo(String title, Uri uri, String coverData, CoverDataType coverDataType, boolean isRead, boolean isUnread, boolean isFavorite) {
        this(title, uri, coverData, coverDataType, isRead, isUnread, isFavorite, -1, -1, null);
    }

    public BookInfo(String title, Uri uri, String coverData, CoverDataType coverDataType, boolean isRead, boolean isUnread, boolean isFavorite, long fileSize, long lastModified) {
        this(title, uri, coverData, coverDataType, isRead, isUnread, isFavorite, fileSize, lastModified, null);
    }

    public BookInfo(String title, Uri uri, String coverData, CoverDataType coverDataType, boolean isRead, boolean isUnread, boolean isFavorite, long fileSize, long lastModified, String fileHash) {
        this.title = title;
        this.uri = uri;
        this.coverData = coverData;
        this.coverDataType = coverDataType;
        this.isRead = isRead;
        this.isUnread = isUnread;
        this.isFavorite = isFavorite;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.fileHash = fileHash;
    }

    public String getTitle() {
        return title;
    }

    public Uri getUri() {
        return uri;
    }

    public String coverData() {
        return coverData;
    }

    public CoverDataType coverDataType() {
        return coverDataType;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public boolean isUnread() {
        return isUnread;
    }

    public void setUnread(boolean unread) {
        isUnread = unread;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public long getFileSize() {
        return fileSize;
    }
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getLastModified() {
        return lastModified;
    }
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }
    @NonNull
    @Override
    public String toString() {
        return title; // 只返回 title 用于 ListView 的显示
    }
}
