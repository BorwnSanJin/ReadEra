package com.example.readera;

import static com.example.readera.Adapter.NovelPageAdapter.NovelPageViewHolder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.readera.Adapter.NovelPageAdapter;
import com.example.readera.databinding.ActivityReadingBinding;
import com.example.readera.fragments.SettingsBottomSheetFragment;
import com.example.readera.model.Bookmark;
import com.example.readera.utiles.NovelReaderManager;
import com.example.readera.utiles.ReadingSettingsManager;
import com.example.readera.utiles.SystemUiController;
import com.example.readera.utiles.TextPager;
import com.example.readera.views.NovelPageView;

import java.util.List;
import java.util.Objects;

public class ReadingActivity extends AppCompatActivity {
    private static final String TAG = "ReadingActivity";

    private ActivityReadingBinding binding;
    private LinearLayout topBar; // 顶部工具栏
    private LinearLayout bottomBar; // 底部工具栏
    private ViewPager2 vp2NovelPages; // 用于显示小说页面的 ViewPager2
    private TextView chapterTitle; // 章节标题/页码显示
    private TextView tvProgressPercentage; // 阅读进度百分比
    private SeekBar sbProgress; // 页面进度条
    private ProgressBar loadingIndicator; // 加载指示器

    private boolean isBarsVisible = false; // 菜单栏是否可见
    private GestureDetector gestureDetector; // 手势检测器，用于处理点击事件
    private SystemUiController systemUiController; // 系统 UI 控制器，用于全屏模式切换

    private final long ANIMATION_DURATION = 300; // 动画持续时间
    private final Handler handler = new Handler(Looper.getMainLooper()); // 用于在主线程更新 UI

    private Uri fileUri; // 当前阅读的小说文件URI

    private NovelPageAdapter pageAdapter; // ViewPager2 的适配器

    private ReadingSettingsManager readingSettings; // 阅读设置管理器

    private boolean isPaginationTriggered = false; // 标志，指示是否已经触发过当前视图尺寸的分页

    private int fixedStatusBarHeight = 0; // 状态栏高度

    // 用于跟踪阅读设置变化的参数，以便在设置改变时重新分页
    private float currentTextSizeSp; // 当前字体大小（SP）
    private float currentLineSpacingExtraDp; // 当前行间距（DP）
    private Typeface currentTypeface; // 当前字体
    private int[] currentPagePaddingPx; // 当前页面内边距（像素）

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化系统 UI 控制器，并进入全屏模式
        systemUiController = new SystemUiController(getWindow(), getWindow().getDecorView(), this);
        systemUiController.enterFullScreenMode();

        // 绑定布局
        binding = ActivityReadingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化阅读设置管理器
        readingSettings = new ReadingSettingsManager(this);

        // 初始化 UI 控件
        topBar = binding.topBar;
        bottomBar = binding.bottomBar;
        vp2NovelPages = binding.vp2NovelPages;
        chapterTitle = binding.chapterTitle;
        tvProgressPercentage = binding.tvProgressPercentage;
        sbProgress = binding.sbProgress;
        loadingIndicator = binding.loadingIndicator;

        View leftTapArea = binding.leftTapArea; // 左侧点击区域
        View centerTapArea = binding.centerTapArea; // 中间点击区域
        View rightTapArea = binding.rightTapArea; // 右侧点击区域

        // --- 基本控件操作 ---
        binding.ivBack.setOnClickListener(v -> finish()); // 返回按钮点击事件
        binding.ivMore.setOnClickListener(v -> {
            // 当 iv_more 被点击时，启动 MoreOptionsActivity
            Intent intent = new Intent(ReadingActivity.this, MoreOptionsActivity.class);
            startActivity(intent);
        });
        binding.ivSettings.setOnClickListener(v -> {
            // 创建并显示底部设置弹窗
            SettingsBottomSheetFragment settingsBottomSheet = SettingsBottomSheetFragment.newInstance();
            settingsBottomSheet.show(getSupportFragmentManager(), settingsBottomSheet.getTag());
        });

        // --- 为书签按钮添加点击监听器 ---
        binding.ivBookmark.setOnClickListener(v -> addBookmark());

        // 获取传递进来的文件 URI
        fileUri = getIntent().getParcelableExtra("FILE_URI");
        if (fileUri == null) {
            Toast.makeText(this, "未找到文件内容，请选择文件", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // --- 设置窗口边距监听器，处理系统栏（状态栏、导航栏）的内边距 ---
        ViewCompat.setOnApplyWindowInsetsListener(binding.readerRootLayout, (v, insets) -> {
            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 记录状态栏高度，以便为 topBar 设置 padding
            if (systemBarsInsets.top > 0) {
                fixedStatusBarHeight = systemBarsInsets.top;
            }
            // 为 topBar 设置 padding：状态栏高度
            topBar.setPadding(
                    topBar.getPaddingLeft(),
                    fixedStatusBarHeight,
                    topBar.getPaddingRight(),
                    topBar.getPaddingBottom()
            );

            // 在这里初始化并缓存当前的阅读设置，以供后续比较
            currentTextSizeSp = readingSettings.getTextSizeSp();
            currentLineSpacingExtraDp = readingSettings.getLineSpacingExtraDp();
            currentTypeface = readingSettings.getTypeface();
            currentPagePaddingPx = readingSettings.getPagePaddingPx();

            return insets; // 返回处理后的 Insets
        });

        // 显示加载指示器
        showLoadingIndicator();

        // --- ViewPager2 全局布局监听器：当 ViewPager2 完成布局时触发分页 ---
        vp2NovelPages.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // 移除监听器，避免重复触发
                vp2NovelPages.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                int vp2Width = vp2NovelPages.getWidth();
                int vp2Height = vp2NovelPages.getHeight();

                Log.d(TAG, "onGlobalLayout 触发。ViewPager2 尺寸: " + vp2Width + "x" + vp2Height);

                // 确保尺寸有效且尚未触发分页
                if (vp2Width > 0 && vp2Height > 0 && !isPaginationTriggered) {
                    isPaginationTriggered = true; // 标记已触发
                    Log.d(TAG, "获取到有效的 ViewPager2 尺寸。触发加载并分页文本。");

                    // 这里的 vp2Width 和 vp2Height 已经是 ViewPager2 实际占据的屏幕区域（系统栏之间）
                    // 只需要从这个区域中扣除 NovelPageView 自身的内部文本边距即可。
                    int[] pagePaddingPx = readingSettings.getPagePaddingPx();
                    int novelPageInternalPaddingPxHorizontal = pagePaddingPx[0] + pagePaddingPx[2]; // 左右内边距之和
                    int novelPageInternalPaddingPxVertical = pagePaddingPx[1] + pagePaddingPx[3]; // 上下内边距之和

                    int actualContentWidthPx = vp2Width - novelPageInternalPaddingPxHorizontal;
                    int actualContentHeightPx = vp2Height - novelPageInternalPaddingPxVertical;

                    Log.d(TAG, "为 TextPager 计算的内容区域: " + actualContentWidthPx + "x" + actualContentHeightPx);

                    // 调用加载并分页文本的方法
                    loadAndPaginateText(fileUri, actualContentWidthPx, actualContentHeightPx);
                }
            }
        });

        // 初始化手势检测器，用于处理点击事件
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // 返回 false 以允许子视图（如 tapArea）接收点击事件
                return false;
            }
        });

        // --- 点击区域监听器 ---
        leftTapArea.setOnClickListener(v -> {
            if (isBarsVisible) { // 如果菜单栏可见，点击左侧区域则隐藏菜单栏
                hideBars();
            } else { // 否则，切换到上一页
                int currentItem = vp2NovelPages.getCurrentItem();
                if (currentItem > 0) {
                    vp2NovelPages.setCurrentItem(currentItem - 1);
                } else {
                    Toast.makeText(this, "已经是第一页了", Toast.LENGTH_SHORT).show();
                }
            }
        });

        centerTapArea.setOnClickListener(v -> toggleBarsVisibility()); // 点击中间区域切换菜单栏可见性

        rightTapArea.setOnClickListener(v -> {
            if (isBarsVisible) { // 如果菜单栏可见，点击右侧区域则隐藏菜单栏
                hideBars();
            } else { // 否则，切换到下一页
                if (pageAdapter != null && vp2NovelPages.getCurrentItem() < pageAdapter.getItemCount() - 1) {
                    vp2NovelPages.setCurrentItem(vp2NovelPages.getCurrentItem() + 1);
                } else {
                    Toast.makeText(this, "已经是最后一页了", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // --- 进度条监听器 ---
        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 用户拖动进度条时
                if (fromUser && pageAdapter != null && pageAdapter.getItemCount() > 0) {
                    int pageIndex = (int) (progress / (float) seekBar.getMax() * (pageAdapter.getItemCount() - 1));
                    if (pageIndex < 0) pageIndex = 0;
                    if (pageIndex >= pageAdapter.getItemCount()) pageIndex = pageAdapter.getItemCount() - 1;

                    // 只有当页面确实改变时才更新 ViewPager2，避免不必要的刷新
                    if (vp2NovelPages.getCurrentItem() != pageIndex) {
                        vp2NovelPages.setCurrentItem(pageIndex, false);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 开始拖动时
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 停止拖动时，可以考虑自动保存书签，但通常是用户手动触发
            }
        });

        // --- ViewPager2 页面改变回调 ---
        vp2NovelPages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateProgressUI(position); // 更新进度 UI
                // 自动保存当前阅读进度
                readingSettings.saveLastReadPage(fileUri, position);
                Log.d(TAG, "自动保存书签到页面: " + position);

                // 当页面切换时，确保新显示的 NovelPageView 应用了最新的阅读设置
                RecyclerView recyclerView = (RecyclerView) vp2NovelPages.getChildAt(0);
                if (recyclerView != null) {
                    NovelPageViewHolder currentHolder = (NovelPageViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
                    if (currentHolder != null && currentHolder.novelPageView != null) {
                        applyNovelPageViewSettings(currentHolder.novelPageView);
                    }
                }
            }
        });

        // 首次加载时立即隐藏菜单栏
        hideBarsImmediately();
    }

    /**
     * 使用 NovelReaderManager 加载并分页文本。
     * 此方法现在将异步处理委托给 NovelReaderManager。
     *
     * @param uri             小说文件 URI
     * @param contentWidthPx  文本内容可用宽度（像素）
     * @param contentHeightPx 文本内容可用高度（像素）
     */
    private void loadAndPaginateText(Uri uri, int contentWidthPx, int contentHeightPx) {
        showLoadingIndicator(); // 显示加载指示器

        // 调用 NovelReaderManager 进行异步加载和分页
        NovelReaderManager.getInstance().loadAndPaginateTextAsync(
                getApplicationContext(), uri, contentWidthPx, contentHeightPx,
                readingSettings.getTextSizeSp(), // 从设置中获取字体大小
                readingSettings.getLineSpacingExtraDp(), // 从设置中获取行间距
                readingSettings.getTypeface(), // 将字体传递给管理器
                new NovelReaderManager.PaginationListener() {
                    @Override
                    public void onPaginationComplete(List<String> pages) {
                        handler.post(() -> { // 确保在主线程更新 UI
                            if (pages.isEmpty()) {
                                Toast.makeText(ReadingActivity.this, "小说内容为空或无法分页！", Toast.LENGTH_LONG).show();
                                finish();
                                hideLoadingIndicator();
                                return;
                            }

                            // 设置 ViewPager2 的适配器
                            pageAdapter = new NovelPageAdapter(pages);
                            vp2NovelPages.setAdapter(pageAdapter);

                            // --- 关键修改：加载文件后跳转到保存的书签页或上次阅读进度 ---
                            int initialPageFromIntent = getIntent().getIntExtra("INITIAL_PAGE", -1); // 从 Intent 获取传入的初始页码
                            int targetPageIndex;

                            if (initialPageFromIntent != -1 && initialPageFromIntent < pageAdapter.getItemCount()) {
                                targetPageIndex = initialPageFromIntent; // 如果有从书签列表传来的有效页码，则使用
                                Log.d(TAG, "从书签列表跳转到页面: " + targetPageIndex);
                            } else {
                                // 否则，使用上次自动保存的页码（即阅读进度）
                                int lastReadPageIndex = readingSettings.getLastReadPage(fileUri);
                                if (lastReadPageIndex >= 0 && lastReadPageIndex < pageAdapter.getItemCount()) {
                                    targetPageIndex = lastReadPageIndex;
                                    Log.d(TAG, "跳转到自动保存的阅读进度页: " + targetPageIndex);
                                } else {
                                    targetPageIndex = 0; // 否则跳转到第一页
                                    Log.d(TAG, "没有找到阅读进度或进度无效，跳转到第一页。");
                                }
                            }
                            vp2NovelPages.setCurrentItem(targetPageIndex, false); // 跳转到目标页，不带动画
                            updateProgressUI(vp2NovelPages.getCurrentItem()); // 更新 UI 显示

                            hideLoadingIndicator(); // 隐藏加载指示器

                            // 确保当前可见的 NovelPageView 应用了所有设置
                            RecyclerView recyclerView = (RecyclerView) vp2NovelPages.getChildAt(0);
                            if (recyclerView != null) {
                                NovelPageViewHolder currentHolder = (NovelPageViewHolder) recyclerView.findViewHolderForAdapterPosition(vp2NovelPages.getCurrentItem());
                                if (currentHolder != null && currentHolder.novelPageView != null) {
                                    applyNovelPageViewSettings(currentHolder.novelPageView);
                                }
                            }
                        });
                    }

                    @Override
                    public void onPaginationFailed(String errorMessage) {
                        handler.post(() -> { // 确保在主线程更新 UI
                            Toast.makeText(ReadingActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            finish(); // 分页失败则关闭 Activity
                            hideLoadingIndicator();
                        });
                    }
                }
        );
    }

    /**
     * 切换菜单栏的可见性。
     */
    private void toggleBarsVisibility() {
        if (isBarsVisible) {
            hideBars();
        } else {
            showBars();
        }
    }

    /**
     * 菜单栏展示动画。
     */
    private void showBars() {
        isBarsVisible = true;
        // 退出全屏模式，显示系统状态栏和导航栏，并设置状态栏颜色
        systemUiController.exitFullScreenMode(R.color.bar_color);

        topBar.setVisibility(View.VISIBLE);
        // 测量 topBar 高度以进行动画
        topBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        ObjectAnimator topAnimator = ObjectAnimator.ofFloat(topBar, "translationY", -topBar.getMeasuredHeight(), 0f);
        topAnimator.setDuration(ANIMATION_DURATION);
        topAnimator.start();

        bottomBar.setVisibility(View.VISIBLE);
        // 测量 bottomBar 高度以进行动画
        bottomBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        ObjectAnimator bottomAnimator = ObjectAnimator.ofFloat(bottomBar, "translationY", bottomBar.getMeasuredHeight(), 0f);
        bottomAnimator.setDuration(ANIMATION_DURATION);
        bottomAnimator.start();
    }

    /**
     * 菜单栏隐藏动画。
     */
    private void hideBars() {
        isBarsVisible = false;

        // 测量 topBar 高度以进行动画
        topBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        ObjectAnimator topAnimator = ObjectAnimator.ofFloat(topBar, "translationY", 0f, -topBar.getMeasuredHeight());
        topAnimator.setDuration(ANIMATION_DURATION);
        topAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                topBar.setVisibility(View.GONE); // 动画结束后隐藏 topBar
                systemUiController.enterFullScreenMode(); // 重新进入全屏模式
            }
        });
        topAnimator.start();

        // 测量 bottomBar 高度以进行动画
        bottomBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        ObjectAnimator bottomAnimator = ObjectAnimator.ofFloat(bottomBar, "translationY", 0f, bottomBar.getMeasuredHeight());
        bottomAnimator.setDuration(ANIMATION_DURATION);
        bottomAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                bottomBar.setVisibility(View.GONE); // 动画结束后隐藏 bottomBar
            }
        });
        bottomAnimator.start();
    }

    /**
     * 立即隐藏菜单栏，不带动画。
     */
    private void hideBarsImmediately() {
        if (topBar != null) topBar.setVisibility(View.GONE);
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
        isBarsVisible = false;
        systemUiController.enterFullScreenMode(); // 确保处于全屏模式
    }

    /**
     * 更新阅读进度相关的 UI 显示。
     *
     * @param currentPage 当前页码（从 0 开始）
     */
    private void updateProgressUI(int currentPage) {
        if (pageAdapter == null || pageAdapter.getItemCount() == 0) {
            tvProgressPercentage.setText("0.0%");
            sbProgress.setProgress(0);
            chapterTitle.setText("无内容");
            return;
        }

        int totalPages = pageAdapter.getItemCount();
        chapterTitle.setText("第 " + (currentPage + 1) + " 页 / 共 " + totalPages + " 页"); // 显示当前页/总页数
        sbProgress.setMax(totalPages - 1); // 进度条最大值设置为总页数-1（因为页码从 0 开始）
        sbProgress.setProgress(currentPage); // 设置进度条当前进度

        double percentage = (currentPage + 1.0) / totalPages * 100.0; // 计算百分比
        tvProgressPercentage.setText(String.format("%.1f%%", percentage)); // 显示百分比，保留一位小数
    }

    /**
     * 显示加载指示器。
     */
    private void showLoadingIndicator() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.VISIBLE);
            Log.d(TAG, "加载指示器设置为可见。");
        }
    }

    /**
     * 隐藏加载指示器。
     */
    private void hideLoadingIndicator() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.GONE);
            Log.d(TAG, "加载指示器设置为隐藏。");
        }
    }

    /**
     * 将当前的阅读设置（字体、大小、行间距、内边距、颜色）应用到指定的 NovelPageView。
     * 这个方法确保视图以正确的样式显示内容。
     *
     * @param pageView 需要应用设置的 NovelPageView
     */
    private void applyNovelPageViewSettings(NovelPageView pageView) {
        if (pageView != null) {
            pageView.setTypeface(readingSettings.getTypeface());
            pageView.setTextSize(readingSettings.getTextSizeSp());
            pageView.setLineSpacingExtra(readingSettings.getLineSpacingExtraDp());
            int[] padding = readingSettings.getPagePaddingPx();
            pageView.setPagePadding(padding[0], padding[1], padding[2], padding[3]);
            pageView.setTextColor(readingSettings.getTextColor());
            pageView.invalidate(); // 请求重绘以应用所有改变
        }
    }

    /**
     * 添加书签功能。
     */
    private void addBookmark() {
        if (fileUri != null && pageAdapter != null) {
            int currentPage = vp2NovelPages.getCurrentItem(); // 获取当前页码
            String displayTitle = getBookmarkDisplayTitle(fileUri, currentPage); // 获取书签显示标题
            readingSettings.addBookmark(new Bookmark(fileUri, currentPage, displayTitle)); // 保存书签
            Toast.makeText(this, "书签已添加：第 " + (currentPage + 1) + " 页", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "手动添加书签到页面: " + currentPage);
        } else {
            Toast.makeText(this, "无法添加书签，请先加载小说。", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 根据文件URI和当前页码生成一个书签的显示标题。
     * 可以根据实际需求调整此逻辑，例如从文件内容中提取章节标题。
     *
     * @param uri       小说文件 URI
     * @param pageIndex 页码
     * @return 生成的书签标题字符串
     */
    private String getBookmarkDisplayTitle(Uri uri, int pageIndex) {
        String fileName = "未知小说";
        try {
            // 尝试从 URI 获取文件名
            if (uri != null) {
                String path = uri.getPath();
                if (path != null) {
                    int cut = path.lastIndexOf('/');
                    if (cut != -1) {
                        fileName = path.substring(cut + 1);
                        if (fileName.contains(".")) {
                            fileName = fileName.substring(0, fileName.lastIndexOf("."));
                        }
                    } else {
                        fileName = path;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取文件名失败: " + e.getMessage());
        }

        // 尝试获取当前页面的第一行内容作为标题的一部分
        String firstLine = getFirstLine(pageIndex);

        // 组合成一个更友好的标题
        return fileName + " - " + firstLine + " (P." + (pageIndex + 1) + ")";
    }

    @NonNull
    private String getFirstLine(int pageIndex) {
        // 从 NovelReaderManager 获取当前的 TextPager 实例，再获取页面内容
        TextPager currentTextPager = NovelReaderManager.getInstance().getCurrentTextPager();
        String pageContent = "";
        if (currentTextPager != null && pageIndex >= 0 && pageIndex < currentTextPager.getPages().size()) {
            pageContent = currentTextPager.getPages().get(pageIndex);
        }

        // 尝试获取第一行作为标题的一部分
        String firstLine = pageContent.split("\n")[0];
        if (firstLine.length() > 30) { // 限制标题长度，避免过长
            firstLine = firstLine.substring(0, 30) + "...";
        } else if (firstLine.trim().isEmpty() && pageContent.length() > 0) {
            // 如果第一行是空的（例如只有换行符），则尝试使用下一行非空内容
            String[] lines = pageContent.split("\n");
            for (int i = 1; i < lines.length; i++) {
                if (!lines[i].trim().isEmpty()) {
                    firstLine = lines[i].trim();
                    if (firstLine.length() > 30) {
                        firstLine = firstLine.substring(0, 30) + "...";
                    }
                    break;
                }
            }
            if (firstLine.trim().isEmpty()) { // 如果检查完所有行仍然为空
                firstLine = "（无标题内容）";
            }
        } else if (firstLine.trim().isEmpty()) { // 如果整个页面内容都是空的
            firstLine = "（空页）";
        }
        return firstLine;
    }

    @Override
    protected void onResume() {
        super.onResume();
        systemUiController.enterFullScreenMode(); // 确保 Activity 恢复时处于全屏模式
        // 检查阅读设置是否发生变化，如果变化则重新分页
        checkForReadingSettingsChangesAndRepaginate();
    }

    /**
     * 检查阅读设置（字体大小、行间距、字体、页面边距）是否已更改。
     * 如果有任何更改，则触发重新分页。
     */
    private void checkForReadingSettingsChangesAndRepaginate() {
        float newTextSizeSp = readingSettings.getTextSizeSp();
        float newLineSpacingExtraDp = readingSettings.getLineSpacingExtraDp();
        Typeface newTypeface = readingSettings.getTypeface();
        int[] newPagePaddingPx = readingSettings.getPagePaddingPx();

        // 比较各项设置是否发生变化
        boolean textSizeChanged = (newTextSizeSp != currentTextSizeSp);
        boolean lineSpacingChanged = (newLineSpacingExtraDp != currentLineSpacingExtraDp);
        // 使用 Objects.equals() 来安全地比较可能为 null 的对象
        boolean typefaceChanged = !Objects.equals(newTypeface, currentTypeface);
        boolean paddingChanged = false;
        if (newPagePaddingPx != null && currentPagePaddingPx != null) {
            if (newPagePaddingPx.length != currentPagePaddingPx.length) {
                paddingChanged = true;
            } else {
                for (int i = 0; i < newPagePaddingPx.length; i++) {
                    if (newPagePaddingPx[i] != currentPagePaddingPx[i]) {
                        paddingChanged = true;
                        break;
                    }
                }
            }
        } else if (newPagePaddingPx != currentPagePaddingPx) { // 其中一个为 null，另一个不为 null
            paddingChanged = true;
        }

        // 如果任何设置发生变化
        if (textSizeChanged || lineSpacingChanged || typefaceChanged || paddingChanged) {
            Log.d(TAG, "阅读设置已更改。重新分页中...");
            Log.d(TAG, String.format("字体大小变化: %b (旧: %.1f, 新: %.1f)", textSizeChanged, currentTextSizeSp, newTextSizeSp));
            Log.d(TAG, String.format("行间距变化: %b (旧: %.1f, 新: %.1f)", lineSpacingChanged, currentLineSpacingExtraDp, newLineSpacingExtraDp));
            Log.d(TAG, String.format("字体变化: %b (旧: %s, 新: %s)", typefaceChanged, currentTypeface, newTypeface));
            Log.d(TAG, String.format("内边距变化: %b (旧: %s, 新: %s)", paddingChanged, arrayToString(currentPagePaddingPx), arrayToString(newPagePaddingPx)));

            // 更新缓存的设置，以便下次比较
            currentTextSizeSp = newTextSizeSp;
            currentLineSpacingExtraDp = newLineSpacingExtraDp;
            currentTypeface = newTypeface;
            currentPagePaddingPx = newPagePaddingPx;

            // 获取当前 ViewPager2 的尺寸，用于重新分页
            int vp2Width = vp2NovelPages.getWidth();
            int vp2Height = vp2NovelPages.getHeight();

            // 确保尺寸有效且文件 URI 存在，然后触发重新分页
            if (vp2Width > 0 && vp2Height > 0 && fileUri != null) {
                int[] pagePaddingPx = readingSettings.getPagePaddingPx();
                int novelPageInternalPaddingPxHorizontal = pagePaddingPx[0] + pagePaddingPx[2];
                int novelPageInternalPaddingPxVertical = pagePaddingPx[1] + pagePaddingPx[3];

                int actualContentWidthPx = vp2Width - novelPageInternalPaddingPxHorizontal;
                int actualContentHeightPx = vp2Height - novelPageInternalPaddingPxVertical;

                // 触发重新加载和分页
                isPaginationTriggered = false; // 重置此标志，允许 loadAndPaginateText 重新执行
                loadAndPaginateText(fileUri, actualContentWidthPx, actualContentHeightPx);
            } else {
                Log.w(TAG, "无法重新分页：ViewPager2 尺寸无效或 fileUri 为 null。");
            }
        } else {
            Log.d(TAG, "阅读设置未改变。");
            // 如果设置没有改变，但可能有新的 NovelPageView 因滚动而创建，
            // 确保其设置已应用。这主要由 onPageSelected 处理。
            // 但对于当前已加载并可见的页面，这里可以额外确保一次。
            RecyclerView recyclerView = (RecyclerView) vp2NovelPages.getChildAt(0);
            if (recyclerView != null) {
                NovelPageViewHolder currentHolder = (NovelPageViewHolder) recyclerView.findViewHolderForAdapterPosition(vp2NovelPages.getCurrentItem());
                if (currentHolder != null && currentHolder.novelPageView != null) {
                    applyNovelPageViewSettings(currentHolder.novelPageView);
                }
            }
        }
    }

    // 辅助方法：将 int 数组转换为字符串，用于日志输出
    private String arrayToString(int[] array) {
        if (array == null) return "null";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // 当窗口焦点改变时，确保恢复全屏模式（例如，从通知栏返回时）
        if (hasFocus) {
            systemUiController.enterFullScreenMode();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 将触摸事件传递给手势检测器，然后才是父类的触摸事件处理
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关键：只取消正在进行的分页任务，不要关闭整个线程池！
        // 线程池的生命周期应该与 Application 绑定
        NovelReaderManager.getInstance().cancelCurrentPaginationTask();
        handler.removeCallbacksAndMessages(null);

        Log.d("ReadingActivity", "onDestroy() called.");
    }

    // 在 ReadingActivity.java 中
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // 更新 Activity 的 Intent
        // 从新的 Intent 中获取数据，并重新加载内容
        Uri newFileUri = intent.getParcelableExtra("FILE_URI");
        int newInitialPage = intent.getIntExtra("INITIAL_PAGE", -1);

        if (!Objects.equals(newFileUri, fileUri)) { // 如果文件 URI 发生变化，则重新加载和分页
            fileUri = newFileUri;
            // 你可能需要重新触发分页逻辑，例如调用 loadAndPaginateText
            // 注意：这里需要获取当前的 ViewPager2 尺寸
            int vp2Width = vp2NovelPages.getWidth();
            int vp2Height = vp2NovelPages.getHeight();
            if (vp2Width > 0 && vp2Height > 0) {
                int[] pagePaddingPx = readingSettings.getPagePaddingPx();
                int novelPageInternalPaddingPxHorizontal = pagePaddingPx[0] + pagePaddingPx[2];
                int novelPageInternalPaddingPxVertical = pagePaddingPx[1] + pagePaddingPx[3];

                int actualContentWidthPx = vp2Width - novelPageInternalPaddingPxHorizontal;
                int actualContentHeightPx = vp2Height - novelPageInternalPaddingPxVertical;

                // 触发重新加载和分页
                isPaginationTriggered = false; // 重置此标志
                loadAndPaginateText(fileUri, actualContentWidthPx, actualContentHeightPx);
            }
        } else { // 如果文件 URI 相同，只是页码变化，则直接跳转到页码
            if (newInitialPage != -1 && newInitialPage != vp2NovelPages.getCurrentItem() && newInitialPage < pageAdapter.getItemCount()) {
                vp2NovelPages.setCurrentItem(newInitialPage, false);
                updateProgressUI(newInitialPage);
            }
        }
    }
}