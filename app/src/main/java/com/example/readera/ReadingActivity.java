package com.example.readera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.readera.Adapter.NovelPageAdapter;
import com.example.readera.databinding.ActivityReadingBinding;
import com.example.readera.utiles.ReadingSettingsManager;
import com.example.readera.utiles.SystemUiController;
import com.example.readera.utiles.TextFileReader;
import com.example.readera.utiles.TextPager;
import com.example.readera.views.NovelPageView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.readera.Adapter.NovelPageAdapter.NovelPageViewHolder;

public class ReadingActivity extends AppCompatActivity {
    private static final String TAG = "ReadingActivity";

    private ActivityReadingBinding binding;
    private LinearLayout topBar;
    private LinearLayout bottomBar;
    private ViewPager2 vp2NovelPages;
    private TextView chapterTitle;
    private TextView tvProgressPercentage;
    private SeekBar sbProgress;
    private ProgressBar loadingIndicator;

    private boolean isBarsVisible = false;
    private GestureDetector gestureDetector;
    private SystemUiController systemUiController;

    private final long ANIMATION_DURATION = 300;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Uri fileUri;
    private TextPager textPager;
    private NovelPageAdapter pageAdapter;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private ReadingSettingsManager readingSettings;

    private boolean isPaginationTriggered = false;

    private int fixedStatusBarHeight = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        systemUiController = new SystemUiController(getWindow(), getWindow().getDecorView(), this);
        systemUiController.enterFullScreenMode();

        binding = ActivityReadingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        readingSettings = new ReadingSettingsManager(this);

        topBar = binding.topBar;
        bottomBar = binding.bottomBar;
        vp2NovelPages = binding.vp2NovelPages;
        chapterTitle = binding.chapterTitle;
        tvProgressPercentage = binding.tvProgressPercentage;
        sbProgress = binding.sbProgress;
        loadingIndicator = binding.loadingIndicator;

        View leftTapArea = binding.leftTapArea;
        View centerTapArea = binding.centerTapArea;
        View rightTapArea = binding.rightTapArea;

        // 基本控件操作
        binding.ivBack.setOnClickListener(v -> finish());
        binding.ivMore.setOnClickListener(v -> {
            // 当 iv_more 被点击时，启动 MoreOptionsActivity
            Intent intent = new Intent(ReadingActivity.this, MoreOptionsActivity.class);
            startActivity(intent);
        });
        binding.ivSettings.setOnClickListener(v -> Toast.makeText(this, "设置", Toast.LENGTH_SHORT).show());

        // --- 为书签按钮添加点击监听器 ---
        binding.ivBookmark.setOnClickListener(v -> addBookmark());

        fileUri = getIntent().getParcelableExtra("FILE_URI");
        if (fileUri == null) {
            Toast.makeText(this, "未找到文件内容，请选择文件", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 设置导航栏
        ViewCompat.setOnApplyWindowInsetsListener(binding.readerRootLayout, (v, insets) -> {
            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());

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


            return insets;
        });

        showLoadingIndicator();

        vp2NovelPages.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                vp2NovelPages.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                int vp2Width = vp2NovelPages.getWidth();
                int vp2Height = vp2NovelPages.getHeight();

                Log.d(TAG, "onGlobalLayout triggered. ViewPager2 Size: " + vp2Width + "x" + vp2Height);

                if (vp2Width > 0 && vp2Height > 0 && !isPaginationTriggered) {
                    isPaginationTriggered = true;
                    Log.d(TAG, "Valid ViewPager2 dimensions obtained. Triggering loadAndPaginateText.");

                    // 这里的 vp2Width 和 vp2Height 已经是 ViewPager2 实际占据的屏幕区域（系统栏之间）
                    // 只需要从这个区域中扣除 NovelPageView 自身的内部文本边距即可。
                    int[] pagePaddingPx = readingSettings.getPagePaddingPx();
                    int novelPageInternalPaddingPxHorizontal = pagePaddingPx[0] + pagePaddingPx[2];
                    int novelPageInternalPaddingPxVertical = pagePaddingPx[1] + pagePaddingPx[3];

                    int actualContentWidthPx = vp2Width - novelPageInternalPaddingPxHorizontal;
                    int actualContentHeightPx = vp2Height - novelPageInternalPaddingPxVertical;

                    Log.d(TAG, "为 TextPager 计算的内容区域: " + actualContentWidthPx + "x" + actualContentHeightPx);

                    loadAndPaginateText(fileUri, actualContentWidthPx, actualContentHeightPx);
                }
            }
        });

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // 如果手势检测器处理了，则不传递给其他点击监听器
                return super.onSingleTapUp(e);
            }
        });

        leftTapArea.setOnClickListener(v -> {
            if (isBarsVisible) {
                hideBars();
            } else {
                int currentItem = vp2NovelPages.getCurrentItem();
                if (currentItem > 0) {
                    vp2NovelPages.setCurrentItem(currentItem - 1);
                } else {
                    Toast.makeText(this, "已经是第一页了", Toast.LENGTH_SHORT).show();
                }
            }
        });

        centerTapArea.setOnClickListener(v -> toggleBarsVisibility());

        rightTapArea.setOnClickListener(v -> {
            if (isBarsVisible) {
                hideBars();
            } else {
                if (pageAdapter != null && vp2NovelPages.getCurrentItem() < pageAdapter.getItemCount() - 1) {
                    vp2NovelPages.setCurrentItem(vp2NovelPages.getCurrentItem() + 1);
                } else {
                    Toast.makeText(this, "已经是最后一页了", Toast.LENGTH_SHORT).show();
                }
            }
        });

        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && pageAdapter != null && pageAdapter.getItemCount() > 0) {
                    int pageIndex = (int) (progress / (float) seekBar.getMax() * (pageAdapter.getItemCount() - 1));
                    if (pageIndex < 0) pageIndex = 0;
                    if (pageIndex >= pageAdapter.getItemCount()) pageIndex = pageAdapter.getItemCount() - 1;

                    vp2NovelPages.setCurrentItem(pageIndex, false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 当用户停止拖动进度条时，可以考虑保存书签
                // addBookmark(); // 也可以在这里自动保存书签，但通常是用户手动触发
            }
        });

        vp2NovelPages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateProgressUI(position);
                // 每次页面切换时，可以自动保存书签
                readingSettings.saveBookmark(fileUri, position);
                Log.d(TAG, "自动保存书签到页面: " + position);
            }
        });

        hideBarsImmediately();
    }

    private void loadAndPaginateText(Uri uri, int contentWidthPx, int contentHeightPx) {
        executorService.execute(() -> {
            handler.post(this::showLoadingIndicator);

            TextFileReader fileReader = new TextFileReader();
            String fullText = fileReader.readTextFromUri(this, uri);
            if (fullText == null) {
                handler.post(() -> {
                    Toast.makeText(ReadingActivity.this, "读取文件失败！", Toast.LENGTH_LONG).show();
                    finish();
                    hideLoadingIndicator();
                });
                return;
            }

            textPager = new TextPager(this, fullText);
            textPager.setTextSize(readingSettings.getTextSizeSp());
            textPager.setLineSpacingExtra(readingSettings.getLineSpacingExtraDp());
            textPager.setVisibleArea(contentWidthPx, contentHeightPx);
            textPager.paginate();

            final List<String> pages = textPager.getPages();

            handler.post(() -> {
                if (pages.isEmpty()) {
                    Toast.makeText(ReadingActivity.this, "小说内容为空或无法分页！", Toast.LENGTH_LONG).show();
                    finish();
                    hideLoadingIndicator();
                    return;
                }

                pageAdapter = new NovelPageAdapter(pages);
                vp2NovelPages.setAdapter(pageAdapter);

                // --- 关键修改：加载文件后跳转到保存的书签页 ---
                int savedPageIndex = readingSettings.getBookmark(fileUri);
                if (savedPageIndex > 0 && savedPageIndex < pageAdapter.getItemCount()) {
                    vp2NovelPages.setCurrentItem(savedPageIndex, false); // 跳转到书签页
                    Log.d(TAG, "跳转到保存的书签页: " + savedPageIndex);
                } else {
                    vp2NovelPages.setCurrentItem(0, false); // 否则跳转到第一页
                    Log.d(TAG, "没有找到书签或书签无效，跳转到第一页。");
                }
                updateProgressUI(vp2NovelPages.getCurrentItem()); // 更新UI显示

                hideLoadingIndicator();

                RecyclerView recyclerView = (RecyclerView) vp2NovelPages.getChildAt(0);
                if (recyclerView != null) {
                    // 找到当前页面的ViewHolder，并应用设置
                    NovelPageViewHolder currentHolder = (NovelPageViewHolder) recyclerView.findViewHolderForAdapterPosition(vp2NovelPages.getCurrentItem());
                    if (currentHolder != null && currentHolder.novelPageView != null) {
                        applyNovelPageViewSettings(currentHolder.novelPageView);
                    }
                }
            });
        });
    }

    private void toggleBarsVisibility() {
        if (isBarsVisible) {
            hideBars();
        } else {
            showBars();
        }
    }

    private void showBars() {
        isBarsVisible = true;
        systemUiController.exitFullScreenMode(R.color.bar_color);

        topBar.setVisibility(View.VISIBLE);
        topBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        ObjectAnimator topAnimator = ObjectAnimator.ofFloat(topBar, "translationY", -topBar.getMeasuredHeight(), 0f);
        topAnimator.setDuration(ANIMATION_DURATION);
        topAnimator.start();

        bottomBar.setVisibility(View.VISIBLE);
        bottomBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        ObjectAnimator bottomAnimator = ObjectAnimator.ofFloat(bottomBar, "translationY", bottomBar.getMeasuredHeight(), 0f);
        bottomAnimator.setDuration(ANIMATION_DURATION);
        bottomAnimator.start();
    }

    private void hideBars() {
        isBarsVisible = false;

        topBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        ObjectAnimator topAnimator = ObjectAnimator.ofFloat(topBar, "translationY", 0f, -topBar.getMeasuredHeight());
        topAnimator.setDuration(ANIMATION_DURATION);
        topAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                topBar.setVisibility(View.GONE);
                systemUiController.enterFullScreenMode();
            }
        });
        topAnimator.start();

        bottomBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        ObjectAnimator bottomAnimator = ObjectAnimator.ofFloat(bottomBar, "translationY", 0f, bottomBar.getMeasuredHeight());
        bottomAnimator.setDuration(ANIMATION_DURATION);
        bottomAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                bottomBar.setVisibility(View.GONE);
            }
        });
        bottomAnimator.start();
    }

    private void hideBarsImmediately() {
        if (topBar != null) topBar.setVisibility(View.GONE);
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
        isBarsVisible = false;
        systemUiController.enterFullScreenMode();
    }

    private void updateProgressUI(int currentPage) {
        if (pageAdapter == null || pageAdapter.getItemCount() == 0) {
            tvProgressPercentage.setText("0.0%");
            sbProgress.setProgress(0);
            chapterTitle.setText("无内容");
            return;
        }

        int totalPages = pageAdapter.getItemCount();
        chapterTitle.setText("第 " + (currentPage + 1) + " 页 / 共 " + totalPages + " 页");
        sbProgress.setMax(totalPages - 1);
        sbProgress.setProgress(currentPage);

        double percentage = (currentPage + 1.0) / totalPages * 100.0;
        tvProgressPercentage.setText(String.format("%.1f%%", percentage));
    }

    private void showLoadingIndicator() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.VISIBLE);
            Log.d(TAG, "Loading indicator set to VISIBLE.");
        }
    }

    private void hideLoadingIndicator() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.GONE);
            Log.d(TAG, "Loading indicator set to GONE.");
        }
    }

    // 将阅读设置应用于 NovelPageView
    private void applyNovelPageViewSettings(NovelPageView pageView) {
        if (pageView != null) {
            pageView.setTypeface(readingSettings.getTypeface());
            pageView.setTextSize(readingSettings.getTextSizeSp());
            pageView.setLineSpacingExtra(readingSettings.getLineSpacingExtraDp());
            int[] padding = readingSettings.getPagePaddingPx();
            pageView.setPagePadding(padding[0], padding[1], padding[2], padding[3]);
            pageView.setTextColor(readingSettings.getTextColor());
        }
    }

    // --- 新增：添加书签功能 ---
    private void addBookmark() {
        if (fileUri != null && pageAdapter != null) {
            int currentPage = vp2NovelPages.getCurrentItem();
            readingSettings.saveBookmark(fileUri, currentPage);
            Toast.makeText(this, "书签已添加：第 " + (currentPage + 1) + " 页", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "手动添加书签到页面: " + currentPage);
        } else {
            Toast.makeText(this, "无法添加书签，请先加载小说。", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        systemUiController.enterFullScreenMode();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            systemUiController.enterFullScreenMode();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }
}