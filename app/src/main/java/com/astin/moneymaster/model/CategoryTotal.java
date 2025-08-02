package com.astin.moneymaster.model;

import androidx.room.ColumnInfo;

public class CategoryTotal {
    @ColumnInfo(name = "categoryName")
    public String categoryName;

    @ColumnInfo(name = "total")
    public double total;

    public CategoryTotal(String categoryName, double total) {
        this.categoryName = categoryName;
        this.total = total;
    }
}
