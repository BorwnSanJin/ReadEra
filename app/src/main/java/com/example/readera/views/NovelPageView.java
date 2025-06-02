package com.example.readera.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
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

    // 新增：系统栏内边距
    private int statusBarPadding = 0;
    private static final String TAG = "NovelPageView"; // 添加 TAG


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
     * 设置系统栏（状态栏和导航栏）引起的额外内边距。
     * 这个方法应该由外部调用，当获取到 Insets 后设置。
     * @param statusBarPadding 状态栏高度（像素）
     */
    public void setSystemBarPadding(int statusBarPadding) {
        if (this.statusBarPadding != statusBarPadding ) {
            this.statusBarPadding = statusBarPadding;
            Log.d(TAG, "setSystemBarPadding: statusBar=" + statusBarPadding );
            recreateStaticLayout(); // 系统栏内边距变化也可能影响布局
            invalidate();
        }
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

        // 计算实际可用于绘制文本的宽度和高度
        // 总宽度 - 左内边距 - 右内边距
        int availableWidth = getWidth() - pagePaddingLeft - pagePaddingRight;
        // 总高度 - 上内边距 - 下内边距 - 状态栏内边距 - 导航栏内边距
        int availableHeight = getHeight() - pagePaddingTop - pagePaddingBottom - statusBarPadding;

        if (availableWidth <= 0 || availableHeight <= 0) {
            Log.w(TAG, "Available content size is too small: " + availableWidth + "x" + availableHeight +
                    ". Check paddings and system bar heights.");
            staticLayout = null;
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            staticLayout = StaticLayout.Builder.obtain(pageText, 0, pageText.length(), textPaint, availableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(dpToPx(lineSpacingExtraDp), 1.0f)
                    .setIncludePad(false) // 通常设置为 false，让 StaticLayout 内部自己处理行高
                    .build();
        } else {
            // 对于 API < 23，使用旧的 StaticLayout 构造函数
            staticLayout = new StaticLayout(pageText, textPaint, availableWidth,
                    Layout.Alignment.ALIGN_NORMAL, 1.0f, dpToPx(lineSpacingExtraDp), false);
        }

        // 可以在这里添加一个检查，如果 staticLayout 的高度超出了 availableHeight，
        // 则说明分页计算可能不准确，或者文本内容过多。
        if (staticLayout != null && staticLayout.getHeight() > availableHeight) {
            Log.w(TAG, "StaticLayout height (" + staticLayout.getHeight() + ") exceeds available height (" + availableHeight + "). " +
                    "This page might have truncated content. Check TextPager logic.");
            // 实际上，TextPager 应该已经根据这个 availableHeight 进行了分页，
            // 如果这里超出，可能是计算误差或 TextPager 的逻辑问题。
            // 在 NovelPageView 层面通常不进行文本裁剪，而是由分页器处理。
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 在 onMeasure 之后，视图的尺寸 (getWidth(), getHeight()) 才是最终确定的
        // 所以在这里调用 recreateStaticLayout 是合适的，如果尺寸发生变化的话
        // onSizeChanged 也会触发 recreateStaticLayout，两者配合确保正确性
        if (getWidth() != 0 && getHeight() != 0 && staticLayout == null) {
            recreateStaticLayout();
        }
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
            canvas.translate(pagePaddingLeft, pagePaddingTop+statusBarPadding);
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