package com.example.readera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
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
import com.google.android.material.navigation.NavigationView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> directoryPickerLauncher;
    private Toolbar toolbar;
    private ListView bookListView;
    private List<BookInfo> bookList;
    private BookAdapter adapter;
    private BookDao bookDao;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView emptyBookTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.my_bookshelf);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.nav_view);
        emptyBookTextView = findViewById(R.id.emptyBookTextView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

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

        bookDao = new BookDao(this);
        bookList = bookDao.getAllBooks();
        adapter = new BookAdapter(this, bookList);
        bookListView = findViewById(R.id.bookListView);
        bookListView.setAdapter(adapter);
        emptyBookTextView.setVisibility(bookList.isEmpty() ? View.VISIBLE : View.GONE);
        Log.d("ContextMenu", "bookListView: " + bookListView);
        registerForContextMenu(bookListView);

        bookListView.setOnItemClickListener((parent, view, position, id) -> {
            BookInfo selectedBook = bookList.get(position);
            startReadingActivity(selectedBook.uri());
        });

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri uri = data.getData();
                            processImportedTextFile(uri);
                        }
                    }
                });

        directoryPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri directoryUri = data.getData();
                            if (directoryUri != null) {
                                importTextFilesFromDirectory(directoryUri);
                            }
                        }
                    }
                });
    }

    private void processImportedTextFile(Uri fileUri) {
        String fileName = getFileNameFromUri(fileUri);
        if (fileName != null) {
            String fileNameWithoutExtension = removeFileExtension(fileName);
            String coverData = "";
            try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 5; i++) {
                    String line = reader.readLine();
                    if (line == null) break;
                    sb.append(line).append("\n");
                }
                coverData = sb.toString().trim();
            } catch (IOException e) {
                Log.e(TAG, "Error reading file for cover text: " + e.getMessage());
                coverData = getString(R.string.default_cover_text);
            }
            addBookToDatabase(new BookInfo(fileNameWithoutExtension, fileUri, coverData, CoverDataType.TEXT));
        }
    }

    private void addBookToDatabase(BookInfo bookInfo) {
        long id = bookDao.addBook(bookInfo);
        if (id > 0) {
            bookList.add(bookInfo);
            adapter.notifyDataSetChanged();
            emptyBookTextView.setVisibility(bookList.isEmpty() ? View.VISIBLE : View.GONE);
            Toast.makeText(this, getString(R.string.book_added, bookInfo.title()), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.add_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void importTextFilesFromDirectory(Uri directoryUri) {
        if (directoryUri != null) {
            try {
                List<Uri> fileUris = listTextFilesInDirectory(directoryUri);
                for (Uri fileUri : fileUris) {
                    String fileName = getFileNameFromUri(fileUri);
                    if (fileName != null) {
                        String fileNameWithoutExtension = removeFileExtension(fileName);
                        String coverData = "";
                        try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
                             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < 5; i++) {
                                String line = reader.readLine();
                                if (line == null) break;
                                sb.append(line).append("\n");
                            }
                            coverData = sb.toString().trim();
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading file for cover text: " + e.getMessage());
                            coverData = getString(R.string.default_cover_text);
                        }
                        addBookToDatabase(new BookInfo(fileNameWithoutExtension, fileUri, coverData, CoverDataType.TEXT));
                    }
                }
                Toast.makeText(this, getString(R.string.books_imported, fileUris.size()), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error importing from directory: " + e.getMessage());
                Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private List<Uri> listTextFilesInDirectory(Uri uri) throws Exception {
        List<Uri> textFiles = new ArrayList<>();
        if (uri != null) {
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);

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

    private String removeFileExtension(String fileName) {
        if (fileName.toLowerCase().endsWith(".txt")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }

    private void startReadingActivity(Uri fileUri) {
        Intent intent = new Intent(this, ReadingActivity.class);
        intent.putExtra("FILE_URI", fileUri);
        startActivity(intent);
    }

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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.bookListView) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.book_context_menu, menu); // 你的上下文菜单 XML 文件
        }
    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.d("ContextMenu", "onContextItemSelected called with item ID: " + item.getItemId());
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        BookInfo bookToDelete = bookList.get(position);

        if (item.getItemId() == R.id.action_delete_book) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_delete)
                    .setMessage(getString(R.string.confirm_delete_book, bookToDelete.title()))
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        deleteBookFromDatabase(bookToDelete);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void deleteBookFromDatabase(BookInfo bookToDelete) {
        int deletedRows = bookDao.deleteBook(bookToDelete.title());
        if (deletedRows > 0) {
            Toast.makeText(this, getString(R.string.book_deleted, bookToDelete.title()), Toast.LENGTH_SHORT).show();
            bookList.remove(bookToDelete);
            adapter.notifyDataSetChanged();
            emptyBookTextView.setVisibility(bookList.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
        }
    }
}