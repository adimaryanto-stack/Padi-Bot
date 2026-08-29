package com.example.padibot.data.local

import androidx.room.*
import com.example.padibot.model.Field
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldDao {
    @Query("SELECT * FROM fields ORDER BY createdAt DESC")
    fun getAllFields(): Flow<List<Field>>

    @Query("SELECT * FROM fields WHERE id = :fieldId LIMIT 1")
    suspend fun getFieldById(fieldId: String): Field?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertField(field: Field)

    @Delete
    suspend fun deleteField(field: Field)

    @Query("DELETE FROM fields WHERE id = :fieldId")
    suspend fun deleteFieldById(fieldId: String)

    @Query("DELETE FROM fields")
    suspend fun deleteAll()
}
