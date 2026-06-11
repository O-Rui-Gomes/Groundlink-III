package com.example.db.dao

import androidx.room.*
import com.example.db.entities.ScheduleDayEntity
import kotlinx.coroutines.flow.Flow

data class MonthYear(
    val year: Int,
    val month: Int
)

@Dao
interface ScheduleDayDao {
    @Query("SELECT * FROM schedule_days WHERE year = :year AND month = :month ORDER BY day ASC")
    fun getSchedulesForMonth(year: Int, month: Int): Flow<List<ScheduleDayEntity>>

    @Query("SELECT * FROM schedule_days WHERE employeeCode = :employeeCode AND year = :year AND month = :month ORDER BY day ASC")
    fun getSchedulesForEmployee(employeeCode: String, year: Int, month: Int): Flow<List<ScheduleDayEntity>>

    @Query("SELECT * FROM schedule_days WHERE employeeCode = :employeeCode ORDER BY year ASC, month ASC, day ASC")
    fun getAllSchedulesForEmployee(employeeCode: String): Flow<List<ScheduleDayEntity>>

    @Query("SELECT * FROM schedule_days WHERE employeeCode = :employeeCode AND year = :year AND month = :month AND day = :day LIMIT 1")
    suspend fun getScheduleForDay(employeeCode: String, year: Int, month: Int, day: Int): ScheduleDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ScheduleDayEntity>)

    @Update
    suspend fun updateSchedule(schedule: ScheduleDayEntity)

    @Query("DELETE FROM schedule_days WHERE year = :year AND month = :month")
    suspend fun deleteSchedulesForMonth(year: Int, month: Int)

    @Query("SELECT * FROM schedule_days WHERE year = :year AND month = :month")
    suspend fun getSchedulesForMonthDirect(year: Int, month: Int): List<ScheduleDayEntity>

    @Query("SELECT DISTINCT year, month FROM schedule_days ORDER BY year DESC, month DESC")
    fun getAvailableMonths(): Flow<List<MonthYear>>

    @Query("DELETE FROM schedule_days")
    suspend fun deleteAll()
}
