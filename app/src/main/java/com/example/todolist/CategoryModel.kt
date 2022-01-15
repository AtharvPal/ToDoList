package com.example.todolist2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CategoryModel(
    @PrimaryKey
    var category: String
)