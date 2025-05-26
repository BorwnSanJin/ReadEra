package com.example.readera.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.readera.R;
import com.example.readera.model.TableOfContents;

import java.util.List;

public class TableOfContentsAdapter extends RecyclerView.Adapter<TableOfContentsAdapter.TocViewHolder> {

    private List<TableOfContents> tocEntries;
    private OnTocEntryClickListener listener;

    public interface OnTocEntryClickListener {
        void onTocEntryClick(TableOfContents entry);
    }

    public TableOfContentsAdapter(List<TableOfContents> tocEntries, OnTocEntryClickListener listener) {
        this.tocEntries = tocEntries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TocViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contents, parent, false);
        return new TocViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TocViewHolder holder, int position) {
        TableOfContents entry = tocEntries.get(position);
        holder.tvTitle.setText(entry.title);
        // 根据深度设置左边距，以实现嵌套效果
        // 例如：每层深度缩进 24dp (转换为像素)
        int indentPx = entry.depth * (int) holder.itemView.getContext().getResources().getDimension(R.dimen.toc_indent_level);
        ((ViewGroup.MarginLayoutParams) holder.tvTitle.getLayoutParams()).leftMargin = indentPx;
        holder.tvTitle.requestLayout(); // 重新布局

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTocEntryClick(entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tocEntries.size();
    }

    public void updateData(List<TableOfContents> newEntries) {
        this.tocEntries = newEntries;
        notifyDataSetChanged();
    }

    static class TocViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;

        public TocViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_toc_title);
        }
    }
}