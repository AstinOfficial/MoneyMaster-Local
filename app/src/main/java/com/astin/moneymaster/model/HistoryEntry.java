package com.astin.moneymaster.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history_entries")
public class HistoryEntry {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String categoryName;
    private double amountPaid;
    private String dateTime;
    private String itemName;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
}
