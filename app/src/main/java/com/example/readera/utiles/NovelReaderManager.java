package com.example.readera.utiles;

import android.content.Context;
import android.graphics.Typeface; // 导入 Typeface
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CancellationException; // 导入 CancellationException

public class NovelReaderManager {
    private static final String TAG = "NovelReaderManager";
    private static NovelReaderManager instance;

    private TextPager currentTextPager;// 存储当前分页好的 TextPager 实例
    private Uri currentFileUri;
    private Future<?> paginationTask; // 用于管理异步分页任务

    private final ExecutorService executorService; // 使用固定线程池
    private final Handler mainThreadHandler; // 用于在主线程回调

    private NovelReaderManager() {
        // 私有构造函数，确保单例
        executorService = Executors.newSingleThreadExecutor(); // 确保只有一个分页任务在进行
        mainThreadHandler = new Handler(Looper.getMainLooper()); // 用于在主线程回调
    }

    public static synchronized NovelReaderManager getInstance() {
        if (instance == null) {
            instance = new NovelReaderManager();
        }
        return instance;
    }

    /**
     * 开始加载和分页文本。如果当前文件已分页且阅读设置未变，则直接返回现有结果。
     *
     * @param context          Application context.
     * @param uri              The URI of the text file.
     * @param contentWidthPx   Content area width for pagination.
     * @param contentHeightPx  Content area height for pagination.
     * @param textSizeSp       Text size in SP.
     * @param lineSpacingExtraDp Line spacing in DP.
     * @param typeface         The Typeface to use for text measurement.
     * @param listener         Callback for pagination completion.
     */
    public void loadAndPaginateTextAsync(Context context, Uri uri,
                                         int contentWidthPx, int contentHeightPx,
                                         float textSizeSp, float lineSpacingExtraDp,
                                         Typeface typeface, // 新增 Typeface 参数
                                         PaginationListener listener) {

        // 步骤 1: 检查是否已分页且设置未变
        // 增加对 Typeface 的比较
        if (currentTextPager != null && uri.equals(currentFileUri) &&
                currentTextPager.getTextSize() == textSizeSp &&
                currentTextPager.getLineSpacingExtra() == lineSpacingExtraDp &&
                currentTextPager.getVisibleAreaWidth() == contentWidthPx &&
                currentTextPager.getVisibleAreaHeight() == contentHeightPx &&
                currentTextPager.getTypeface() == typeface&&
                currentTextPager.getPages() != null &&
                !currentTextPager.getPages().isEmpty()) {

            Log.d(TAG, "文件已分页且所有设置未变，直接返回。");
            listener.onPaginationComplete(currentTextPager.getPages());
            return;
        }

        // 步骤 2: 取消前一个正在进行的分页任务（如果有）

        cancelCurrentPaginationTask();
        // 步骤 3: 标记当前正在处理的文件 URI
        currentFileUri = uri;

        // 步骤 4: 启动新的分页任务
        paginationTask = executorService.submit(() -> {
            Log.d(TAG, "开始新的分页任务，URI: " + uri.getLastPathSegment());
            TextFileReader fileReader = new TextFileReader();
            String fullText = fileReader.readTextFromUri(context, uri);
            Log.d(TAG, "readTextFromUri 返回的 fullText 是否为 null: " + (fullText == null));
            if (fullText == null) {
                Log.e(TAG, "读取文件失败：" + uri);
                listener.onPaginationFailed("读取文件失败！");
                return;
            }

            try {
                // 重新创建 TextPager 实例，因为文件或设置已更改
                currentTextPager = new TextPager(context, fullText);
                currentTextPager.setTextSize(textSizeSp);
                currentTextPager.setLineSpacingExtra(lineSpacingExtraDp);
                currentTextPager.setVisibleArea(contentWidthPx, contentHeightPx);
                currentTextPager.setTypeface(typeface); // 设置字体

                Log.d(TAG, "TextPager 开始分页...");
                currentTextPager.paginate(); // 这里可能抛出 InterruptedException

                // 检查任务是否在分页完成后被取消 (在 paginate() 内部检查更及时)
                if (Thread.currentThread().isInterrupted()) {
                    Log.d(TAG, "分页任务完成但随后被中断。");
                    return; // 立即返回，不触发完成回调
                }

                List<String> pages = currentTextPager.getPages();
                if (pages.isEmpty()) {
                    listener.onPaginationFailed("小说内容为空或无法分页！");
                } else {
                    listener.onPaginationComplete(pages);
                }
            } catch (InterruptedException e) {
                // 捕获到 InterruptedException，说明任务被取消
                Log.d(TAG, "分页任务被中断，取消回调。");
                // 通常不调用 onPaginationFailed，因为这不是真正的失败，而是用户主动取消
                // 如果需要，可以在这里提供一个取消的提示，但通常UI层会自己处理加载指示器
            } catch (CancellationException e) {
                // Future.cancel(true) 也会导致 CancellationException
                Log.d(TAG, "分页任务被取消。");
            } catch (Exception e) {
                Log.e(TAG, "分页过程中发生错误", e);
                listener.onPaginationFailed("分页失败：" + e.getMessage());
            }
        });
    }

    // 获取当前分页器的实例
    public TextPager getCurrentTextPager() {
        return currentTextPager;
    }

    // 获取当前分页文件的URI
    public Uri getCurrentFileUri() {
        return currentFileUri;
    }

    public void cancelCurrentPaginationTask() {
        if (paginationTask != null && !paginationTask.isDone()) {
            Log.d(TAG, "Cancelling current pagination task.");
            boolean cancelled = paginationTask.cancel(true); // true 表示中断正在执行的线程
            Log.d(TAG, "Cancellation result: " + cancelled);
        }
        paginationTask = null;  // 即使任务为空或已完成，也清除引用
    }

    /**
     * 接口用于回调分页结果。
     * onPaginationComplete 和 onPaginationFailed 都将在主线程回调。
     */
    public interface PaginationListener {
        void onPaginationComplete(List<String> pages);
        void onPaginationFailed(String errorMessage);
    }

    /**
     * 在应用退出时调用，关闭线程池。
     * 注意：仅当NovelReaderManager的生命周期与整个Application绑定时才推荐调用。
     */
    public void shutdown() {
        if (!executorService.isShutdown()) {
            executorService.shutdownNow(); // 尝试立即关闭所有执行中的任务
            Log.d(TAG, "NovelReaderManager 线程池已关闭。");
        }
    }
}