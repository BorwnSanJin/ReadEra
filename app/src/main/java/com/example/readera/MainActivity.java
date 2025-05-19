package com.example.readera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.readera.Adapter.BookAdapter;
import com.example.readera.Dao.BookDao;
import com.example.readera.Enum.CoverDataType;
import com.example.readera.model.BookInfo;
import com.google.android.material.navigation.NavigationView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityResultLauncher<Intent> filePickerLauncher;// 用于启动文件选择器的 ActivityResultLauncher
    private ActivityResultLauncher<Intent> directoryPickerLauncher;// 用于启动目录选择器的 ActivityResultLauncher
    private Toolbar toolbar; // 应用工具栏
    private ListView bookListView;// 显示书籍列表的 ListView
    private List<BookInfo> bookList;// 存储书籍信息的列表
    private BookAdapter adapter;// ListView 的适配器
    private BookDao bookDao;// 用于访问书籍数据的 DAO
    private DrawerLayout drawerLayout;// 侧滑菜单布局
    private NavigationView navigationView;// 侧滑菜单的 NavigationView
    private TextView emptyBookTextView;// 当书籍列表为空时显示的 TextView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // 初始化工具栏并设置为 ActionBar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.my_bookshelf); // 设置工具栏标题
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 显示返回按钮 (用于打开侧滑菜单)
            getSupportActionBar().setHomeButtonEnabled(true); // 允许点击返回按钮
        }

        // 初始化侧滑菜单相关的 View
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.nav_view);
        emptyBookTextView = findViewById(R.id.emptyBookTextView);

        // 创建 ActionBarDrawerToggle 以处理工具栏和侧滑菜单的联动
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);// 为侧滑菜单添加监听器
        toggle.syncState();// 同步工具栏上的菜单状态

        // 设置侧滑菜单的点击监听器
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_bookshelf) {
                // 当前已在书架页面
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, R.string.settings_function_developing, Toast.LENGTH_SHORT).show();
                // TODO: 启动设置 Activity
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // 初始化 BookDao 并获取所有书籍
        bookDao = new BookDao(this);
        bookList = bookDao.getAllBooks();
        // 初始化 BookAdapter 并设置给 ListView
        adapter = new BookAdapter(this, bookList);
        bookListView = findViewById(R.id.bookListView);
        bookListView.setAdapter(adapter);
        // 根据书籍列表是否为空显示/隐藏空列表提示
        emptyBookTextView.setVisibility(bookList.isEmpty() ? View.VISIBLE : View.GONE);
        Log.d("ContextMenu", "bookListView: " + bookListView);
        registerForContextMenu(bookListView);
        // 设置 ListView 的点击监听器，点击书籍条目打开阅读 Activity
        bookListView.setOnItemClickListener((parent, view, position, id) -> {
            BookInfo selectedBook = bookList.get(position);
            startReadingActivity(selectedBook.getUri());
        });

        // 初始化文件选择器的 ActivityResultLauncher
        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri uri = data.getData();
                            processImportedTextFile(uri);// 处理导入的文本文件
                        }
                    }
                });
        // 初始化目录选择器的 ActivityResultLauncher
        directoryPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri directoryUri = data.getData();
                            if (directoryUri != null) {
                                importTextFilesFromDirectory(directoryUri);// 从导入的目录中导入文本文件
                            }
                        }
                    }
                });
    }

    //计算hash
    private String calculateFileHash(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                return null;
            }
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            Log.e(TAG, "Error calculating file hash: " + e.getMessage());
            return null;
        }
    }

    // 获取文件大小和最后修改时间
    private Pair<Long, Long> getFileMetadata(Uri uri) {
        long size = -1;
        long lastModified = -1;
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE);
                if (sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex);
                }
                int modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED);
                if (modifiedIndex != -1) {
                    lastModified = cursor.getLong(modifiedIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file metadata: " + e.getMessage());
        }
        return new Pair<>(size, lastModified);
    }

    // 将书籍信息添加到数据库并更新 UI
    private void addBookToDatabase(BookInfo bookInfo) {
        Log.d(TAG, "Checking if book exists: URI=" + bookInfo.getUri() +
                ", Size=" + bookInfo.getFileSize() + ", Modified="
                + bookInfo.getLastModified() + ", Hash=" + bookInfo.getFileHash());
        boolean exists = bookDao.isBookExists(bookInfo);
        Log.d(TAG, "Book exists: " + exists);
        if(!exists){
            long id = bookDao.addBook(bookInfo);
            if (id > 0) {
                bookList.add(bookInfo);
                adapter.notifyDataSetChanged();
                emptyBookTextView.setVisibility(bookList.isEmpty() ? View.VISIBLE : View.GONE);
                Toast.makeText(this, getString(R.string.book_added, bookInfo.getTitle()), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.add_failed, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.book_already_exists, bookInfo.getTitle()), Toast.LENGTH_SHORT).show();
        }
    }

    // 处理导入的文本文件
    private void processImportedTextFile(Uri fileUri) {
        String fileName = getFileNameFromUri(fileUri);// 获取文件名
        if (fileName != null) {
            String fileNameWithoutExtension = removeFileExtension(fileName);
            Pair<Long, Long> metadata = getFileMetadata(fileUri);
            String fileHash = calculateFileHash(fileUri);
            BookInfo newBook = new BookInfo(fileNameWithoutExtension, fileUri, null, CoverDataType.TEXT,
                    false, false, false, metadata.first, metadata.second, fileHash);
            addBookToDatabase(newBook);
        }
    }


    // 从目录中导入文本文件
    private void importTextFilesFromDirectory(Uri directoryUri) {
        if (directoryUri != null) {
            try {
                List<Uri> fileUris = listTextFilesInDirectory(directoryUri);
                int importedCount = 0;
                for (Uri fileUri : fileUris) {
                    String fileName = getFileNameFromUri(fileUri);
                    if (fileName != null) {
                        String fileNameWithoutExtension = removeFileExtension(fileName);
                        Pair<Long, Long> metadata = getFileMetadata(fileUri);
                        String fileHash = calculateFileHash(fileUri);

                        BookInfo newBook = new BookInfo(fileNameWithoutExtension, fileUri, null,
                                CoverDataType.TEXT, false, false, false,
                                metadata.first, metadata.second, fileHash);
                        // 在这里使用标准的 addBookToDatabase 方法
                        addBookToDatabase(newBook);
                        if (!bookDao.isBookExists(newBook)) {
                            importedCount++;
                        }else {
                            Log.d(TAG, "Book already exists: " + newBook.getTitle());
                        }
                    }
                }
                if (importedCount > 0) {
                    Toast.makeText(this, getString(R.string.books_imported, importedCount), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.no_new_books_imported, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error importing from directory: " + e.getMessage());
                Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 列出指定目录下的所有文本文件 URI
    private List<Uri> listTextFilesInDirectory(Uri uri) throws Exception {
        List<Uri> textFiles = new ArrayList<>();
        if (uri != null) {
            try {
                // 请求持久的 URI 权限，以便后续可以访问
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                // 查询目录下的子文档
                android.database.Cursor cursor = getContentResolver().query(
                        DocumentsContract.buildChildDocumentsUriUsingTree(uri,
                                DocumentsContract.getTreeDocumentId(uri)),
                        new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                DocumentsContract.Document.COLUMN_MIME_TYPE},
                        null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String documentId = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID));
                        String displayName = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                        String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE));
                        // 判断是否为文本文件或以 .txt 结尾的文件名
                        if ("text/plain".equals(mimeType) || displayName.toLowerCase().endsWith(".txt")) {
                            Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId);
                            textFiles.add(fileUri);
                        }
                    }
                    cursor.close();
                }
            } catch (SecurityException securityException) {
                Log.e(TAG, "Permission denied to access directory: " + uri, securityException);
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                throw securityException;
            } catch (Exception e) {
                Log.e(TAG, "Error listing files in directory: " + uri, e);
                throw e;
            }
        }
        return textFiles;
    }

    // 从 URI 中获取文件名
    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri != null && "content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (displayNameIndex != -1) {
                        fileName = cursor.getString(displayNameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name: " + e.getMessage());
            }
        }
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }
        return fileName;
    }

    // 移除文件名中的扩展名
    private String removeFileExtension(String fileName) {
        if (fileName.toLowerCase().endsWith(".txt")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }

    // 启动阅读 Activity
    private void startReadingActivity(Uri fileUri) {
        Intent intent = new Intent(this, ReadingActivity.class);
        intent.putExtra("FILE_URI", fileUri);
        startActivity(intent);
    }

    // 创建选项菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // 打开文件选择器
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        filePickerLauncher.launch(intent);
    }

    //打开目录选择器
    private void openDirectoryPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        directoryPickerLauncher.launch(intent);
    }


    // 处理选项菜单的点击事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add) {
            openFilePicker();
            return true;
        } else if (id == R.id.action_import) {
            openDirectoryPicker();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 创建上下文菜单
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.bookListView) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.book_context_menu, menu); // 你的上下文菜单 XML 文件
        }
    }

    // 处理上下文菜单的点击事件
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.d("ContextMenu", "onContextItemSelected called with item ID: " + item.getItemId());
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        BookInfo bookToDelete = bookList.get(position);

        if (item.getItemId() == R.id.action_delete_book) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_delete)
                    .setMessage(getString(R.string.confirm_delete_book, bookToDelete.getTitle()))
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        deleteBookFromDatabase(bookToDelete);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    // 从数据库删除书籍并更新 UI
    private void deleteBookFromDatabase(BookInfo bookToDelete) {
        int deletedRows = bookDao.deleteBook(bookToDelete.getTitle());
        if (deletedRows > 0) {
            Toast.makeText(this, getString(R.string.book_deleted, bookToDelete.getTitle()), Toast.LENGTH_SHORT).show();
            bookList.remove(bookToDelete);
            adapter.notifyDataSetChanged();
            emptyBookTextView.setVisibility(bookList.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
        }
    }
}