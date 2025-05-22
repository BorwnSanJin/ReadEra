package com.example.readera.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

public class NovelPageView extends View {

    private TextPaint textPaint;
    private StaticLayout staticLayout;
    private String pageText;

    // 阅读设置
    private int textSizePx; // 文本大小，单位像素
    private int textColor; // 文本颜色
    private int lineSpacingExtraPx; // 行间距，单位像素
    private Typeface textTypeface; // 字体
    private int pagePaddingLeft, pagePaddingTop, pagePaddingRight, pagePaddingBottom; // 页面内边距

    public NovelPageView(Context context) {
        super(context);
        init(context, null);
    }

    public NovelPageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public NovelPageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG); // 开启抗锯齿，使文本更平滑
        // 默认设置 - 这些将在 ReadingActivity 中更精确地设置
        setTextColor(android.graphics.Color.BLACK); // 默认黑色文本
        setTextSize(18); // 默认 18sp
        setLineSpacingExtra(4); // 默认 4dp
        // 替换为您的字体，例如：
        // setTypeface(ResourcesCompat.getFont(context, R.font.your_font_name));
        // 或者使用系统字体：setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));

        // 页面内容默认内边距，如果父布局未设置
        setPagePadding(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics())
        );
    }

    // --- 公共设置方法，用于阅读设置 ---

    public void setTextSize(int sp) {
        this.textSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
        textPaint.setTextSize(textSizePx);
        requestLayout(); // 文本大小改变时请求重新测量布局
        invalidate(); // 重绘视图
    }

    public void setTextColor(int color) {
        this.textColor = color;
        textPaint.setColor(textColor);
        invalidate(); // 重绘视图
    }

    public void setLineSpacingExtra(int dp) {
        this.lineSpacingExtraPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
        requestLayout();
        invalidate();
    }

    public void setTypeface(Typeface typeface) {
        this.textTypeface = typeface;
        textPaint.setTypeface(textTypeface);
        requestLayout();
        invalidate();
    }

    public void setPagePadding(int left, int top, int right, int bottom) {
        this.pagePaddingLeft = left;
        this.pagePaddingTop = top;
        this.pagePaddingRight = right;
        this.pagePaddingBottom = bottom;
        requestLayout();
        invalidate();
    }

    /**
     * 设置页面的文本内容。
     * @param text 要显示在此页面的字符串内容。
     */
    public void setPageText(String text) {
        this.pageText = text;
        recreateStaticLayout(); // 文本或尺寸改变时重新创建布局
        invalidate(); // 请求重绘
    }

    /**
     * 使用当前尺寸和文本重新创建 StaticLayout。
     * 这对于正确的文本换行至关重要。
     */
    private void recreateStaticLayout() {
        if (getWidth() == 0 || getHeight() == 0 || pageText == null) {
            staticLayout = null; // 如果尺寸未准备好，则不创建
            return;
        }

        // 计算文本可用的宽度，考虑内边距
        int availableWidth = getWidth() - pagePaddingLeft - pagePaddingRight;

        if (availableWidth <= 0) {
            staticLayout = null;
            return;
        }

        // 对于 API 23+ 优先使用 StaticLayout.Builder
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            staticLayout = StaticLayout.Builder.obtain(pageText, 0, pageText.length(), textPaint, availableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(lineSpacingExtraPx, 1.0f) // add, mult (添加额外间距，倍数)
                    .setIncludePad(false) // 是否包含额外的垂直填充（顶部/底部）
                    .build();
        } else {
            // 对于较新的 API 已废弃，但为了兼容性需要
            staticLayout = new StaticLayout(pageText, textPaint, availableWidth,
                    Layout.Alignment.ALIGN_NORMAL, 1.0f, lineSpacingExtraPx, false);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 测量后，如果尺寸改变，重新创建 StaticLayout
        recreateStaticLayout();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            recreateStaticLayout(); // 视图尺寸改变时重新创建
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (staticLayout != null) {
            // 绘制前应用内边距
            canvas.save();
            canvas.translate(pagePaddingLeft, pagePaddingTop);
            staticLayout.draw(canvas);
            canvas.restore();
        }
    }

    /**
     * 将 DP 转换为 PX 的辅助方法，用于 TextPager 中的内容测量。
     * @param dpValue DP 值
     * @return PX 值
     */
    public int dpToPx(int dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, getResources().getDisplayMetrics());
    }

    /**
     * 返回文本可用的实际内容宽度，不包括内边距。
     * 由 TextPager 用于计算可见区域。
     */
    public int getContentWidth() {
        return getWidth() - pagePaddingLeft - pagePaddingRight;
    }

    /**
     * 返回文本可用的实际内容高度，不包括内边距。
     * 由 TextPager 用于计算可见区域。
     */
    public int getContentHeight() {
        return getHeight() - pagePaddingTop - pagePaddingBottom;
    }

    /**
     * 返回用于渲染的当前 TextPaint。
     * 由 TextPager 用于获取字体指标。
     */
    public TextPaint getTextPaint() {
        return textPaint;
    }
}