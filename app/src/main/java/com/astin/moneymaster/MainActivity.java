package com.astin.moneymaster;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.viewpager2.widget.ViewPager2;

import com.astin.moneymaster.adapter.ViewPagerAdapter;
import com.astin.moneymaster.helper.FileProviderHelper;
import com.astin.moneymaster.helper.RoomHelper;
import com.astin.moneymaster.model.AppDatabase;
import com.astin.moneymaster.model.HistoryEntry;
import com.astin.moneymaster.model.HistoryEntryDao;
import com.astin.moneymaster.model.PaymentItem;
import com.astin.moneymaster.model.PaymentItemDao;
import com.astin.moneymaster.model.TempDatabase;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    public ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ProgressBar progressBar;

    private final ActivityResultLauncher<Intent> importLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        mergeDatabaseFromUri(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        progressBar.setVisibility(View.GONE);
        initializeApp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_backup) {
            showPopupMenu(findViewById(R.id.menu_backup));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showPopupMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.backup_popup_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.menu_share_db) {
                exportDatabase();
                return true;
            } else if (menuItem.getItemId() == R.id.menu_import_db) {
                importDatabase();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void exportDatabase() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {

                AppDatabase.closeDatabase();

                File dbFile = getDatabasePath("money_master_db");
                Log.d("ExportPath", "Original DB path: " + dbFile.getAbsolutePath());

                File exportDir = new File(getFilesDir(), "backup");
                if (!exportDir.exists()) exportDir.mkdirs();

                File exportFile = new File(exportDir, "money_master_db_backup.db");

                // Safely copy file
                try (InputStream in = new FileInputStream(dbFile);
                     OutputStream out = new FileOutputStream(exportFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }

                Uri contentUri = FileProvider.getUriForFile(
                        this,
                        "com.astin.moneymaster.provider",
                        exportFile
                );

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/octet-stream");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                runOnUiThread(() -> startActivity(Intent.createChooser(shareIntent, "Share database via")));
            } catch (Exception e) {
                Log.e("ExportError", "Export failed", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }



    private void importDatabase() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        importLauncher.launch(intent);
    }






    private void mergeDatabaseFromUri(Uri uri) {
        RoomHelper.mergeDatabaseFromUri(this, uri, new RoomHelper.MergeCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Imported completed", Toast.LENGTH_SHORT).show();
                    recreate();
                });
            }



            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Imported failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }











    private void initializeApp() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        String[] tabTitles = {
                "Pay \n(" + getCurrentDate() + ")",
                "History"
        };

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(tabTitles[position])
        ).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + position);
                if (position == 0 && fragment instanceof PayFragment) {
                    ((PayFragment) fragment).refreshList();
                } else if (position == 1 && fragment instanceof HistoryFragment) {
                    ((HistoryFragment) fragment).refreshData();

                }
            }
        });

        tabLayout.post(() -> {
            TabLayout.Tab payTab = tabLayout.getTabAt(0);
            if (payTab != null && payTab.view != null) {
                payTab.view.setOnLongClickListener(v -> {
                    onPayTabLongClicked();
                    return true;
                });
            }
        });
    }

    private void onPayTabLongClicked() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Budget Balances")
                .setMessage("Are you sure you want to reset all budget balances to their original budget amounts?")
                .setPositiveButton("Yes", (dialog, which) -> resetAllBudgetBalances())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void resetAllBudgetBalances() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                db.paymentItemDao().resetAllBudgetBalances();

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "All budget balances reset", Toast.LENGTH_SHORT).show();

                    if (viewPager.getCurrentItem() == 0) {
                        PayFragment fragment = (PayFragment) getSupportFragmentManager()
                                .findFragmentByTag("f" + viewPager.getCurrentItem());
                        if (fragment != null) fragment.refreshList();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Failed to reset: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
    }
}
