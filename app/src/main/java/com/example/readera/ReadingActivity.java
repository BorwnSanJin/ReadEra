package com.example.readera;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class ReadingActivity extends AppCompatActivity {
    private static final String TAG = "ReadingActivity";
    private TextView textView;
    private SeekBar fontSizeSeekBar;
    private Button fontStyleButton;

    private Uri fileUri;
    private long bytesRead = 0;
    private static final int CHUNK_SIZE = 1024 * 100; // 每次读取 100KB

    private float currentFontSize = 16f;
    private Typeface currentFont;

    private boolean isLoading = false;
    private final Object lock = new Object(); // 用于同步加载状态

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.read), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textView = findViewById(R.id.readingTextView);
        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);
        fontStyleButton = findViewById(R.id.fontStyleButton);

        // 获取传递过来的文件 URI
        fileUri = getIntent().getParcelableExtra("FILE_URI");
        if (fileUri != null) {
            // 首次加载少量内容
            loadNextChunk();
        } else {
            textView.setText("无法加载小说内容");
        }

        // 从 SharedPreferences 加载上次的设置
        loadSettings();
        applySettings();

        fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentFontSize = 12f + progress; // 设置字体大小范围
                applySettings();
                saveFontSize();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        fontStyleButton.setOnClickListener(v -> {
            // 切换字体，例如宋体和默认字体
            currentFont = (currentFont == null) ?
                    ResourcesCompat.getFont(this, R.font.serif) :
                    null;
            applySettings();
            saveFont();
        });

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
                long skipped = inputStream.skip(bytesRead);
                if (skipped < bytesRead) {
                    Log.e(TAG, "跳过字节失败或已到达文件末尾");
                    runOnUiThread(() -> isLoading = false);
                    return;
                }

                char[] buffer = new char[CHUNK_SIZE];
                int charsRead;
                StringBuilder chunkBuilder = new StringBuilder();

                charsRead = reader.read(buffer);
                if (charsRead > 0) {
                    String chunk = new String(buffer, 0, charsRead);
                    chunkBuilder.append(chunk);
                    bytesRead += chunk.getBytes(StandardCharsets.UTF_8).length; // 更新已读取的字节数
                    runOnUiThread(() -> textView.append(chunkBuilder));
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

    private void applySettings() {
        textView.setTextSize(currentFontSize);
        textView.setTypeface(currentFont);
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("reading_settings", MODE_PRIVATE);
        currentFontSize = prefs.getFloat("font_size", 16f);
        String fontName = prefs.getString("font_name", null);
        if ("serif".equals(fontName)) {
            currentFont = ResourcesCompat.getFont(this, R.font.serif);
        } else {
            currentFont = Typeface.DEFAULT; // 默认字体
        }
    }

    private void saveFontSize() {
        SharedPreferences.Editor editor = getSharedPreferences("reading_settings", MODE_PRIVATE).edit();
        editor.putFloat("font_size", currentFontSize);
        editor.apply();
    }

    private void saveFont() {
        SharedPreferences.Editor editor = getSharedPreferences("reading_settings", MODE_PRIVATE).edit();
        editor.putString("font_name", (currentFont == null) ? null : "serif"); // 简单保存字体名称
        editor.apply();
    }

}
