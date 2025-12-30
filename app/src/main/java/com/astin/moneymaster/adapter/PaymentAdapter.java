package com.astin.moneymaster.adapter;

import static com.astin.moneymaster.MainActivity.PREFS_NAME;
import static com.astin.moneymaster.MainActivity.SELECTED_APP_KEY;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.astin.moneymaster.R;
import com.astin.moneymaster.helper.RoomHelper;
import com.astin.moneymaster.model.AppDatabase;
import com.astin.moneymaster.model.MonthBalance;
import com.astin.moneymaster.model.PaymentItem;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.ViewHolder> {

    private List<PaymentItem> cat_item_list;
    private final Context context;
    private final OnDataChangedListener dataChangedListener;
    private final OnNavigateListener navigateListener;
    private SharedPreferences sharedPreferences;
    private PackageManager packageManager;

    public interface OnDataChangedListener {
        void onDataChanged();
    }



    public interface OnNavigateListener {
        void onNavigateToHistory();
    }

    public PaymentAdapter(Context context, List<PaymentItem> cat_item_list, OnDataChangedListener dataChangedListener, OnNavigateListener navigateListener) {
        this.context = context;
        this.cat_item_list = cat_item_list;
        this.dataChangedListener = dataChangedListener;
        this.navigateListener = navigateListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemNameText, balanceText;
        EditText amounttxt;
        MaterialButton payButton;

        public ViewHolder(View itemView) {
            super(itemView);
            itemNameText = itemView.findViewById(R.id.itemNameText);
            balanceText = itemView.findViewById(R.id.balanceText);
            amounttxt = itemView.findViewById(R.id.amounttxt);
            payButton = itemView.findViewById(R.id.payButton);
        }
    }

    @NonNull
    @Override
    public PaymentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_payment_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentAdapter.ViewHolder holder, int position) {
        PaymentItem item = cat_item_list.get(position);
        holder.itemNameText.setText(item.getName());
        holder.balanceText.setText("(" + item.getBudget_balance() + " / " + item.getBudget() + ")");

        if (item.getBudget_balance() < 0) {
            holder.balanceText.setTextColor(context.getResources().getColor(R.color.holo_red_dark));
        } else {
            holder.balanceText.setTextColor(context.getResources().getColor(R.color.gray_text));
        }


        View.OnClickListener payAction = v -> {
            String amountStr = holder.amounttxt.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                try {
                    double amount = Double.parseDouble(amountStr);
                    new Thread(() -> {
                        double currentBalance = item.getBudget_balance();
                        double newBalance = currentBalance - amount;
                        item.setBudget_balance(newBalance);

                        AppDatabase db = AppDatabase.getInstance(context);
                        db.paymentItemDao().updateBudgetBalance(item.getId(), item.getBudget_balance());
                        RoomHelper.recordHistory(context, item, amount);

                        ((Activity) context).runOnUiThread(() -> {
                            holder.amounttxt.setText("");
                            notifyItemChanged(position);
                            Toast.makeText(context, "Paid " + amount + " for " + item.getName(), Toast.LENGTH_SHORT).show();
                            launchSelectedApp();
                            dataChangedListener.onDataChanged();
                        });
                    }).start();
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Invalid amount format", Toast.LENGTH_SHORT).show();
                }
            } else {
                launchSelectedApp();
            }
        };

        View.OnLongClickListener payActionWithNoAPPOpen = v -> {
            String amountStr = holder.amounttxt.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                try {
                    double amount = Double.parseDouble(amountStr);
                    new Thread(() -> {
                        double currentBalance = item.getBudget_balance();
                        double newBalance = currentBalance - amount;
                        item.setBudget_balance(newBalance);

                        AppDatabase db = AppDatabase.getInstance(context);
                        db.paymentItemDao().updateBudgetBalance(item.getId(), item.getBudget_balance());

                        Double currentMonthBalance = db.monthBalanceDao().getBalanceOnce();
                        if (currentMonthBalance == null) {
                            currentMonthBalance = 0.0;
                        }
                        double newMonthBalance = currentMonthBalance - amount;
                        db.monthBalanceDao().insertOrUpdate(
                                new MonthBalance(newMonthBalance)
                        );


                        RoomHelper.recordHistory(context, item, amount);

                        ((Activity) context).runOnUiThread(() -> {
                            holder.amounttxt.setText("");
                            notifyItemChanged(position);
                            moveToHistoryFragment();
                            Toast.makeText(context, "Paid " + amount + " for " + item.getName(), Toast.LENGTH_SHORT).show();
                            dataChangedListener.onDataChanged();
                        });
                    }).start();
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Invalid amount format", Toast.LENGTH_SHORT).show();
                }
            } else {
                moveToHistoryFragment();
            }

            return true;
        };



        holder.payButton.setOnClickListener(payAction);
        holder.payButton.setOnLongClickListener(payActionWithNoAPPOpen);





        holder.amounttxt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {
                holder.payButton.performClick();
                return true;
            }
            return false;
        });

        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Manage Item")
                    .setMessage("What do you want to do with \"" + item.getName() + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        new Thread(() -> {
                            AppDatabase db = AppDatabase.getInstance(context);
                            db.paymentItemDao().deleteById(item.getId());

                            ((Activity) context).runOnUiThread(() -> {
                                Toast.makeText(context, "Deleted " + item.getName(), Toast.LENGTH_SHORT).show();
                                int currentPosition = cat_item_list.indexOf(item);
                                if (currentPosition != -1) {
                                    cat_item_list.remove(currentPosition);
                                    notifyItemRemoved(currentPosition);
                                    notifyItemRangeChanged(currentPosition, cat_item_list.size());
                                    dataChangedListener.onDataChanged();
                                }
                            });
                        }).start();
                    })
                    .setNeutralButton("Reset", (dialog, which) -> {
                        // Show confirmation dialog for Reset
                        new AlertDialog.Builder(context)
                                .setTitle("Reset Balance")
                                .setMessage("Do you really want to reset balance for \"" + item.getName() + "\"?")
                                .setPositiveButton("Yes", (confirmDialog, confirmWhich) -> {
                                    new Thread(() -> {
                                        AppDatabase db = AppDatabase.getInstance(context);
                                        db.paymentItemDao().resetBudgetBalanceById(item.getId());

                                        ((Activity) context).runOnUiThread(() -> {
                                            Toast.makeText(context, "Reset balance for " + item.getName(), Toast.LENGTH_SHORT).show();

                                            int currentPosition = cat_item_list.indexOf(item);
                                            if (currentPosition != -1) {
                                                double itemBudget = item.getBudget();
                                                item.setBudget_balance(itemBudget);
                                                notifyItemChanged(currentPosition);
                                                dataChangedListener.onDataChanged();
                                            }
                                        });
                                    }).start();
                                })
                                .setNegativeButton("No", null)
                                .show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });



    }

    private void moveToHistoryFragment() {
        if (navigateListener != null) {
            navigateListener.onNavigateToHistory();
        }
    }

    private void launchSelectedApp() {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        packageManager = context.getPackageManager();
        String selectedPackage = sharedPreferences.getString(SELECTED_APP_KEY, "");

        if (selectedPackage.isEmpty()) {
            Toast.makeText(context, "Please select an app from Menu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedPackage.equals("NONE")) {
            Toast.makeText(context, "No app selected. Skipping launch.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent launchIntent = packageManager.getLaunchIntentForPackage(selectedPackage);
            if (launchIntent != null) {
                context.startActivity(launchIntent);
            } else {
                Toast.makeText(context, "Cannot launch the selected app. It may have been uninstalled.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Error launching app: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return cat_item_list.size();
    }

    public void setPaymentItems(List<PaymentItem> newItems) {
        this.cat_item_list = newItems;
        notifyDataSetChanged();
    }
}
