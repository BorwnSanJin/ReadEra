package com.example.readera.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.readera.Adapter.BookmarkAdapter;
import com.example.readera.R;
import com.example.readera.ReadingActivity;
import com.example.readera.model.Bookmark;
import com.example.readera.utiles.ReadingSettingsManager;

import java.util.ArrayList;
import java.util.List;

public class BookmarksFragment extends Fragment implements BookmarkAdapter.OnBookmarkClickListener  {

    private RecyclerView recyclerView;
    private TextView tvEmptyBookmarks;
    private BookmarkAdapter bookmarkAdapter;
    private ReadingSettingsManager readingSettingsManager;
    private List<Bookmark> bookmarks; // 用于存储书签数据
    private Bookmark currentLongPressedBookmark = null;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bookmarks, container, false);

        recyclerView = view.findViewById(R.id.rv_bookmarks);
        tvEmptyBookmarks = view.findViewById(R.id.tv_empty_bookmarks); // 找到空书签提示文本

        readingSettingsManager = new ReadingSettingsManager(getContext()); // 初始化 ReadingSettingsManager

        setupRecyclerView(); // 设置 RecyclerView
        loadBookmarks();     // 加载书签数据并更新UI

        registerForContextMenu(recyclerView); // 注册 RecyclerView 以便显示上下文菜单

        return view;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // 初始化 Adapter，并传入一个空的列表以及当前 Fragment 作为监听器
        // 实际数据将在 loadBookmarks() 中填充
        bookmarkAdapter = new BookmarkAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(bookmarkAdapter);
    }

    /**
     * 从 ReadingSettingsManager 加载书签数据，并更新 UI。
     */
    private void loadBookmarks() {
        bookmarks = readingSettingsManager.getAllBookmarks(); // 获取所有书签
        if (bookmarks.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyBookmarks.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyBookmarks.setVisibility(View.GONE);
            bookmarkAdapter.updateBookmarks(bookmarks); // 更新适配器数据
        }
    }

    @Override
    public void onBookmarkClick(Bookmark bookmark) {
        // 当用户点击书签时，跳转回 ReadingActivity 并打开指定页面
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), ReadingActivity.class);
            intent.putExtra("FILE_URI", bookmark.getFileUri());
            // 传递书签页码，以便 ReadingActivity 可以直接跳转
            intent.putExtra("INITIAL_PAGE", bookmark.getPageNumber());

            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            // 您可能希望在跳转后关闭包含此 Fragment 的 Activity
            // 如果 BookmarksFragment 位于一个独立的 Activity 中，可以在这里调用 getActivity().finish();
        }
    }

    @Override
    public void onBookmarkLongClick(Bookmark bookmark) {
        // 长按书签，存储该书签并打开上下文菜单
        currentLongPressedBookmark = bookmark; // 存储当前长按的书签
        requireActivity().openContextMenu(recyclerView); // 为 RecyclerView 打开上下文菜单
    }
    @Override
    public void onResume() {
        super.onResume();
        // 当 Fragment 重新可见时，重新加载书签，以防在其他地方（如 ReadingActivity）添加或删除了书签
        loadBookmarks();
    }


    // --- 上下文菜单相关方法 ---

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
                                    @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.rv_bookmarks) {
            MenuInflater inflater = requireActivity().getMenuInflater();
            // --- 关键修改：加载你的 menu 文件 ---
            inflater.inflate(R.menu.delete_context_menu, menu); // 假设你的菜单文件是 your_context_menu.xml

            // 同样，如果你想确保标题显示为“删除书签”，可以再次设置
            MenuItem deleteItem = menu.findItem(R.id.action_delete); // 注意：ID 变为 action_delete
            if (deleteItem != null) {
                deleteItem.setTitle("删除书签");
            }
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        final Bookmark bookmarkToDelete = currentLongPressedBookmark;

        // --- 关键修改：检查菜单项 ID ---
        if (item.getItemId() == R.id.action_delete && bookmarkToDelete != null) { // 注意：ID 变为 action_delete
            // 第一次确认 (AlertDialog)
            new AlertDialog.Builder(getContext())
                    .setTitle("确认删除")
                    .setMessage("确定要删除此书签吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        // 第二次确认 (Toast 和实际删除)
                        readingSettingsManager.removeBookmark(bookmarkToDelete);
                        loadBookmarks();
                        Toast.makeText(getContext(), "书签已删除", Toast.LENGTH_SHORT).show();
                        currentLongPressedBookmark = null;
                    })
                    .setNegativeButton("取消", (dialog, which) -> {
                        currentLongPressedBookmark = null;
                        dialog.dismiss();
                    })
                    .show();
            return true; // 事件已处理
        }

        return super.onContextItemSelected(item);
    }
}