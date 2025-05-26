package com.example.readera.Adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.readera.R;
import com.example.readera.utiles.ReadingSettingsManager;
import com.example.readera.views.NovelPageView;

import java.util.List;

public class NovelPageAdapter extends RecyclerView.Adapter<NovelPageAdapter.NovelPageViewHolder> {

    private final List<String> pages;
    private ReadingSettingsManager readingSettingsManager; // 添加此成员变量

    public NovelPageAdapter(List<String> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public NovelPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 在 ViewHolder 创建时初始化 ReadingSettingsManager
        if (readingSettingsManager == null) {
            readingSettingsManager = new ReadingSettingsManager(parent.getContext());
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_novel_page, parent, false);
        return new NovelPageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NovelPageViewHolder holder, int position) {
        String pageContent = pages.get(position);
        holder.novelPageView.setPageText(pageContent);
        Log.d("NovelPageAdapter", "绑定页面 " + position);

        // 关键：在这里应用阅读设置
        applyNovelPageViewSettings(holder.novelPageView);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    public List<String > getPages() {
        return this.pages;
    }

    public static class NovelPageViewHolder extends RecyclerView.ViewHolder {
        public NovelPageView novelPageView; // 对您的自定义视图的引用

        public NovelPageViewHolder(@NonNull View itemView) {
            super(itemView);
            novelPageView = itemView.findViewById(R.id.novel_page_view);
        }
    }

    /**
            * 将当前的阅读设置（字体、大小、行间距、内边距、颜色）应用到指定的 NovelPageView。
     *
            * @param pageView 需要应用设置的 NovelPageView
     */
    private void applyNovelPageViewSettings(NovelPageView pageView) {
        if (pageView != null && readingSettingsManager != null) {
            pageView.setTypeface(readingSettingsManager.getTypeface());
            pageView.setTextSize(readingSettingsManager.getTextSizeSp());
            pageView.setLineSpacingExtra(readingSettingsManager.getLineSpacingExtraDp());
            int[] padding = readingSettingsManager.getPagePaddingPx();
            pageView.setPagePadding(padding[0], padding[1], padding[2], padding[3]);
            pageView.setTextColor(readingSettingsManager.getTextColor());
            pageView.invalidate(); // 请求重绘以应用所有改变
            Log.d("NovelPageAdapter", "应用设置到 NovelPageView: " + pageView.hashCode());
        }
    }
}