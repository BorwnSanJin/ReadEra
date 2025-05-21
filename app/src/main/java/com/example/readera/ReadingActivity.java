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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class ReadingActivity extends AppCompatActivity {
    private static final String TAG = "ReadingActivity";

    private LinearLayout topBar;//顶部栏
    private LinearLayout bottomBar;//底部栏
    private ScrollView scrollViewContent;
    private TextView tvReaderContent;//阅读内容

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
        //初始化
        topBar = findViewById(R.id.top_bar);
        bottomBar = findViewById(R.id.bottom_bar);
        scrollViewContent = findViewById(R.id.scroll_view_content);
        tvReaderContent = findViewById(R.id.tv_reader_content);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reader_root_layout),(v, insets) -> {
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

            // 确保 scrollViewContent 的内容始终在状态栏下方且不被导航栏遮挡
            scrollViewContent.setPadding(
                    scrollViewContent.getPaddingLeft(), // 保持 XML 中的左右 padding
                    fixedStatusBarHeight, // 顶部内边距等于实际状态栏高度
                    scrollViewContent.getPaddingRight(),
                    fixedNavigationBarHeight  // 底部内边距等于实际导航栏高度
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




        // 3. 初始化手势检测器
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                toggleBarsVisibility();
                return true;
            }
        });

        // 4. 将手势检测器应用于阅读内容区域
        scrollViewContent.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        // 获取传递过来的文件 URI
        fileUri = getIntent().getParcelableExtra("FILE_URI");
        if (fileUri != null) {
            // 首次加载少量内容
            loadNextChunk();
        } else {
            tvReaderContent.setText("无法加载小说内容");
        }
    }

    private void loadNextChunk() {
        synchronized (lock) {
            if (isLoading || fileUri == null) {
                return;
            }
            isLoading = true;
        }

        new Thread(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,StandardCharsets.UTF_8))) {

                Log.d(TAG,"开始读取文件");
                // 跳过已经读取的字节
                // inputStream.skip(bytesRead) 在某些情况下可能无法跳过精确的字节数，
                // 特别是对于字符编码文件。更稳健的方法是读取并丢弃字符。
                long currentBytesRead = 0;
                while (currentBytesRead < bytesRead) {
                    int charCode = reader.read();
                    if (charCode == -1) { // Reach end of stream
                        Log.e(TAG, "跳过字节失败或已到达文件末尾");
                        runOnUiThread(() -> isLoading = false);
                        return;
                    }
                    currentBytesRead += String.valueOf((char) charCode).getBytes(StandardCharsets.UTF_8).length;
                }

                char[] buffer = new char[CHUNK_SIZE];
                int charsRead;
                StringBuilder chunkBuilder = new StringBuilder();

                charsRead = reader.read(buffer);
                if (charsRead > 0) {
                    String chunk = new String(buffer, 0, charsRead);
                    chunkBuilder.append(chunk);
                    bytesRead += chunk.getBytes(StandardCharsets.UTF_8).length; // 更新已读取的字节数
                    runOnUiThread(() -> {
                        // 在 UI 线程更新 TextView，保持滚动位置
                        int scrollY = scrollViewContent.getScrollY();
                        tvReaderContent.append(chunkBuilder);
                        scrollViewContent.scrollTo(0, scrollY); // 恢复滚动位置
                    });
                } else if (charsRead == -1) {
                    runOnUiThread(() -> Toast.makeText(this, "文件已读完", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                Log.e(TAG, "读取文件块失败: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "读取文件失败", Toast.LENGTH_SHORT).show());
            } finally {
                runOnUiThread(() -> isLoading = false);
            }
        }).start();
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
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.top_bar_color)); // 假设您在 colors.xml 中定义了 top_bar_color
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

}