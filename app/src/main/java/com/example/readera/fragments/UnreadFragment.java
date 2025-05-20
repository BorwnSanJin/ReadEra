package com.example.readera.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.readera.Adapter.BookAdapter;
import com.example.readera.Dao.BookDao;
import com.example.readera.R;
import com.example.readera.model.BookInfo;
import com.example.readera.utiles.ReadingUtils;

import java.util.ArrayList;
import java.util.List;

public class UnreadFragment extends Fragment {
    private ListView unreadBookListView;
    private List<BookInfo> unreadBookList;
    private BookAdapter adapter;
    private BookDao bookDao;
    private TextView emptyUnreadBookTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_unread, container, false);
        unreadBookListView = view.findViewById(R.id.unreadBookListView);
        emptyUnreadBookTextView = view.findViewById(R.id.emptyUnreadBookTextView);

        bookDao = new BookDao(requireContext());
        unreadBookList = new ArrayList<>(); // 初始化列表

        // 实例化适配器，并传入回调
        adapter = new BookAdapter(requireContext(), unreadBookList, () -> {
            // 当书籍状态改变时，重新加载未读书籍
            loadUnreadBooks();
        });
        unreadBookListView.setAdapter(adapter);
        emptyUnreadBookTextView.setVisibility(unreadBookList.isEmpty() ? View.VISIBLE : View.GONE);
        // 设置点击监听器等，与 BookShelfFragment 类似
        unreadBookListView.setOnItemClickListener((parent, v, position, id)->{
             BookInfo selectedBook = unreadBookList.get(position);
             ReadingUtils.startReadingActivity(requireContext(),selectedBook.getUri());
        });


        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        loadUnreadBooks(); // 在 Fragment 可见时加载未读书籍
    }

    private void loadUnreadBooks() {
        unreadBookList.clear();
        unreadBookList.addAll(bookDao.getAllUnreadBooks()); // 调用 DAO 的未读方法
        adapter.notifyDataSetChanged();
        emptyUnreadBookTextView.setVisibility(unreadBookList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}