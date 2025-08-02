package com.astin.moneymaster.adapter;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.astin.moneymaster.R;
import com.astin.moneymaster.helper.RoomHelper;
import com.astin.moneymaster.model.AppDatabase;
import com.astin.moneymaster.model.PaymentItem;

import java.util.List;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.ViewHolder> {

    private List<PaymentItem> cat_item_list;
    private final Context context;
    private final OnDataChangedListener dataChangedListener;

    RoomHelper roomHelper =new RoomHelper();;

    public interface OnDataChangedListener {
        void onDataChanged(); // Triggered when DB is changed
    }

    public PaymentAdapter(Context context, List<PaymentItem> cat_item_list, OnDataChangedListener dataChangedListener) {
        this.context = context;
        this.cat_item_list = cat_item_list;
        this.dataChangedListener = dataChangedListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemText;
        Button payButton;
        EditText amounttxt;

        public ViewHolder(View itemView) {
            super(itemView);
            itemText = itemView.findViewById(R.id.itemText);
            payButton = itemView.findViewById(R.id.payButton);
            amounttxt = itemView.findViewById(R.id.amounttxt);
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
        String displayText = item.getName() + "\n (" + item.getBudget_balance() + " / " + item.getBudget() + ")";
        holder.itemText.setText(displayText);

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
                        RoomHelper.recordHistory(context,item, amount);

                        ((Activity) context).runOnUiThread(() -> {
                            holder.amounttxt.setText("");
                            notifyItemChanged(position);
                            Toast.makeText(context, "Paid " + amount + " for " + item.getName(), Toast.LENGTH_SHORT).show();
                            launchGooglePay();
                            dataChangedListener.onDataChanged();  // Notify UI to refresh data
                        });
                    }).start();

                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Invalid amount format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Please enter an amount.. Opening Gpay", Toast.LENGTH_SHORT).show();
                launchGooglePay();
            }
        };

        holder.payButton.setOnClickListener(payAction);

        holder.amounttxt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {
                holder.payButton.performClick();
                return true;
            }
            return false;
        });

        holder.itemView.setOnLongClickListener(v -> {
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Delete Item")
                    .setMessage("Are you sure you want to delete \"" + item.getName() + "\"?")
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
                                    dataChangedListener.onDataChanged(); // Update UI
                                }
                            });
                        }).start();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    private void launchGooglePay() {
        Intent launchIntent = new Intent();
        launchIntent.setClassName("com.google.android.apps.nbu.paisa.user",
                "com.google.nbu.paisa.flutter.gpay.app.LauncherActivity");
        try {
            context.startActivity(launchIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "Google Pay app not found", Toast.LENGTH_SHORT).show();
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
