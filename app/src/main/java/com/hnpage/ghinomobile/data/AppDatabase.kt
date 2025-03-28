package com.hnpage.ghinomobile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Transaction::class, Payment::class], version = 3) // Tăng version lên 3
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "debt_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Thêm migration mới
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE transactions_new (id TEXT PRIMARY KEY NOT NULL, contactName TEXT NOT NULL, phoneNumber TEXT NOT NULL, amount REAL NOT NULL, type TEXT NOT NULL, date INTEGER NOT NULL, note TEXT NOT NULL, isReminderSet INTEGER NOT NULL)")
                database.execSQL("INSERT INTO transactions_new (id, contactName, phoneNumber, amount, type, date, note, isReminderSet) SELECT id, contactName, phoneNumber, amount, type, date, note, isReminderSet FROM transactions")
                database.execSQL("DROP TABLE transactions")
                database.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
                database.execSQL("""
                    CREATE TABLE payments (
                        id TEXT PRIMARY KEY NOT NULL,
                        transactionId TEXT NOT NULL,
                        amount REAL NOT NULL,
                        date INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        FOREIGN KEY (transactionId) REFERENCES transactions(id) ON DELETE CASCADE
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Thêm chỉ mục cho transactionId
                database.execSQL("CREATE INDEX index_payments_transactionId ON payments(transactionId)")
            }
        }
    }
}