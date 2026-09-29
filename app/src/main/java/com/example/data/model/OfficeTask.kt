package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "office_tasks")
data class OfficeTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    val dueDate: Long = System.currentTimeMillis() + 86400000L * 2,
    val linkedDocId: Long? = null,
    val linkedDocTitle: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
