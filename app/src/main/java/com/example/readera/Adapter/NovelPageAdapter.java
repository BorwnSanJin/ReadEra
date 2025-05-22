package com.example.readera.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.readera.R;
import com.example.readera.views.NovelPageView;

import java.util.List;

public class NovelPageAdapter extends RecyclerView.Adapter<NovelPageAdapter.NovelPageViewHolder> {

    private final List<String> pages;

    public NovelPageAdapter(List<String> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public NovelPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_novel_page, parent, false);
        return new NovelPageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NovelPageViewHolder holder, int position) {
        String pageContent = pages.get(position);
        holder.novelPageView.setPageText(pageContent);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    public static class NovelPageViewHolder extends RecyclerView.ViewHolder {
        public NovelPageView novelPageView; // 对您的自定义视图的引用

        public NovelPageViewHolder(@NonNull View itemView) {
            super(itemView);
            novelPageView = itemView.findViewById(R.id.novel_page_view);
        }
    }
}