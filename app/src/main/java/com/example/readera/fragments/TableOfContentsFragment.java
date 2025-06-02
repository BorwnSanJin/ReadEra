package com.example.readera.fragments;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.readera.Adapter.TableOfContentsAdapter;
import com.example.readera.MoreOptionsActivity;
import com.example.readera.R;
import com.example.readera.ReadingActivity;
import com.example.readera.model.TableOfContents;
import com.example.readera.utiles.NovelReaderManager;
import com.example.readera.utiles.TableOfContentsGenerator;
import com.example.readera.utiles.TextPager;

import java.util.ArrayList;
import java.util.List;

public class TableOfContentsFragment extends Fragment implements TableOfContentsAdapter.OnTocEntryClickListener {

    private static final String ARG_FILE_URI = "file_uri";
    private static final String TAG = "TableOfContentsFragment";
    private static final String ARG_TABLE_OF_CONTENTS = "table_of_contents"; // 修改为专门的目录数据 key
    private RecyclerView recyclerView;
    private TextView emptyTocText;
    private TableOfContentsAdapter chapterAdapter;
    private Uri fileUri; // 当前阅读的文件 URI
    private List<TableOfContents> cachedTocEntries; // 新增：用于存储从Bundle传入的目录数据

    // 工厂方法，用于创建 Fragment 实例并传递参数
    public static TableOfContentsFragment newInstance(Uri fileUri,List<TableOfContents> tocEntries) {
        TableOfContentsFragment fragment = new TableOfContentsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE_URI, fileUri);
        if (tocEntries != null) {
            args.putSerializable(ARG_TABLE_OF_CONTENTS, new ArrayList<>(tocEntries)); // 传递目录数据
        }
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            fileUri = getArguments().getParcelable(ARG_FILE_URI);
            // 从 Bundle 中获取缓存的目录数据
            cachedTocEntries = (List<TableOfContents>) getArguments().getSerializable(ARG_TABLE_OF_CONTENTS);
            if (cachedTocEntries == null) {
                cachedTocEntries = new ArrayList<>(); // 确保不为null
            }
        } else {
            cachedTocEntries = new ArrayList<>(); // 确保不为null
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contents, container, false);

        recyclerView = view.findViewById(R.id.rv_contents);
        emptyTocText = view.findViewById(R.id.tv_empty_toc);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // 初始化适配器，传入一个空的列表和点击监听器
        chapterAdapter = new TableOfContentsAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(chapterAdapter);
        // 只有当没有缓存数据时才尝试加载目录
        if (cachedTocEntries.isEmpty()) {
            Log.d(TAG, "Bundle中没有缓存目录数据，尝试从NovelReaderManager加载。");
            loadTableOfContents();
        } else {
            Log.d(TAG, "Bundle中存在缓存目录数据，直接显示。条目数: " + cachedTocEntries.size());
            // 如果有缓存数据，直接更新UI
            updateUI(cachedTocEntries);
        }

        return view;
    }

    private void loadTableOfContents() {
        if (fileUri == null) {
            Log.e(TAG, "File URI is null, cannot load table of contents.");
            showEmptyTocMessage("文件路径无效");
            return;
        }

        // 获取当前 ReadingActivity 中正在使用的 TextPager 实例
        // 这是一个同步调用，因为 TextPager 应该已经在 ReadingActivity 中被创建并分页完成
        TextPager currentTextPager = NovelReaderManager.getInstance().getCurrentTextPager();

        if (currentTextPager != null && !currentTextPager.getPages().isEmpty()) {
            // 使用 TableOfContentsGenerator 来生成目录
            // 这里的 20 表示每隔 20 页生成一个目录条目（如果未识别到章节标题）
            List<TableOfContents> tocEntries = TableOfContentsGenerator.generateSimpleToc(currentTextPager);

            if (!tocEntries.isEmpty()) {
                chapterAdapter.updateData(tocEntries);
                emptyTocText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                showEmptyTocMessage("未找到目录内容");
            }
        } else {
            showEmptyTocMessage("小说内容未加载或未分页");
        }
    }

    private void updateUI(List<TableOfContents> tocEntries) {
        if (tocEntries != null && !tocEntries.isEmpty()) {
            chapterAdapter.updateData(tocEntries);
            emptyTocText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            showEmptyTocMessage("未找到目录内容");
        }
    }

    private void showEmptyTocMessage(String message) {
        emptyTocText.setText(message);
        emptyTocText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }


    @Override
    public void onTocEntryClick(TableOfContents entry) {

        // 当用户点击目录项时，跳转到 ReadingActivity 的对应页面
        if (getActivity() instanceof ReadingActivity|| getActivity() instanceof MoreOptionsActivity) {
            // 创建 Intent 返回给 ReadingActivity，并带上目标页码
            Intent intent = new Intent(getActivity(), ReadingActivity.class);
            intent.putExtra("FILE_URI", fileUri); // 确保文件 URI 也传回去
            intent.putExtra("INITIAL_PAGE", entry.pageIndex);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // 确保回到已有的 ReadingActivity 实例
            startActivity(intent);
            // 也可以选择直接通过 Activity 内部方法跳转，如果 TableOfContentsFragment 是 ReadingActivity 的内部 Fragment
            // ((ReadingActivity) getActivity()).jumpToPage(entry.pageIndex);
            // 关键：在启动 ReadingActivity 后，关闭当前的 MoreOptionsActivity
            // 因为 TableOfContentsFragment 是在 MoreOptionsActivity 中，所以要关闭它的父 Activity
            if (getActivity() instanceof MoreOptionsActivity) {
                getActivity().finish(); // 关闭 MoreOptionsActivity
            }
        } else {
            Toast.makeText(getContext(), "无法跳转到页面，Activity类型不匹配", Toast.LENGTH_SHORT).show();
        }
        if (getActivity() instanceof ReadingActivity) {
            // 如果 TableOfContentsFragment 是直接添加到 ReadingActivity 上的
            // 你可能需要通过FragmentManager来移除它
            getParentFragmentManager().popBackStack(); // 返回上一个 Fragment
        }

    }
}