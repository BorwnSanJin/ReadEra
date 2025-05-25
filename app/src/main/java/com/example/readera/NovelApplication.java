package com.example.readera; // 替换为你的应用包名

import android.app.Application;
import com.example.readera.utiles.NovelReaderManager;

public class NovelApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 在应用启动时初始化 NovelReaderManager (尽管是单例，显式调用一次 getInstance 确保初始化)
        NovelReaderManager.getInstance();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // 在应用终止时关闭线程池，清理资源
        // 注意：onTerminate() 不保证在所有情况下都被调用，但这是一个最佳实践
        NovelReaderManager.getInstance().shutdown();
    }
}