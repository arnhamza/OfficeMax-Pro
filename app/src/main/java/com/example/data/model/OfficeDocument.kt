package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "office_documents")
data class OfficeDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val type: DocType,
    val content: String,
    val metadataJson: String = "",
    val filePath: String? = null,
    val fileSizeBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val categoryTag: String = "General"
)
