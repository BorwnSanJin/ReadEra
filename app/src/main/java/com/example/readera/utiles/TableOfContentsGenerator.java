package com.example.readera.utiles;

import android.util.Log;
import com.example.readera.model.TableOfContents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TableOfContentsGenerator {

    private static final String TAG = "TableOfContentsGenerator";

    public static List<TableOfContents> generateSimpleToc(TextPager textPager) {
        List<TableOfContents> tocEntries = new ArrayList<>();
        if (textPager == null || textPager.getPages().isEmpty()) {
            Log.d(TAG, "TextPager is null or empty, returning empty TOC.");
            return tocEntries;
        }

        List<String> pages = textPager.getPages();
        int totalPages = pages.size();

        // 增强的中文章节标题识别正则表达式
        // 匹配 "第X章", "第X节", "第X回", "第一章", "第一节", "第一回" 等
        // 支持数字和中文数字（零一二三四五六七八九十百千万）
        // 增加对常见的小说标题格式的匹配，例如：
        // - "卷一", "上篇", "下篇"
        // - 纯数字或英文数字开头的行，后面跟着短标题
        // - 可能的标题会以换行符分隔，所以使用 Pattern.MULTILINE
        Pattern chapterPattern = Pattern.compile(
                "^[\\s　]*(第[零一二三四五六七八九十百千万\\d]+[章节回篇卷集部])|" + // 例如：  第一章, 第十章, 第一节, 第一回, 第五卷
                        "^[\\s　]*(卷[一二三四五六七八九十百千万\\d]+)|" +               // 例如：  卷一, 卷二
                        "^[\\s　]*(上篇|中篇|下篇)|" +                                 // 例如：  上篇
                        "^[\\s　]*(序章|楔子|引子|前言|尾声|结语|后记)|" +              // 明确匹配这些特殊章节
                        // 原来的这行被移除或注释掉，因为它可能导致误判：
                        // "^[\\s　]*([\\u4E00-\\u9FA5]{2,8}[章节回卷集])|" +
                        "^[\\s　]*(Chapter\\s*\\d+)|" +                               // 例如：  Chapter 1, Chapter 01
                        "^[\\s　]*(Section\\s*\\d+)|" +                               // 例如：  Section 1
                        "^[\\s　]*((\\d+|[A-Z])\\s*[\\u3001\\u002E\\u002D\\u0020]\\s*([\\u4E00-\\u9FA5a-zA-Z0-9]{2,30}))$", // 例如：  1. 标题, A. 标题, 1 - 标题
                Pattern.MULTILINE // 允许 ^ 匹配每行的开头
        );

        Set<String> identifiedChapterTitles = new HashSet<>();

        for (int i = 0; i < totalPages; i++) {
            String pageContent = pages.get(i);
            //Log.d(TAG, "Processing page " + i + ", content start: \"" + pageContent.substring(0, Math.min(pageContent.length(), 100)).replace("\n", "\\n") + "\"");

            Matcher matcher = chapterPattern.matcher(pageContent);
            while (matcher.find()) {
                String potentialTitle = matcher.group(0).trim();

                //Log.d(TAG, "Potential title found on page " + i + ": \"" + potentialTitle + "\"");

                if (potentialTitle.length() > 2 && potentialTitle.length() < 50 && !identifiedChapterTitles.contains(potentialTitle)) {
                    // 过滤掉过短的纯汉字短语或数字，例如 "第一" (不带章)
                    if (!potentialTitle.matches("^[\\u4E00-\\u9FA5]{1,2}$") && !potentialTitle.matches("^[\\d一二三四五六七八九十]{1,2}$")) {
                        tocEntries.add(new TableOfContents(potentialTitle, i));
                        identifiedChapterTitles.add(potentialTitle);
                        //Log.d(TAG, "Added TOC entry: \"" + potentialTitle + "\" at page " + i);
                        break; // 找到一个章节就停止在该页的查找
                    } else {
                        Log.d(TAG, "Filtered out potential title (too short/generic): \"" + potentialTitle + "\"");
                    }
                } else {
                    Log.d(TAG, "Filtered out potential title (length/duplicate): \"" + potentialTitle + "\"");
                }
            }
            // 移除了 !chapterFoundOnPage 块，这意味着只有显式识别到章节标题才会添加目录项
            // 不再使用 minPagesPerEntry 逻辑
        }

        // 后处理：如果目录为空但有内容，添加一个“开始阅读”条目
        if (tocEntries.isEmpty() && totalPages > 0) {
            tocEntries.add(new TableOfContents("开始阅读", 0));
            Log.d(TAG, "TOC is empty, added '开始阅读' entry.");
        }
        Log.d(TAG, "Generated TOC with " + tocEntries.size() + " entries.");
        return tocEntries;
    }

    // isTitleAlreadyAdded 和 getFirstNonEmptyLine 方法保持不变
    private static boolean isTitleAlreadyAdded(List<TableOfContents> existingEntries, String newTitle) {
        return existingEntries.stream().anyMatch(entry -> entry.title.equals(newTitle));
    }

    private static String getFirstNonEmptyLine(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                return trimmedLine;
            }
        }
        return "";
    }
}