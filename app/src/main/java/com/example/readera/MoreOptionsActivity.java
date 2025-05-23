package com.example.readera;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView; // 导入 TextView 类

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.readera.fragments.BookmarksFragment;
import com.example.readera.fragments.TableOfContentsFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MoreOptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_options); // 设置我们刚刚创建的布局
        EdgeToEdge.enable(this);
        // 获取顶部工具栏的引用
        LinearLayout moreOptionsTopBar = findViewById(R.id.more_options_top_bar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.more_options_top_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            moreOptionsTopBar.setPadding(
                    moreOptionsTopBar.getPaddingLeft(),
                    systemBars.top,
                    moreOptionsTopBar.getPaddingRight(),
                    moreOptionsTopBar.getPaddingBottom()
            );

            return insets;
        });

        ViewPager2 viewPager = findViewById(R.id.view_pager_more_options);
        TabLayout tabLayout = findViewById(R.id.tab_layout_more_options);
        ImageView backButton = findViewById(R.id.iv_more_back);
        // 设置 ViewPager2 的适配器
        MoreOptionsPagerAdapter adapter = new MoreOptionsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // 使用 TabLayoutMediator 将 TabLayout 和 ViewPager2 关联起来
        // 并在标签页切换时更新顶部标题
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    String titleText;
                    switch (position) {
                        case 0:
                           tab.setText("目录");
                            break;
                        case 1:
                            tab.setText("书签");  // 第二个标签是“书签”
                            break;
                        default:
                            tab.setText("未知"); // 默认文本，以防万一
                    }
                }).attach();


        // 处理返回按钮点击事件
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 关闭当前 Activity，返回到上一个界面
            }
        });

        tabLayout.setScrollPosition(0, 0f, true);
    }

    /**
     * ViewPager2 的适配器，负责创建并管理要显示的 Fragment
     */
    private class MoreOptionsPagerAdapter extends FragmentStateAdapter {

        public MoreOptionsPagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @Override
        public int getItemCount() {
            return 2; // 我们有两个标签页：目录和书签
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new TableOfContentsFragment(); // 返回目录 Fragment 实例
                case 1:
                    return new BookmarksFragment();       // 返回书签 Fragment 实例
                default:
                    throw new IllegalStateException("Unexpected position: " + position);
            }
        }
    }
}