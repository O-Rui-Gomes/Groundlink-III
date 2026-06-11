package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ShiftTimeCalculator {

    fun parseShiftTimes(shiftText: String): Pair<String, String>? {
        val regex = Regex("(\\d{2}:\\d{2})")
        val matches = regex.findAll(shiftText).map { it.value }.toList()
        if (matches.size >= 2) {
            return Pair(matches[0], matches[1])
        }
        return null
    }

    fun calculateHours(startStr: String, endStr: String): Double {
        try {
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            val startTime = sdf.parse(startStr) ?: return 0.0
            val endTime = sdf.parse(endStr) ?: return 0.0

            var diff = endTime.time - startTime.time
            if (diff < 0) {
                // Crosses midnight, e.g. 22:00 to 06:00
                diff += 24 * 60 * 60 * 1000L
            }
            return diff.toDouble() / (1000 * 60 * 60)
        } catch (e: Exception) {
            return 0.0
        }
    }

    fun calculateNightHours(
        startStr: String,
        endStr: String,
        nightStartHour: Int = 22,
        nightEndHour: Int = 7
    ): Double {
        try {
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            val startTime = sdf.parse(startStr) ?: return 0.0
            val endTime = sdf.parse(endStr) ?: return 0.0

            val startCal = Calendar.getInstance().apply {
                time = startTime
                set(Calendar.YEAR, 2026)
                set(Calendar.MONTH, 0)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val endCal = Calendar.getInstance().apply {
                time = endTime
                set(Calendar.YEAR, 2026)
                set(Calendar.MONTH, 0)
                set(Calendar.DAY_OF_MONTH, 1)
                if (endTime.before(startTime)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            var nightMinutes = 0
            val stepMinutes = 5
            val current = startCal.clone() as Calendar

            while (current.before(endCal)) {
                val hour = current.get(Calendar.HOUR_OF_DAY)
                var isNight = false
                if (nightStartHour > nightEndHour) {
                    if (hour >= nightStartHour || hour < nightEndHour) {
                        isNight = true
                    }
                } else {
                    if (hour in nightStartHour until nightEndHour) {
                        isNight = true
                    }
                }

                if (isNight) {
                    nightMinutes += stepMinutes
                }
                current.add(Calendar.MINUTE, stepMinutes)
            }

            return nightMinutes.toDouble() / 60.0
        } catch (e: Exception) {
            return 0.0
        }
    }

    data class ShiftRange(val startMin: Int, val endMin: Int)

    fun getShiftRange(dayIndex: Int, shiftText: String): ShiftRange? {
        val times = parseShiftTimes(shiftText) ?: return null
        val startParts = times.first.split(":")
        val endParts = times.second.split(":")
        if (startParts.size < 2 || endParts.size < 2) return null
        val sH = startParts[0].toIntOrNull() ?: return null
        val sM = startParts[1].toIntOrNull() ?: return null
        val eH = endParts[0].toIntOrNull() ?: return null
        val eM = endParts[1].toIntOrNull() ?: return null
        
        val sMin = sH * 60 + sM
        val eMin = eH * 60 + eM
        
        val dayAnchor = dayIndex * 24 * 60
        val startAbs = dayAnchor + sMin
        val endAbs = dayAnchor + eMin + (if (eMin < sMin) 24 * 60 else 0)
        return ShiftRange(startAbs, endAbs)
    }

    fun calculateRestBetweenShifts(
        prevShiftDayIndex: Int,
        prevShiftText: String,
        currShiftDayIndex: Int,
        currShiftText: String
    ): Double? {
        val prevRange = getShiftRange(prevShiftDayIndex, prevShiftText) ?: return null
        val currRange = getShiftRange(currShiftDayIndex, currShiftText) ?: return null
        
        val restMinutes = currRange.startMin - prevRange.endMin
        return restMinutes / 60.0
    }
}
