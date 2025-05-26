package com.example.readera.utiles;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat; // 用于字体

import com.example.readera.R; // 确保导入您的 R 文件
import com.example.readera.model.Bookmark;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ReadingSettingsManager {

    private static final String PREFS_NAME = "reading_settings";
    private static final String KEY_TEXT_SIZE = "text_size_sp";
    private static final String KEY_LINE_SPACING = "line_spacing_dp";
    private static final String KEY_PADDING_LEFT = "padding_left_px";
    private static final String KEY_PADDING_TOP = "padding_top_px";
    private static final String KEY_PADDING_RIGHT = "padding_right_px";
    private static final String KEY_PADDING_BOTTOM = "padding_bottom_px";
    private static final String KEY_TEXT_COLOR = "text_color";
    private static final String KEY_BACKGROUND_COLOR = "background_color"; // 新增背景颜色键
    private static final String KEY_FONT_PATH = "font_path"; // 用于存储字体文件路径或资源ID

    // --- 修改：书签相关键 ---
    // KEY_LAST_READ_PAGE_PREFIX 用于存储每个文件的最后阅读页码（自动保存的进度）
    private static final String KEY_LAST_READ_PAGE_PREFIX = "last_read_page_";
    // KEY_ALL_BOOKMARKS 用于存储用户手动添加的书签列表
    private static final String KEY_ALL_BOOKMARKS = "all_user_bookmarks";

    private SharedPreferences prefs;
    private Context context;
    private Gson gson;

    // 默认值
    private static final float  DEFAULT_TEXT_SIZE_SP = 18f;
    private static final float  DEFAULT_LINE_SPACING_DP = 8f;
    private static final int DEFAULT_PADDING_DP = 16;
    private static final int DEFAULT_BACKGROUND_COLOR_RES = R.color.reading_bg_white; // 默认白色
    private static final int DEFAULT_TEXT_COLOR = R.color.reading_text_dark;// 默认深色文本
    private static final String DEFAULT_FONT_NAME  = "system_default"; // 默认无特定字体
    // --- 单例模式相关修改 ---
    private static ReadingSettingsManager instance;
    public ReadingSettingsManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // --- 关键修改：使用 GsonBuilder 注册 UriTypeAdapter ---
        gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriTypeAdapter())
                .create();
    }
    // 提供一个公共的静态方法来获取实例
    public static synchronized ReadingSettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new ReadingSettingsManager(context);
        }
        return instance;
    }

    public float getTextSizeSp() {
        return prefs.getFloat(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE_SP);
    }

    public void saveTextSize(float textSizeSp) {
        prefs.edit().putFloat(KEY_TEXT_SIZE, textSizeSp).apply();
    }

    //行间距设置
    public float  getLineSpacingExtraDp() {
        return prefs.getFloat(KEY_LINE_SPACING, DEFAULT_LINE_SPACING_DP);
    }

    public void saveLineSpacingExtra(int lineSpacingDp) {
        prefs.edit().putFloat(KEY_LINE_SPACING, lineSpacingDp).apply();
    }

    /**
     * 获取页边距数组 [left, top, right, bottom]，单位为像素 (px)。
     */
    public int[] getPagePaddingPx() {
        int defaultPaddingPx = dpToPx(DEFAULT_PADDING_DP);
        int left = prefs.getInt(KEY_PADDING_LEFT, defaultPaddingPx);
        int top = prefs.getInt(KEY_PADDING_TOP, defaultPaddingPx);
        int right = prefs.getInt(KEY_PADDING_RIGHT, defaultPaddingPx);
        int bottom = prefs.getInt(KEY_PADDING_BOTTOM, defaultPaddingPx);
        return new int[]{left, top, right, bottom};
    }

    /**
     * 设置页边距，单位为像素 (px)。
     */
    public void setPagePaddingPx(int left, int top, int right, int bottom) {
        prefs.edit()
                .putInt(KEY_PADDING_LEFT, left)
                .putInt(KEY_PADDING_TOP, top)
                .putInt(KEY_PADDING_RIGHT, right)
                .putInt(KEY_PADDING_BOTTOM, bottom)
                .apply();
    }

    //文本颜色设置
    public int getTextColor() {
        return prefs.getInt(KEY_TEXT_COLOR, DEFAULT_TEXT_COLOR);
    }

    public void saveTextColor(int color) {
        prefs.edit().putInt(KEY_TEXT_COLOR, color).apply();
    }

    // 新增：获取背景颜色
    public int getBackgroundColor() {
        int defaultColor = ContextCompat.getColor(context, DEFAULT_BACKGROUND_COLOR_RES);
        // 默认背景颜色是一个资源ID，需要转换为实际的int颜色值进行存储和比较
        return prefs.getInt(KEY_BACKGROUND_COLOR, defaultColor);
    }

    public void saveBackgroundColor(int newBgColor) {
        prefs.edit().putInt(KEY_BACKGROUND_COLOR, newBgColor).apply();
    }

    //字体设置
    public Typeface getTypeface() {
        String fontIdentifier = prefs.getString(KEY_FONT_PATH, DEFAULT_FONT_NAME);
        switch (fontIdentifier) {
            case "serif":
                try {
                    return ResourcesCompat.getFont(context, R.font.serif);
                } catch (Exception e) {
                    Log.e("ReadingSettingsManager", "Error loading serif font: " + e.getMessage());
                    return Typeface.DEFAULT;
                }
            case "system_default":
            default:
                return Typeface.DEFAULT;
        }
    }

    public void saveFont(String fontIdentifier) {
        prefs.edit().putString(KEY_FONT_PATH, fontIdentifier).apply();
    }

    /**
     * 将 dp 值转换为 px 值。
     */
    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    // --- 新增书签方法 ---

    /**
     * 获取用于存储特定文件最后阅读页码的键。
     * 使用 URI 的字符串表示作为键，因为它是唯一的。
     * @param fileUri 文件的 URI
     * @return 对应文件最后阅读页码的 SharedPreferences 键
     */
    private String getLastReadPageKey(Uri fileUri) {
        if (fileUri == null){
            return "";
        }
        return KEY_LAST_READ_PAGE_PREFIX  + fileUri.toString();
    }

    /**
     * 保存指定文件的**最后阅读页码**（自动保存的进度）。
     * @param fileUri 文件的 URI
     * @param pageIndex 当前阅读的页码（从0开始）
     */
    public void saveLastReadPage(Uri fileUri, int pageIndex) {
        if (fileUri != null) {
            prefs.edit().putInt(getLastReadPageKey(fileUri), pageIndex).apply();
            Log.d("ReadingSettingsManager", "保存最后阅读页码: " + pageIndex + " for " + fileUri.toString());
        }
    }

    /**
     * 获取指定文件的**最后阅读页码**。
     * @param fileUri 文件的 URI
     * @return 保存的页码（从0开始），如果没有保存则返回0
     */
    public int getLastReadPage(Uri fileUri) {
        if (fileUri != null) {
            return prefs.getInt(getLastReadPageKey(fileUri), 0); // 默认返回第一页 (0)
        }
        return 0;
    }
    // --- 新增：管理用户手动添加的书签列表的方法 ---
    public void addBookmark (Bookmark bookmark){
        List<Bookmark> bookmarks = getAllBookmarks();
        // 检查是否已存在完全相同的书签，避免重复添加
        if (!bookmarks.contains(bookmark)) {
            bookmarks.add(0, bookmark); // 新书签添加到列表顶部，最新添加的显示在前面
            saveAllBookmarks(bookmarks);
            Log.d("ReadingSettingsManager", "书签已添加: " + bookmark.getDisplayTitle() + ", 页码: " + bookmark.getPageNumber());
        } else {
            Log.d("ReadingSettingsManager", "书签已存在，未重复添加: " + bookmark.getDisplayTitle() + ", 页码: " + bookmark.getPageNumber());
        }
    }

    /**
            * 移除一个书签。
            * @param bookmark 要移除的 Bookmark 对象
     */
    public void removeBookmark(Bookmark bookmark) {
        List<Bookmark> bookmarks = getAllBookmarks();
        // 使用 Iterator 安全地移除元素，避免ConcurrentModificationException
        Iterator<Bookmark> iterator = bookmarks.iterator();
        boolean removed = false;
        while (iterator.hasNext()) {
            Bookmark b = iterator.next();
            if (b.equals(bookmark)) { // 使用 Bookmark 类的 equals 方法进行比较
                iterator.remove();
                removed = true;
                break; // 假设书签是唯一的，找到并移除后即可退出
            }
        }

        if (removed) {
            saveAllBookmarks(bookmarks);
            Log.d("ReadingSettingsManager", "书签已移除: " + bookmark.getDisplayTitle() + ", 页码: " + bookmark.getPageNumber());
        } else {
            Log.d("ReadingSettingsManager", "书签未找到，无法移除: " + bookmark.getDisplayTitle() + ", 页码: " + bookmark.getPageNumber());
        }
    }
    /**
     * 获取所有用户手动添加的书签列表。
     * @return 书签列表，如果没有则返回空列表
     */
    public List<Bookmark> getAllBookmarks() {
        String json = prefs.getString(KEY_ALL_BOOKMARKS, "[]");
        Type type = new TypeToken<ArrayList<Bookmark>>() {}.getType();
        List<Bookmark> bookmarks = gson.fromJson(json, type);
        return bookmarks != null ? bookmarks : new ArrayList<>();
    }

    /**
     * 保存整个用户书签列表。
     * 这是一个私有方法，供 addBookmark 和 removeBookmark 内部调用。
     * @param bookmarks 要保存的书签列表
     */
    private void saveAllBookmarks(List<Bookmark> bookmarks) {
        String json = gson.toJson(bookmarks);
        prefs.edit().putString(KEY_ALL_BOOKMARKS, json).apply();
    }

}