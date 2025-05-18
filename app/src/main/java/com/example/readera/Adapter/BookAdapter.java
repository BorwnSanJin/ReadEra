package com.example.readera.Adapter;

import static com.example.readera.Enum.CoverDataType.TEXT;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.readera.BookInfo;
import com.example.readera.Enum.CoverDataType;
import com.example.readera.R;
import android.widget.Toast;
import java.util.List;

public class BookAdapter extends ArrayAdapter<BookInfo> {

    private final LayoutInflater inflater;
    public BookAdapter(@NonNull Context context, List<BookInfo> books) {
        super(context, R.layout.item_book,books);
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_book, parent, false);
            holder = new ViewHolder();
            holder.bookImageView = convertView.findViewById(R.id.bookImageView);
            holder.bookTitleTextView = convertView.findViewById(R.id.bookTitleTextView);
            holder.unreadButton = convertView.findViewById(R.id.markAsUnreadButton);
            holder.readButton = convertView.findViewById(R.id.markAsReadButton);
            holder.favoriteButton = convertView.findViewById(R.id.markAsFavoriteButton);
            // 初始化按钮等其他 View
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        BookInfo currentBook = getItem(position);
        if (currentBook != null) {
            holder.bookTitleTextView.setText(currentBook.title());
            loadCover(holder.bookImageView, currentBook.coverData(), currentBook.coverDataType());

            holder.unreadButton.setOnClickListener(v -> {
                v.setSelected(!v.isSelected()); // 切换选中状态
                Toast.makeText(getContext(), R.string.mark_unread_toast, Toast.LENGTH_SHORT).show();
                // TODO: 实现标记为待读的逻辑
            });

            holder.readButton.setOnClickListener(v -> {
                v.setSelected(!v.isSelected()); // 切换选中状态
                Toast.makeText(getContext(), R.string.mark_read_toast, Toast.LENGTH_SHORT).show();
                // TODO: 实现标记为已读的逻辑
            });

            holder.favoriteButton.setOnClickListener(v -> {
                v.setSelected(!v.isSelected()); // 切换选中状态
                Toast.makeText(getContext(), R.string.mark_favorite_toast, Toast.LENGTH_SHORT).show();
                // TODO: 实现标记为收藏的逻辑
            });

        }
        return convertView;
    }

    private void loadCover(ImageView imageView, String coverData, CoverDataType coverDataType) {
        if (coverData != null && coverDataType != null) {
            switch (coverDataType) {
                case TEXT:
                    // 生成文本封面 (需要实现 generateCoverBitmap 方法)
                    Bitmap textCover = generateCoverBitmap(coverData, 80, 120);
                    imageView.setImageBitmap(textCover);
                    break;
                case RESOURCE_ID:
                    try {
                        int resourceId = Integer.parseInt(coverData);
                        imageView.setImageResource(resourceId);
                    } catch (NumberFormatException e) {
                        imageView.setImageResource(R.drawable.ic_book_placeholder);
                        Log.e("BookAdapter", "Invalid resource ID: " + coverData);
                    }
                    break;
                case URI:
                    imageView.setImageURI(Uri.parse(coverData));
                    break;
                case PDF_PAGE:
                    // TODO: 实现从 PDF 文件中提取第一页并显示
                    imageView.setImageResource(R.drawable.ic_pdf_placeholder);
                    break;
                default:
                    imageView.setImageResource(R.drawable.ic_book_placeholder);
                    break;
            }
        } else {
            imageView.setImageResource(R.drawable.ic_book_placeholder);
        }
    }

    private Bitmap generateCoverBitmap(String text, int width, int height) {
        // 实现文本生成封面的逻辑 (与之前的代码相同)
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(14f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        int x = width / 2;
        int y = (height / 2) + (bounds.height() / 2);
        canvas.drawText(text, x, y, textPaint);
        return bitmap;
    }

    static class ViewHolder {
        ImageView bookImageView;
        TextView bookTitleTextView;
        // 按钮等其他 View
        Button unreadButton;
        Button readButton;
        Button favoriteButton;
    }

}
