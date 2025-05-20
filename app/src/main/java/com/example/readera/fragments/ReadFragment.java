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

public class ReadFragment extends Fragment {
    private ListView readBookListView;
    private List<BookInfo> readBookList;
    private BookAdapter adapter;
    private BookDao bookDao;
    private TextView emptyReadBookTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_read, container, false);
        readBookListView = view.findViewById(R.id.readBookListView);
        emptyReadBookTextView = view.findViewById(R.id.emptyReadBookTextView);

        bookDao = new BookDao(requireContext());
        readBookList = new ArrayList<>();
        //实例化适配器
        adapter = new BookAdapter(requireContext(),readBookList,()->{
            loadReadBooks();
        });
        // 设置 ListView 的点击监听器，点击书籍条目打开阅读 Activity
        readBookListView.setOnItemClickListener((parent, v, position, id) -> {
            BookInfo selectedBook = readBookList.get(position);
            ReadingUtils.startReadingActivity(requireContext(),selectedBook.getUri());
        });

        readBookListView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReadBooks();
    }

    private void loadReadBooks() {
        readBookList.clear();
        readBookList.addAll(bookDao.getAllReadBooks());
        adapter.notifyDataSetChanged();
        emptyReadBookTextView.setVisibility(readBookList.isEmpty()?View.VISIBLE:View.GONE);
    }
}
