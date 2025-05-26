package com.example.readera.model;

import java.io.Serializable;

public class TableOfContents implements Serializable {
    public String title; // 目录项的标题（例如“第1章”）
    public int pageIndex; // 对应的页码 (0-based)
    public int depth; // 目录层级（如果适用，例如EPUB）

    public TableOfContents(String title, int pageIndex) {
        this(title, pageIndex, 0); // 默认深度为0
    }

    public TableOfContents(String title, int pageIndex, int depth) {
        this.title = title;
        this.pageIndex = pageIndex;
        this.depth = depth;
    }

    @Override
    public String toString() {
        return "TableOfContentsEntry{" +
               "title='" + title + '\'' +
               ", pageIndex=" + pageIndex +
               ", depth=" + depth +
               '}';
    }
}