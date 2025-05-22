package com.example.readera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2; // 导入 ViewPager2

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class ReadingActivity extends AppCompatActivity {
    private static final String TAG = "ReadingActivity";

    private LinearLayout topBar; // 顶部栏
    private LinearLayout bottomBar; // 底部栏
    private ImageView ivBack; // 添加这个 ImageView 引用
    private ViewPager2 vp2NovelPages; // ViewPager2 引用

    private boolean isBarsVisible = false;
    private GestureDetector gestureDetector;
    private WindowInsetsControllerCompat windowInsetsController;
    // 关键改变：用于存储状态栏和导航栏的“物理”高度
    // 这些值一旦获取到就不会变，因为它们代表了屏幕区域
    private int fixedStatusBarHeight = 0;
    private int fixedNavigationBarHeight = 0;

    private final long ANIMATION_DURATION = 300; // 动画时长


    private Uri fileUri;
    private long bytesRead = 0;
    private static final int CHUNK_SIZE = 1024 * 100; // 每次读取 100KB


    private boolean isLoading = false;
    private final Object lock = new Object(); // 用于同步加载状态

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 设置全屏模式（沉浸式阅读）
        setFullScreenMode();

        setContentView(R.layout.activity_reading);
        // 初始化视图组件
        topBar = findViewById(R.id.top_bar);
        bottomBar = findViewById(R.id.bottom_bar);
        vp2NovelPages = findViewById(R.id.vp2_novel_pages); // 初始化 ViewPager2

        // 初始化点击区域
        View leftTapArea = findViewById(R.id.left_tap_area);
        View centerTapArea = findViewById(R.id.center_tap_area);
        View rightTapArea = findViewById(R.id.right_tap_area);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reader_root_layout), (v, insets) -> {
            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 只有当获取到的高度大于0时才更新我们的“固定”高度
            // 这样可以确保我们记住的是实际的物理高度，而不是系统栏隐藏时的0
            if (systemBarsInsets.top > 0) {
                fixedStatusBarHeight = systemBarsInsets.top;
            }
            if (systemBarsInsets.bottom > 0) {
                fixedNavigationBarHeight = systemBarsInsets.bottom;
            }

            Log.d(TAG, "OnApplyWindowInsetsListener: fixedStatusBarHeight=" + fixedStatusBarHeight
                    + ", fixedNavigationBarHeight=" + fixedNavigationBarHeight);

            // 确保 topBar 的内容始终在状态栏下方
            topBar.setPadding(
                    topBar.getPaddingLeft(),
                    fixedStatusBarHeight, // 使用实际状态栏高度
                    topBar.getPaddingRight(),
                    topBar.getPaddingBottom()
            );

            // 在布局完成后，设置 topBar 的初始隐藏位置
            // 确保 topBar 的高度已经确定，这样 translationY 才准确
            topBar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (topBar.getHeight() > 0) {
                        if (!isBarsVisible) { // 仅在初始隐藏状态时设置
                            topBar.setTranslationY(-topBar.getHeight());
                        }
                        topBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            });
            return insets;
        });


        // 3. 初始化手势检测器 - 保留此项，但也要为点击区域设置 OnClickListener
        // 如果您完全依赖点击区域进行交互，可以移除 GestureDetector.SimpleOnGestureListener 中的 onSingleTapUp。
        // 如果您希望一般屏幕点击也能切换菜单栏，则保留它。
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // 这将捕获一般屏幕点击。
                // 如果您只想通过中间点击区域切换，请移除此行。
                // toggleBarsVisibility();
                return true; // 返回 true 以消费事件
            }
        });

        // 为点击区域设置点击监听器
        leftTapArea.setOnClickListener(v -> {
            // 在 ViewPager2 中导航到上一页
            int currentItem = vp2NovelPages.getCurrentItem();
            if (currentItem > 0) {
                vp2NovelPages.setCurrentItem(currentItem - 1);
            } else {
                Toast.makeText(this, "已经是第一页了", Toast.LENGTH_SHORT).show();
            }
        });

        centerTapArea.setOnClickListener(v -> {
            // 切换菜单栏的可见性
            toggleBarsVisibility();
        });

        rightTapArea.setOnClickListener(v -> {
            // 在 ViewPager2 中导航到下一页
            int currentItem = vp2NovelPages.getCurrentItem();
            if (vp2NovelPages.getAdapter() != null && currentItem < vp2NovelPages.getAdapter().getItemCount() - 1) { // 假设适配器已设置且有项目
                vp2NovelPages.setCurrentItem(currentItem + 1);
            } else {
                Toast.makeText(this, "已经是最后一页了", Toast.LENGTH_SHORT).show();
            }
        });

        // 获取返回按钮的引用并设置点击监听器
        ivBack = findViewById(R.id.iv_back); // 初始化 ivBack
        ivBack.setOnClickListener(v -> {
            finish(); // 调用 onBackPressed() 来模拟系统返回行为
        });


        // 获取传递过来的文件 URI 与此请求无关，但保留在此处以供上下文参考。
        fileUri = getIntent().getParcelableExtra("FILE_URI");

    }

    private void setFullScreenMode() {
        Window window = getWindow();
        // 让内容延伸到系统栏区域
        WindowCompat.setDecorFitsSystemWindows(window, false);

        windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (windowInsetsController != null) {
            // 默认隐藏状态栏和导航栏
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            // 设置系统栏行为：用户从边缘滑动可临时呼出系统栏
            windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

            // 初始设置状态栏为透明，确保内容在下方
            window.setStatusBarColor(Color.TRANSPARENT);
            // 设置状态栏图标为深色，以适应默认的白色背景（如果您的阅读背景是深色，这里应设置为true）
            windowInsetsController.setAppearanceLightStatusBars(true);
        }
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

        // 1. 显示系统栏（状态栏和导航栏）
        if (windowInsetsController != null) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
            // 设置状态栏颜色为顶部栏的颜色
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.bar_color)); // 假设您在 colors.xml 中定义了 bar_color
            // 根据顶部栏颜色调整状态栏图标颜色
            // 如果顶部栏是浅色，状态栏图标应该是深色 (true)
            // 如果顶部栏是深色，状态栏图标应该是浅色 (false)
            windowInsetsController.setAppearanceLightStatusBars(false); // 示例：如果顶部栏是灰色/深色，图标应为浅色
        }

        // 2. 顶部栏动画：从上方滑下
        topBar.setVisibility(View.VISIBLE);
        // 使用实际高度来计算偏移，确保从屏幕顶部完全滑入
        topBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED); // 强制测量以获取高度
        ObjectAnimator topAnimator = ObjectAnimator.ofFloat(topBar, "translationY", -topBar.getMeasuredHeight(), 0f);
        topAnimator.setDuration(ANIMATION_DURATION);
        topAnimator.start();

        // 3. 底部栏动画：从下方滑上
        bottomBar.setVisibility(View.VISIBLE);
        // 使用实际高度来计算偏移，确保从屏幕底部完全滑入
        bottomBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED); // 强制测量以获取高度
        ObjectAnimator bottomAnimator = ObjectAnimator.ofFloat(bottomBar, "translationY", bottomBar.getMeasuredHeight(), 0f);
        bottomAnimator.setDuration(ANIMATION_DURATION);
        bottomAnimator.start();
    }

    private void hideBars() {
        isBarsVisible = false;

        // 1. 顶部栏动画：向上滑出屏幕
        // 使用实际高度来计算偏移
        topBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED); // 强制测量以获取高度
        ObjectAnimator topAnimator = ObjectAnimator.ofFloat(topBar, "translationY", 0f, -topBar.getMeasuredHeight());
        topAnimator.setDuration(ANIMATION_DURATION);
        topAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                topBar.setVisibility(View.GONE);
                // 动画结束后隐藏系统栏
                if (windowInsetsController != null) {
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
                    // 隐藏后将状态栏颜色设置回透明
                    getWindow().setStatusBarColor(Color.TRANSPARENT);
                    // 隐藏后状态栏图标颜色设置为深色（适应白色背景）
                    windowInsetsController.setAppearanceLightStatusBars(true);
                }
            }
        });
        topAnimator.start();

        // 2. 底部栏动画：向下滑出屏幕
        // 使用实际高度来计算偏移
        bottomBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED); // 强制测量以获取高度
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

    // 在 onResume 确保在 Activity 返回时重新进入全屏模式
    @Override
    protected void onResume() {
        super.onResume();
        setFullScreenMode();
    }

    // 在 onWindowFocusChanged 确保在焦点变化时保持全屏模式
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setFullScreenMode();
        }
    }

    // 如果您希望将一般屏幕点击事件分发给 GestureDetector，请添加此方法
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 如果您保留了 GestureDetector.SimpleOnGestureListener 中的一般屏幕点击行为，
        // 则需要此方法将触摸事件传递给它。
        // 如果您完全依赖于定义的点击区域，则可以移除此方法。
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }
}