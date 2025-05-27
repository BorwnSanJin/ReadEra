package com.example.readera.utiles;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    private static final String PREFS_NAME = "app_theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    // 主题模式常量，与 AppCompatDelegate.MODE_NIGHT_xxx 对应
    public static final int THEME_MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO; // 亮色模式
    public static final int THEME_MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES; // 深色模式
    public static final int THEME_MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; // 根据系统设置

    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int themeMode = prefs.getInt(KEY_THEME_MODE, THEME_MODE_SYSTEM); // 默认跟随系统
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    public static void saveThemeMode(Context context, int themeMode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_THEME_MODE, themeMode);
        editor.apply();
    }

    public static int getSavedThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, THEME_MODE_SYSTEM);
    }

    /**
     * 检查当前是否处于深色模式 (通过 AppCompatDelegate 的设置)
     * 注意：这不代表系统是深色模式，而是应用当前生效的模式
     */
    public static boolean isDarkModeEnabled(Context context) {
        int savedMode = getSavedThemeMode(context);
        if (savedMode == THEME_MODE_DARK) {
            return true;
        } else if (savedMode == THEME_MODE_LIGHT) {
            return false;
        } else { // THEME_MODE_SYSTEM, 判断系统当前是否为深色模式
            return (context.getResources().getConfiguration().uiMode &
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
    }
}