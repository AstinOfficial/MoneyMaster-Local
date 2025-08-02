package com.astin.moneymaster.model;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {PaymentItem.class, HistoryEntry.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract PaymentItemDao paymentItemDao();
    public abstract HistoryEntryDao historyEntryDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "money_master_db"
            ).fallbackToDestructiveMigration().build();
        }
        return instance;
    }
}
