package com.example.readera.utiles;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TextFileReader {
    private static final String TAG = "TextFileReader";
    /**
            * 从给定 Uri 读取文本内容，优先尝试 UTF-8，然后是 GBK。
            *
            * @param context 应用程序上下文。
            * @param uri 要读取的文件的 Uri。
            * @return 完整的文本内容字符串，如果读取失败则返回 null。
            */
    public String readTextFromUri(Context context, Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            Log.d(TAG, "Successfully read file with UTF-8 encoding.");
            return stringBuilder.toString();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read file with UTF-8, attempting GBK: " + e.getMessage());
            // 回退到 GBK
            stringBuilder.setLength(0); // 清空 StringBuilder
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                Log.d(TAG, "Successfully read file with GBK encoding.");
                return stringBuilder.toString();
            } catch (IOException e2) {
                Log.e(TAG, "Failed to read file with GBK encoding: " + e2.getMessage());
                return null;
            }
        }
    }
}
