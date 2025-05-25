package com.example.readera.model;

import android.net.Uri;

public class Bookmark {
    private Uri fileUri;
    private int pageNumber;
    private String displayTitle;
    public Bookmark(Uri fileUri, int pageNumber, String displayTitle) {
        this.fileUri = fileUri;
        this.pageNumber = pageNumber;
        this.displayTitle = displayTitle;
    }

    public Uri getFileUri() {
        return fileUri;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getDisplayTitle() {
        return displayTitle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bookmark bookmark = (Bookmark) o;
        return pageNumber == bookmark.pageNumber &&
                fileUri.equals(bookmark.fileUri);
    }

    @Override
    public int hashCode() {
        int result = fileUri.hashCode();
        result = 31 * result + pageNumber;
        return result;
    }
}
