package com.example.readera.fragments;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.readera.R;
// import your.package.name.adapter.ChapterAdapter; // 假设你有一个ChapterAdapter

public class TableOfContentsFragment extends Fragment {

    private RecyclerView recyclerView;
    // private ChapterAdapter chapterAdapter; // 如果你使用 RecyclerView 来显示章节列表

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_table_of_contents, container, false);

        recyclerView = view.findViewById(R.id.rv_table_of_contents);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: 在这里初始化你的 ChapterAdapter 并设置给 RecyclerView
        // 例如：
        // List<Chapter> chapters = getChapterListFromDatabase(); // 获取章节数据
        // chapterAdapter = new ChapterAdapter(chapters);
        // recyclerView.setAdapter(chapterAdapter);

        // TODO: 如果目录为空，可以显示一个提示文本
        // TextView emptyTocText = view.findViewById(R.id.tv_empty_toc);
        // if (chapters.isEmpty()) {
        //     emptyTocText.setVisibility(View.VISIBLE);
        //     recyclerView.setVisibility(View.GONE);
        // } else {
        //     emptyTocText.setVisibility(View.GONE);
        //     recyclerView.setVisibility(View.VISIBLE);
        // }

        return view;
    }
}