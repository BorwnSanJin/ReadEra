package com.example.readera;

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

    public BookInfo(String title, Uri uri, String coverData, CoverDataType coverDataType) {
        this(title, uri, coverData, coverDataType, false, false, false); // 默认状态：未读
    }

    public BookInfo(String title, Uri uri, String coverData, CoverDataType coverDataType, boolean isRead, boolean isUnread, boolean isFavorite) {
        this.title = title;
        this.uri = uri;
        this.coverData = coverData;
        this.coverDataType = coverDataType;
        this.isRead = isRead;
        this.isUnread = isUnread;
        this.isFavorite = isFavorite;
    }

    public String title() {
        return title;
    }

    public Uri uri() {
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

    @NonNull
    @Override
    public String toString() {
        return title; // 只返回 title 用于 ListView 的显示
    }
}
