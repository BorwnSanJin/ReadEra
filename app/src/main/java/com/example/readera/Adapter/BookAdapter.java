package com.example.readera.Adapter;

import static com.example.readera.Enum.CoverDataType.TEXT;

import android.content.Context;
import com.example.readera.utiles.CoverUtils;
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

import com.example.readera.model.BookInfo;
import com.example.readera.Dao.BookDao;
import com.example.readera.Enum.CoverDataType;
import com.example.readera.R;
import android.widget.Toast;
import java.util.List;

public class BookAdapter extends ArrayAdapter<BookInfo> {

    private static final int COVER_WIDTH = 80;
    private static final int COVER_HEIGHT = 120;

    private final LayoutInflater inflater;
    private final BookDao bookDao; // 添加 BookDao 实例

    public BookAdapter(@NonNull Context context, List<BookInfo> books) {
        super(context, R.layout.item_book,books);
        inflater = LayoutInflater.from(context);
        bookDao = new BookDao(context); // 初始化 BookDao
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
       if(convertView == null){
           convertView = inflater.inflate(R.layout.item_book,parent,false);
           holder = new ViewHolder(convertView);
       }else {
           holder = (ViewHolder) convertView.getTag();
       }
        BookInfo currentBook = getItem(position);
        if (currentBook != null) {
            setupBookItemView(holder, currentBook);
        }
        return convertView;
    }

    private void setupBookItemView(ViewHolder holder, BookInfo currentBook) {
        holder.bookTitleTextView.setText(currentBook.getTitle());
        loadCover(holder.bookImageView, currentBook);

        holder.unreadButton.setSelected(currentBook.isUnread());
        holder.readButton.setSelected(currentBook.isRead());
        holder.favoriteButton.setSelected(currentBook.isFavorite());

        final String bookTitle = currentBook.getTitle();

        holder.unreadButton.setOnClickListener(v -> {
            boolean isUnread = !currentBook.isUnread();
            bookDao.updateBookUnreadStatus(bookTitle, isUnread);
            currentBook.setUnread(isUnread);
            if (isUnread && currentBook.isRead()) {
                bookDao.updateBookReadStatus(bookTitle, false);
                currentBook.setRead(false);
            }
            notifyDataSetChanged();
            Toast.makeText(getContext(), isUnread ? R.string.mark_unread_toast : R.string.unmark_unread_toast, Toast.LENGTH_SHORT).show();
        });

        holder.readButton.setOnClickListener(v -> {
            boolean isRead = !currentBook.isRead();
            bookDao.updateBookReadStatus(bookTitle, isRead);
            currentBook.setRead(isRead);
            if (isRead && currentBook.isUnread()) {
                bookDao.updateBookUnreadStatus(bookTitle, false);
                currentBook.setUnread(false);
            }
            notifyDataSetChanged();
            Toast.makeText(getContext(), isRead ? R.string.mark_read_toast : R.string.unmark_read_toast, Toast.LENGTH_SHORT).show();
        });

        holder.favoriteButton.setOnClickListener(v -> {
            boolean isFavorite = !currentBook.isFavorite();
            bookDao.updateBookFavoriteStatus(bookTitle, isFavorite);
            currentBook.setFavorite(isFavorite);
            holder.favoriteButton.setSelected(isFavorite);
            notifyDataSetChanged();
            Toast.makeText(getContext(), isFavorite ? R.string.mark_favorite_toast : R.string.mark_unFavorite_toast, Toast.LENGTH_SHORT).show();
        });
    }

    private void loadCover(ImageView imageView, BookInfo bookInfo) {
        if (bookInfo != null ) {
            if (bookInfo.coverDataType() == TEXT) {
                imageView.setImageBitmap(CoverUtils.generateCoverBitmap(bookInfo.getTitle(), COVER_WIDTH, COVER_HEIGHT));
            } else if (bookInfo.coverDataType() == CoverDataType.RESOURCE_ID) {
                try {
                    int resourceId = Integer.parseInt(bookInfo.coverData());
                    imageView.setImageResource(resourceId);
                } catch (NumberFormatException e) {
                    imageView.setImageResource(R.drawable.ic_book_placeholder);
                    Log.e("BookAdapter", "Invalid resource ID: " +  bookInfo.coverData());
                }
            } else if (bookInfo.coverDataType() == CoverDataType.URI) {
                imageView.setImageURI(Uri.parse(bookInfo.coverData()));
            } else if (bookInfo.coverDataType() == CoverDataType.PDF_PAGE) {
                //todo 实现根据pdf第一页生成封面
                imageView.setImageResource(R.drawable.ic_pdf_placeholder);
            } else {
                imageView.setImageResource(R.drawable.ic_book_placeholder);
            }
        } else {
            imageView.setImageResource(R.drawable.ic_book_placeholder);
        }
    }



    static class ViewHolder {
        ImageView bookImageView;
        TextView bookTitleTextView;
        // 按钮等其他 View
        Button unreadButton;
        Button readButton;
        Button favoriteButton;
        ViewHolder(View view) {
            bookImageView = view.findViewById(R.id.bookImageView);
            bookTitleTextView = view.findViewById(R.id.bookTitleTextView);
            unreadButton = view.findViewById(R.id.markAsUnreadButton);
            readButton = view.findViewById(R.id.markAsReadButton);
            favoriteButton = view.findViewById(R.id.markAsFavoriteButton);
            view.setTag(this);
        }
    }

}
