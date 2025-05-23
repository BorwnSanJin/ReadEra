package com.example.readera.utiles;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.Window;

import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SystemUiController {
    private final Window window;
    private final WindowInsetsControllerCompat windowInsetsController;
    private final Context context; // 需要 Context 来获取资源中的颜色


    public SystemUiController(Window window, View decorView, Context context) {
        this.window = window;
        this.context = context;
        // 确保 decorView 不为空，以便获取控制器
        if (decorView == null) {
            throw new IllegalArgumentException("DecorView cannot be null for WindowInsetsControllerCompat.");
        }
        this.windowInsetsController = WindowCompat.getInsetsController(window, decorView);
    }

    /**
     * 将 Activity 设置为沉浸式全屏模式。
     * 隐藏系统栏并将状态栏设置为透明。
     */
    public void enterFullScreenMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false);
        if (windowInsetsController != null) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            // 允许通过滑动手势临时显示系统栏
            windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            window.setStatusBarColor(Color.TRANSPARENT);
            // 如果内容较暗，状态栏图标显示为亮色；如果内容较亮，图标显示为暗色
            windowInsetsController.setAppearanceLightStatusBars(true);
        }
    }

    /**
     * 退出全屏模式并显示系统栏。
     *
     * @param barColorResId 要设置为状态栏颜色的资源 ID。
     */
    public void exitFullScreenMode(int barColorResId) {
        if (windowInsetsController != null) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
            window.setStatusBarColor(ContextCompat.getColor(context, barColorResId));
            windowInsetsController.setAppearanceLightStatusBars(false); // 亮色状态栏的图标变为暗色
        }
    }

}
