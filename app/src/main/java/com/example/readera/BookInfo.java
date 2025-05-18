package com.example.readera;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.readera.Enum.CoverDataType;

public record BookInfo(String title, Uri uri,String coverData , CoverDataType coverDataType) {

    @NonNull
    @Override
    public String toString() {
        return title; // 只返回 title 用于 ListView 的显示
    }
}
