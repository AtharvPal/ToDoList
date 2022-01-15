package com.example.todolist2

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CategoryDao {

    @Insert()
    suspend fun insertCategory(category:CategoryModel)

    @Query("Select * from CategoryModel")
    fun getCategories():LiveData<List<CategoryModel>>

    @Query("Select category from CategoryModel")
    fun getCategories2():List<String>

    @Query("Delete from CategoryModel where category=:category")
    fun deleteCategory(category: String)

}