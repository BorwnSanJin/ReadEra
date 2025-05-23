package com.example.readera.utiles;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.readera.R; // 假设 R.font.serif 在这里

public class ReadingSettingsManager {

    private final Context context;

    // 默认设置
    private int textSizeSp = 18;
    private int lineSpacingExtraDp = 4;
    private int pagePaddingLeftDp = 16;
    private int pagePaddingTopDp = 8;
    private int pagePaddingRightDp = 16;
    private int pagePaddingBottomDp = 8;
    private int textColorResId = android.R.color.black;
    private int fontResId = R.font.serif; // 默认字体

    public ReadingSettingsManager(Context context) {
        this.context = context;
        // 在实际应用中，您可以从 SharedPreferences 或数据库加载这些设置
    }

    public int getTextSizeSp() {
        return textSizeSp;
    }

    public int getLineSpacingExtraDp() {
        return lineSpacingExtraDp;
    }

    /**
     * 以像素为单位返回页面内边距值。
     * @return int 数组: [左, 上, 右, 下]
     */
    public int[] getPagePaddingPx() {
        return new int[]{
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pagePaddingLeftDp, context.getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pagePaddingTopDp, context.getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pagePaddingRightDp, context.getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pagePaddingBottomDp, context.getResources().getDisplayMetrics())
        };
    }

    public int getTextColor() {
        return ContextCompat.getColor(context, textColorResId);
    }

    public Typeface getTypeface() {
        try {
            return ResourcesCompat.getFont(context, fontResId);
        } catch (Exception e) {
            // 记录此错误
            return null; // 回退到系统默认字体
        }
    }

    // 如果这些设置可以被用户修改，您可以添加 setter 方法
    // public void setTextSizeSp(int textSizeSp) { this.textSizeSp = textSizeSp; }
    // ... 以及保存/加载设置的方法
}