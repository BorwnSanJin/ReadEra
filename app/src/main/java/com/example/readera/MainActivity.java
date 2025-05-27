package com.example.readera;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.readera.fragments.BookShelfFragment;
import com.example.readera.fragments.FavoriteFragment;
import com.example.readera.fragments.ReadFragment;
import com.example.readera.fragments.SettingsFragment;
import com.example.readera.fragments.UnreadFragment;
import com.example.readera.utiles.ThemeManager;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity{
    private Toolbar toolbar; // 应用工具栏
    private DrawerLayout drawerLayout;// 侧滑菜单布局
    private NavigationView navigationView;// 侧滑菜单的 NavigationView
    // 用于在 Activity 重建时保存当前选中的导航项ID
    private static final String SELECTED_NAV_ITEM_ID = "selected_nav_item_id";
    private int currentSelectedItemId = R.id.nav_bookshelf; // 默认选中书架


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this); // <-- 新增这行代码
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 初始化工具栏并设置为 ActionBar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 初始化侧滑菜单相关的 View
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.nav_view);

        // 创建 ActionBarDrawerToggle 以处理工具栏和侧滑菜单的联动
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);// 为侧滑菜单添加监听器
        toggle.syncState();// 同步工具栏上的菜单状态

        // 设置侧滑菜单的点击监听器
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            currentSelectedItemId = id; // 更新当前选中的ID
            if (id == R.id.nav_bookshelf) {
                displayFragment(new BookShelfFragment());
                toolbar.setTitle(R.string.my_bookshelf);
            } else if (id == R.id.nav_read) {
                displayFragment(new ReadFragment());
                toolbar.setTitle(R.string.read_books);
            } else if (id == R.id.nav_unread) {
                displayFragment(new UnreadFragment());
                toolbar.setTitle(R.string.unread_books);
            } else if (id == R.id.nav_favorite) {
                displayFragment(new FavoriteFragment());
                toolbar.setTitle(R.string.favorite_books);
            } else if (id == R.id.nav_settings) {
                displayFragment(new SettingsFragment());
                toolbar.setTitle(R.string.settings);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
        // 根据 savedInstanceState 决定是否显示默认 Fragment
        if (savedInstanceState == null) {
            // 第一次创建 Activity 时，显示默认的书架页面
            displayFragment(new BookShelfFragment());
            navigationView.setCheckedItem(R.id.nav_bookshelf);
            toolbar.setTitle(R.string.my_bookshelf);
            currentSelectedItemId = R.id.nav_bookshelf; // 确保初始状态正确
        } else {
            // Activity 被重建时，恢复之前选中的导航项和 Fragment
            currentSelectedItemId = savedInstanceState.getInt(SELECTED_NAV_ITEM_ID, R.id.nav_bookshelf);
            // 恢复 FragmentManager 之前的状态，不需要手动replace Fragment
            // FragmentManager 会自动重新附加已有的 Fragment
            // 但是为了更新 Toolbar 标题和 NavigationView 选中状态，我们需要手动更新
            navigationView.setCheckedItem(currentSelectedItemId);
            // 延迟更新标题，因为 Fragment 还没完全恢复，findFragmentById 可能返回 null
            // 或者我们可以直接根据 currentSelectedItemId 来设置标题
            updateToolbarTitle(currentSelectedItemId);
        }


        // 处理返回按钮事件
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStackImmediate();
                    updateToolbarTitleAndNavItem();
                } else {
                    finish();
                }
            }
        });
    }
    // 保存 Activity 状态，在重建时使用
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_NAV_ITEM_ID, currentSelectedItemId);
    }

    private void displayFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    // 适配重建后恢复侧滑菜单和标题
    private void updateToolbarTitle(int selectedItemId) {
        if (selectedItemId == R.id.nav_bookshelf) {
            toolbar.setTitle(R.string.my_bookshelf);
        } else if (selectedItemId == R.id.nav_read) {
            toolbar.setTitle(R.string.read_books);
        } else if (selectedItemId == R.id.nav_unread) {
            toolbar.setTitle(R.string.unread_books);
        } else if (selectedItemId == R.id.nav_favorite) {
            toolbar.setTitle(R.string.favorite_books);
        } else if (selectedItemId == R.id.nav_settings) {
            toolbar.setTitle(R.string.settings);
        }
    }
    // 更新Toolbar标题和侧滑菜单选中项（用于返回键处理）
    private void updateToolbarTitleAndNavItem() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof BookShelfFragment) {
            toolbar.setTitle(R.string.my_bookshelf);
            navigationView.setCheckedItem(R.id.nav_bookshelf);
            currentSelectedItemId = R.id.nav_bookshelf;
        } else if (currentFragment instanceof ReadFragment) {
            toolbar.setTitle(R.string.read_books);
            navigationView.setCheckedItem(R.id.nav_read);
            currentSelectedItemId = R.id.nav_read;
        } else if (currentFragment instanceof UnreadFragment) {
            toolbar.setTitle(R.string.unread_books);
            navigationView.setCheckedItem(R.id.nav_unread);
            currentSelectedItemId = R.id.nav_unread;
        } else if (currentFragment instanceof FavoriteFragment) {
            toolbar.setTitle(R.string.favorite_books);
            navigationView.setCheckedItem(R.id.nav_favorite);
            currentSelectedItemId = R.id.nav_favorite;
        } else if (currentFragment instanceof SettingsFragment) {
            toolbar.setTitle(R.string.settings);
            navigationView.setCheckedItem(R.id.nav_settings);
            currentSelectedItemId = R.id.nav_settings;
        } else {
            // 默认情况，如果 Fragment 恢复但无法识别，则回到书架
            toolbar.setTitle(R.string.my_bookshelf);
            navigationView.setCheckedItem(R.id.nav_bookshelf);
            currentSelectedItemId = R.id.nav_bookshelf;
            // 并且可能需要重新显示书架Fragment，以防万一
            // displayFragment(new BookShelfFragment()); // 注意：这可能导致无限循环或不必要的Fragment替换
        }
    }
}