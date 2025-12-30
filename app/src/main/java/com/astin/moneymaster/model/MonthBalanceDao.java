package com.astin.moneymaster.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface MonthBalanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(MonthBalance balance);

    @Query("SELECT balance FROM month_balance WHERE id = 1")
    LiveData<Double> getBalanceLive();

    @Query("SELECT balance FROM month_balance WHERE id = 1")
    Double getBalanceOnce();

    @Query("UPDATE month_balance SET balance = balance - :amount WHERE id = 1")
    void reduceBalance(double amount);

    @Query("UPDATE month_balance SET balance = :amount WHERE id = 1")
    void setBalance(double amount);
}
