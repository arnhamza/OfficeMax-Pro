package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DocType
import com.example.data.model.OfficeDocument
import com.example.data.model.OfficeTask
import kotlinx.coroutines.flow.Flow

@Dao
interface OfficeDao {

    @Query("SELECT * FROM office_documents ORDER BY lastModified DESC")
    fun getAllDocuments(): Flow<List<OfficeDocument>>

    @Query("SELECT * FROM office_documents WHERE id = :id")
    fun getDocumentById(id: Long): Flow<OfficeDocument?>

    @Query("SELECT * FROM office_documents WHERE id = :id")
    suspend fun getDocumentByIdOnce(id: Long): OfficeDocument?

    @Query("SELECT * FROM office_documents WHERE type = :type ORDER BY lastModified DESC")
    fun getDocumentsByType(type: DocType): Flow<List<OfficeDocument>>

    @Query("SELECT * FROM office_documents WHERE isFavorite = 1 ORDER BY lastModified DESC")
    fun getFavoriteDocuments(): Flow<List<OfficeDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: OfficeDocument): Long

    @Update
    suspend fun updateDocument(doc: OfficeDocument)

    @Delete
    suspend fun deleteDocument(doc: OfficeDocument)

    @Query("DELETE FROM office_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Query("SELECT COUNT(*) FROM office_documents")
    fun getDocumentCount(): Flow<Int>

    // Tasks
    @Query("SELECT * FROM office_tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<OfficeTask>>

    @Query("SELECT * FROM office_tasks WHERE linkedDocId = :docId")
    fun getTasksForDocument(docId: Long): Flow<List<OfficeTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: OfficeTask): Long

    @Update
    suspend fun updateTask(task: OfficeTask)

    @Delete
    suspend fun deleteTask(task: OfficeTask)

    @Query("DELETE FROM office_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}
