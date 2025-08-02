package com.astin.moneymaster.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "payment_items")
public class PaymentItem {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private double budget;
    private double budget_balance;

    public PaymentItem() {}

    public PaymentItem(String name, double budget) {
        this.name = name;
        this.budget = budget;
        this.budget_balance = budget;
    }

    // ID Getter & Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    // Budget Getters & Setters
    public double getBudget_balance() { return budget_balance; }
    public void setBudget_balance(double budget_balance) { this.budget_balance = budget_balance; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }
}
