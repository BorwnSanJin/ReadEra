package com.example.readera.utiles;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import androidx.core.content.res.ResourcesCompat; // 用于字体

import com.example.readera.R; // 确保导入您的 R 文件

public class ReadingSettingsManager {

    private static final String PREFS_NAME = "reading_settings";
    private static final String KEY_TEXT_SIZE = "text_size_sp";
    private static final String KEY_LINE_SPACING = "line_spacing_dp";
    private static final String KEY_PADDING_LEFT = "padding_left_px";
    private static final String KEY_PADDING_TOP = "padding_top_px";
    private static final String KEY_PADDING_RIGHT = "padding_right_px";
    private static final String KEY_PADDING_BOTTOM = "padding_bottom_px";
    private static final String KEY_TEXT_COLOR = "text_color";
    private static final String KEY_FONT_PATH = "font_path"; // 用于存储字体文件路径或资源ID

    // --- 新增：书签相关键 ---
    private static final String KEY_BOOKMARK_PREFIX = "bookmark_"; // 用于存储特定文件的书签

    private SharedPreferences prefs;
    private Context context;

    // 默认值
    private static final int DEFAULT_TEXT_SIZE_SP = 18;
    private static final int DEFAULT_LINE_SPACING_DP = 8;
    private static final int DEFAULT_PADDING_DP = 16;
    private static final int DEFAULT_TEXT_COLOR = Color.BLACK;
    private static final String DEFAULT_FONT_PATH = ""; // 默认无特定字体

    public ReadingSettingsManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getTextSizeSp() {
        return prefs.getInt(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE_SP);
    }

    public void setTextSizeSp(int size) {
        prefs.edit().putInt(KEY_TEXT_SIZE, size).apply();
    }

    public int getLineSpacingExtraDp() {
        return prefs.getInt(KEY_LINE_SPACING, DEFAULT_LINE_SPACING_DP);
    }

    public void setLineSpacingExtraDp(int spacing) {
        prefs.edit().putInt(KEY_LINE_SPACING, spacing).apply();
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

    public int getTextColor() {
        return prefs.getInt(KEY_TEXT_COLOR, DEFAULT_TEXT_COLOR);
    }

    public void setTextColor(int color) {
        prefs.edit().putInt(KEY_TEXT_COLOR, color).apply();
    }

    public Typeface getTypeface() {
        String fontPath = prefs.getString(KEY_FONT_PATH, DEFAULT_FONT_PATH);
        if (!fontPath.isEmpty()) {
            try {
                // 如果是资源ID，例如 R.font.your_font
                int resId = Integer.parseInt(fontPath);
                return ResourcesCompat.getFont(context, resId);
            } catch (NumberFormatException e) {
                // 否则，尝试从 assets 或文件路径加载
                // 这里的实现取决于您如何存储和加载字体文件
                // 例如：return Typeface.createFromAsset(context.getAssets(), fontPath);
                // 或者更复杂的逻辑来处理外部文件
                return Typeface.DEFAULT; // 暂时返回默认字体
            }
        }
        return Typeface.DEFAULT; // 默认字体
    }

    public void setTypeface(String fontPath) {
        prefs.edit().putString(KEY_FONT_PATH, fontPath).apply();
    }

    /**
     * 将 dp 值转换为 px 值。
     */
    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    // --- 新增书签方法 ---

    /**
     * 生成一个唯一的书签键，基于文件URI。
     * 可以使用 URI 的字符串表示或其哈希值。
     * @param fileUri 文件的 URI
     * @return 对应文件书签的 SharedPreferences 键
     */
    private String getBookmarkKey(Uri fileUri) {
        // 使用 URI 的哈希值作为键的一部分，避免URI过长
        // 注意：URI的String形式可以很长，直接用作键可能不佳。
        // 一个更健壮的方法是使用文件路径的哈希值或更短的唯一标识符。
        // 这里为了简化，我们直接使用 URI 的 toString() 作为键的一部分。
        return KEY_BOOKMARK_PREFIX + fileUri.toString().hashCode();
    }

    /**
     * 保存指定文件的书签（当前页码）。
     * @param fileUri 文件的 URI
     * @param pageIndex 当前阅读的页码（从0开始）
     */
    public void saveBookmark(Uri fileUri, int pageIndex) {
        if (fileUri != null) {
            prefs.edit().putInt(getBookmarkKey(fileUri), pageIndex).apply();
        }
    }

    /**
     * 获取指定文件的书签（保存的页码）。
     * @param fileUri 文件的 URI
     * @return 保存的页码（从0开始），如果没有书签则返回0
     */
    public int getBookmark(Uri fileUri) {
        if (fileUri != null) {
            return prefs.getInt(getBookmarkKey(fileUri), 0); // 默认返回第一页 (0)
        }
        return 0;
    }

    /**
     * 移除指定文件的书签。
     * @param fileUri 文件的 URI
     */
    public void removeBookmark(Uri fileUri) {
        if (fileUri != null) {
            prefs.edit().remove(getBookmarkKey(fileUri)).apply();
        }
    }
}