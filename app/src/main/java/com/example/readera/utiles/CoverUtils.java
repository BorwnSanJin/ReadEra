package com.example.readera.utiles;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.text.TextUtils;

public class CoverUtils {
    public static Bitmap generateCoverBitmap(String text, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.LTGRAY);


        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.BLACK);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        float initialTextSize = 20f;
        textPaint.setTextSize(initialTextSize); // 初始字号，后续可能调整
        float textWidth = width * 0.9f;

        // 处理文本换行和省略
        String displayText = formatText(text, textPaint, (int)textWidth, 3);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline;
        //计算基线
        if(displayText.contains("\n")){
            baseline =height/3f;
            textPaint.setTextSize(15f);
        }else {
            baseline = height / 2f - (fm.top + fm.bottom) / 2f;
        }
        // 绘制文本
        String[] lines = displayText.split("\n");
        float y = baseline;
        if(lines.length>1){
            textPaint.setTextSize(15f); // 多行文本设置字体为 15f
        }
        for (String line : lines) {
            canvas.drawText(line, width / 2f, y, textPaint);
            y += -fm.ascent + fm.descent + fm.leading; // 增加行间距
        }
        return bitmap;
    }

    /**
     * 格式化文本，使其每行显示指定字数，并处理省略。
     *
     * @param text      原始文本
     * @param paint     画笔，用于测量文本宽度
     * @param maxWidth  最大宽度
     * @param maxLines  最大行数
     * @return 格式化后的文本
     */
    private static String formatText(String text, TextPaint paint, int maxWidth, int maxLines){
        StringBuilder formattedText = new StringBuilder();
        int lineCount = 0;
        for (int i = 0; i < text.length() && lineCount < maxLines; i += 4) {
            int endIndex = Math.min(i + 4, text.length());
            String line = text.substring(i, endIndex);
            formattedText.append(line);
            if (endIndex < text.length() && lineCount < maxLines - 1) {
                formattedText.append("\n");
            }
            lineCount++;
        }
        String result = formattedText.toString().trim();
        if (text.length() > result.length() && maxLines > 0 && lineCount >= maxLines) {
            // 检查是否需要添加省略号，避免在正好是最大行数时添加
            String[] resultLines = result.split("\n");
            if (resultLines.length == maxLines && text.length() > result.length()) {
                if (resultLines.length > 0) {
                    resultLines[resultLines.length - 1] = TextUtils.ellipsize(resultLines[resultLines.length - 1], paint, maxWidth, TextUtils.TruncateAt.END).toString();
                    result = TextUtils.join("\n", resultLines);
                } else {
                    result = TextUtils.ellipsize(text, paint, maxWidth * maxLines, TextUtils.TruncateAt.END).toString();
                }
            }
        }
        System.out.println("display:" + result + ", lineCount:" + lineCount);
        return result;
    }
}
