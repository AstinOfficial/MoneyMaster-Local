package com.astin.moneymaster.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "month_balance")
public class MonthBalance {

    @PrimaryKey
    public int id = 1; // SINGLE ROW ONLY

    public double balance;

    public MonthBalance(double balance) {
        this.balance = balance;
    }
}
