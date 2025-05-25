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

public class FavoriteFragment extends Fragment {
    private ListView favoriteBookListView;
    private List<BookInfo> favoriteBookList;
    private BookAdapter adapter;
    private BookDao bookDao;
    private TextView emptyFavoriteBookTextView;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorite,container,false);
        favoriteBookListView  = view.findViewById(R.id.favoriteBookListView);
        emptyFavoriteBookTextView = view.findViewById(R.id.emptyFavoriteBookTextView);

        bookDao = new BookDao(requireContext());
        favoriteBookList = new ArrayList<>();

        //实例化适配器传入回调函数
        adapter = new BookAdapter(requireContext(),favoriteBookList , ()->{
            // 当书籍状态改变时，重新加载未读书籍
            loadFavoriteBooks();
        });
        favoriteBookListView.setAdapter(adapter);
        // 设置 ListView 的点击监听器，点击书籍条目打开阅读 Activity
        favoriteBookListView.setOnItemClickListener((parent, v, position, id) -> {
            BookInfo selectedBook = favoriteBookList.get(position);
            ReadingUtils.startReadingActivity(requireContext(),selectedBook.getUri());
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavoriteBooks(); // 在 Fragment 可见时加载未读书籍
    }

    private void loadFavoriteBooks() {
        favoriteBookList.clear();
        favoriteBookList.addAll(bookDao.getAllFavoriteBooks()); // 调用 DAO 的未读方法
        adapter.notifyDataSetChanged();
        emptyFavoriteBookTextView.setVisibility(favoriteBookList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}