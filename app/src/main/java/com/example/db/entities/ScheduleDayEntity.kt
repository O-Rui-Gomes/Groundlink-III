package com.example.db.entities

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "schedule_days",
    indices = [
        Index(value = ["employeeCode", "year", "month", "day"], unique = true)
    ],
    primaryKeys = ["employeeCode", "year", "month", "day"]
)
data class ScheduleDayEntity(
    val employeeCode: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val originalShift: String,
    val currentShift: String,
    val isModified: Boolean = false,
    val alterationType: String? = null, // "Supervisor Request", "Trade", "Manual"
    val alterationNote: String? = null,
    val tradeWithEmployeeCode: String? = null,
    val isOvertime: Boolean = false,
    val overtimeHours: Double = 0.0,
    val isHoliday: Boolean = false,
    val manualOvertimeHours: Double? = null,
    val manualNightHours: Double? = null,
    val manualHoliday: Boolean? = null,
    val tradeAccountability: String? = null, // "COWORKER_OWES_ME", "I_OWE_COWORKER", "COWORKER_PAID_ME", "I_PAID_COWORKER", "BALANCED"
    val tradeOwedHours: Double = 0.0
)
