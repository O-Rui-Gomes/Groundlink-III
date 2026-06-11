package com.example.db

import com.example.db.dao.EmployeeDao
import com.example.db.dao.ScheduleDayDao
import com.example.db.dao.MonthYear
import com.example.db.entities.EmployeeEntity
import com.example.db.entities.ScheduleDayEntity
import kotlinx.coroutines.flow.Flow

class RosterRepository(
    private val employeeDao: EmployeeDao,
    private val scheduleDayDao: ScheduleDayDao
) {
    val allEmployees: Flow<List<EmployeeEntity>> = employeeDao.getAllEmployees()
    val myProfile: Flow<EmployeeEntity?> = employeeDao.getMyProfileFlow()
    val availableMonths: Flow<List<MonthYear>> = scheduleDayDao.getAvailableMonths()

    suspend fun getMyProfileDirect(): EmployeeEntity? = employeeDao.getMyProfile()

    suspend fun setMe(code: String) {
        employeeDao.clearMeFlag()
        employeeDao.setMeFlag(code)
    }

    suspend fun getEmployeeByCode(code: String): EmployeeEntity? {
        return employeeDao.getEmployeeByCode(code)
    }

    fun getSchedulesForMonth(year: Int, month: Int): Flow<List<ScheduleDayEntity>> {
        return scheduleDayDao.getSchedulesForMonth(year, month)
    }

    fun getSchedulesForEmployee(employeeCode: String, year: Int, month: Int): Flow<List<ScheduleDayEntity>> {
        return scheduleDayDao.getSchedulesForEmployee(employeeCode, year, month)
    }

    fun getAllSchedulesForEmployee(employeeCode: String): Flow<List<ScheduleDayEntity>> {
        return scheduleDayDao.getAllSchedulesForEmployee(employeeCode)
    }

    suspend fun getScheduleForDay(employeeCode: String, year: Int, month: Int, day: Int): ScheduleDayEntity? {
        return scheduleDayDao.getScheduleForDay(employeeCode, year, month, day)
    }

    suspend fun saveParsedRoster(
        employees: List<EmployeeEntity>,
        schedules: List<ScheduleDayEntity>,
        year: Int,
        month: Int
    ) {
        val existingSchedulesMap = scheduleDayDao.getSchedulesForMonthDirect(year, month)
            .associateBy { "${it.employeeCode}_${it.day}" }

        val mergedSchedules = schedules.map { newDay ->
            val key = "${newDay.employeeCode}_${newDay.day}"
            val existing = existingSchedulesMap[key]
            if (existing != null && (existing.isModified || existing.manualOvertimeHours != null || existing.manualNightHours != null || existing.manualHoliday != null || existing.tradeAccountability != null)) {
                // Intelligent merge: keep the user's manual modifications, trades, overrides and notes,
                // but update originalShift so they can audit behind-the-scenes schedule updates.
                existing.copy(originalShift = newDay.originalShift)
            } else {
                newDay
            }
        }

        // We clear existing schedules to prevent orphans, then write the merged list
        scheduleDayDao.deleteSchedulesForMonth(year, month)
        
        // Insert/update employee listings
        employeeDao.insertEmployees(employees)
        
        // Add parsed shift cell mappings
        scheduleDayDao.insertSchedules(mergedSchedules)
    }

    suspend fun updateScheduleDay(schedule: ScheduleDayEntity) {
        scheduleDayDao.updateSchedule(schedule)
    }

    suspend fun insertOrUpdateScheduleDay(schedule: ScheduleDayEntity) {
        scheduleDayDao.insertSchedules(listOf(schedule))
    }

    suspend fun deleteAllData() {
        employeeDao.deleteAll()
        scheduleDayDao.deleteAll()
    }
}
