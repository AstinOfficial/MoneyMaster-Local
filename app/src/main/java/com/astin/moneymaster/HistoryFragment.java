package com.astin.moneymaster;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.astin.moneymaster.adapter.CategoryAdapter;
import com.astin.moneymaster.adapter.DayAdapter;
import com.astin.moneymaster.adapter.ExpenseAdapter;
import com.astin.moneymaster.helper.RoomHelper;
import com.astin.moneymaster.model.AppDatabase;
import com.astin.moneymaster.model.HistoryEntry;
import com.astin.moneymaster.model.HistoryEntryDao;
import com.astin.moneymaster.model.PaymentItem;
import com.astin.moneymaster.model.PaymentItemDao;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryFragment extends Fragment {

    private ExpenseAdapter expenseAdapter;

    private Spinner monthSpinner;
    private RecyclerView dayRecyclerView, itemsRecyclerView;
    private TextView monthSpendtxt, daySpendtxt;
    private FloatingActionButton plusButton;
    private ProgressBar progressBar;
    private static final String TAG = "HistoryFragmentDEBUG";
    private ToggleButton categoryToggle;
    private boolean isShowingCategories = false;
    private String currentMonth;
    private ViewPager2 viewPager;



    public HistoryFragment() {
        super(R.layout.fragment_history);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            viewPager = mainActivity.viewPager;
        }

//         To see all the entries
//        RoomHelper.loadAllHistoryEntries(requireContext());

        monthSpinner = view.findViewById(R.id.monthSpinner);
        dayRecyclerView = view.findViewById(R.id.dayRecyclerView);
        itemsRecyclerView = view.findViewById(R.id.itemsRecyclerView);
        monthSpendtxt = view.findViewById(R.id.monthSpendtxt);
        daySpendtxt = view.findViewById(R.id.daySpendtxt);
        plusButton = view.findViewById(R.id.plusButton);
        progressBar = view.findViewById(R.id.progressBar);
        categoryToggle = view.findViewById(R.id.toggleButton);
        dayRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));



        dayRecyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setUserInputEnabled(false);
            }
        });

        itemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        itemsRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        viewPager.setUserInputEnabled(true);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        viewPager.setUserInputEnabled(true);
                        break;

                    case MotionEvent.ACTION_UP:
                        viewPager.setUserInputEnabled(false);
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        viewPager.setUserInputEnabled(true);
                        break;

                    case MotionEvent.ACTION_OUTSIDE:
                        viewPager.setUserInputEnabled(false);
                        break;

                    default:
                        viewPager.setUserInputEnabled(false);
                        break;
                }

                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                Log.d(TAG, "onTouchEvent: " + e.toString());
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
                Log.d(TAG, "onRequestDisallowInterceptTouchEvent: " + disallowIntercept);
            }
        });

        loadToSpinner();






        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedMonth = parent.getItemAtPosition(position).toString();
                currentMonth = selectedMonth; // Save the current month selection

                // Load appropriate view based on toggle state
                if (isShowingCategories) {
                    loadCategoriesForMonth(selectedMonth);
                } else {
                    loadDaysForMonth(selectedMonth);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });



        categoryToggle.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                categoryToggle.setText("CATEGORY VIEW: ALL");
                Log.d("ASTINCHECK", "Long Click -> CATEGORY VIEW: ALL");

                loadAllMonthsCategoriesView(); // ✅ Use common function
                return true;
            }
        });






        // Set up the toggle button listener
        categoryToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            monthSpinner.setVisibility(View.VISIBLE);
            monthSpendtxt.setVisibility(View.VISIBLE);
            isShowingCategories = isChecked;
            String selectedMonth = monthSpinner.getSelectedItem() != null
                    ? monthSpinner.getSelectedItem().toString()
                    : null;

            if (selectedMonth != null) {
                if (isChecked) {
                    // Load categories view
                    loadCategoriesForMonth(selectedMonth);
                    categoryToggle.setText("Categories View");
                } else {
                    // Load days view
                    loadDaysForMonth(selectedMonth);
                    categoryToggle.setText("Days View");
                }
            }
        });



        plusButton.setOnClickListener(v -> ShowDialogBox());

        monthSpendtxt.setOnClickListener(v -> {
            String selectedMonth = monthSpinner.getSelectedItem() != null
                    ? monthSpinner.getSelectedItem().toString()
                    : null;

            if (selectedMonth != null) {
                calculateMonthlySpendingByCategory(selectedMonth);
            } else {
                Toast.makeText(getContext(), "No month selected", Toast.LENGTH_SHORT).show();
            }
        });



    }

    public void refreshList() {
        loadToSpinner();
    }



    @Override
    public void onResume() {
        super.onResume();
        viewPager.setUserInputEnabled(false);
    }

    /**
     * Loads categories across ALL months (common reusable function).
     */
    private void loadAllMonthsCategoriesView() {
        monthSpinner.setVisibility(View.INVISIBLE);
        monthSpendtxt.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        RoomHelper.loadCategoriesForAllMonths(requireContext(), new RoomHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(ArrayList<String> categories, Map<String, Double> categoryTotals, double totalSpend) {
                progressBar.setVisibility(View.GONE);

                CategoryAdapter categoryAdapter = new CategoryAdapter(categories, category -> {
                    loadItemsForCategoryAllMonths(category);
                });

                dayRecyclerView.setAdapter(categoryAdapter);
                monthSpendtxt.setText("Total Spend (All Months): " + totalSpend);

                if (!categories.isEmpty()) {
                    String firstCategory = categories.get(0);
                    categoryAdapter.selectCategory(firstCategory);
                    loadItemsForCategoryAllMonths(firstCategory);
                } else {
                    daySpendtxt.setText("Category Total: 0");
                }

                categoryToggle.setEnabled(true);
                plusButton.setEnabled(true);
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to load all months", Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
            }
        });
    }


    private void loadItemsForCategoryAllMonths(String category) {
        Log.d("CATHECK", "loadItemsForCategoryAllMonths");

        ArrayList<HistoryEntry> itemList = new ArrayList<>();

        expenseAdapter = new ExpenseAdapter(itemList, new ExpenseAdapter.OnItemActionListener() {
            @Override
            public void onItemLongClick(HistoryEntry item) {
                showDeleteConfirmationDialog(item);
            }

            @Override
            public void onItemEditClick(HistoryEntry item) {
                // Month isn’t needed here because we’re showing ALL months
                showEditDialog(null, item.getDateTime(), item);
            }
        });

        itemsRecyclerView.setAdapter(expenseAdapter);

        RoomHelper.loadItemsForCategoryAllMonths(requireContext(), category, new RoomHelper.OnHistoryDataListener() {
            @Override
            public void onDataLoaded(ArrayList<HistoryEntry> items, double totalSpend) {
                itemList.clear();
                itemList.addAll(items);
                daySpendtxt.setText("Category Total (All Months): " + totalSpend);
                expenseAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load items", Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
            }
        });
    }





    // New method to load categories for selected month
    private void loadCategoriesForMonth(String monthYear) {
        RoomHelper.loadCategoriesForMonth(requireContext(), monthYear, new RoomHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(ArrayList<String> categories, Map<String, Double> categoryTotals, double totalMonthSpend) {
                CategoryAdapter categoryAdapter = new CategoryAdapter(categories, category -> {
                    loadItemsForCategory(monthYear, category);  // Or pass actual selected month
                });

                dayRecyclerView.setAdapter(categoryAdapter);
                monthSpendtxt.setText("Month Total: " + totalMonthSpend);

                if (!categories.isEmpty()) {
                    String firstCategory = categories.get(0);
                    categoryAdapter.selectCategory(firstCategory);
                    loadItemsForCategory(monthYear, firstCategory);
                } else {
                    daySpendtxt.setText("Category Total: 0");
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void ShowDialogBox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Missing Items");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.missingdialogbox, null);
        builder.setView(dialogView);

        // Find views
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        EditText editItemBudget = dialogView.findViewById(R.id.editItemBudget);
        EditText editDate = dialogView.findViewById(R.id.addDate);
        EditText editItem = dialogView.findViewById(R.id.edititemname);

        // Load categories from Firebase into spinner
        List<String> categoryList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categoryList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        RoomHelper.loadCategoryNames(getContext(),new RoomHelper.OnCategoryNamesListener() {
            @Override
            public void onCategoriesLoaded(List<String> categories) {
                categoryList.clear();
                categoryList.addAll(categories);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load categories", Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
            }
        });

        // Set up date picker
        editDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar selectedCalendar = Calendar.getInstance();
                        selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0);

                        SimpleDateFormat datetime = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                        String selectedDate = datetime.format(selectedCalendar.getTime());

                        editDate.setText(selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        builder.setPositiveButton("Save", (dialog, which) -> {
            String selectedCategory = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "";
            String itemBudgetStr = editItemBudget.getText().toString().trim();
            String itemNameStr = editItem.getText().toString().trim();
            String date = editDate.getText().toString().trim();

            if (selectedCategory.isEmpty() || itemBudgetStr.isEmpty() || date.isEmpty()) {
                Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            double itemBudget = Double.parseDouble(itemBudgetStr);

            // 1. Save to history
            try {
                RoomHelper.recordHistory(requireContext(),selectedCategory, itemBudgetStr, date, itemNameStr);
            } catch (ParseException e) {
                Toast.makeText(getContext(), "Try again later", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Update budget_balance using FirebaseHelper
            RoomHelper.updateBudgetBalance(getContext(), selectedCategory, itemBudget, new RoomHelper.OnBudgetUpdateListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Saved: " + selectedCategory + ", " + itemBudget + ", " + date, Toast.LENGTH_SHORT).show();
                    refreshData();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Failed to update budget balance: " + error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, error);
                }
            });

        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void calculateMonthlySpendingByCategory(String monthYear) {
        RoomHelper.calculateMonthlySpendingByCategory(requireContext(), monthYear, new RoomHelper.OnCategorySpendingListener() {
            @Override
            public void onDataLoaded(Map<String, Double> categoryTotals) {
                // Same as before
                List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(categoryTotals.entrySet());
                Collections.sort(sortedEntries, (e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

                SpannableStringBuilder message = new SpannableStringBuilder("Spending in " + monthYear + ":\n\n");

                for (Map.Entry<String, Double> entry : sortedEntries) {
                    String category = entry.getKey();
                    String amount = String.format("%.2f", entry.getValue());

                    int start = message.length();
                    message.append(category);
                    int end = message.length();

                    message.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, 0);
                    message.append(": ").append(amount).append("\n");
                }

                new AlertDialog.Builder(getContext())
                        .setTitle("Monthly Category Spending")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
            }
        });

    }


    private void loadItemsForCategory(String monthYearInput, String category) {

        Log.d("CATHECK","loadItemsForCategory");


        ArrayList<HistoryEntry> itemList = new ArrayList<>();

        expenseAdapter = new ExpenseAdapter(itemList, new ExpenseAdapter.OnItemActionListener() {
            @Override
            public void onItemLongClick(HistoryEntry item) {
                showDeleteConfirmationDialog(item);
            }

            @Override
            public void onItemEditClick(HistoryEntry item) {
                showEditDialog(monthYearInput, item.getDateTime(), item);
            }
        });

        itemsRecyclerView.setAdapter(expenseAdapter);

        // Use Room instead of Firebase
        RoomHelper.loadItemsForCategory(requireContext(), monthYearInput, category, new RoomHelper.OnHistoryDataListener() {
            @Override
            public void onDataLoaded(ArrayList<HistoryEntry> items, double totalSpend) {
                itemList.clear();
                itemList.addAll(items);
                daySpendtxt.setText("Category Total: " + totalSpend);
                expenseAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load items", Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
            }
        });
    }



    private void loadToSpinner() {
        progressBar.setVisibility(View.VISIBLE);

        // 🔑 Check if we are in ALL view mode
        if ("CATEGORY VIEW: ALL".equals(categoryToggle.getText().toString())) {
            loadAllMonthsCategoriesView();
            return; // ✅ Exit early
        }


        // 🔑 Otherwise normal month-based loading
        monthSpinner.setVisibility(View.VISIBLE);
        monthSpendtxt.setVisibility(View.VISIBLE);

        ArrayList<String> monthsyearArray = new ArrayList<>();
        ArrayAdapter<String> Monthadapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, monthsyearArray);
        Monthadapter.setDropDownViewResource(R.layout.spinner_item_dark);
        monthSpinner.setAdapter(Monthadapter);

        RoomHelper.loadMonthsFromRoom(requireContext(), new RoomHelper.OnMonthsLoadedListener() {
            @Override
            public void onMonthsLoaded(ArrayList<String> months) {
                monthsyearArray.clear();
                monthsyearArray.addAll(months);
                Monthadapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);

                if (!monthsyearArray.isEmpty()) {
                    monthSpinner.setSelection(0);
                    currentMonth = monthsyearArray.get(0);
                    Log.d("MMTEST", "RoomHelper.loadMonths Latest month selected: " + currentMonth);

                    if (isShowingCategories) {
                        loadCategoriesForMonth(currentMonth);
                    } else {
                        loadDaysForMonth(currentMonth);
                    }

                    categoryToggle.setEnabled(true);
                    plusButton.setEnabled(true);
                } else {
                    Toast.makeText(getContext(), "No expense data available", Toast.LENGTH_SHORT).show();

                    monthSpendtxt.setText("Month Total: 0");
                    daySpendtxt.setText("Day Total: 0");

                    dayRecyclerView.setAdapter(null);
                    itemsRecyclerView.setAdapter(null);

                    categoryToggle.setEnabled(false);
                    plusButton.setEnabled(false);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load months", Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
                progressBar.setVisibility(View.GONE);
            }
        });
    }



    private void loadDaysForMonth(String month) {


        RoomHelper.loadDaysForMonth(requireContext(), month, new RoomHelper.OnDaysLoadedListener() {
            @Override
            public void onDaysLoaded(ArrayList<String> days, double totalMonthSpend) {
                ArrayList<String> dayList = new ArrayList<>(days);
                DayAdapter dayAdapter = new DayAdapter(dayList, day -> {
                    loadItemsForDay(month, day); // Already written
                });
                dayRecyclerView.setAdapter(dayAdapter);
                monthSpendtxt.setText("Month Total: " + totalMonthSpend);
                daySpendtxt.setText("Day Total: 0");

                if (!dayList.isEmpty()) {
                    String latestDay = dayList.get(0);
                    dayAdapter.selectDay(latestDay);
                    loadItemsForDay(month, latestDay);
                    dayRecyclerView.scrollToPosition(0);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load days", Toast.LENGTH_SHORT).show();
                Log.e("MMTEST", error);
            }
        });

    }



    private void loadItemsForDay(String month, String day) {

        Log.d("MMTEST", " MAIN DAY FUNCTION  MONTH="+month +" DAY = "+day);

        ArrayList<HistoryEntry> itemList = new ArrayList<>();
        expenseAdapter = new ExpenseAdapter(itemList, new ExpenseAdapter.OnItemActionListener() {
            @Override
            public void onItemLongClick(HistoryEntry item) {
                showDeleteConfirmationDialog(item);
            }

            @Override
            public void onItemEditClick(HistoryEntry item) {
                showEditDialog(month, day, item);
            }
        });
        itemsRecyclerView.setAdapter(expenseAdapter);



        RoomHelper.loadItemsForDay(getContext(), month, day, new RoomHelper.OnHistoryDataListener() {
            @Override
            public void onDataLoaded(ArrayList<HistoryEntry> items, double totalSpend) {
                itemList.clear();
                itemList.addAll(items);
                expenseAdapter.notifyDataSetChanged();
                daySpendtxt.setText("Day Total: " + totalSpend);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load items for day", Toast.LENGTH_SHORT).show();
                Log.e("MMTEST", error);
            }
        });



    }

    private void showEditDialog(String month, String dayInput, HistoryEntry item) {
        String dayOnly = dayInput;
        SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        fullFormat.setLenient(false);
        try {
            Date parsedDate = fullFormat.parse(dayInput);
            SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
            dayOnly = dayFormat.format(parsedDate);
        } catch (ParseException e) {
            Log.w("ASTIN", "Invalid date format for 'day': " + dayInput);
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_expense, null);
        Spinner editSpinnerCategory = dialogView.findViewById(R.id.EditspinnerCategory);
        EditText editAmount = dialogView.findViewById(R.id.editAmount);
        EditText editItem = dialogView.findViewById(R.id.editItemName);
        EditText editDate = dialogView.findViewById(R.id.editDate);

        String currentDate = dayInput + " " + month;
        editDate.setText(currentDate);

        editDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view, year, monthNEW, dayOfMonth) -> {
                        Calendar selectedCalendar = Calendar.getInstance();
                        selectedCalendar.set(year, monthNEW, dayOfMonth, 0, 0, 0);
                        SimpleDateFormat datetime = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                        editDate.setText(datetime.format(selectedCalendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        List<String> categoryList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categoryList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editSpinnerCategory.setAdapter(adapter);

        RoomHelper.loadCategoryNames(getContext(), new RoomHelper.OnCategoryNamesListener() {
            @Override
            public void onCategoriesLoaded(List<String> categories) {
                categoryList.clear();
                categoryList.addAll(categories);
                adapter.notifyDataSetChanged();

                int index = categoryList.indexOf(item.getCategoryName());
                if (index >= 0) {
                    editSpinnerCategory.setSelection(index);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load categories", Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
            }
        });

        editAmount.setText(String.valueOf(item.getAmountPaid()));
        String itemNameStr = item.getItemName();
        if (itemNameStr == null || itemNameStr.trim().isEmpty()) {
            editItem.setHint("Item Name (Optional)");
        } else {
            editItem.setText(itemNameStr);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Expense")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newCategory = editSpinnerCategory.getSelectedItem().toString();
                    String itemName = editItem.getText().toString().trim();
                    String amountStr = editAmount.getText().toString().trim();
                    String date = editDate.getText().toString().trim();

                    if (newCategory.isEmpty() || amountStr.isEmpty() || date.isEmpty()) {
                        Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double newAmount;
                    try {
                        newAmount = Double.parseDouble(amountStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Invalid amount entered", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (currentDate.equals(date)) {
                        RoomHelper.updateHistoryEntryWithBudgetLogic(
                                getContext(),
                                item,
                                newCategory,
                                newAmount,
                                itemName,
                                new RoomHelper.OnBudgetUpdateListener() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(getContext(), "Expense updated", Toast.LENGTH_SHORT).show();
                                        refreshData();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "Failed to update history entry: " + error);
                                        Toast.makeText(getContext(), "Failed to update: " + error, Toast.LENGTH_SHORT).show();
                                    }
                                }
                        );
                    } else {
                        RoomHelper.handleDateChangeAndUpdateExpense(
                                requireContext(),
                                item,
                                newCategory,
                                newAmount,
                                amountStr,
                                date,
                                itemName,
                                new RoomHelper.OnBudgetUpdateListener() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(getContext(), "Expense updated", Toast.LENGTH_SHORT).show();
                                        refreshData();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void showDeleteConfirmationDialog(HistoryEntry item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    deleteExpenseFromRoom(item,false);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteExpenseFromRoom(HistoryEntry item, boolean skipBudgetUpdate) {
        RoomHelper.deleteHistoryEntry(
                getContext(),
                item.getId(),
                item.getCategoryName(),
                item.getAmountPaid(),
                skipBudgetUpdate,
                new RoomHelper.OnDeleteListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onSuccess() {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Expense deleted", Toast.LENGTH_SHORT).show();
                            refreshList();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show());
                    }
                }
        );
    }




    public void refreshData() {
        loadToSpinner();
    }

}