package com.astin.moneymaster.model;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {PaymentItem.class, HistoryEntry.class}, version = 2)
public abstract class TempDatabase extends RoomDatabase {

    public abstract PaymentItemDao paymentItemDao();
    public abstract HistoryEntryDao historyEntryDao();

}
