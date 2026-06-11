package com.example.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.db.entities.EmployeeEntity
import com.example.db.entities.ScheduleDayEntity
import java.util.Calendar

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun scheduleAlarmsForMonth(
        context: Context,
        schedules: List<ScheduleDayEntity>,
        alertEnabled: Boolean,
        alertMinutes: Int,
        coworkers: List<EmployeeEntity> = emptyList()
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // We cancel existing alarms for days 1 to 31 to clean up and avoid duplicate alerts
        for (day in 1..31) {
            val intent = Intent(context, ShiftAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                day,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_NO_CREATE
                }
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        if (!alertEnabled) {
            Log.d(TAG, "Shift Alerts disabled. Cleared all shift alarms.")
            return
        }

        val today = Calendar.getInstance()

        schedules.forEach { dayEntity ->
            val shift = dayEntity.currentShift.trim()
            if (shift.isNotEmpty() && !shift.equals("OFF", ignoreCase = true)) {
                val times = ShiftTimeCalculator.parseShiftTimes(shift)
                if (times != null) {
                    val startStr = times.first // E.g., "08:30"
                    val parts = startStr.split(":")
                    if (parts.size >= 2) {
                        val hour = parts[0].toIntOrNull()
                        val min = parts[1].toIntOrNull()
                        if (hour != null && min != null) {
                            val alertCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, dayEntity.year)
                                set(Calendar.MONTH, dayEntity.month - 1)
                                set(Calendar.DAY_OF_MONTH, dayEntity.day)
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, min)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                                add(Calendar.MINUTE, -alertMinutes) // Subtract alarm-lead minutes
                            }

                            // Only schedule if alarm time is in the future
                            if (alertCal.after(today)) {
                                val intent = Intent(context, ShiftAlarmReceiver::class.java).apply {
                                    putExtra("shift_title", dayEntity.currentShift)
                                    putExtra("shift_time", startStr)
                                    putExtra("day", dayEntity.day)
                                    if (dayEntity.isModified && dayEntity.alterationType == "Trade" && !dayEntity.tradeWithEmployeeCode.isNullOrEmpty()) {
                                        val coworker = coworkers.find { it.code == dayEntity.tradeWithEmployeeCode }
                                        putExtra("coworker_name", coworker?.name ?: "Employee #${dayEntity.tradeWithEmployeeCode}")
                                    }
                                }

                                val pendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    dayEntity.day,
                                    intent,
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                    } else {
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                    }
                                )

                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        if (alarmManager.canScheduleExactAlarms()) {
                                            alarmManager.setExactAndAllowWhileIdle(
                                                AlarmManager.RTC_WAKEUP,
                                                alertCal.timeInMillis,
                                                pendingIntent
                                            )
                                        } else {
                                            alarmManager.setAndAllowWhileIdle(
                                                AlarmManager.RTC_WAKEUP,
                                                alertCal.timeInMillis,
                                                pendingIntent
                                            )
                                        }
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        alarmManager.setAndAllowWhileIdle(
                                            AlarmManager.RTC_WAKEUP,
                                            alertCal.timeInMillis,
                                            pendingIntent
                                        )
                                    } else {
                                        alarmManager.set(
                                            AlarmManager.RTC_WAKEUP,
                                            alertCal.timeInMillis,
                                            pendingIntent
                                        )
                                    }
                                    Log.d(TAG, "Scheduled shift alarm for Day ${dayEntity.day} at ${alertCal.time}")
                                } catch (e: SecurityException) {
                                    // Fallback to standard alarm
                                    alarmManager.set(
                                        AlarmManager.RTC_WAKEUP,
                                        alertCal.timeInMillis,
                                        pendingIntent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
