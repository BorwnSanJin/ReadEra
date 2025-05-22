package com.example.readera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
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
import androidx.core.content.res.ResourcesCompat; // 用于字体
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.readera.Adapter.NovelPageAdapter;
import com.example.readera.databinding.ActivityReadingBinding;
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

import static com.example.readera.Adapter.NovelPageAdapter.NovelPageViewHolder; // 导入内部类

public class ReadingActivity extends AppCompatActivity {
    private static final String TAG = "ReadingActivity";

    private ActivityReadingBinding binding;
    private LinearLayout topBar;
    private LinearLayout bottomBar;
    private ImageView ivBack;
    private ViewPager2 vp2NovelPages;
    private TextView chapterTitle;
    private TextView tvProgressPercentage;
    private SeekBar sbProgress;
    private ProgressBar loadingIndicator;

    private boolean isBarsVisible = false;
    private GestureDetector gestureDetector;
    private WindowInsetsControllerCompat windowInsetsController;

    private final long ANIMATION_DURATION = 300;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable hideBarsRunnable;

    private Uri fileUri;
    private TextPager textPager;
    private NovelPageAdapter pageAdapter;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    // 阅读设置（匹配 NovelPageView 默认值或传递它们）
    private int defaultTextSizeSp = 18;
    private int defaultLineSpacingExtraDp = 4;
    // 这些内边距是 NovelPageView *内部* 的，而不是 ViewPager2 的内边距
    private int novelPageInternalPaddingLeftDp = 16;
    private int novelPageInternalPaddingTopDp = 8;
    private int novelPageInternalPaddingRightDp = 16;
    private int novelPageInternalPaddingBottomDp = 8;

    private boolean isPaginationTriggered = false;

    // 关键改变：用于存储状态栏和导航栏的“物理”高度
    // 这些值一旦获取到就不会变，因为它们代表了屏幕区域
    private int fixedStatusBarHeight = 0;
    private int fixedNavigationBarHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenMode();
        binding = ActivityReadingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

        ivBack = binding.ivBack;
        ivBack.setOnClickListener(v -> finish());

        binding.ivMore.setOnClickListener(v -> Toast.makeText(this, "更多功能", Toast.LENGTH_SHORT).show());
        binding.ivSettings.setOnClickListener(v -> Toast.makeText(this, "设置", Toast.LENGTH_SHORT).show());
        binding.ivBookmark.setOnClickListener(v -> Toast.makeText(this, "书签", Toast.LENGTH_SHORT).show());

        fileUri = getIntent().getParcelableExtra("FILE_URI");
        if (fileUri == null) {
            Toast.makeText(this, "未找到文件内容，请选择文件", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //设置导航栏
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reader_root_layout),(v,insets)->{
            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // 只有当获取到的高度大于0时才更新我们的“固定”高度
            if (systemBarsInsets.top > 0) {
                fixedStatusBarHeight = systemBarsInsets.top;
            }
            if (systemBarsInsets.bottom > 0) {
                fixedNavigationBarHeight = systemBarsInsets.bottom;
            }
            // 1. 为 topBar 设置 padding：状态栏高度 + 刘海屏顶部高度
            topBar.setPadding(
                    topBar.getPaddingLeft(),
                    fixedStatusBarHeight ,
                    topBar.getPaddingRight(),
                    topBar.getPaddingBottom()
            );
            return insets;
        });


        showLoadingIndicator();

        // 关键改变：确保在 ViewPager2 获得实际尺寸后再进行分页
        // onGlobalLayoutListener 现在应该获取 ViewPager2 的尺寸
        // 然后我们将计算 NovelPageView 的 *内部内容区域*。
        vp2NovelPages.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int vp2Width = vp2NovelPages.getWidth();
                int vp2Height = vp2NovelPages.getHeight();

                Log.d(TAG, "onGlobalLayout triggered. ViewPager2 Size: " + vp2Width + "x" + vp2Height);

                // 只有当 ViewPager2 具有有效尺寸并且分页尚未触发时才进行分页
                if (vp2Width > 0 && vp2Height > 0 && !isPaginationTriggered) {
                    isPaginationTriggered = true; // 设置标志，防止重复触发
                    Log.d(TAG, "Valid ViewPager2 dimensions obtained. Triggering loadAndPaginateText.");

                    vp2NovelPages.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // 计算 NovelPageView 中文本可用的内容区域
                    // vp2NovelPages 的 paddingTop/Bottom 是用于整体布局的，
                    // NovelPageView 的内部内边距在此基础上添加。
                    // TextPager 的实际内容宽度/高度应为：
                    // ViewPager2_宽度 - ViewPager2_水平内边距 - NovelPageView_水平内边距*2
                    // ViewPager2_高度 - ViewPager2_垂直内边距 - NovelPageView_垂直内边距*2

                    // 这些是应用于 activity_reading.xml 中 ViewPager2 本身的内边距
                    int vp2HorizontalPadding = vp2NovelPages.getPaddingStart() + vp2NovelPages.getPaddingEnd();
                    int vp2VerticalPadding = vp2NovelPages.getPaddingTop() + vp2NovelPages.getPaddingBottom();


                    // 将 NovelPageView 的内部 DP 内边距转换为 PX
                    int novelPageInternalPaddingPxHorizontal = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, novelPageInternalPaddingLeftDp + novelPageInternalPaddingRightDp, getResources().getDisplayMetrics());
                    int novelPageInternalPaddingPxVertical = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, novelPageInternalPaddingTopDp + novelPageInternalPaddingBottomDp, getResources().getDisplayMetrics());

                    // 计算 TextPager 的最终可用内容区域
                    int actualContentWidthPx = vp2Width - vp2HorizontalPadding - novelPageInternalPaddingPxHorizontal;
                    int actualContentHeightPx = vp2Height - vp2VerticalPadding - novelPageInternalPaddingPxVertical;


                    Log.d(TAG, "为 TextPager 计算的内容区域: " + actualContentWidthPx + "x" + actualContentHeightPx);

                    loadAndPaginateText(fileUri, actualContentWidthPx, actualContentHeightPx);
                }
            }
        });


        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return super.onSingleTapUp(e);
            }
        });

        leftTapArea.setOnClickListener(v -> {
            if(isBarsVisible){
                hideBars();
            }else {
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
            if(isBarsVisible){
                hideBars();
            }else {
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

            }
        });

        vp2NovelPages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateProgressUI(position);

            }
        });

        hideBarsImmediately();

    }

    private void setFullScreenMode() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

            window.setStatusBarColor(Color.TRANSPARENT);
            windowInsetsController.setAppearanceLightStatusBars(true);
        }
    }

    private void loadAndPaginateText(Uri uri, int contentWidthPx, int contentHeightPx) {
        executorService.execute(() -> {
            handler.post(this::showLoadingIndicator);

            String fullText = readTextFromUri(this, uri);
            if (fullText == null) {
                handler.post(() -> {
                    Toast.makeText(ReadingActivity.this, "读取文件失败！", Toast.LENGTH_LONG).show();
                    finish();
                    hideLoadingIndicator();
                });
                return;
            }

            textPager = new TextPager(this, fullText);
            textPager.setTextSize(defaultTextSizeSp);
            textPager.setLineSpacingExtra(defaultLineSpacingExtraDp);
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
                updateProgressUI(0);

                hideLoadingIndicator();

                // 通过适配器或获取当前视图设置初始页面视图设置
                // 这将把字体和内边距应用到 NovelPageView 实例
                // 获取 ViewPager2 内部的 RecyclerView
                RecyclerView recyclerView = (RecyclerView) vp2NovelPages.getChildAt(0);
                NovelPageViewHolder firstHolder = (NovelPageViewHolder) recyclerView.findViewHolderForAdapterPosition(0);
                if (firstHolder != null) {
                    applyNovelPageViewSettings(firstHolder.novelPageView);
                } else {
                    // 如果第一个视图不立即可用（例如，由于缓存），
                    // 设置将在绑定时应用。
                    // 确保 NovelPageAdapter 的 onBindViewHolder 调用 setPageText
                    // 这反过来会触发 NovelPageView 中的 recreateStaticLayout。
                    // 或者，如果您需要将通用设置应用于所有视图，
                    // 考虑在 NovelPageAdapter 的 onCreateViewHolder 中执行此操作
                    // 或维护一个通用设置对象。
                }
            });
        });
    }

    private String readTextFromUri(Context context, Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "读取文件失败 (UTF-8): " + e.getMessage());
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "GBK"))) {
                stringBuilder.setLength(0);
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                Log.d(TAG, "尝试GBK编码读取成功。");
                return stringBuilder.toString();
            } catch (IOException e2) {
                Log.e(TAG, "尝试GBK编码失败: " + e2.getMessage());
                return null;
            }
        }
        return stringBuilder.toString();
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

        if (windowInsetsController != null) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.bar_color));
            windowInsetsController.setAppearanceLightStatusBars(false);
        }

        topBar.setVisibility(View.VISIBLE);
        topBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        // 动画是从完全隐藏位置开始的
        ObjectAnimator topAnimator = ObjectAnimator.ofFloat(topBar, "translationY", -topBar.getMeasuredHeight(), 0f);
        topAnimator.setDuration(ANIMATION_DURATION);
        topAnimator.start();

        bottomBar.setVisibility(View.VISIBLE);
        bottomBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        // 动画是从完全隐藏位置开始的
        ObjectAnimator bottomAnimator = ObjectAnimator.ofFloat(bottomBar, "translationY", bottomBar.getMeasuredHeight(), 0f);
        bottomAnimator.setDuration(ANIMATION_DURATION);
        bottomAnimator.start();


    }

    private void hideBars() {
        isBarsVisible = false;
        handler.removeCallbacks(hideBarsRunnable);

        topBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        ObjectAnimator topAnimator = ObjectAnimator.ofFloat(topBar, "translationY", 0f, -topBar.getMeasuredHeight());
        topAnimator.setDuration(ANIMATION_DURATION);
        topAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                topBar.setVisibility(View.GONE);
                if (windowInsetsController != null) {
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
                    getWindow().setStatusBarColor(Color.TRANSPARENT);
                    windowInsetsController.setAppearanceLightStatusBars(true);
                }
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
        handler.removeCallbacks(hideBarsRunnable);
        if (topBar != null) topBar.setVisibility(View.GONE);
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
        isBarsVisible = false;
        if (windowInsetsController != null) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            windowInsetsController.setAppearanceLightStatusBars(true);
        }
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

    // 新的辅助方法，用于将设置应用于 NovelPageView
    private void applyNovelPageViewSettings(NovelPageView pageView) {
        if (pageView != null) {
            // 如果您有字体，请应用它
            // 确保 R.font.your_font_name 存在，否则使用系统默认字体
            try {
                pageView.setTypeface(ResourcesCompat.getFont(this, R.font.serif));
            } catch (Exception e) {
                Log.e(TAG, "字体未找到或加载字体错误: " + e.getMessage());
                pageView.setTypeface(null); // 使用默认系统字体
            }

            pageView.setTextSize(defaultTextSizeSp);
            pageView.setLineSpacingExtra(defaultLineSpacingExtraDp);
            // 将 DP 转换为 PX 以进行内部内边距设置
            pageView.setPagePadding(
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, novelPageInternalPaddingLeftDp, getResources().getDisplayMetrics()),
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, novelPageInternalPaddingTopDp, getResources().getDisplayMetrics()),
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, novelPageInternalPaddingRightDp, getResources().getDisplayMetrics()),
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, novelPageInternalPaddingBottomDp, getResources().getDisplayMetrics())
            );
            // 您也可以在这里设置文本颜色或使其可配置
            pageView.setTextColor(ContextCompat.getColor(this, android.R.color.black)); // 示例：黑色文本
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        setFullScreenMode();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setFullScreenMode();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 即使不再直接调用 `hideBarsWithDelay`，最好还是移除回调
        if (handler != null && hideBarsRunnable != null) {
            handler.removeCallbacks(hideBarsRunnable);
        }
        executorService.shutdownNow();
    }
}