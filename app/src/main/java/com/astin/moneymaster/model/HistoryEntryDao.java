package com.astin.moneymaster.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface HistoryEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HistoryEntry entry);

    @Query("SELECT * FROM history_entries ORDER BY dateTime DESC")
    List<HistoryEntry> getAllEntries();

    @Query("DELETE FROM history_entries")
    void deleteAll();

    @Delete
    void deleteEntry(HistoryEntry entry);

    @Query("DELETE FROM history_entries WHERE id = :entryId")
    void deleteById(int entryId);

    @Query("SELECT * FROM history_entries WHERE dateTime LIKE :month || '%'")
    List<HistoryEntry> getAllEntriesForMonth(String month);  // month = "2025-08"

    @Query("SELECT * FROM history_entries WHERE dateTime LIKE :month || '%' AND categoryName = :category ORDER BY dateTime DESC")
    List<HistoryEntry> getEntriesByMonthAndCategory(String month, String category);


    @Query("SELECT categoryName, SUM(amountPaid) as total FROM history_entries WHERE dateTime LIKE :month || '%' GROUP BY categoryName")
    List<CategoryTotal> getCategoryTotalsForMonth(String month);


    @Update
    void update(HistoryEntry entry);







}

