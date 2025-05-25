package com.example.readera.Adapter;// BookmarkAdapter.java
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.readera.R;
import com.example.readera.model.Bookmark;

import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder> {

    private List<Bookmark> bookmarkList;
    private OnBookmarkClickListener listener;

    public interface OnBookmarkClickListener {
        void onBookmarkClick(Bookmark bookmark);
        void onBookmarkLongClick(Bookmark bookmark); // 用于删除等操作
    }

    public BookmarkAdapter(List<Bookmark> bookmarkList, OnBookmarkClickListener listener) {
        this.bookmarkList = bookmarkList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookmark, parent, false); // 您需要创建 item_bookmark.xml 布局
        return new BookmarkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
        Bookmark bookmark = bookmarkList.get(position);
        holder.tvBookmarkTitle.setText(bookmark.getDisplayTitle());
        holder.tvBookmarkPage.setText("页码: " + (bookmark.getPageNumber() + 1)); // 页码通常从1开始显示

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookmarkClick(bookmark);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onBookmarkLongClick(bookmark);
            }
            return true; // 消费长按事件
        });
    }

    @Override
    public int getItemCount() {
        return bookmarkList.size();
    }

    public void updateBookmarks(List<Bookmark> newBookmarks) {
        this.bookmarkList = newBookmarks;
        notifyDataSetChanged();
    }

    static class BookmarkViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookmarkTitle;
        TextView tvBookmarkPage;

        public BookmarkViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookmarkTitle = itemView.findViewById(R.id.tv_bookmark_title); // 需要在 item_bookmark.xml 中定义
            tvBookmarkPage = itemView.findViewById(R.id.tv_bookmark_page);   // 需要在 item_bookmark.xml 中定义
        }
    }
}