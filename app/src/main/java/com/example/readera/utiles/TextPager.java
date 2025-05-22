package com.example.readera.utiles;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
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
    private String fullText;
    private List<String> pages;

    private TextPaint textPaint; // 现在我们从 NovelPageView 获取此对象
    private int visibleWidth; // 文本可用宽度，不包括 NovelPageView 的内部内边距
    private int visibleHeight; // 文本可用高度，不包括 NovelPageView 的内部内边距
    private int textSizeSp;
    private int lineSpacingExtraDp;

    // 我们将传递 NovelPageView 的 TextPaint 实例和内容尺寸
    // 或者让 TextPager 根据设置计算它们。
    // 为简单起见，让 TextPager 管理 TextPaint 属性。
    // 但它需要从视图获取实际的可见区域。

    public TextPager(Context context, String fullText) {
        this.context = context;
        this.fullText = fullText;
        this.pages = new ArrayList<>();
        this.textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(android.graphics.Color.BLACK); // 默认颜色，可以覆盖
    }

    public void setTextSize(int sp) {
        this.textSizeSp = sp;
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics()));
    }

    public void setLineSpacingExtra(int dp) {
        this.lineSpacingExtraDp = dp;
        // 行间距将由 StaticLayout 处理，但 TextPager 需要该值
        // 以正确预测能容纳多少文本。
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

    public List<String> getPages() {
        return pages;
    }

    public void paginate() {
        pages.clear();
        if (fullText == null || fullText.isEmpty() || visibleWidth <= 0 || visibleHeight <= 0) {
            Log.w(TAG, "无法分页：文本为空或可见区域未设置/为零。");
            return;
        }

        int start = 0;
        while (start < fullText.length()) {
            // 使用 StaticLayout 测量能容纳多少字符
            StaticLayout staticLayout;
            int end = fullText.length();
            int measuredLines = 0;

            // 迭代以找到适合的最大子字符串
            // 这个循环是为了找到完美的“end”索引
            // 更优化的方法是二分查找
            // 为简单起见，我们将逐字符或逐行增加
            int currentEnd = start;
            int lastGoodEnd = start;

            while (currentEnd <= fullText.length()) {
                String subText = fullText.substring(start, currentEnd);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    staticLayout = StaticLayout.Builder.obtain(subText, 0, subText.length(), textPaint, visibleWidth)
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, lineSpacingExtraDp, context.getResources().getDisplayMetrics()), 1.0f)
                            .setIncludePad(false)
                            .build();
                } else {
                    staticLayout = new StaticLayout(subText, textPaint, visibleWidth,
                            Layout.Alignment.ALIGN_NORMAL, 1.0f, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, lineSpacingExtraDp, context.getResources().getDisplayMetrics()), false);
                }

                if (staticLayout.getHeight() <= visibleHeight) {
                    lastGoodEnd = currentEnd;
                    if (currentEnd == fullText.length()) {
                        // 所有剩余文本都适合
                        break;
                    }
                    // 尝试容纳更多文本，为了性能逐词或逐大块增加
                    currentEnd = findNextWordEnd(fullText, currentEnd);
                    if (currentEnd == lastGoodEnd) { // 防止在找不到更多单词时无限循环
                        currentEnd++; // 回退到逐字符
                    }
                } else {
                    // 这块太大，lastGoodEnd 是能容纳的最大块
                    break;
                }
            }

            // 如果没有文本能容纳，这意味着 visibleHeight 或 Width 对于即使一个字符也太小了
            if (lastGoodEnd == start) {
                // 处理边缘情况，即使一个字符也不适合或循环卡住
                // 至少添加一个字符以避免在可见区域非常小的情况下无限循环
                if (start < fullText.length()) {
                    lastGoodEnd = Math.min(start + 1, fullText.length()); // 至少添加 1 个字符
                    // 即使此部分页面超出高度，也强制添加以防止无限循环
                    Log.w(TAG, "单个字符超出页面高度。强制对一个字符进行分页。");
                } else {
                    break; // 没有更多文本了
                }
            }


            String page = fullText.substring(start, lastGoodEnd);
            pages.add(page);
            start = lastGoodEnd;
            Log.d(TAG, "分页页面 " + pages.size() + ", 长度: " + page.length());
        }
        Log.d(TAG, "分页完成。总页数: " + pages.size());
    }

    // 辅助方法，用于查找下一个单词的结尾，如果没有找到空格则只增加
    private int findNextWordEnd(String text, int startIndex) {
        int nextSpace = text.indexOf(' ', startIndex + 1);
        int nextNewline = text.indexOf('\n', startIndex + 1);

        if (nextSpace == -1 && nextNewline == -1) {
            return text.length(); // 没有更多空格或换行符，取所有剩余文本
        }
        if (nextSpace == -1) {
            return nextNewline + 1; // 如果没有空格，优先换行符
        }
        if (nextNewline == -1) {
            return nextSpace + 1; // 如果没有换行符，优先空格
        }
        return Math.min(nextSpace, nextNewline) + 1; // 取空格或换行符中较早的一个
    }
}