package com.astin.moneymaster.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PaymentItemDao {

    @Insert
    void insert(PaymentItem item);

    @Query("SELECT * FROM payment_items")
    List<PaymentItem> getAllItems();

    @Query("SELECT * FROM payment_items")
    List<PaymentItem> getAllItemsOnce();


    @Query("DELETE FROM payment_items")
    void deleteAll();

    @Query("UPDATE payment_items SET budget_balance = :newBalance WHERE id = :id")
    void updateBudgetBalance(int id, Double newBalance);

    @Query("DELETE FROM payment_items WHERE id = :id")
    void deleteById(int id);

    @Query("UPDATE payment_items SET budget_balance = budget")
    void resetAllBudgetBalances();

    @Query("UPDATE payment_items SET budget_balance = budget WHERE id = :id")
    void resetBudgetBalanceById(int id);

    @Query("UPDATE payment_items SET budget_balance = :newBalance WHERE name = :categoryName")
    void updateBudgetBalance(String categoryName, double newBalance);

    @Query("SELECT * FROM payment_items WHERE name = :categoryName LIMIT 1")
    PaymentItem getCategoryByName(String categoryName);

    @Query("SELECT name FROM payment_items")
    List<String> getAllCategoryNamesRaw();


}

