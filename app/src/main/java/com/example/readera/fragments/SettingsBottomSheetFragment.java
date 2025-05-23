package com.example.readera.fragments; // 建议放在 fragments 包下

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView; // 示例：如果你的设置页面有文本

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.example.readera.R; // 确保导入你的 R 文件

public class SettingsBottomSheetFragment extends BottomSheetDialogFragment {

    // 通常用于创建 Fragment 实例的方法
    public static SettingsBottomSheetFragment newInstance() {
        return new SettingsBottomSheetFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 加载设置页面的布局文件
        View view = inflater.inflate(R.layout.fragment_settings_bottom_sheet, container, false);

        // TODO: 在这里初始化你的设置控件并添加逻辑
        // 示例：
        // TextView textSizeOption = view.findViewById(R.id.text_size_option);
        // textSizeOption.setOnClickListener(v -> {
        //     // 处理文本大小设置
        //     // dismiss(); // 处理完后可以关闭 BottomSheet
        // });

        return view;
    }

    // 可选：设置 BottomSheet 的样式，例如背景透明度
    // @Override
    // public void onCreate(Bundle savedInstanceState) {
    //     super.onCreate(savedInstanceState);
    //     // 使用一个没有背景的样式，以便自定义布局的圆角和背景
    //     setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
    // }
}