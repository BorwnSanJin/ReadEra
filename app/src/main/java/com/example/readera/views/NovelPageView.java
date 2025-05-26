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

import java.util.Objects;

//自定义view
public class NovelPageView extends View {

    private TextPaint textPaint;
    private StaticLayout staticLayout;
    private String pageText = "";

    // 阅读设置
    private float  textSizeSp; // 文本大小，单位像素
    private int textColor; // 文本颜色
    private float  lineSpacingExtraDp; // 行间距，单位像素
    private Typeface textTypeface; // 字体
    private int pagePaddingLeft, pagePaddingTop, pagePaddingRight, pagePaddingBottom; // 页面内边距

    public NovelPageView(Context context) {
        super(context);
        init();
    }

    public NovelPageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NovelPageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG); // 开启抗锯齿，使文本更平滑
        // 默认设置 - 这些将被 ReadingSettingsManager 覆盖
        this.textSizeSp = 18f;
        this.lineSpacingExtraDp = 8f;
        this.textColor = android.graphics.Color.BLACK;
        this.textTypeface = Typeface.DEFAULT;
        textPaint.setColor(this.textColor);
        textPaint.setTextSize(spToPx(this.textSizeSp));
        textPaint.setTypeface(this.textTypeface);

        int defaultPaddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        setPagePadding(defaultPaddingPx, defaultPaddingPx / 2, defaultPaddingPx, defaultPaddingPx / 2);
    }

    // --- 公共设置方法，用于阅读设置 ---

    public void setTextSize(float  sp) {
        if (this.textSizeSp != sp) {
            this.textSizeSp = sp;
            textPaint.setTextSize(spToPx(this.textSizeSp));
            recreateStaticLayout();
            invalidate();
        }
    }

    public void setTextColor(int color) {
        if (this.textColor != color) {
            this.textColor = color;
            textPaint.setColor(this.textColor);
            invalidate();
        }
    }

    public void setLineSpacingExtra(float dp) {
        if (this.lineSpacingExtraDp != dp) {
            this.lineSpacingExtraDp = dp;
            recreateStaticLayout();
            invalidate();
        }
    }

    public void setTypeface(Typeface typeface) {
        if (!Objects.equals(this.textTypeface, typeface)) {
            this.textTypeface = typeface;
            textPaint.setTypeface(this.textTypeface);
            recreateStaticLayout();
            invalidate();
        }
    }

    public void setPagePadding(int left, int top, int right, int bottom) {
        if (this.pagePaddingLeft != left || this.pagePaddingTop != top ||
                this.pagePaddingRight != right || this.pagePaddingBottom != bottom) {
            this.pagePaddingLeft = left;
            this.pagePaddingTop = top;
            this.pagePaddingRight = right;
            this.pagePaddingBottom = bottom;
            recreateStaticLayout();
            invalidate();
        }
    }

    /**
     * 设置页面的文本内容。
     * @param text 要显示在此页面的字符串内容。
     */
    public void setPageText(String text) {
        if (text == null) {
            this.pageText = "";
        } else {
            this.pageText = text;
        }
        recreateStaticLayout();
        invalidate();
    }

    /**
     * 使用当前尺寸和文本重新创建 StaticLayout。
     * 这对于正确的文本换行至关重要。
     */
    private void recreateStaticLayout() {
        if (getWidth() == 0 || getHeight() == 0 || pageText == null) {
            staticLayout = null;
            return;
        }

        int availableWidth = getWidth() - pagePaddingLeft - pagePaddingRight;
        if (availableWidth <= 0) {
            staticLayout = null;
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            staticLayout = StaticLayout.Builder.obtain(pageText, 0, pageText.length(), textPaint, availableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(dpToPx(lineSpacingExtraDp), 1.0f)
                    .setIncludePad(false)
                    .build();
        } else {
            staticLayout = new StaticLayout(pageText, textPaint, availableWidth,
                    Layout.Alignment.ALIGN_NORMAL, 1.0f, dpToPx(lineSpacingExtraDp), false);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
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

    private float spToPx(float spValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, getResources().getDisplayMetrics());
    }

    /**
     * 将 DP 转换为 PX 的辅助方法，用于 TextPager 中的内容测量。
     * @param dpValue DP 值
     * @return PX 值
     */
    private float dpToPx(float dpValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, getResources().getDisplayMetrics());
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