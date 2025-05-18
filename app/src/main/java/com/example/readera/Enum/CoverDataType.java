package com.example.readera.Enum;

public enum CoverDataType {
    /**
     * coverData 存储的是用于生成封面的文本内容。
     * 例如，从 TXT 文件开头读取的几行文字。
     */
    TEXT,
    /**
     * coverData 存储的是 Android 资源 ID（一个 int 值）。
     * 通常指向 res/drawable 目录下的图片资源（例如 R.drawable.default_cover）。
     */
    RESOURCE_ID,
    /**
     * coverData 存储的是一个统一资源标识符 (URI)。
     * 可以是本地文件路径 (file://...) 或网络链接 (http://..., https://...)，指向封面图片。
     */
    URI,
    /**
     * coverData 存储的是一个统一资源标识符 (URI)。
     * 可以是本地文件路径 (file://...) 或网络链接 (http://..., https://...)，指向封面图片。
     */
    PDF_PAGE
    // 可以根据未来需求添加更多类型
}
