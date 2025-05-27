package com.example.readera.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast; // 导入 Toast，用于提示用户

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.readera.R;
import com.example.readera.utiles.ThemeManager; // 导入 ThemeManager

public class SettingsFragment extends Fragment {

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 使用正确的布局文件，这里假设您已经将其修改为包含 darkModeSwitch 的布局
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        @SuppressLint("UseSwitchCompatOrMaterialCode") // 抑制Lint警告，如果您使用的是androidx.appcompat.widget.SwitchCompat则可以移除
        Switch switchDarkMode = view.findViewById(R.id.darkModeSwitch); // 从 'view' 中查找 Switch

        // 初始化 Switch 的状态：根据 ThemeManager 中保存的主题模式设置
        int savedThemeMode = ThemeManager.getSavedThemeMode(requireContext());
        switchDarkMode.setChecked(savedThemeMode == ThemeManager.THEME_MODE_DARK);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int selectedThemeMode;
            if (isChecked) {
                // 切换到深色模式
                selectedThemeMode = AppCompatDelegate.MODE_NIGHT_YES;
            } else {
                // 切换到浅色模式
                selectedThemeMode = AppCompatDelegate.MODE_NIGHT_NO;
            }

            // 1. 保存主题模式到 SharedPreferences
            ThemeManager.saveThemeMode(requireContext(), selectedThemeMode);

            // 2. 应用主题模式
            AppCompatDelegate.setDefaultNightMode(selectedThemeMode);

            // 3. 强制重建当前 Activity
            // 这一步是关键！它会销毁并重建当前 Activity，确保新主题完全应用，
            // 并且 Activity 会重新加载其当前的 Fragment (即 SettingsFragment)，
            // 从而使您停留在设置页面。
            requireActivity().recreate();
        });
    }

}