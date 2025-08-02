package com.astin.moneymaster;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.astin.moneymaster.adapter.ViewPagerAdapter;
import com.astin.moneymaster.model.AppDatabase;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public ViewPager2 viewPager;

    private TabLayout tabLayout;
    private ProgressBar progressBar;

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
                super.onPageSelected(position);

                if (position == 0) { // Payment tab
                    Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + position);
                    if (fragment instanceof PayFragment) {
                        ((PayFragment) fragment).refreshList();
                    }
                }

                if (position == 1) { // History tab
                    Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + position);
                    if (fragment instanceof HistoryFragment) {
                        ((HistoryFragment) fragment).refreshData();
                    }
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

                    if (viewPager.getCurrentItem() == 0) { // Pay tab
                        PayFragment fragment = (PayFragment) getSupportFragmentManager()
                                .findFragmentByTag("f" + viewPager.getCurrentItem());

                        if (fragment != null) {
                            fragment.refreshList();
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Failed to reset balances: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }
}
