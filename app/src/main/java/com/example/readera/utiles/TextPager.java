package com.example.readera.utiles;

import android.content.Context;
import android.graphics.Paint;
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
    private int textSizeSp; // 字体大小（sp单位），用于设置 textPaint
    private int lineSpacingExtraDp; // 额外行间距（dp单位）

    // 用于整个文本或大块文本的 StaticLayout 实例
    private StaticLayout fullLayout;
    private float lineSpacingExtraPx; // 预先计算的行间距像素值

    public TextPager(Context context, String fullText) {
        this.context = context;
        this.fullText = fullText;
        this.pages = new ArrayList<>();
        this.textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG); // 启用抗锯齿
        textPaint.setColor(android.graphics.Color.BLACK); // 默认颜色，可覆盖
    }

    public void setTextSize(int sp) {
        this.textSizeSp = sp;
        // 将 sp 单位转换为像素，并设置给 textPaint
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics()));
    }

    public void setLineSpacingExtra(int dp) {
        this.lineSpacingExtraDp = dp;
        // 将 dp 单位转换为像素，用于 StaticLayout
        this.lineSpacingExtraPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
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

    /**
     * 对完整文本进行分页，生成每一页的内容。
     * 优化点：通过一次性创建 StaticLayout 并利用其行信息来提高效率。
     */
    public void paginate() {
        pages.clear();
        if (fullText == null || fullText.isEmpty() || visibleWidth <= 0 || visibleHeight <= 0) {
            Log.w(TAG, "无法分页：文本为空或可见区域未设置/为零。");
            return;
        }

        // --- 步骤 1: 为整个文本创建单个 StaticLayout ---
        // 这是最重要的性能优化。 StaticLayout 会处理文本换行和测量所有行。
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            fullLayout = StaticLayout.Builder.obtain(fullText, 0, fullText.length(), textPaint, visibleWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL) // 文本对齐方式
                    .setLineSpacing(lineSpacingExtraPx, 1.0f) // 设置行间距（额外像素，乘数）
                    .setIncludePad(false) // 与 NovelPageView 渲染保持一致，不包含 StaticLayout 默认的额外填充
                    .build();
        } else {
            // 对于旧版 API (API < 23)
            fullLayout = new StaticLayout(fullText, textPaint, visibleWidth,
                    Layout.Alignment.ALIGN_NORMAL, 1.0f, lineSpacingExtraPx, false); // 与 NovelPageView 渲染保持一致
        }

        int startLine = 0; // 当前页面在 fullLayout 中的起始行索引
        int currentTextOffset = 0; // 当前页面在 fullText 中的起始字符偏移量（用于日志和最终截取）

        // 循环直到所有行都被分页
        while (startLine < fullLayout.getLineCount()) {
            int currentPageHeight = 0; // 当前页已累积的高度
            int endLine = startLine; // 当前页面在 fullLayout 中的结束行索引（不包含）

            // --- 步骤 2: 遍历 fullLayout 中的行，以找到页面边界 ---
            while (endLine < fullLayout.getLineCount()) {
                // 获取当前行的高度
                int lineHeight = fullLayout.getLineBottom(endLine) - fullLayout.getLineTop(endLine);

                if (currentPageHeight + lineHeight > visibleHeight) {
                    // 如果加上当前行会超出页面高度，则当前行是下一页的第一行。
                    // 特殊情况：如果这是当前页的第一行（即 endLine == startLine），并且它本身就超出了可见高度，
                    // 我们仍然必须包含它，否则会陷入无限循环且无法前进。
                    if (endLine == startLine) {
                        endLine++; // 强制包含这一行，即使它超出高度
                    }
                    break; // 找到当前页的结束位置
                }
                currentPageHeight += lineHeight; // 累加行高
                endLine++; // 移动到下一行
            }

            // --- 步骤 3: 计算当前页的文本范围 ---
            // 获取当前页在 fullText 中的起始字符偏移量
            int pageStartOffset = fullLayout.getLineStart(startLine);
            int pageEndOffset; // 当前页在 fullText 中的结束字符偏移量

            // 根据循环结束时的 endLine 值确定结束偏移量
            if (endLine > fullLayout.getLineCount()) {
                // 如果 endLine 超出了总行数，说明所有剩余文本都适合当前页，取最后一行的结束偏移量
                pageEndOffset = fullLayout.getLineEnd(fullLayout.getLineCount() - 1);
            } else if (endLine == startLine) {
                // 如果只添加了一行（例如，因为第一行就溢出了），或者根本没添加行
                // 此时 endLine 仍等于 startLine，但我们必须保证至少前进一个字符，防止死循环。
                // 默认取当前行的结束偏移量
                pageEndOffset = fullLayout.getLineEnd(startLine);
                // 额外的安全检查：如果计算出的 pageEndOffset 没有前进，则至少推进一个字符
                if (pageEndOffset <= pageStartOffset && pageStartOffset < fullText.length()) {
                    pageEndOffset = Math.min(pageStartOffset + 1, fullText.length());
                    Log.w(TAG, "由于内容不适合，强制分页前进一个字符。");
                }
            } else {
                // 正常情况：当前页的结束是前一行的结束（endLine - 1）
                pageEndOffset = fullLayout.getLineEnd(endLine - 1);
            }

            // 安全检查：确保结束偏移量不会超出 fullText 的实际长度
            if (pageEndOffset > fullText.length()) {
                pageEndOffset = fullText.length();
            }

            // 安全检查：确保起始偏移量有效且不大于结束偏移量
            if (pageStartOffset < 0) pageStartOffset = 0;
            if (pageStartOffset >= fullText.length() && fullText.length() > 0) {
                pageStartOffset = fullText.length() -1;
            }
            if (pageEndOffset < pageStartOffset) {
                pageEndOffset = pageStartOffset; // 确保结束不小于开始
            }


            // 截取当前页的文本内容并添加到 pages 列表中
            String pageContent = fullText.substring(pageStartOffset, pageEndOffset);
            pages.add(pageContent);
            Log.d(TAG, "分页页面 " + pages.size() + ", 长度: " + pageContent.length() + ", 测量高度: " + currentPageHeight + "px");

            // 移动到下一页的起始行和字符偏移量
            startLine = endLine; // 下一页从 fullLayout 中本页结束的行开始
            currentTextOffset = pageEndOffset; // 更新字符偏移量，用于日志和下一次循环

            // 跳过下一页开头可能存在的空白字符，确保页面内容从非空白字符开始
            while (currentTextOffset < fullText.length() && Character.isWhitespace(fullText.charAt(currentTextOffset))) {
                currentTextOffset++;
                // 这里的跳过逻辑会影响 currentTextOffset，但不会直接影响 startLine。
                // startLine 会在下一次循环开始时通过 endLine 正确地指向下一页的文本起始行。
            }
        }
        Log.d(TAG, "分页完成。总页数: " + pages.size());
    }
}