package com.example.utils

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.InputStream
import java.io.IOException

data class EmployeeSchedule(
    val title: String,
    val sequence: String,
    val name: String,
    val code: String,
    val dailySchedules: List<String>
)

data class WordPos(
    val text: String,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float
) {
    val xCenter: Float get() = (minX + maxX) / 2f
    val yCenter: Float get() = (minY + maxY) / 2f
}

data class WorkerHeader(
    val title: String,
    val sequence: String,
    val name: String,
    val code: String,
    val yCoord: Float
)

class RosterStripper : PDFTextStripper() {
    val words = mutableListOf<WordPos>()

    init {
        sortByPosition = true
    }

    @Throws(IOException::class)
    override fun writeString(lineText: String, textPositions: List<TextPosition>) {
        if (textPositions.isEmpty()) return

        // Segment characters in the line parsed by PDFBox layout engine into clean words
        var currentWordText = StringBuilder()
        var minX = textPositions[0].xDirAdj
        var maxX = textPositions[0].xDirAdj + textPositions[0].widthDirAdj
        var minY = textPositions[0].yDirAdj
        var maxY = textPositions[0].yDirAdj + textPositions[0].heightDir

        currentWordText.append(textPositions[0].unicode)

        for (i in 1 until textPositions.size) {
            val prevChar = textPositions[i - 1]
            val currentChar = textPositions[i]
            val prevRight = prevChar.xDirAdj + prevChar.widthDirAdj
            val gap = currentChar.xDirAdj - prevRight

            val isSpace = currentChar.unicode == " " || currentChar.unicode == "\t"
            if (gap < 4.0f && !isSpace) {
                currentWordText.append(currentChar.unicode)
                maxX = currentChar.xDirAdj + currentChar.widthDirAdj
                minY = minOf(minY, currentChar.yDirAdj)
                maxY = maxOf(maxY, currentChar.yDirAdj + currentChar.heightDir)
            } else {
                val text = currentWordText.toString().trim()
                if (text.isNotEmpty()) {
                    words.add(WordPos(text, minX, maxX, minY, maxY))
                }
                currentWordText = StringBuilder()
                if (!isSpace) {
                    currentWordText.append(currentChar.unicode)
                    minX = currentChar.xDirAdj
                    maxX = currentChar.xDirAdj + currentChar.widthDirAdj
                    minY = currentChar.yDirAdj
                    maxY = currentChar.yDirAdj + currentChar.heightDir
                } else {
                    if (i + 1 < textPositions.size) {
                        val next = textPositions[i + 1]
                        minX = next.xDirAdj
                        maxX = next.xDirAdj + next.widthDirAdj
                        minY = next.yDirAdj
                        maxY = next.yDirAdj + next.heightDir
                    }
                }
            }
        }
        val text = currentWordText.toString().trim()
        if (text.isNotEmpty()) {
            words.add(WordPos(text, minX, maxX, minY, maxY))
        }
    }
}

object PDFParser {

    fun parseRosterPdf(context: Context, pdfUri: Uri): List<EmployeeSchedule> {
        return context.contentResolver.openInputStream(pdfUri)?.use { inputStream ->
            parseRosterPdfStream(inputStream)
        } ?: emptyList()
    }

    fun parseRosterPdfStream(inputStream: InputStream): List<EmployeeSchedule> {
        val schedules = mutableListOf<EmployeeSchedule>()

        try {
            PDDocument.load(inputStream).use { document ->
                val pageCount = document.numberOfPages
                for (pageNo in 1..pageCount) {
                    val stripper = RosterStripper()
                    stripper.startPage = pageNo
                    stripper.endPage = pageNo
                    stripper.getText(document)

                    val pageSchedules = parsePage(stripper.words)
                    schedules.addAll(pageSchedules)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return schedules
    }

    private fun parsePage(words: List<WordPos>): List<EmployeeSchedule> {
        if (words.isEmpty()) return emptyList()

        // 1. Detect day numbers to define columns grid mathematically
        val calendarColumns = detectDayColumns(words)
        if (calendarColumns.isEmpty()) return emptyList()

        val totalDays = calendarColumns.size
        val firstColX = calendarColumns[0]
        val colWidth = if (totalDays > 1) {
            calendarColumns[1] - calendarColumns[0]
        } else {
            20f
        }

        // 2. Locate all worker rows headers
        val headers = detectWorkerHeaders(words)
        if (headers.isEmpty()) return emptyList()

        val sortedHeaders = headers.sortedBy { it.yCoord }
        val pageSchedules = mutableListOf<EmployeeSchedule>()

        // Estimate row height dynamically using gaps between consecutive headers
        val gaps = mutableListOf<Float>()
        for (i in 0 until sortedHeaders.size - 1) {
            val gap = sortedHeaders[i + 1].yCoord - sortedHeaders[i].yCoord
            if (gap in 15f..80f) {
                gaps.add(gap)
            }
        }
        val estimatedRowHeight = if (gaps.isNotEmpty()) {
            gaps.sorted()[gaps.size / 2]
        } else {
            30f
        }

        for (idx in sortedHeaders.indices) {
            val currentHeader = sortedHeaders[idx]
            
            // Seamlessly span the entire row from midpoint to midpoint
            val yStart = if (idx == 0) {
                currentHeader.yCoord - (estimatedRowHeight * 0.6f)
            } else {
                (sortedHeaders[idx - 1].yCoord + currentHeader.yCoord) / 2f
            }
            
            val yEnd = if (idx < sortedHeaders.size - 1) {
                (currentHeader.yCoord + sortedHeaders[idx + 1].yCoord) / 2f
            } else {
                currentHeader.yCoord + (estimatedRowHeight * 0.6f)
            }

            // Gather elements lying in this worker's vertical line band
            val workerWords = words.filter { it.yCenter in yStart..yEnd }

            val dailyShifts = MutableList(totalDays) { "" }
            val shiftWordsByDay = List(totalDays) { mutableListOf<WordPos>() }

            for (w in workerWords) {
                val relativeX = w.xCenter - firstColX
                val dayIdx = Math.round(relativeX / colWidth).toInt()
                if (dayIdx in 0 until totalDays) {
                    shiftWordsByDay[dayIdx].add(w)
                }
            }

            // Sort vertical words inside each day cell from top-to-bottom and join
            for (d in 0 until totalDays) {
                val dayWords = shiftWordsByDay[d].sortedBy { it.yCenter }
                val texts = dayWords.map { it.text }
                dailyShifts[d] = joinCellTexts(texts)
            }

            pageSchedules.add(
                EmployeeSchedule(
                    title = currentHeader.title,
                    sequence = currentHeader.sequence,
                    name = currentHeader.name,
                    code = currentHeader.code,
                    dailySchedules = dailyShifts
                )
            )
        }

        return pageSchedules
    }

    private fun detectDayColumns(words: List<WordPos>): List<Float> {
        // Group all integer day words by Y coordinator levels
        val candidateDayWords = words.filter {
            val num = it.text.toIntOrNull()
            num != null && num in 1..31
        }

        val groupedByY = mutableListOf<MutableList<WordPos>>()
        for (w in candidateDayWords) {
            var added = false
            for (group in groupedByY) {
                val avgY = group.map { it.yCenter }.average()
                if (kotlin.math.abs(w.yCenter - avgY) < 4.0) {
                    group.add(w)
                    added = true
                    break
                }
            }
            if (!added) {
                groupedByY.add(mutableListOf(w))
            }
        }

        // Search the row containing horizontal dates
        var bestRow = emptyList<WordPos>()
        var maxUniqueCount = 0

        for (group in groupedByY) {
            val uniqueInts = group.mapNotNull { it.text.toIntOrNull() }.toSet()
            if (uniqueInts.size >= 20 && uniqueInts.size > maxUniqueCount) {
                maxUniqueCount = uniqueInts.size
                bestRow = group
            }
        }

        val detectedDays = bestRow.mapNotNull { w ->
            w.text.toIntOrNull()?.let { num -> Pair(num, w.xCenter) }
        }.sortedBy { it.first }

        if (detectedDays.size < 2) return emptyList()

        val minD = detectedDays.first().first
        val xMinVal = detectedDays.first().second

        val maxD = detectedDays.last().first
        val xMaxVal = detectedDays.last().second

        val numDays = maxD // usually 30 or 31
        val estimatedColWidth = (xMaxVal - xMinVal) / (maxD - minD).toFloat()

        val columnsList = mutableListOf<Float>()
        for (d in 1..numDays) {
            val colX = xMinVal + (d - minD) * estimatedColWidth
            columnsList.add(colX)
        }
        return columnsList
    }

    private fun detectWorkerHeaders(words: List<WordPos>): List<WorkerHeader> {
        val workerHeaders = mutableListOf<WorkerHeader>()

        // 4-digit code candidates representing operational worker staff boundaries
        val codeCandidates = words.filter {
            it.text.matches(Regex("^\\d{4}$")) && it.yCenter in 50f..760f
        }

        for (codeWord in codeCandidates) {
            val y = codeWord.yCenter
            val levelWords = words.filter { kotlin.math.abs(it.yCenter - y) < 5.0f }.sortedBy { it.xCenter }

            // Find the sequence number (numeric word satisfying sequence range criteria)
            val codeIndex = levelWords.indexOf(codeWord)
            if (codeIndex == -1) continue

            var seqIndex = -1
            for (i in 0 until codeIndex) {
                val w = levelWords[i]
                if (w.text.matches(Regex("^\\d+$")) && w.text.toInt() in 1..45) {
                    seqIndex = i
                    break
                }
            }

            if (seqIndex != -1) {
                val titleWords = levelWords.subList(0, seqIndex)
                val titleString = titleWords.map { text }.joinToString(" ").trim()

                val nameWords = levelWords.subList(seqIndex + 1, codeIndex)
                val nameString = nameWords.map { text }.joinToString(" ").trim()

                val titleUp = titleString.uppercase()
                val hasOperationalPrefix = titleUp.contains("TDA") || titleUp.contains("CSA") || 
                        titleUp.contains("OPS") || titleUp.contains("LST") || titleUp.contains("SUP") ||
                        titleUp.contains("ADMIN") || titleUp.contains("TRAINER") || titleUp.contains("DSM") || 
                        titleUp.contains("SM") || titleUp.contains("RRPP")

                if ((hasOperationalPrefix || titleString.isNotEmpty() || nameString.length > 2) && nameString.isNotEmpty()) {
                    workerHeaders.add(
                        WorkerHeader(
                            title = titleString,
                            sequence = levelWords[seqIndex].text,
                            name = nameString,
                            code = codeWord.text,
                            yCoord = y
                        )
                    )
                }
            }
        }

        return workerHeaders
    }

    private fun joinCellTexts(texts: List<String>): String {
        if (texts.isEmpty()) return ""
        val sb = java.lang.StringBuilder()
        for (t in texts) {
            val trimmed = t.trim()
            if (trimmed.isEmpty()) continue
            if (sb.isEmpty()) {
                sb.append(trimmed)
            } else {
                sb.append("\n").append(trimmed)
            }
        }
        return sb.toString()
    }

    fun convertToCsv(schedules: List<EmployeeSchedule>): String {
        if (schedules.isEmpty()) return ""

        val sb = java.lang.StringBuilder()
        val maxDays = schedules.maxOfOrNull { it.dailySchedules.size } ?: 0

        // CSV Header structure
        sb.append("Title,Sequence,Name,Code")
        for (day in 1..maxDays) {
            sb.append(",Day $day")
        }
        sb.append("\n")

        for (sched in schedules) {
            sb.append("${escapeCsv(sched.title)},${escapeCsv(sched.sequence)},${escapeCsv(sched.name)},${escapeCsv(sched.code)}")
            for (dayIdx in 0 until maxDays) {
                val value = if (dayIdx < sched.dailySchedules.size) {
                    sched.dailySchedules[dayIdx]
                } else {
                    ""
                }
                sb.append(",${escapeCsv(value)}")
            }
            sb.append("\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            val replaced = value.replace("\"", "\"\"")
            return "\"$replaced\""
        }
        return value
    }
}
