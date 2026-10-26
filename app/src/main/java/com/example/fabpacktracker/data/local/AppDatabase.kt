package com.example.fabpacktracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// создание database
@Database(entities = [AssetPack::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun assetPackDao() : AssetPackDao
    companion object { // внутри будет втроенный прямо в класс синглтон компаньйон
        @Volatile
        private var INSTANCE : AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "fab_pack_tracker_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
