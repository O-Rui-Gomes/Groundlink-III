package com.example.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val code: String,
    val name: String,
    val title: String,
    val sequence: String,
    val isMe: Boolean = false
)
