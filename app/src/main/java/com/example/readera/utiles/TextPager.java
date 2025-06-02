package com.example.readera.utiles;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.util.TypedValue;
import java.util.ArrayList;
import java.util.List;

public class TextPager {
    private static final String TAG = "TextPager";

    private Context context;
    private String fullText; // 完整文本内容
    private List<String> pages; // 存储分页后的每一页内容

    private TextPaint textPaint; // 用于文本测量的画笔
    private int visibleWidth; // 文本可用的宽度（像素），不包括 NovelPageView 的内部内边距
    private int visibleHeight; // 文本可用的高度（像素），不包括 NovelPageView 的内部内边距
    private float textSizeSp; // 字体大小（sp单位）
    private float lineSpacingExtraDp; // 额外行间距（dp单位）
    private Typeface typeface; // 新增：用于文本测量的字体

    // 用于整个文本或大块文本的 StaticLayout 实例
    private StaticLayout fullLayout;
    private float lineSpacingExtraPx; // 预先计算的行间距像素值

    public TextPager(Context context, String fullText) {
        this.context = context.getApplicationContext(); // 使用 Application Context 避免内存泄漏
        this.fullText = (fullText != null) ? fullText : ""; // **关键：确保 fullText 不为 null**
        this.pages = new ArrayList<>(); // 初始化 pages 列表
        this.textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG); // 启用抗锯齿
        textPaint.setColor(android.graphics.Color.BLACK); // 默认颜色，可根据主题更改
    }

    // 设置文本大小
    public void setTextSize(float sp) {
        this.textSizeSp = sp;
    }

    // 设置额外行间距
    public void setLineSpacingExtra(float dp) {
        this.lineSpacingExtraDp = dp;
    }

    /**
     * 设置文本将被渲染区域的实际像素尺寸。
     * 这应该是 NovelPageView 内部内边距 *之内* 的宽度和高度。
     * @param widthPx 可用宽度，单位像素。
     * @param heightPx 可用高度，单位像素。
     */
    public void setVisibleArea(int widthPx, int heightPx) {
        this.visibleWidth = widthPx;
        this.visibleHeight = heightPx;
    }

    // 设置字体
    public void setTypeface(Typeface typeface) {
        this.typeface = typeface;
    }

    // 获取分页后的页面列表
    public List<String> getPages() {
        return pages;
    }

    // 获取当前字体大小（sp）
    public float getTextSize() {
        return textSizeSp;
    }

    // 获取当前行间距（dp）
    public float getLineSpacingExtra() {
        return lineSpacingExtraDp;
    }

    // 获取可见区域宽度
    public int getVisibleAreaWidth() {
        return visibleWidth;
    }

    // 获取可见区域高度
    public int getVisibleAreaHeight() {
        return visibleHeight;
    }

    // 获取当前字体
    public Typeface getTypeface() {
        return typeface;
    }

    /**
     * 对完整文本进行分页，生成每一页的内容。
     * **关键：在每次调用时都会清除之前的分页结果并重新计算。**
     * @throws InterruptedException 如果分页任务被中断
     */
    public void paginate() throws InterruptedException {
        // **关键：在每次分页开始时清空旧的页面数据**
        pages.clear();
        fullLayout = null; // 清除旧的 StaticLayout 实例

        if (fullText.isEmpty() || visibleWidth <= 0 || visibleHeight <= 0) {
            Log.w(TAG, "无法分页：文本为空或可见区域未设置/为零。");
            return;
        }

        // 核心：在分页前一次性设置并计算所有必要的 TextPaint 属性和行间距
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, textSizeSp, context.getResources().getDisplayMetrics()));
        textPaint.setTypeface(typeface);
        this.lineSpacingExtraPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, lineSpacingExtraDp, context.getResources().getDisplayMetrics());

        // 步骤 1: 为整个文本创建单个 StaticLayout
        // 使用 builder 模式来创建 StaticLayout，更灵活且推荐
        StaticLayout.Builder builder = StaticLayout.Builder.obtain(fullText, 0, fullText.length(), textPaint, visibleWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL) // 文本对齐方式
                .setLineSpacing(lineSpacingExtraPx, 1.0f) // 设置行间距（额外像素，乘数）
                .setIncludePad(false); // 与 NovelPageView 渲染保持一致，不包含 StaticLayout 默认的额外填充

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            fullLayout = builder.build();
        } else {
            // 对于旧版 API (API < 23)，使用旧的构造函数
            // 注意：旧构造函数的 lineSpacingMultiplier 默认为 1.0f，lineSpacingAdd 为 extra。
            // 这里我们希望 lineSpacingExtraPx 是 extra，所以 multiplier 保持 1.0f。
            fullLayout = new StaticLayout(fullText, textPaint, visibleWidth,
                    Layout.Alignment.ALIGN_NORMAL, 1.0f, lineSpacingExtraPx, false); // false 代表 includePad
        }

        int startLine = 0; // 当前页面在 fullLayout 中的起始行索引

        // 循环直到所有行都被分页
        while (startLine < fullLayout.getLineCount()) {
            // **关键：在每次循环开始时检查中断状态**
            if (Thread.currentThread().isInterrupted()) {
                Log.d(TAG, "TextPager 分页任务被中断。");
                throw new InterruptedException("Pagination task interrupted."); // 抛出中断异常
            }

            // 步骤 2: 遍历 fullLayout 中的行，以找到页面边界
            int pageEndLine = startLine;
            while (pageEndLine < fullLayout.getLineCount()) {
                // 计算当前页的理论高度（从 startLine 到 pageEndLine - 1）
                int currentBlockHeight = fullLayout.getLineBottom(pageEndLine) - fullLayout.getLineTop(startLine);

                // 如果加上当前行会超出页面高度
                if (currentBlockHeight > visibleHeight) {
                    // 如果是第一行就超出高度，那么这一行就是一页
                    if (pageEndLine == startLine) {
                        pageEndLine++; // 强制包含这一行，即使它超出高度
                    }
                    break; // 找到当前页的结束位置
                }
                pageEndLine++; // 移动到下一行
            }

            // 步骤 3: 计算当前页的文本范围
            int pageStartOffset = fullLayout.getLineStart(startLine);
            int pageEndOffset;

            if (pageEndLine > fullLayout.getLineCount()) {
                // 如果 pageEndLine 超出了总行数，说明所有剩余文本都适合当前页
                pageEndOffset = fullText.length();
            } else {
                // 正常情况：当前页的结束偏移量是下一页的起始行偏移量
                pageEndOffset = fullLayout.getLineStart(pageEndLine);
            }

            // 安全检查和极端情况处理
            // 确保偏移量有效且不越界
            pageStartOffset = Math.max(0, pageStartOffset);
            pageEndOffset = Math.min(pageEndOffset, fullText.length());

            // 确保结束偏移量不小于起始偏移量
            // 如果计算出的页面范围为空，但文本仍有内容，强制前进一个字符以避免死循环
            if (pageStartOffset == pageEndOffset && pageStartOffset < fullText.length()) {
                pageEndOffset = Math.min(pageStartOffset + 1, fullText.length());
                Log.w(TAG, "由于内容不适合，强制分页前进一个字符。");
            } else if (pageStartOffset == pageEndOffset && pageStartOffset == fullText.length()) {
                // 如果已经到达文本末尾且没有新增内容，则退出循环
                break;
            }

            // 截取当前页的文本内容并添加到 pages 列表中
            String pageContent = fullText.substring(pageStartOffset, pageEndOffset);
            pages.add(pageContent);
            //Log.d(TAG, "分页页面 " + pages.size() + ", 行范围 [" + startLine + ", " + (pageEndLine - 1) + "], 文本长度: " + pageContent.length());

            // 移动到下一页的起始行
            startLine = pageEndLine;
        }
        Log.d(TAG, "分页完成。总页数: " + pages.size());
    }
}