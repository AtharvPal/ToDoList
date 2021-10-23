package com.example.todolist2

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [CategoryModel::class], version = 1)
abstract class CategoryDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao


    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: CategoryDatabase? = null

        fun getDatabase(context: Context): CategoryDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CategoryDatabase::class.java,
                    "category.db"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}