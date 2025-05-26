package com.example.readera.fragments; // 建议放在 fragments 包下

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView; // 示例：如果你的设置页面有文本
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.readera.utiles.ReadingSettingsManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.example.readera.R; // 确保导入你的 R 文件

import java.util.ArrayList;
import java.util.List;

public class SettingsBottomSheetFragment extends BottomSheetDialogFragment {
    private static final String TAG = "SettingsBottomSheet";
    private ImageView btnFontSizeDecrease;
    private ImageView btnFontSizeIncrease;
    private TextView tvCurrentFontSize;

    private View bgWhiteContainer;
    private View bgGrayContainer;
    private View bgGreenContainer;
    private View bgDarkContainer;

    private View bgWhite;
    private View bgGray;
    private View bgGreen;
    private View bgDark;

    private List<View> backgroundContainers;

    private Button btnFontSystemDefault;
    private Button btnFontSerif;
    private List<Button> fontButtons;


    private ReadingSettingsManager readingSettingsManager;
    private OnSettingsChangeListener settingsChangeListener;
    // 定义回调接口，宿主 Activity 需要实现此接口
    public interface OnSettingsChangeListener {
        void onReadingSettingsChanged();
    }

    // 通常用于创建 Fragment 实例的方法
    public static SettingsBottomSheetFragment newInstance() {
        return new SettingsBottomSheetFragment();
    }

    @Override
    public void onAttach(@NonNull Context context){
        super.onAttach(context);
        if (context instanceof OnSettingsChangeListener) {
            settingsChangeListener = (OnSettingsChangeListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnSettingsChangeListener");
        }
        readingSettingsManager = new ReadingSettingsManager(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 加载设置页面的布局文件
        View view = inflater.inflate(R.layout.fragment_settings_bottom_sheet, container, false);

        // 初始化字体大小控件
        btnFontSizeDecrease = view.findViewById(R.id.btn_font_size_decrease);
        btnFontSizeIncrease = view.findViewById(R.id.btn_font_size_increase);
        tvCurrentFontSize = view.findViewById(R.id.tv_current_font_size);

        // 初始化背景选择容器 (FrameLayout)
        bgWhiteContainer = view.findViewById(R.id.bg_white_container);
        bgGrayContainer = view.findViewById(R.id.bg_gray_container);
        bgGreenContainer = view.findViewById(R.id.bg_green_container);
        bgDarkContainer = view.findViewById(R.id.bg_dark_container);

        // 初始化实际的背景颜色 View
        bgWhite = view.findViewById(R.id.bg_white);
        bgGray = view.findViewById(R.id.bg_gray);
        bgGreen = view.findViewById(R.id.bg_green);
        bgDark = view.findViewById(R.id.bg_dark);

        backgroundContainers = new ArrayList<>();
        backgroundContainers.add(bgWhiteContainer);
        backgroundContainers.add(bgGrayContainer);
        backgroundContainers.add(bgGreenContainer);
        backgroundContainers.add(bgDarkContainer);

        btnFontSystemDefault = view.findViewById(R.id.btn_font_system_default);
        btnFontSerif = view.findViewById(R.id.btn_font_serif);
        fontButtons = new ArrayList<>();
        fontButtons.add(btnFontSystemDefault);
        fontButtons.add(btnFontSerif);
        setupListeners();
        updateUI(); // 根据当前设置更新 UI

        return view;
    }

    private void setupListeners() {
        // 字体大小调节
        btnFontSizeDecrease.setOnClickListener(v -> {
            float currentSize = readingSettingsManager.getTextSizeSp();
            if (currentSize > 12f) {
                readingSettingsManager.saveTextSize(currentSize - 1f);
                updateUI();
                notifySettingsChanged();
            } else {
                Toast.makeText(getContext(), "已经是最小字体了", Toast.LENGTH_SHORT).show();
            }
        });

        btnFontSizeIncrease.setOnClickListener(v -> {
            float currentSize = readingSettingsManager.getTextSizeSp();
            if (currentSize < 28f) {
                readingSettingsManager.saveTextSize(currentSize + 1f);
                updateUI();
                notifySettingsChanged();
            } else {
                Toast.makeText(getContext(), "已经是最大字体了", Toast.LENGTH_SHORT).show();
            }
        });

        // 背景颜色选择
        bgWhiteContainer.setOnClickListener(v -> selectBackground(
                ContextCompat.getColor(requireContext(), R.color.reading_bg_white),
                ContextCompat.getColor(requireContext(), R.color.reading_text_dark)));
        bgGrayContainer.setOnClickListener(v -> selectBackground(
                ContextCompat.getColor(requireContext(), R.color.reading_bg_gray),
                ContextCompat.getColor(requireContext(), R.color.reading_text_dark)));
        bgGreenContainer.setOnClickListener(v -> selectBackground(
                ContextCompat.getColor(requireContext(), R.color.reading_bg_greenish),
                ContextCompat.getColor(requireContext(), R.color.reading_text_dark)));
        bgDarkContainer.setOnClickListener(v -> selectBackground(
                ContextCompat.getColor(requireContext(), R.color.reading_bg_dark),
                ContextCompat.getColor(requireContext(), R.color.reading_text_light)));

        // 字体选择
        btnFontSystemDefault.setOnClickListener(v -> selectFont("system_default"));
        btnFontSerif.setOnClickListener(v -> selectFont("serif"));
    }

    private void selectBackground(int bgColor, int textColor) {
        readingSettingsManager.saveBackgroundColor(bgColor);
        readingSettingsManager.saveTextColor(textColor);
        updateBackgroundSelectionUI(bgColor);
        notifySettingsChanged();
    }

    private void selectFont(String fontIdentifier) {
        readingSettingsManager.saveFont(fontIdentifier);
        updateFontSelectionUI(fontIdentifier);
        notifySettingsChanged();
    }

    private void updateUI() {
        tvCurrentFontSize.setText(String.format("%.0fsp", readingSettingsManager.getTextSizeSp()));
        updateBackgroundSelectionUI(readingSettingsManager.getBackgroundColor());

        // 获取当前字体以便更新 UI
        String currentFontIdentifier = "system_default"; // 默认
        if (readingSettingsManager.getTypeface().equals(ResourcesCompat.getFont(requireContext(), R.font.serif))) {
            currentFontIdentifier = "serif";
        }
        updateFontSelectionUI(currentFontIdentifier);
    }

    /**
     * 根据当前背景色设置选中状态。
     * @param currentBgColor 当前选中的背景颜色（int 类型）
     */
    private void updateBackgroundSelectionUI(int currentBgColor) {
        // 预定义颜色值，与你在 backgroundClickListener 中设置的一致
        int whiteColor = ContextCompat.getColor(requireContext(), R.color.reading_bg_white);
        int grayColor = ContextCompat.getColor(requireContext(), R.color.reading_bg_gray);
        int greenColor = ContextCompat.getColor(requireContext(), R.color.reading_bg_greenish);
        int darkColor = ContextCompat.getColor(requireContext(), R.color.reading_bg_dark);

        for (View container : backgroundContainers) {
            View actualBgView = null;
            int containerId = container.getId();
            if (containerId == R.id.bg_white_container) {
                actualBgView = bgWhite;
            } else if (containerId == R.id.bg_gray_container) {
                actualBgView = bgGray;
            } else if (containerId == R.id.bg_green_container) {
                actualBgView = bgGreen;
            } else if (containerId == R.id.bg_dark_container) {
                actualBgView = bgDark;
            }

            if (actualBgView != null) {
                // 更可靠地获取 View 的背景颜色
                int containerBgColor = ((android.graphics.drawable.ColorDrawable) actualBgView.getBackground()).getColor();
                if (containerBgColor == currentBgColor) {
                    container.setBackgroundResource(R.drawable.bg_option_selected);
                } else {
                    container.setBackgroundResource(R.drawable.bg_option_border_unselected);
                }
            }
        }
    }

    /**
     * 更新字体选择按钮的选中状态。
     * @param currentFontIdentifier 当前选中的字体标识符
     */
    private void updateFontSelectionUI(String currentFontIdentifier) {
        for (Button button : fontButtons) {
            boolean isSelected = false;
            if (button.getId() == R.id.btn_font_system_default && "system_default".equals(currentFontIdentifier)) {
                isSelected = true;
            } else if (button.getId() == R.id.btn_font_serif && "serif".equals(currentFontIdentifier)) {
                isSelected = true;
            }

            if (isSelected) {
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_500));
                button.setBackgroundResource(R.drawable.bg_option_selected);
            } else {
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
                button.setBackgroundResource(R.drawable.bg_option_border_unselected);
            }
        }
    }



    /**
     * 通知宿主 Activity 设置已更改。
     */
    private void notifySettingsChanged() {
        if (settingsChangeListener != null) {
            settingsChangeListener.onReadingSettingsChanged();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        settingsChangeListener = null;
    }

}