package com.astin.moneymaster.model;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                PaymentItem.class,
                HistoryEntry.class,
                MonthBalance.class
        },
        version = 3
)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract PaymentItemDao paymentItemDao();
    public abstract HistoryEntryDao historyEntryDao();
    public abstract MonthBalanceDao monthBalanceDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "money_master_db"
                    ).fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public static synchronized void closeDatabase() {
        if (instance != null && instance.isOpen()) {
            instance.close();
            instance = null;
        }
    }
}

