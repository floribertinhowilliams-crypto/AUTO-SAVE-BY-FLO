package com.floribert.autosaveflopro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DetectedContactEntity::class, VoucherEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
}
