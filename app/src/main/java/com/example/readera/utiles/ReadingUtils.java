package com.example.readera.utiles;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.example.readera.ReadingActivity;

public class ReadingUtils {
    /**
     * 启动 ReadingActivity 来阅读指定的书籍。
     * @param context 用于启动 Activity 的 Context 对象（可以是 Activity 或 Fragment 的 Context）。
     * @param fileUri 要阅读的书籍文件的 Uri。
     */
    public static void startReadingActivity(Context context, Uri fileUri) {
        Intent intent = new Intent(context, ReadingActivity.class);
        intent.putExtra("FILE_URI", fileUri);
        context.startActivity(intent);
    }
}
