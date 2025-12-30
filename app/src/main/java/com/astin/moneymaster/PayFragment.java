package com.astin.moneymaster;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.astin.moneymaster.adapter.PaymentAdapter;
import com.astin.moneymaster.model.AppDatabase;
import com.astin.moneymaster.model.MonthBalance;
import com.astin.moneymaster.model.PaymentItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.view.inputmethod.InputMethodManager;


public class PayFragment extends Fragment {

    private RecyclerView recyclerView;
    private PaymentAdapter adapter;
    private EditText etMonthBalance;
    private List<PaymentItem> cat_item_list = new ArrayList<>();
    private FloatingActionButton plusButton;
    private ProgressBar progressBar;
    private ViewPager2 viewPager;

    // 🔑 Important flags
    private boolean isUpdatingFromDb = false;
    private final Handler balanceHandler = new Handler(Looper.getMainLooper());
    private Runnable saveBalanceRunnable;

    public PayFragment() {
        super(R.layout.fragment_pay);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            viewPager = mainActivity.viewPager;
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        plusButton = view.findViewById(R.id.plusButton);
        progressBar = view.findViewById(R.id.progressBar);
        etMonthBalance = view.findViewById(R.id.etMonthBalance);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PaymentAdapter(
                requireContext(),
                cat_item_list,
                this::refreshList,
                this::navigateToHistory
        );
        recyclerView.setAdapter(adapter);

        refreshList();

        plusButton.setOnClickListener(v -> showAddItemDialog());


        setupMonthBalanceSaveOnDone();
        observeMonthBalance();
    }

    /* ---------------- MONTH BALANCE ---------------- */
    private void setupMonthBalanceSaveOnDone() {
        etMonthBalance.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {

                String value = etMonthBalance.getText().toString().trim();
                if (value.isEmpty()) return true;

                double balance;
                try {
                    balance = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(),
                            "Invalid balance",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }

                new Thread(() -> {
                    AppDatabase
                            .getInstance(requireContext())
                            .monthBalanceDao()
                            .insertOrUpdate(new MonthBalance(balance));
                }).start();

                // hide keyboard + clear focus
                etMonthBalance.clearFocus();
                hideKeyboard();
                return true;
            }
            return false;
        });
    }




    private void observeMonthBalance() {
        AppDatabase
                .getInstance(requireContext())
                .monthBalanceDao()
                .getBalanceLive()
                .observe(getViewLifecycleOwner(), balance -> {
                    if (balance == null) return;

                    String newVal = String.valueOf(balance);
                    String current = etMonthBalance.getText().toString();

                    if (balance < 0) {
                        etMonthBalance.setTextColor(
                                getResources().getColor(android.R.color.holo_red_light)
                        );
                    } else {
                        etMonthBalance.setTextColor(
                                getResources().getColor(android.R.color.white)
                        );
                    }

                    if (!newVal.equals(current)) {
                        isUpdatingFromDb = true;
                        etMonthBalance.setText(newVal);
                        etMonthBalance.setSelection(newVal.length());
                        isUpdatingFromDb = false;
                    }
                });
    }


    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(
                    etMonthBalance.getWindowToken(),
                    0
            );
        }
    }


    /* ---------------- NAVIGATION ---------------- */

    private void navigateToHistory() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).viewPager.setCurrentItem(1, true);
        }
    }

    /* ---------------- CATEGORY LIST ---------------- */

    public void refreshList() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<PaymentItem> items = AppDatabase
                    .getInstance(requireContext())
                    .paymentItemDao()
                    .getAllItems();

            requireActivity().runOnUiThread(() -> {
                cat_item_list.clear();
                cat_item_list.addAll(items);
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);

                if (cat_item_list.isEmpty()) {
                    Toast.makeText(requireContext(),
                            "No category, please add it",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /* ---------------- ADD CATEGORY DIALOG ---------------- */

    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Category");

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_category_item, null);

        EditText nameInput = dialogView.findViewById(R.id.editItemName);
        EditText budgetInput = dialogView.findViewById(R.id.editItemBudget);

        builder.setView(dialogView);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String budgetStr = budgetInput.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Please enter a category name",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            name = name.toUpperCase(Locale.getDefault());

            if (budgetStr.isEmpty()) {
                budgetStr = "0";
            }

            double budget;
            try {
                budget = Double.parseDouble(budgetStr);
            } catch (NumberFormatException e) {
                budget = 0;
            }

            PaymentItem item = new PaymentItem(name, budget);

            new Thread(() -> {
                try {
                    AppDatabase
                            .getInstance(requireContext())
                            .paymentItemDao()
                            .insert(item);

                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(),
                                "Category added",
                                Toast.LENGTH_SHORT).show();
                        refreshList();
                    });
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "Failed to add item",
                                    Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewPager != null) {
            viewPager.setUserInputEnabled(true);
        }
    }
}
