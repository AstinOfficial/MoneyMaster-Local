package com.astin.moneymaster.helper;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.room.Room;

import com.astin.moneymaster.model.AppDatabase;
import com.astin.moneymaster.model.CategoryTotal;
import com.astin.moneymaster.model.HistoryEntry;
import com.astin.moneymaster.model.HistoryEntryDao;
import com.astin.moneymaster.model.MonthBalance;
import com.astin.moneymaster.model.MonthBalanceDao;
import com.astin.moneymaster.model.PaymentItem;
import com.astin.moneymaster.model.PaymentItemDao;
import com.astin.moneymaster.model.TempDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.text.TextUtils;


public class RoomHelper {

    public interface OnMonthsLoadedListener {
        void onMonthsLoaded(ArrayList<String> months);
        void onError(String error);
    }

    public interface OnDaysLoadedListener {
        void onDaysLoaded(ArrayList<String> days, double totalMonthSpend);
        void onError(String error);
    }

    public interface OnHistoryDataListener {
        void onDataLoaded(ArrayList<HistoryEntry> items, double totalSpend);
        void onError(String error);
    }

    public interface OnBudgetUpdateListener {
        void onSuccess();
        void onError(String error);
    }



    public interface OnCategoryNamesListener {
        void onCategoriesLoaded(List<String> categories);
        void onError(String error);
    }

    public interface OnDeleteListener {
        void onSuccess();
        void onError(String error);
    }
    public interface OnCategorySpendingListener {
        void onDataLoaded(Map<String, Double> categoryTotals);
        void onError(String error);
    }


    public static void loadAllHistoryEntries(Context context) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<HistoryEntry> allEntries = db.historyEntryDao().getAllEntries();  // Already exists in your DAO

            Log.d("ROOMHELPER", "TOTAL entries in DB: " + allEntries.size());

            for (HistoryEntry entry : allEntries) {
                Log.d("ROOMHELPER", "Entry → Category: " + entry.getCategoryName()
                        + ", Amount: " + entry.getAmountPaid()
                        + ", DateTime: " + entry.getDateTime()
                        + ", Item: " + entry.getItemName());
            }
        }).start();
    }












    public static void loadItemsForCategory(Context context, String monthYearInput, String category, OnHistoryDataListener listener) {
        new Thread(() -> {
            try {
                // Step 1: Parse the month
                SimpleDateFormat inputFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                Date parsedDate = inputFormat.parse(monthYearInput);
                String dbMonth = dbFormat.format(parsedDate);  // "2025-08"


                // Step 2: Fetch from database
                AppDatabase db = AppDatabase.getInstance(context);
                List<HistoryEntry> entries = db.historyEntryDao().getEntriesByMonthAndCategory(dbMonth, category);

                double totalSpend = 0;
                ArrayList<HistoryEntry> itemList = new ArrayList<>();

                for (HistoryEntry entry : entries) {

                    totalSpend += entry.getAmountPaid();
                    itemList.add(entry);
                }

                double finalTotalSpend = totalSpend;

                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onDataLoaded(itemList, finalTotalSpend);
                });

            } catch (ParseException e) {
                e.printStackTrace();

                new Handler(Looper.getMainLooper()).post(() ->
                        listener.onError("Invalid month format"));
            }
        }).start();
    }


    public static void mergeDatabaseFromUri(Context context, Uri uri, MergeCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {
                File tempFile = new File(context.getFilesDir(), "temp_imported.db");

                try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                     OutputStream outputStream = new FileOutputStream(tempFile)) {

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                }

                if (!tempFile.exists()) {
                    callback.onError("Temp DB file not found after copy.");
                    return;
                }

                TempDatabase tempDb = Room.databaseBuilder(
                                context,
                                TempDatabase.class,
                                "temp_imported_db")
                        .createFromFile(tempFile)
                        .allowMainThreadQueries()
                        .build();

                AppDatabase localDb = AppDatabase.getInstance(context);

                // DAOs
                PaymentItemDao localItemDao = localDb.paymentItemDao();
                HistoryEntryDao localHistoryDao = localDb.historyEntryDao();
                MonthBalanceDao localMonthDao = localDb.monthBalanceDao();

                PaymentItemDao importedItemDao = tempDb.paymentItemDao();
                HistoryEntryDao importedHistoryDao = tempDb.historyEntryDao();
                MonthBalanceDao importedMonthDao = tempDb.monthBalanceDao();

                // Fetch data
                List<PaymentItem> importedItems = importedItemDao.getAllItemsOnce();
                List<HistoryEntry> importedHistory = importedHistoryDao.getAllEntriesOnce();

                List<PaymentItem> existingItems = localItemDao.getAllItemsOnce();
                List<HistoryEntry> existingHistory = localHistoryDao.getAllEntriesOnce();

                // ---------------- MERGE PAYMENT ITEMS ----------------
                for (PaymentItem item : importedItems) {
                    boolean isDuplicate = false;
                    for (PaymentItem existing : existingItems) {
                        if (existing.getName().equals(item.getName()) &&
                                existing.getBudget() == item.getBudget()) {
                            isDuplicate = true;
                            break;
                        }
                    }
                    if (!isDuplicate) {
                        PaymentItem newItem = new PaymentItem(item.getName(), item.getBudget());
                        newItem.setBudget_balance(item.getBudget_balance());
                        localItemDao.insert(newItem);
                    }
                }

                // ---------------- MERGE HISTORY ----------------
                for (HistoryEntry entry : importedHistory) {
                    boolean isDuplicate = false;
                    for (HistoryEntry existing : existingHistory) {
                        if (TextUtils.equals(existing.getCategoryName(), entry.getCategoryName()) &&
                                TextUtils.equals(existing.getItemName(), entry.getItemName()) &&
                                existing.getAmountPaid() == entry.getAmountPaid() &&
                                TextUtils.equals(existing.getDateTime(), entry.getDateTime())) {
                            isDuplicate = true;
                            break;
                        }
                    }
                    if (!isDuplicate) {
                        HistoryEntry newEntry = new HistoryEntry();
                        newEntry.setCategoryName(entry.getCategoryName());
                        newEntry.setItemName(entry.getItemName());
                        newEntry.setAmountPaid(entry.getAmountPaid());
                        newEntry.setDateTime(entry.getDateTime());
                        localHistoryDao.insert(newEntry);
                    }
                }

                // ---------------- MERGE MONTH BALANCE ----------------
                Double localBalance = localMonthDao.getBalanceOnce();
                Double importedBalance = importedMonthDao.getBalanceOnce();

                double finalBalance = 0.0;

                if (localBalance != null) {
                    finalBalance += localBalance;
                }

                if (importedBalance != null) {
                    finalBalance += importedBalance;
                }

                // Save merged balance ONLY if at least one exists
                if (localBalance != null || importedBalance != null) {
                    localMonthDao.insertOrUpdate(new MonthBalance(finalBalance));
                }

                // Cleanup
                tempDb.close();
                tempFile.delete();

                callback.onSuccess();

            } catch (Exception e) {
                Log.e("RoomHelper", "Merge failed", e);
                callback.onError(e.getMessage());
            }
        });
    }


    public interface MergeCallback {
        void onSuccess();
        void onError(String error);
    }




    public static void recordHistory(Context context, PaymentItem cat_item, double amountPaid) {
        if (cat_item == null || cat_item.getName() == null || cat_item.getName().isEmpty()) {
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a"));
        String cat_name = cat_item.getName().toUpperCase(Locale.getDefault());

        HistoryEntry entry = new HistoryEntry();
        entry.setCategoryName(cat_name);
        entry.setAmountPaid(amountPaid);
        entry.setDateTime(timestamp);
        entry.setItemName(null);

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            db.historyEntryDao().insert(entry);
        }).start();
    }


    public static void recordHistory(Context context, String cat_item, String amountPaid, String datetime, String itemName) throws ParseException {
        if (cat_item == null || amountPaid == null || datetime == null) {
            return;
        }

        cat_item = cat_item.toUpperCase(Locale.getDefault());
        itemName = (itemName != null) ? itemName.toUpperCase(Locale.getDefault()) : null;

        SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        Date dateOnly = inputDateFormat.parse(datetime);

        // Get current time (hours, minutes, seconds)
        Calendar now = Calendar.getInstance();
        Calendar combined = Calendar.getInstance();
        combined.setTime(dateOnly);
        combined.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
        combined.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
        combined.set(Calendar.SECOND, now.get(Calendar.SECOND));

        // Format to: 2025-08-02 09:52:49 pm
        SimpleDateFormat finalFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault());
        String finalDateTime = finalFormat.format(combined.getTime());

        // Parse amountPaid to double
        double amount = Double.parseDouble(amountPaid);

        // Create and fill HistoryEntry
        HistoryEntry entry = new HistoryEntry();
        entry.setCategoryName(cat_item);
        entry.setAmountPaid(amount);
        entry.setDateTime(finalDateTime);  // Save formatted full timestamp here
        entry.setItemName(itemName);

        // Insert into Room in background
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            db.historyEntryDao().insert(entry);
        }).start();
    }

    public static void calculateMonthlySpendingByCategory(Context context, String monthYearInput, OnCategorySpendingListener listener) {
        new Thread(() -> {
            try {
                // Convert "August 2025" to "2025-08"
                SimpleDateFormat inputFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                Date parsedDate = inputFormat.parse(monthYearInput);
                String dbMonth = dbFormat.format(parsedDate);

                AppDatabase db = AppDatabase.getInstance(context);
                List<CategoryTotal> results = db.historyEntryDao().getCategoryTotalsForMonth(dbMonth);

                Map<String, Double> categoryTotals = new HashMap<>();
                for (CategoryTotal result : results) {
                    categoryTotals.put(result.categoryName, result.total);
                }

                new Handler(Looper.getMainLooper()).post(() -> listener.onDataLoaded(categoryTotals));

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() ->
                        listener.onError("Failed to calculate spending: " + e.getMessage()));
            }
        }).start();
    }


    public static void updateBudgetBalance(Context context, String categoryName, double amountToDeduct, OnBudgetUpdateListener listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                PaymentItemDao dao = db.paymentItemDao();

                // Fetch category
                PaymentItem item = dao.getCategoryByName(categoryName);
                if (item == null) {
                    postError(listener, "Category not found");
                    return;
                }

                double currentBalance = item.getBudget_balance();
                double newBalance = currentBalance - amountToDeduct;

                dao.updateBudgetBalance(categoryName, newBalance);

                // Notify success on main thread
                new Handler(Looper.getMainLooper()).post(listener::onSuccess);

            } catch (Exception e) {
                postError(listener, "Failed to update budget balance: " + e.getMessage());
            }
        });
    }

    // Helper method to post error on main thread
    private static void postError(OnBudgetUpdateListener listener, String errorMsg) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onError(errorMsg));
    }

    public interface OnCategoriesLoadedListener {
        void onCategoriesLoaded(ArrayList<String> categories, Map<String, Double> categoryTotals, double totalMonthSpend);
        void onError(String error);
    }

    public static void loadCategoriesForAllMonths(Context context, OnCategoriesLoadedListener listener) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<HistoryEntry> entries = db.historyEntryDao().getAllEntries();

                Map<String, Double> categoryTotals = new HashMap<>();
                ArrayList<String> categories = new ArrayList<>();
                double totalSpend = 0;

                for (HistoryEntry entry : entries) {
                    String category = entry.getCategoryName();
                    double amount = entry.getAmountPaid();

                    totalSpend += amount;
                    categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);

                    if (!categories.contains(category)) {
                        categories.add(category);
                    }
                }

                // Sort by highest spend
                categories.sort((c1, c2) -> Double.compare(categoryTotals.get(c2), categoryTotals.get(c1)));

                double finalTotalSpend = totalSpend;
                ArrayList<String> finalCategories = new ArrayList<>(categories);

                new Handler(Looper.getMainLooper()).post(() ->
                        listener.onCategoriesLoaded(finalCategories, categoryTotals, finalTotalSpend));

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        listener.onError("Failed to load categories: " + e.getMessage()));
            }
        }).start();
    }

    public static void loadItemsForCategoryAllMonths(Context context, String category, OnHistoryDataListener listener) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<HistoryEntry> entries = db.historyEntryDao().getEntriesByCategoryOrdered(category);

                double totalSpend = 0;
                ArrayList<HistoryEntry> itemList = new ArrayList<>();

                for (HistoryEntry entry : entries) {
                    totalSpend += entry.getAmountPaid();
                    itemList.add(entry);
                }

                double finalTotalSpend = totalSpend;

                new Handler(Looper.getMainLooper()).post(() ->
                        listener.onDataLoaded(itemList, finalTotalSpend));

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        listener.onError("Failed to load items: " + e.getMessage()));
            }
        }).start();
    }




    public static void loadCategoriesForMonth(Context context, String monthYearInput,
                                              RoomHelper.OnCategoriesLoadedListener listener) {
        new Thread(() -> {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                Date parsedDate = inputFormat.parse(monthYearInput);
                String dbMonth = dbFormat.format(parsedDate);  // e.g., "2025-08"

                AppDatabase db = AppDatabase.getInstance(context);
                List<HistoryEntry> allEntries = db.historyEntryDao().getAllEntriesForMonth(dbMonth);

                Map<String, Double> categoryTotals = new HashMap<>();
                ArrayList<String> categoryList = new ArrayList<>();
                double[] totalMonthSpend = new double[]{0};

                for (HistoryEntry entry : allEntries) {
                    String category = entry.getCategoryName();
                    double amount = entry.getAmountPaid();

                    totalMonthSpend[0] += amount;

                    categoryTotals.put(category,
                            categoryTotals.getOrDefault(category, 0.0) + amount);

                    if (!categoryList.contains(category)) {
                        categoryList.add(category);
                    }
                }

                Collections.sort(categoryList, (c1, c2) -> {
                    double a = categoryTotals.get(c1);
                    double b = categoryTotals.get(c2);
                    return Double.compare(b, a);
                });

                new Handler(Looper.getMainLooper()).post(() ->
                        listener.onCategoriesLoaded(categoryList, categoryTotals, totalMonthSpend[0]));

            } catch (ParseException e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() ->
                        listener.onError("Invalid month format"));
            }
        }).start();
    }



    public static void handleDateChangeAndUpdateExpense(Context context,
                                                        HistoryEntry oldEntry,
                                                        String newCategory,
                                                        double newAmount,
                                                        String amountStr,
                                                        String newDate,
                                                        String itemName,
                                                        OnBudgetUpdateListener listener) {
        AppDatabase db = AppDatabase.getInstance(context);
        PaymentItemDao dao = db.paymentItemDao();

        new Thread(() -> {
            try {
                String oldCategory = oldEntry.getCategoryName();
                double oldAmount = oldEntry.getAmountPaid();

                if (!oldCategory.equalsIgnoreCase(newCategory)) {
                    adjustBudgetsForCategoryChange(dao, oldCategory, oldAmount, newCategory, newAmount);
                }

                recordHistory(context, newCategory, amountStr, newDate, itemName);
                db.historyEntryDao().deleteById(oldEntry.getId());

                new Handler(Looper.getMainLooper()).post(listener::onSuccess);
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        listener.onError("Update failed: " + e.getMessage()));
            }
        }).start();
    }



    private static void adjustBudgetsForCategoryChange(PaymentItemDao dao,
                                                       String oldCategory,
                                                       double oldAmount,
                                                       String newCategory,
                                                       double newAmount) {
        PaymentItem oldCatItem = dao.getCategoryByName(oldCategory);
        if (oldCatItem != null) {
            double restored = oldCatItem.getBudget_balance() + oldAmount;
            dao.updateBudgetBalance(oldCategory, restored);
        }

        PaymentItem newCatItem = dao.getCategoryByName(newCategory);
        if (newCatItem != null) {
            double deducted = newCatItem.getBudget_balance() - newAmount;
            dao.updateBudgetBalance(newCategory, deducted);
        }
    }








    public static void updateHistoryEntryWithBudgetLogic(Context context,
                                                         HistoryEntry item,
                                                         String newCategory,
                                                         double newAmount,
                                                         String newItemName,
                                                         OnBudgetUpdateListener listener) {

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                HistoryEntryDao historyDao = db.historyEntryDao();
                PaymentItemDao categoryDao = db.paymentItemDao();

                // Get old values
                String oldCategory = item.getCategoryName();
                double oldAmount = item.getAmountPaid();

                item.setCategoryName(newCategory);
                item.setAmountPaid(newAmount);
                item.setItemName(newItemName);
                historyDao.update(item);  // Update history row

                if (!oldCategory.equalsIgnoreCase(newCategory)) {
                    Log.d("ROOMCHECK", "updateHistoryEntryWithBudgetLogic: NOT EQUAL ");
                    PaymentItem oldCatItem = categoryDao.getCategoryByName(oldCategory);
                    if (oldCatItem != null) {
                        double restoredBalance = oldCatItem.getBudget_balance() + oldAmount;
                        categoryDao.updateBudgetBalance(oldCategory, restoredBalance);
                    }

                    // Step 2: Deduct from new category
                    PaymentItem newCatItem = categoryDao.getCategoryByName(newCategory);
                    if (newCatItem != null) {
                        double newBalance = newCatItem.getBudget_balance() - newAmount;
                        categoryDao.updateBudgetBalance(newCategory, newBalance);
                    }

                } else {
                    // Same category, adjust budget
                    Log.d("ROOMCHECK", "updateHistoryEntryWithBudgetLogic: EQUAL ");
                    PaymentItem itemToAdjust = categoryDao.getCategoryByName(newCategory);
                    if (itemToAdjust != null) {
                        double adjustedBalance = itemToAdjust.getBudget_balance() + oldAmount - newAmount;
                        categoryDao.updateBudgetBalance(newCategory, adjustedBalance);
                    }
                }

                // Notify success on main thread
                new Handler(Looper.getMainLooper()).post(listener::onSuccess);

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        listener.onError("Update failed: " + e.getMessage()));
            }
        });
    }


    public static void deleteHistoryEntry(Context context, int entryId, String categoryName, double amountPaid, boolean skipBudgetUpdate, OnDeleteListener listener) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);

                // Delete the history entry
                db.historyEntryDao().deleteById(entryId);

                // Conditionally update budget
                if (!skipBudgetUpdate) {
                    PaymentItem item = db.paymentItemDao().getCategoryByName(categoryName);
                    if (item != null) {
                        double currentBalance = item.getBudget_balance();
                        double newBalance = currentBalance + amountPaid;
                        db.paymentItemDao().updateBudgetBalance(categoryName, newBalance);
                    }
                }

                new Handler(Looper.getMainLooper()).post(listener::onSuccess);

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        listener.onError("Error deleting entry: " + e.getMessage()));
            }
        }).start();
    }







    public static void loadItemsForDay(Context context, String monthYear, String day, OnHistoryDataListener listener) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<HistoryEntry> entries = db.historyEntryDao().getAllEntries();

                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());

                ArrayList<HistoryEntry> dayItems = new ArrayList<>();
                double totalSpend = 0;

                for (HistoryEntry entry : entries) {
                    try {
                        Date date = inputFormat.parse(entry.getDateTime());
                        if (date != null &&
                                monthYearFormat.format(date).equals(monthYear) &&
                                dayFormat.format(date).equals(day)) {
                            dayItems.add(entry);
                            totalSpend += entry.getAmountPaid();
                        }
                    } catch (ParseException ignored) {}
                }

                double finalTotalSpend = totalSpend;
                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onDataLoaded(dayItems, finalTotalSpend);
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onError("Failed to load items: " + e.getMessage());
                });
            }
        }).start();
    }



    public static void loadDaysForMonth(Context context, String monthYear, OnDaysLoadedListener listener) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<HistoryEntry> entries = db.historyEntryDao().getAllEntries();

                Set<String> daySet = new HashSet<>();
                double totalMonthSpend = 0.0;

                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());

                for (HistoryEntry entry : entries) {
                    try {
                        Date date = inputFormat.parse(entry.getDateTime());
                        if (date != null) {
                            String entryMonthYear = monthYearFormat.format(date);
                            if (entryMonthYear.equals(monthYear)) {
                                String day = dayFormat.format(date);
                                daySet.add(day);
                                totalMonthSpend += entry.getAmountPaid();
                            }
                        }
                    } catch (ParseException ignored) {}
                }

                List<String> dayList = new ArrayList<>(daySet);

                // Sort descending: 31 → 1
                Collections.sort(dayList, (d1, d2) -> {
                    try {
                        return Integer.parseInt(d2) - Integer.parseInt(d1);
                    } catch (NumberFormatException e) {
                        return d2.compareTo(d1);
                    }
                });

                double finalTotalMonthSpend = totalMonthSpend;
                ArrayList<String> finalDayList = new ArrayList<>(dayList);

                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onDaysLoaded(finalDayList, finalTotalMonthSpend);
                });


            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onError("Failed to load days: " + e.getMessage());
                });
            }
        }).start();
    }



    public static void loadCategoryNames(Context context, OnCategoryNamesListener listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<String> categoryList = db.paymentItemDao().getAllCategoryNamesRaw();

                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onCategoriesLoaded(categoryList);
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onError("Failed to load categories: " + e.getMessage());
                });
            }
        });
    }




    public static void loadMonthsFromRoom(Context context, OnMonthsLoadedListener listener) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<HistoryEntry> entries = db.historyEntryDao().getAllEntries();

                Set<String> monthsSet = new HashSet<>();
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

                for (HistoryEntry entry : entries) {
                    try {
                        Date date = inputFormat.parse(entry.getDateTime());
                        if (date != null) {
                            String monthYear = monthYearFormat.format(date);
                            monthsSet.add(monthYear);
                        }
                    } catch (ParseException ignored) {}
                }

                List<String> monthsList = new ArrayList<>(monthsSet);

                // Sort months in descending order (latest first)
                Collections.sort(monthsList, (m1, m2) -> {
                    try {
                        Date d1 = monthYearFormat.parse(m1);
                        Date d2 = monthYearFormat.parse(m2);
                        return d2.compareTo(d1); // descending
                    } catch (ParseException e) {
                        return m2.compareTo(m1);
                    }
                });

                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onMonthsLoaded(new ArrayList<>(monthsList));
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onError("Failed to load months: " + e.getMessage());
                });
            }
        }).start();
    }
}
