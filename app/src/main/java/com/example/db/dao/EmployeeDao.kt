package com.example.db.dao

import androidx.room.*
import com.example.db.entities.EmployeeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE code = :code LIMIT 1")
    suspend fun getEmployeeByCode(code: String): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE isMe = 1 LIMIT 1")
    fun getMyProfileFlow(): Flow<EmployeeEntity?>

    @Query("SELECT * FROM employees WHERE isMe = 1 LIMIT 1")
    suspend fun getMyProfile(): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<EmployeeEntity>)

    @Query("UPDATE employees SET isMe = 0")
    suspend fun clearMeFlag()

    @Query("UPDATE employees SET isMe = 1 WHERE code = :code")
    suspend fun setMeFlag(code: String)

    @Query("DELETE FROM employees")
    suspend fun deleteAll()
}
