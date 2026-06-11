package com.example.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.db.entities.ScheduleDayEntity
import java.io.File
import java.io.FileWriter
import java.util.Calendar
import java.util.Locale

object CalendarExporter {
    fun exportToIcs(context: Context, schedules: List<ScheduleDayEntity>, year: Int, month: Int) {
        if (schedules.isEmpty()) {
            Toast.makeText(context, "No schedule data to export!", Toast.LENGTH_SHORT).show()
            return
        }

        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\n")
        sb.append("VERSION:2.0\n")
        sb.append("PRODID:-//Groundlink III//Shift Calendar Exporter//EN\n")
        sb.append("CALSCALE:GREGORIAN\n")

        val today = Calendar.getInstance()
        val dtStamp = String.format(Locale.US, "%04d%02d%02dT%02d%02d%02dZ", 
            today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH),
            today.get(Calendar.HOUR_OF_DAY), today.get(Calendar.MINUTE), today.get(Calendar.SECOND)
        )

        schedules.forEach { dayEntity ->
            val shift = dayEntity.currentShift.trim()
            if (shift.isNotEmpty() && !shift.equals("OFF", ignoreCase = true)) {
                val times = ShiftTimeCalculator.parseShiftTimes(shift)
                if (times != null) {
                    val startStr = times.first
                    val endStr = times.second

                    val startParts = startStr.split(":")
                    val endParts = endStr.split(":")
                    if (startParts.size >= 2 && endParts.size >= 2) {
                        val sH = startParts[0].toIntOrNull() ?: 0
                        val sM = startParts[1].toIntOrNull() ?: 0
                        val eH = endParts[0].toIntOrNull() ?: 0
                        val eM = endParts[1].toIntOrNull() ?: 0

                        val startCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, dayEntity.year)
                            set(Calendar.MONTH, dayEntity.month - 1)
                            set(Calendar.DAY_OF_MONTH, dayEntity.day)
                            set(Calendar.HOUR_OF_DAY, sH)
                            set(Calendar.MINUTE, sM)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val endCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, dayEntity.year)
                            set(Calendar.MONTH, dayEntity.month - 1)
                            set(Calendar.DAY_OF_MONTH, dayEntity.day)
                            set(Calendar.HOUR_OF_DAY, eH)
                            set(Calendar.MINUTE, eM)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                            if (eH < sH || (eH == sH && eM < sM)) {
                                add(Calendar.DAY_OF_MONTH, 1) // Crosses midnight
                            }
                        }

                        val dtStart = formatIcsTime(startCal)
                        val dtEnd = formatIcsTime(endCal)

                        val summary = "Shift: ${dayEntity.currentShift}"
                        val description = buildString {
                            append("Shift details: ${dayEntity.currentShift}\\n")
                            if (dayEntity.isModified) {
                                append("Modified: Yes (${dayEntity.alterationType ?: "Manual"})\\n")
                                if (!dayEntity.alterationNote.isNullOrEmpty()) {
                                    append("Note: ${dayEntity.alterationNote}\\n")
                                }
                                if (dayEntity.alterationType == "Trade" && !dayEntity.tradeWithEmployeeCode.isNullOrEmpty()) {
                                    append("Traded with Employee: ${dayEntity.tradeWithEmployeeCode}\\n")
                                }
                            }
                            if (dayEntity.isHoliday) {
                                append("Holiday rate applied\\n")
                            }
                        }

                        val uid = "groundlink-${dayEntity.year}-${dayEntity.month}-${dayEntity.day}-${dayEntity.employeeCode}@groundlink.com"

                        sb.append("BEGIN:VEVENT\n")
                        sb.append("UID:$uid\n")
                        sb.append("DTSTAMP:$dtStamp\n")
                        sb.append("DTSTART:$dtStart\n")
                        sb.append("DTEND:$dtEnd\n")
                        sb.append("SUMMARY:$summary\n")
                        sb.append("DESCRIPTION:$description\n")
                        sb.append("END:VEVENT\n")
                    }
                }
            }
        }

        sb.append("END:VCALENDAR\n")

        try {
            val fileName = "Groundlink_Schedule_${year}_${month}.ics"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)
            writer.write(sb.toString())
            writer.close()

            val authority = "${context.packageName}.fileprovider"
            val fileUri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/calendar"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Groundlink Shift Calendar: $month/$year")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Export Calendar")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share calendar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun formatIcsTime(cal: Calendar): String {
        return String.format(Locale.US, "%04d%02d%02dT%02d%02d%02d",
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND)
        )
    }
}
