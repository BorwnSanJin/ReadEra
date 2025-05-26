// TextFileReader.java
package com.example.readera.utiles;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets; // 引入 StandardCharsets

//文本读取
public class TextFileReader {
    private static final String TAG = "TextFileReader";

    public String readTextFromUri(Context context, Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            // 尝试 UTF-8
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                Log.d(TAG, "Successfully read file with UTF-8 encoding.");
                return stringBuilder.toString();
            }
        } catch (IOException e) {
            // 如果 UTF-8 读取失败，尝试 GBK
            Log.e(TAG, "Failed to read file with UTF-8, attempting GBK: " + e.getMessage());
            stringBuilder.setLength(0); // 清空 StringBuilder

            try {
                try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "GBK"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    Log.d(TAG, "Successfully read file with GBK encoding.");
                    return stringBuilder.toString();
                }
            } catch (IOException e2) {
                // GBK 也失败了
                Log.e(TAG, "Failed to read file with GBK encoding: " + e2.getMessage());
                return null;
            }
        } catch (SecurityException e) {
            // 显式捕获 SecurityException
            Log.e(TAG, "Permission Denial: Failed to open InputStream for URI: " + uri + " - " + e.getMessage(), e);
            // 这里应该向用户显示错误信息，或者处理权限不足的情况
            return null;
        } catch (Exception e) { // 捕获所有其他未知异常
            Log.e(TAG, "An unexpected error occurred while reading file for URI: " + uri + " - " + e.getMessage(), e);
            return null;
        }
        // 确保所有路径都有返回值
    }
}