package com.example.readera;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;

import androidx.activity.EdgeToEdge;
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
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity{
    private Toolbar toolbar; // 应用工具栏
    private DrawerLayout drawerLayout;// 侧滑菜单布局
    private NavigationView navigationView;// 侧滑菜单的 NavigationView


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        // 设置默认显示的页面
        displayFragment(new BookShelfFragment());
        navigationView.setCheckedItem(R.id.nav_bookshelf);
        toolbar.setTitle(R.string.my_bookshelf);

        // 处理返回按钮事件
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStackImmediate();
                    updateToolbarTitle();
                } else {
                    finish();
                }
            }
        });
    }

    private void displayFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    private void updateToolbarTitle() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof BookShelfFragment) {
            toolbar.setTitle(R.string.my_bookshelf);
        } else if (currentFragment instanceof ReadFragment) {
            toolbar.setTitle(R.string.read_books);
        } else if (currentFragment instanceof UnreadFragment) {
            toolbar.setTitle(R.string.unread_books);
        } else if (currentFragment instanceof FavoriteFragment) {
            toolbar.setTitle(R.string.favorite_books);
        } else if (currentFragment instanceof SettingsFragment) {
            toolbar.setTitle(R.string.settings);
        }
    }
}