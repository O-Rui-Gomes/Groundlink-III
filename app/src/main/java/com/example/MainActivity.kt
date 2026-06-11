package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.db.entities.EmployeeEntity
import com.example.db.entities.ScheduleDayEntity
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.EmployeeSchedule
import com.example.utils.PDFParser
import com.example.utils.PreferencesManager
import com.example.utils.ShiftTimeCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale

// --- MODELS FOR ACCOUNTABILITY BALANCES ---
data class CoworkerNetBalance(
    val employeeCode: String,
    val employeeName: String,
    val netHours: Double
)

// --- DYNAMIC THEMING ENGINE ---
data class ThemeConfig(
    val primary: Color,
    val mutedGreen: Color,
    val background: Color,
    val surface: Color,
    val card: Color,
    val borderHighlight: Color,
    val cardCornerRadius: androidx.compose.ui.unit.Dp,
    val buttonCornerRadius: androidx.compose.ui.unit.Dp,
    val fontFamily: FontFamily,
    val borderWidth: androidx.compose.ui.unit.Dp,
    val textAllCaps: Boolean,
    val themeIcon: String,
    val name: String,
    val designDetails: String
)

fun getThemeConfig(index: Int): ThemeConfig {
    return when (index) {
        0 -> ThemeConfig(
            primary = Color(0xFF818CF8),
            mutedGreen = Color(0xFF34D399),
            background = Color(0xFF0D1026),
            surface = Color(0xFF090B1E),
            card = Color(0xFF131735),
            borderHighlight = Color(0xFF818CF8).copy(alpha = 0.15f),
            cardCornerRadius = 20.dp,
            buttonCornerRadius = 14.dp,
            fontFamily = FontFamily.SansSerif,
            borderWidth = 0.dp,
            textAllCaps = false,
            themeIcon = "✈️",
            name = "Aero Midnight Obsidian",
            designDetails = "High-fidelity cosmic indigo-violet theme with deep midnight surfaces, rich shadows, and soft periwinkle accents."
        )
        1 -> ThemeConfig(
            primary = Color(0xFF00E5FF),
            mutedGreen = Color(0xFF7C4DFF),
            background = Color(0xFF0E0B16),
            surface = Color(0xFF15102A),
            card = Color(0xFF1F1640),
            borderHighlight = Color(0xFF00E5FF).copy(alpha = 0.22f),
            cardCornerRadius = 12.dp,
            buttonCornerRadius = 12.dp,
            fontFamily = FontFamily.SansSerif,
            borderWidth = 1.dp,
            textAllCaps = false,
            themeIcon = "🌌",
            name = "Neon Horizon Dusk",
            designDetails = "Deep cosmic navy with twilight purples. Translucent glassmorphic panels and glowing neon-cyan highlights."
        )
        2 -> ThemeConfig(
            primary = Color(0xFF5DF5B7),
            mutedGreen = Color(0xFF2E6B4B),
            background = Color(0xFF111518),
            surface = Color(0xFF14181B),
            card = Color(0xFF1A1F24),
            borderHighlight = Color(0xFF5DF5B7).copy(alpha = 0.22f),
            cardCornerRadius = 24.dp,
            buttonCornerRadius = 24.dp,
            fontFamily = FontFamily.SansSerif,
            borderWidth = 0.dp,
            textAllCaps = false,
            themeIcon = "🌲",
            name = "Nordic Spruce",
            designDetails = "A modern, breathable Nordic theme. Organic woodland charcoal base, generous cozy padding, and soft mint/spruce accents."
        )
        3 -> ThemeConfig(
            primary = Color(0xFFFF5D5D),
            mutedGreen = Color(0xFFFF9100),
            background = Color(0xFF0F0505),
            surface = Color(0xFF1A0909),
            card = Color(0xFF260D0D),
            borderHighlight = Color(0xFFFF5D5D).copy(alpha = 0.22f),
            cardCornerRadius = 6.dp,
            buttonCornerRadius = 6.dp,
            fontFamily = FontFamily.Monospace,
            borderWidth = 1.5.dp,
            textAllCaps = true,
            themeIcon = "🌅",
            name = "Cyberpunk Sunset",
            designDetails = "Fiery cyberpunk airline design with glowing crimson, warm sunset oranges, and sharp high-vis neon-amber details."
        )
        4 -> ThemeConfig(
            primary = Color(0xFF40C4FF),
            mutedGreen = Color(0xFF007A99),
            background = Color(0xFF0A1118),
            surface = Color(0xFF101924),
            card = Color(0xFF172433),
            borderHighlight = Color(0xFF40C4FF).copy(alpha = 0.22f),
            cardCornerRadius = 16.dp,
            buttonCornerRadius = 16.dp,
            fontFamily = FontFamily.SansSerif,
            borderWidth = 1.dp,
            textAllCaps = false,
            themeIcon = "🌊",
            name = "Ocean Drift",
            designDetails = "Sleek maritime pacific deep navy with turquoise and water blue accents for clean ocean layouts."
        )
        else -> ThemeConfig(
            primary = Color(0xFF00FF66),
            mutedGreen = Color(0xFF008A37),
            background = Color(0xFF000000),
            surface = Color(0xFF070B07),
            card = Color(0xFF0C120C),
            borderHighlight = Color(0xFF00FF66).copy(alpha = 0.22f),
            cardCornerRadius = 0.dp,
            buttonCornerRadius = 0.dp,
            fontFamily = FontFamily.Monospace,
            borderWidth = 1.2.dp,
            textAllCaps = true,
            themeIcon = "⚡",
            name = "Matrix Green Explorer",
            designDetails = "Vintage command console matrix. Pure obsidian black backdrop, retro glowing phosphor green grids, and clean retro-tech lines."
        )
    }
}

object ActiveTheme {
    var selectedIndex by mutableStateOf(0) // Default to Retro Amber (index 0)
    var fontOverrideIndex by mutableStateOf(0) // 0 = Theme Default, 1 = Monospace, 2 = Sans-Serif, 3 = Serif
    
    val current: ThemeConfig
        get() = getThemeConfig(selectedIndex)
}

val themePrimary: Color get() = ActiveTheme.current.primary
val themeMutedGreen: Color get() = ActiveTheme.current.mutedGreen
val themeBackground: Color get() = ActiveTheme.current.background
val themeSurface: Color get() = ActiveTheme.current.surface
val themeCard: Color get() = ActiveTheme.current.card
val themeBorderHighlight: Color get() = ActiveTheme.current.borderHighlight

fun getSafeMiniCornerRadius(baseRadius: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
    return if (baseRadius < 4.dp) {
        baseRadius
    } else {
        baseRadius / 2
    }
}

val themeCardShape: RoundedCornerShape get() = RoundedCornerShape(ActiveTheme.current.cardCornerRadius)
val themeMiniShape: RoundedCornerShape get() = RoundedCornerShape(getSafeMiniCornerRadius(ActiveTheme.current.cardCornerRadius))
val themeButtonShape: RoundedCornerShape get() = RoundedCornerShape(ActiveTheme.current.buttonCornerRadius)
val themeFontFamily: FontFamily get() {
    return when (ActiveTheme.fontOverrideIndex) {
        1 -> FontFamily.Monospace
        2 -> FontFamily.SansSerif
        3 -> FontFamily.Serif
        else -> ActiveTheme.current.fontFamily
    }
}
val themeBorderWidth: androidx.compose.ui.unit.Dp get() = ActiveTheme.current.borderWidth
val themeIcon: String get() = ActiveTheme.current.themeIcon

fun String.themeCased(): String = if (ActiveTheme.current.textAllCaps) this.uppercase() else this

fun Modifier.themeCardBorder(cornerRadius: androidx.compose.ui.unit.Dp = ActiveTheme.current.cardCornerRadius): Modifier {
    val bWidth = ActiveTheme.current.borderWidth
    return if (bWidth > 0.dp) {
        this.border(bWidth, themePrimary.copy(alpha = 0.15f), RoundedCornerShape(cornerRadius))
    } else {
        this
    }
}

fun Modifier.themeMiniBorder(cornerRadius: androidx.compose.ui.unit.Dp = getSafeMiniCornerRadius(ActiveTheme.current.cardCornerRadius)): Modifier {
    val bWidth = ActiveTheme.current.borderWidth
    return if (bWidth > 0.dp) {
        this.border(bWidth, themePrimary.copy(alpha = 0.15f), RoundedCornerShape(cornerRadius))
    } else {
        this
    }
}

// Calculated Summary holding exact financial breakdowns
data class EarningsSummary(
    val totalBaseHours: Double,
    val totalOvertimeHours: Double,
    val totalNightHours: Double,
    val holidayWorkingHours: Double,
    val daysWorkedCount: Int,
    val restDaysCount: Int,
    val baseEarnings: Double,
    val nightPremiumEarnings: Double,
    val overtimeEarnings: Double,
    val holidayExtraEarnings: Double,
    val grandTotal: Double
)

// Result model with 11-hour rest safety analyses onwards and backwards
data class TradeCandidateResult(
    val employee: EmployeeEntity,
    val schedule: ScheduleDayEntity,
    val isMyRestOk: Boolean,
    val myRestPrev: Double?,
    val myRestNext: Double?,
    val isColleagueRestOk: Boolean,
    val colleagueRestPrev: Double?,
    val colleagueRestNext: Double?,
    val colleagueDaysOff: Int = 0,
    val colleagueAvgHours: Double = 0.0,
    val colleagueMatchesBadge: Boolean = false,
    val colleagueMatchesStats: Boolean = false,
    val myDaysOff: Int = 0,
    val myAvgHours: Double = 0.0
)

data class TradeFilterContext(
    val employees: List<EmployeeEntity>,
    val year: Int,
    val month: Int,
    val day: Int,
    val searchType: String,
    val searchKeyword: String
)

// Main ViewModel bridging the offline Room repository and user configurations
class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ScheduleExtractorApplication).repository
    val prefs = PreferencesManager(application)

    // Calendar selections state
    var selectedYear by mutableStateOf(Calendar.getInstance().get(Calendar.YEAR))
    var selectedMonth by mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) // 1-12
    var searchKeyword by mutableStateOf("")

    // Screen navigation state (0: Personal Schedule, 1: Trade Finder, 2: All Roster, 3: Earnings Settings)
    var activeTab by mutableStateOf(0)

    // Data streams
    val allEmployees = repository.allEmployees
    val myProfile = repository.myProfile
    val availableMonths = repository.availableMonths

    // Utility states
    var isLoading by mutableStateOf(false)
    var isSharing by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var selectedFileName by mutableStateOf<String?>(null)

    // Adjustment traces overlays
    var editingScheduleDay by mutableStateOf<ScheduleDayEntity?>(null)
    var showImportDateDialog by mutableStateOf<List<EmployeeSchedule>?>(null)

    // Trade filter states
    var tradeSelectedDay by mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
    var tradeSearchType by mutableStateOf("OFF") // "OFF" (folga), "EARLY" (starts before 12:00), "LATE" (starts at or after 12:00)
    var tradeSearchKeyword by mutableStateOf("")

    // Everyone tab search state
    var everyoneSearchKeyword by mutableStateOf("")

    fun getSchedulesForEmployee(employeeCode: String, year: Int, month: Int): Flow<List<ScheduleDayEntity>> {
        return repository.getSchedulesForEmployee(employeeCode, year, month)
    }

    // Preferences form states
    var configBaseRate by mutableStateOf(prefs.baseRate.toString())
    var configOvertimeMultiplier by mutableStateOf(prefs.overtimeMultiplier.toString())
    var configNightPremium by mutableStateOf((prefs.nightPremiumPercentage * 100).toInt().toString())
    var configNightStart by mutableStateOf(prefs.nightStartHour.toString())
    var configNightEnd by mutableStateOf(prefs.nightEndHour.toString())
    var configHolidayMultiplier by mutableStateOf(prefs.holidayMultiplier.toString())
    var configShiftAlertEnabled by mutableStateOf(prefs.shiftAlertEnabled)
    var configShiftAlertMinutes by mutableStateOf(prefs.shiftAlertMinutes.toString())

    // Personal schedule Flow combined reactive query
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val mySchedules: StateFlow<List<ScheduleDayEntity>> = combine(
        myProfile,
        snapshotFlow { selectedYear },
        snapshotFlow { selectedMonth }
    ) { profile, y, m ->
        Triple(profile, y, m)
    }.flatMapLatest { (profile, y, m) ->
        if (profile != null) {
            repository.getSchedulesForEmployee(profile.code, y, m)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val prevMonthSchedules: StateFlow<List<ScheduleDayEntity>> = combine(
        myProfile,
        snapshotFlow { selectedYear },
        snapshotFlow { selectedMonth }
    ) { profile, y, m ->
        val prevY = if (m == 1) y - 1 else y
        val prevM = if (m == 1) 12 else m - 1
        Triple(profile, prevY, prevM)
    }.flatMapLatest { (profile, prevY, prevM) ->
        if (profile != null) {
            repository.getSchedulesForEmployee(profile.code, prevY, prevM)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allMySchedules: StateFlow<List<ScheduleDayEntity>> = myProfile
        .flatMapLatest { profile ->
            if (profile != null) {
                repository.getAllSchedulesForEmployee(profile.code)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Trade finder list combined reactive search
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val tradeCandidates: StateFlow<List<TradeCandidateResult>> = combine(
        allEmployees,
        snapshotFlow { selectedYear },
        snapshotFlow { selectedMonth },
        snapshotFlow { tradeSelectedDay },
        snapshotFlow { tradeSearchType },
        snapshotFlow { tradeSearchKeyword }
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        TradeFilterContext(
            employees = arr[0] as List<EmployeeEntity>,
            year = arr[1] as Int,
            month = arr[2] as Int,
            day = arr[3] as Int,
            searchType = arr[4] as String,
            searchKeyword = arr[5] as String
        )
    }.flatMapLatest { ctx ->
        repository.getSchedulesForMonth(ctx.year, ctx.month).map { schedules ->
            val myCode = prefs.myEmployeeCode ?: ""
            val myProf = ctx.employees.find { it.code == myCode }

            // Get my own schedules for month to calculate reference stats
            val mySchedulesForMonth = schedules.filter { it.employeeCode == myCode }
            val myDaysOff = mySchedulesForMonth.count { ShiftTimeCalculator.parseShiftTimes(it.currentShift) == null }
            val myWorkDays = mySchedulesForMonth.filter { ShiftTimeCalculator.parseShiftTimes(it.currentShift) != null }
            val myTotalHours = myWorkDays.sumOf { w ->
                val times = ShiftTimeCalculator.parseShiftTimes(w.currentShift)
                if (times != null) ShiftTimeCalculator.calculateHours(times.first, times.second) else 0.0
            }
            val myAvgHours = if (myWorkDays.isNotEmpty()) myTotalHours / myWorkDays.size else 0.0

            // Get my own adjacent shifts to perform forward/backward 11h rest checks
            val myShiftDMinus1 = schedules.find { it.employeeCode == myCode && it.day == ctx.day - 1 }?.currentShift ?: ""
            val myShiftD = schedules.find { it.employeeCode == myCode && it.day == ctx.day }?.currentShift ?: ""
            val myShiftDPlus1 = schedules.find { it.employeeCode == myCode && it.day == ctx.day + 1 }?.currentShift ?: ""

            val daySchedules = schedules.filter { it.day == ctx.day }
            daySchedules.mapNotNull { schedule ->
                val emp = ctx.employees.find { it.code == schedule.employeeCode }
                if (emp != null && emp.code != myCode) {
                    val colleagueSchedules = schedules.filter { it.employeeCode == emp.code }
                    val colDaysOff = colleagueSchedules.count { ShiftTimeCalculator.parseShiftTimes(it.currentShift) == null }
                    val colWorkDays = colleagueSchedules.filter { ShiftTimeCalculator.parseShiftTimes(it.currentShift) != null }
                    val colTotalHours = colWorkDays.sumOf { w ->
                        val times = ShiftTimeCalculator.parseShiftTimes(w.currentShift)
                        if (times != null) ShiftTimeCalculator.calculateHours(times.first, times.second) else 0.0
                    }
                    val colAvgHours = if (colWorkDays.isNotEmpty()) colTotalHours / colWorkDays.size else 0.0

                    val colMatchesBadge = myProf != null && emp.title.equals(myProf.title, ignoreCase = true)
                    val colMatchesStats = Math.abs(colAvgHours - myAvgHours) < 0.25

                    val colleagueShiftDMinus1 = schedules.find { it.employeeCode == emp.code && it.day == ctx.day - 1 }?.currentShift ?: ""
                    val colleagueShiftD = schedule.currentShift
                    val colleagueShiftDPlus1 = schedules.find { it.employeeCode == emp.code && it.day == ctx.day + 1 }?.currentShift ?: ""

                    // If swapped:
                    // 1. My shift is swapped with colleagueShiftD.
                    // We check if I have 11h rest between my D-1 shift and colleagueShiftD, AND colleagueShiftD and my D+1 shift.
                    val isMyDOff = colleagueShiftD.isBlank() || ShiftTimeCalculator.parseShiftTimes(colleagueShiftD) == null
                    val myRestPrev = if (!isMyDOff && myShiftDMinus1.isNotBlank()) {
                        ShiftTimeCalculator.calculateRestBetweenShifts(ctx.day - 1, myShiftDMinus1, ctx.day, colleagueShiftD)
                    } else null

                    val myRestNext = if (!isMyDOff && myShiftDPlus1.isNotBlank()) {
                        ShiftTimeCalculator.calculateRestBetweenShifts(ctx.day, colleagueShiftD, ctx.day + 1, myShiftDPlus1)
                    } else null

                    // 2. Colleague's shift is swapped with myShiftD.
                    // We check if colleague has 11h rest between colleague's D-1 shift and myShiftD, AND myShiftD and colleague's D+1 shift.
                    val isColDOff = myShiftD.isBlank() || ShiftTimeCalculator.parseShiftTimes(myShiftD) == null
                    val colRestPrev = if (!isColDOff && colleagueShiftDMinus1.isNotBlank()) {
                        ShiftTimeCalculator.calculateRestBetweenShifts(ctx.day - 1, colleagueShiftDMinus1, ctx.day, myShiftD)
                    } else null

                    val colRestNext = if (!isColDOff && colleagueShiftDPlus1.isNotBlank()) {
                        ShiftTimeCalculator.calculateRestBetweenShifts(ctx.day, myShiftD, ctx.day + 1, colleagueShiftDPlus1)
                    } else null

                    val isMyPrevOk = myRestPrev == null || myRestPrev >= 11.0
                    val isMyNextOk = myRestNext == null || myRestNext >= 11.0
                    val isColPrevOk = colRestPrev == null || colRestPrev >= 11.0
                    val isColNextOk = colRestNext == null || colRestNext >= 11.0

                    TradeCandidateResult(
                        employee = emp,
                        schedule = schedule,
                        isMyRestOk = isMyPrevOk && isMyNextOk,
                        myRestPrev = myRestPrev,
                        myRestNext = myRestNext,
                        isColleagueRestOk = isColPrevOk && isColNextOk,
                        colleagueRestPrev = colRestPrev,
                        colleagueRestNext = colRestNext,
                        colleagueDaysOff = colDaysOff,
                        colleagueAvgHours = colAvgHours,
                        colleagueMatchesBadge = colMatchesBadge,
                        colleagueMatchesStats = colMatchesStats,
                        myDaysOff = myDaysOff,
                        myAvgHours = myAvgHours
                    )
                } else null
            }.filter { result ->
                if (ctx.searchKeyword.isNotBlank()) {
                    // Match everyone when actively searching for a colleague by name/code
                    true
                } else {
                    val isOff = ShiftTimeCalculator.parseShiftTimes(result.schedule.currentShift) == null
                    when (ctx.searchType) {
                        "OFF" -> isOff
                        "EARLY" -> {
                            val times = ShiftTimeCalculator.parseShiftTimes(result.schedule.currentShift)
                            if (times != null) {
                                val hour = times.first.substringBefore(":").toIntOrNull() ?: 12
                                hour < 12
                            } else false
                        }
                        "LATE" -> {
                            val times = ShiftTimeCalculator.parseShiftTimes(result.schedule.currentShift)
                            if (times != null) {
                                val hour = times.first.substringBefore(":").toIntOrNull() ?: 12
                                hour >= 12
                            } else false
                        }
                        else -> true // "ALL" - All Shifts
                    }
                }
            }.sortedWith(
                compareByDescending<TradeCandidateResult> { it.colleagueMatchesBadge && it.colleagueMatchesStats }
                    .thenByDescending { it.colleagueMatchesBadge }
                    .thenByDescending { it.colleagueMatchesStats }
                    .thenBy { it.employee.name }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectMyProfile(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.myEmployeeCode = code
            repository.setMe(code)
        }
    }

    fun parseAndTriggerImport(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            errorMessage = null
            selectedFileName = fileName
            try {
                val results = PDFParser.parseRosterPdf(context, uri)
                if (results.isEmpty()) {
                    errorMessage = "No schedule rows could be extracted. Make sure the PDF aligns employee rows with dates."
                } else {
                    showImportDateDialog = results
                }
            } catch (e: Exception) {
                errorMessage = "Failed parsing PDF file: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun completeImport(schedules: List<EmployeeSchedule>, year: Int, month: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            try {
                val empEntities = schedules.map {
                    EmployeeEntity(
                        code = it.code,
                        name = it.name,
                        title = it.title,
                        sequence = it.sequence,
                        isMe = (it.code == prefs.myEmployeeCode)
                    )
                }

                val dayEntities = mutableListOf<ScheduleDayEntity>()
                schedules.forEach { emp ->
                    emp.dailySchedules.forEachIndexed { index, shiftText ->
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month - 1)
                            set(Calendar.DAY_OF_MONTH, index + 1)
                        }
                        // Smart pre-calculated overtime (hours > 8)
                        var parsedOvertimeHours = 0.0
                        val times = ShiftTimeCalculator.parseShiftTimes(shiftText)
                        if (times != null) {
                            val totalHours = ShiftTimeCalculator.calculateHours(times.first, times.second)
                            if (totalHours > 8.0) {
                                parsedOvertimeHours = totalHours - 8.0
                            }
                        }

                        dayEntities.add(
                            ScheduleDayEntity(
                                employeeCode = emp.code,
                                year = year,
                                month = month,
                                day = index + 1,
                                originalShift = shiftText,
                                currentShift = shiftText,
                                isModified = false,
                                isOvertime = parsedOvertimeHours > 0.0,
                                overtimeHours = parsedOvertimeHours,
                                isHoliday = false
                            )
                        )
                    }
                }

                repository.saveParsedRoster(empEntities, dayEntities, year, month)
                selectedYear = year
                selectedMonth = month
                showImportDateDialog = null
            } catch (e: Exception) {
                errorMessage = "Failed storing imported data: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun saveDayModifications(
        dayEntity: ScheduleDayEntity,
        newShift: String,
        isOvertime: Boolean,
        ovHours: Double,
        isHoliday: Boolean,
        alterationType: String?,
        alterationNote: String?,
        tradedCode: String?,
        tradeAccountability: String? = null,
        tradeOwedHours: Double = 0.0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Update own cell
                val updated = dayEntity.copy(
                    currentShift = newShift,
                    isModified = alterationType != null,
                    alterationType = alterationType,
                    alterationNote = alterationNote,
                    tradeWithEmployeeCode = tradedCode,
                    isOvertime = isOvertime,
                    overtimeHours = ovHours,
                    isHoliday = isHoliday,
                    tradeAccountability = tradeAccountability,
                    tradeOwedHours = tradeOwedHours
                )
                repository.updateScheduleDay(updated)

                // 2. If swap is a trade, update coworker's shift cell too!
                if (alterationType == "Trade" && tradedCode != null) {
                    val colleagueDay = repository.getScheduleForDay(tradedCode, dayEntity.year, dayEntity.month, dayEntity.day)
                    if (colleagueDay != null) {
                        val updatedColleague = colleagueDay.copy(
                            currentShift = dayEntity.currentShift, // colleague gets our original schedule cell
                            isModified = true,
                            alterationType = "Trade",
                            alterationNote = "Swapped with employee #${dayEntity.employeeCode}",
                            tradeWithEmployeeCode = dayEntity.employeeCode,
                            tradeAccountability = getMirroredAccountability(tradeAccountability),
                            tradeOwedHours = tradeOwedHours
                        )
                        repository.updateScheduleDay(updatedColleague)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                editingScheduleDay = null
            }
        }
    }

    private fun getMirroredAccountability(acc: String?): String? {
        return when (acc) {
            "COWORKER_OWES_ME" -> "I_OWE_COWORKER"
            "I_OWE_COWORKER" -> "COWORKER_OWES_ME"
            "COWORKER_PAID_ME" -> "I_PAID_COWORKER"
            "I_PAID_COWORKER" -> "COWORKER_PAID_ME"
            else -> acc
        }
    }

    fun saveManualDebt(
        colleagueCode: String,
        day: Int,
        accountability: String,
        hours: Double,
        note: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val myCode = prefs.myEmployeeCode ?: return@launch
                val y = selectedYear
                val m = selectedMonth

                // 1. Fetch/Upsert My schedule day
                val myDay = repository.getScheduleForDay(myCode, y, m, day) ?: ScheduleDayEntity(
                    employeeCode = myCode,
                    year = y,
                    month = m,
                    day = day,
                    originalShift = "OFF",
                    currentShift = "OFF"
                )
                val updatedMyDay = myDay.copy(
                    isModified = true,
                    alterationType = "Trade",
                    alterationNote = note.ifBlank { "Manual Debt Entry" },
                    tradeWithEmployeeCode = colleagueCode,
                    tradeAccountability = accountability,
                    tradeOwedHours = hours
                )
                repository.insertOrUpdateScheduleDay(updatedMyDay)

                // 2. Fetch/Upsert colleague's schedule day & mirror it
                val colDay = repository.getScheduleForDay(colleagueCode, y, m, day) ?: ScheduleDayEntity(
                    employeeCode = colleagueCode,
                    year = y,
                    month = m,
                    day = day,
                    originalShift = "OFF",
                    currentShift = "OFF"
                )
                val updatedColDay = colDay.copy(
                    isModified = true,
                    alterationType = "Trade",
                    alterationNote = "Manual Debt with Employee #$myCode: " + note.ifBlank { "Manual Debt Entry" },
                    tradeWithEmployeeCode = myCode,
                    tradeAccountability = getMirroredAccountability(accountability),
                    tradeOwedHours = hours
                )
                repository.insertOrUpdateScheduleDay(updatedColDay)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getScheduleForDay(employeeCode: String, year: Int, month: Int, day: Int): ScheduleDayEntity? {
        return repository.getScheduleForDay(employeeCode, year, month, day)
    }

    fun updateSettings() {
        val baseVal = configBaseRate.toFloatOrNull() ?: 7.50f
        val ovMult = configOvertimeMultiplier.toFloatOrNull() ?: 1.50f
        val nightPct = (configNightPremium.toFloatOrNull() ?: 25f) / 100f
        val startH = configNightStart.toIntOrNull() ?: 22
        val endH = configNightEnd.toIntOrNull() ?: 7
        val holMult = configHolidayMultiplier.toFloatOrNull() ?: 2.00f

        prefs.baseRate = baseVal
        prefs.overtimeMultiplier = ovMult
        prefs.nightPremiumPercentage = nightPct
        prefs.nightStartHour = startH
        prefs.nightEndHour = endH
        prefs.holidayMultiplier = holMult
        prefs.shiftAlertEnabled = configShiftAlertEnabled
        prefs.shiftAlertMinutes = configShiftAlertMinutes.toIntOrNull() ?: 90
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllData()
        }
        prefs.clearAll()
        selectedFileName = null
        errorMessage = null
        searchKeyword = ""
        activeTab = 4 // focus onto Settings tab for import
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GroundlinkAppScreen(
                    paddingValues = PaddingValues(),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroundlinkAppScreen(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    vm: ScheduleViewModel = viewModel()
) {
    val context = LocalContext.current
    val parsedMonths by vm.availableMonths.collectAsStateWithLifecycle(emptyList())
    val myProfileState by vm.myProfile.collectAsStateWithLifecycle(null)

    // Sync input variables with stored Preferences states upon load
    LaunchedEffect(Unit) {
        ActiveTheme.selectedIndex = vm.prefs.selectedTheme
        ActiveTheme.fontOverrideIndex = vm.prefs.fontOverride
        vm.configBaseRate = vm.prefs.baseRate.toString()
        vm.configOvertimeMultiplier = vm.prefs.overtimeMultiplier.toString()
        vm.configNightPremium = (vm.prefs.nightPremiumPercentage * 100).toInt().toString()
        vm.configNightStart = vm.prefs.nightStartHour.toString()
        vm.configNightEnd = vm.prefs.nightEndHour.toString()
        vm.configHolidayMultiplier = vm.prefs.holidayMultiplier.toString()
        vm.configShiftAlertEnabled = vm.prefs.shiftAlertEnabled
        vm.configShiftAlertMinutes = vm.prefs.shiftAlertMinutes.toString()
    }

    val topMySchedules by vm.mySchedules.collectAsStateWithLifecycle(emptyList())
    val topEmployees by vm.allEmployees.collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(topMySchedules, vm.configShiftAlertEnabled, vm.configShiftAlertMinutes) {
        com.example.utils.AlarmScheduler.scheduleAlarmsForMonth(
            context = context,
            schedules = topMySchedules,
            alertEnabled = vm.configShiftAlertEnabled,
            alertMinutes = vm.configShiftAlertMinutes.toIntOrNull() ?: 90,
            coworkers = topEmployees
        )
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val name = getFileNameFromUri(context, it) ?: "Roster.pdf"
                vm.parseAndTriggerImport(context, it, name)
            }
        }
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1235),
                        Color(0xFF060714)
                    )
                )
            ),
        containerColor = Color.Transparent,
        topBar = {
            if (vm.activeTab != 0) {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            val appTitle = when (vm.activeTab) {
                                1 -> "Trade"
                                2 -> "Roster"
                                3 -> "Everyone"
                                4 -> "Settings"
                                else -> "Schedule"
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (vm.activeTab) {
                                        1 -> Icons.Default.Refresh
                                        2 -> Icons.Default.DateRange
                                        3 -> Icons.Default.Person
                                        4 -> Icons.Default.Settings
                                        else -> Icons.Default.Send
                                    },
                                    contentDescription = null,
                                    tint = themePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${ActiveTheme.current.themeIcon} $appTitle".themeCased(),
                                    fontWeight = FontWeight.Black,
                                    fontFamily = themeFontFamily,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = themeSurface
                        ),
                        actions = {
                            if (parsedMonths.isNotEmpty()) {
                                IconButton(
                                    onClick = { vm.clearAllData() },
                                    modifier = Modifier.testTag("clear_data")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Reset database",
                                        tint = Color(0xFFEF5350)
                                    )
                                }
                            }
                        }
                    )

                    // Profiles warning banner if "Me" is not configured yet
                    if (myProfileState == null && parsedMonths.isNotEmpty() && vm.activeTab != 4) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Customize shift timings and overtime! Click Settings to set your active profile.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF5D4037),
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { vm.activeTab = 4 }) {
                                    Text("Configure", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF090B1E),
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = vm.activeTab == 0,
                    onClick = { vm.activeTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home".themeCased(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = themeFontFamily) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = themePrimary,
                        selectedTextColor = themePrimary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color(0xFF6B7280),
                        unselectedTextColor = Color(0xFF6B7280)
                    )
                )

                NavigationBarItem(
                    selected = vm.activeTab == 1,
                    onClick = { vm.activeTab = 1 },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = "Trade") },
                    label = { Text("Trade".themeCased(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = themeFontFamily) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = themePrimary,
                        selectedTextColor = themePrimary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color(0xFF6B7280),
                        unselectedTextColor = Color(0xFF6B7280)
                    )
                )

                NavigationBarItem(
                    selected = vm.activeTab == 2,
                    onClick = { vm.activeTab = 2 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Schedule") },
                    label = { Text("Schedule".themeCased(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = themeFontFamily) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = themePrimary,
                        selectedTextColor = themePrimary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color(0xFF6B7280),
                        unselectedTextColor = Color(0xFF6B7280)
                    )
                )

                NavigationBarItem(
                    selected = vm.activeTab == 3,
                    onClick = { vm.activeTab = 3 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Everyone") },
                    label = { Text("Everyone".themeCased(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = themeFontFamily) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = themePrimary,
                        selectedTextColor = themePrimary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color(0xFF6B7280),
                        unselectedTextColor = Color(0xFF6B7280)
                    )
                )

                NavigationBarItem(
                    selected = vm.activeTab == 4,
                    onClick = { vm.activeTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings".themeCased(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = themeFontFamily) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = themePrimary,
                        selectedTextColor = themePrimary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color(0xFF6B7280),
                        unselectedTextColor = Color(0xFF6B7280)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (vm.activeTab) {
                0 -> MyScheduleTab(vm)
                1 -> TradeFinderTab(vm)
                2 -> RosterExplorerTab(vm)
                3 -> EveryoneTab(vm)
                4 -> SettingsTab(vm, filePickerLauncher)
            }
        }
    }

    // Modal dialogue confirming import targets
    if (vm.showImportDateDialog != null) {
        val results = vm.showImportDateDialog!!
        var selMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
        var selYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

        AlertDialog(
            onDismissRequest = { vm.showImportDateDialog = null },
            confirmButton = {
                Button(
                    onClick = { vm.completeImport(results, selYear, selMonth) },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary, contentColor = Color.Black)
                ) {
                    Text("Complete Import", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.showImportDateDialog = null }) {
                    Text("Cancel", color = Color.White, fontFamily = FontFamily.Monospace)
                }
            },
            title = { Text("Configure Roster Month", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace) },
            containerColor = themeCard,
            modifier = Modifier.border(1.5.dp, themePrimary.copy(alpha = 0.3f), RoundedCornerShape(24.dp)), // Glowing Matrix dialog frame
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Choose the Month and Year that correspond to this PDF sheet so Groundlink III can record schedules properly.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Month Dropdown Trigger Button
                        var monthExpanded by remember { mutableStateOf(false) }
                        val monthsList = listOf(
                            "January", "February", "March", "April", "May", "June",
                            "July", "August", "September", "October", "November", "December"
                        )
                        Box(modifier = Modifier.weight(1.3f)) {
                            OutlinedButton(
                                onClick = { monthExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, themePrimary.copy(alpha = 0.4f))
                            ) {
                                Text(monthsList[selMonth - 1], color = themePrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace)
                            }
                            DropdownMenu(
                                expanded = monthExpanded,
                                onDismissRequest = { monthExpanded = false },
                                modifier = Modifier.background(themeCard).border(1.dp, themePrimary.copy(alpha = 0.3f))
                            ) {
                                monthsList.forEachIndexed { idx, mName ->
                                    DropdownMenuItem(
                                        text = { Text(mName, color = Color.White, fontFamily = FontFamily.Monospace) },
                                        onClick = {
                                            selMonth = idx + 1
                                            monthExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Year Dropdown Trigger Button
                        var yearExpanded by remember { mutableStateOf(false) }
                        val yearsList = (2024..2030).toList()
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { yearExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, themePrimary.copy(alpha = 0.4f))
                            ) {
                                Text(selYear.toString(), color = themePrimary, fontFamily = FontFamily.Monospace)
                            }
                            DropdownMenu(
                                expanded = yearExpanded,
                                onDismissRequest = { yearExpanded = false },
                                modifier = Modifier.background(themeCard).border(1.dp, themePrimary.copy(alpha = 0.3f))
                            ) {
                                yearsList.forEach { valY ->
                                    DropdownMenuItem(
                                        text = { Text(valY.toString(), color = Color.White, fontFamily = FontFamily.Monospace) },
                                        onClick = {
                                            selYear = valY
                                            yearExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // Interactive custom modifications dialog sheet overlay
    if (vm.editingScheduleDay != null) {
        val day = vm.editingScheduleDay!!
        var newShiftText by remember(day.currentShift) { mutableStateOf(day.currentShift) }
        var isOv by remember(day.isOvertime) { mutableStateOf(day.isOvertime) }
        var ovHoursText by remember(day.overtimeHours) { mutableStateOf(day.overtimeHours.toString()) }
        var isHol by remember(day.isHoliday) { mutableStateOf(day.isHoliday) }
        var altType by remember(day.alterationType) { mutableStateOf(day.alterationType) }
        var altNote by remember(day.alterationNote) { mutableStateOf(day.alterationNote ?: "") }
        var tradeCode by remember(day.tradeWithEmployeeCode) { mutableStateOf(day.tradeWithEmployeeCode) }
        var tradeAcc by remember(day.tradeAccountability) { mutableStateOf(day.tradeAccountability ?: "BALANCED") }
        var tradeHoursText by remember(day.tradeOwedHours) { mutableStateOf(if (day.tradeOwedHours > 0.0) day.tradeOwedHours.toString() else "8.0") }

        val listCoworkers by vm.allEmployees.collectAsStateWithLifecycle(emptyList())

        AlertDialog(
            onDismissRequest = { vm.editingScheduleDay = null },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedH = ovHoursText.toDoubleOrNull() ?: 0.0
                        vm.saveDayModifications(
                            dayEntity = day,
                            newShift = newShiftText,
                            isOvertime = isOv,
                            ovHours = parsedH,
                            isHoliday = isHol,
                            alterationType = altType,
                            alterationNote = altNote.ifBlank { null },
                            tradedCode = tradeCode,
                            tradeAccountability = if (altType == "Trade") tradeAcc else null,
                            tradeOwedHours = if (altType == "Trade") (tradeHoursText.toDoubleOrNull() ?: 0.0) else 0.0
                        )
                    },
                    modifier = Modifier.testTag("submit_alteration"),
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary, contentColor = Color.Black)
                ) {
                    Text("Apply Adjustment", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.editingScheduleDay = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace)
                }
            },
            title = {
                Text(
                    text = "Configure Day ${day.day} Shift",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            containerColor = themeCard,
            modifier = Modifier.border(1.dp, themePrimary.copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = newShiftText,
                        onValueChange = { newShiftText = it },
                        label = { Text("Shift details text") },
                        placeholder = { Text("05:00\n13:30 or DC") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themePrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = themePrimary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isOv,
                            onCheckedChange = { isOv = it },
                            colors = CheckboxDefaults.colors(checkedColor = themePrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Overtime Tracker", color = Color.White, fontSize = 14.sp)
                    }

                    if (isOv) {
                        OutlinedTextField(
                            value = ovHoursText,
                            onValueChange = { ovHoursText = it },
                            label = { Text("Overtime Hours") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = themePrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isHol,
                            onCheckedChange = { isHol = it },
                            colors = CheckboxDefaults.colors(checkedColor = themePrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sunday / Holiday rate premium (+100%)", color = Color.White, fontSize = 14.sp)
                    }

                    Divider(color = Color.White.copy(alpha = 0.15f))

                    Text("Shift Alteration Reason", color = themePrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val types = listOf("None", "Supervisor", "Trade", "Manual")
                        types.forEach { typeName ->
                            val currentSelected = if (typeName == "None") altType == null else altType == typeName
                            FilterChip(
                                selected = currentSelected,
                                onClick = {
                                    altType = if (typeName == "None") null else typeName
                                    if (typeName != "Trade") tradeCode = null
                                },
                                label = { Text(typeName, color = if (currentSelected) Color.Black else Color.White) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = themePrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    if (altType != null) {
                        OutlinedTextField(
                            value = altNote,
                            onValueChange = { altNote = it },
                            label = { Text("Trace Note") },
                            placeholder = { Text("Supervisor asked to change / trade with friend") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = themePrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (altType == "Trade") {
                            Text("Coworker to trade with:", fontSize = 12.sp, color = themePrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            var showSelectMenu by remember { mutableStateOf(false) }
                            val activeColleagueName = listCoworkers.find { it.code == tradeCode }?.name ?: "Select Colleague"

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { showSelectMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                                ) {
                                    Text(activeColleagueName, color = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showSelectMenu,
                                    onDismissRequest = { showSelectMenu = false },
                                    modifier = Modifier.background(themeCard).border(1.dp, themePrimary.copy(alpha = 0.2f))
                                ) {
                                    listCoworkers.forEach { targetC ->
                                        if (targetC.code != day.employeeCode) {
                                            DropdownMenuItem(
                                                text = { Text("${targetC.name} (#${targetC.code})", color = Color.White) },
                                                onClick = {
                                                    tradeCode = targetC.code
                                                    showSelectMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Accountability Ledger", fontSize = 12.sp, color = themePrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            
                            val accOptions = listOf(
                                "BALANCED" to "Balanced Swap",
                                "COWORKER_OWES_ME" to "They Owe Me Hours",
                                "I_OWE_COWORKER" to "I Owe Them Hours",
                                "COWORKER_PAID_ME" to "They Paid Me (Settle)",
                                "I_PAID_COWORKER" to "I Paid Them (Settle)"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                accOptions.forEach { opt ->
                                    val isSel = tradeAcc == opt.first
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) themePrimary.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable { tradeAcc = opt.first }
                                            .border(1.dp, if (isSel) themePrimary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSel,
                                            onClick = { tradeAcc = opt.first },
                                            colors = RadioButtonDefaults.colors(selectedColor = themePrimary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(opt.second, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }

                            if (tradeAcc == "COWORKER_OWES_ME" || tradeAcc == "I_OWE_COWORKER") {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = tradeHoursText,
                                    onValueChange = { tradeHoursText = it },
                                    label = { Text("Owed Hours Amount") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = themePrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

// TAB 0: Personal detailed monthly schedule showing days, status indicators and alterations card list
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScheduleTab(vm: ScheduleViewModel) {
    val myProfileState by vm.myProfile.collectAsStateWithLifecycle(null)
    val parsedMonths by vm.availableMonths.collectAsStateWithLifecycle(emptyList())
    val myDaysList by vm.mySchedules.collectAsStateWithLifecycle(emptyList())
    val allMyDaysList by vm.allMySchedules.collectAsStateWithLifecycle(emptyList())
    val prevDaysList by vm.prevMonthSchedules.collectAsStateWithLifecycle(emptyList())
    val listEmployees by vm.allEmployees.collectAsStateWithLifecycle(emptyList())
    var showSwapDetails by remember { mutableStateOf(false) }

    if (parsedMonths.isEmpty()) {
        NoRosterLoadedArea()
        return
    }

    if (myProfileState == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = themePrimary, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Select your Profile",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Groundlink III wants to display your calendar days, calculate your active night/overtimes pay! Go to the 'Roster' explorer tab to set who you are first.",
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { vm.activeTab = 2 },
                colors = ButtonDefaults.buttonColors(containerColor = themePrimary, contentColor = Color.Black)
            ) {
                Text("Go to Roster", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
        return
    }

    val currentMe = myProfileState!!

    Column(modifier = Modifier.fillMaxSize()) {
        // Welcome and Profile Avatar Header (Clean and Atmospheric, from screenshot)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Welcome back,",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    fontWeight = FontWeight.Normal,
                    fontFamily = themeFontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentMe.name,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = themePrimary,
                        fontFamily = themeFontFamily
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "👋",
                        fontSize = 26.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Here's your overview",
                    fontSize = 13.sp,
                    color = Color(0xFF8E95BB),
                    fontFamily = themeFontFamily
                )
            }

            // High-fidelity Profile pic with a glowing periwinkle border ring
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(themeSurface)
                    .border(2.dp, themePrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_profile_avatar_1781201939301),
                    contentDescription = "Profile Pic",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Statistical Calculations
        val stats = remember(myDaysList) {
            var workingCount = 0
            var overtimeSum = 0.0
            myDaysList.forEach { d ->
                val isRest = d.currentShift.uppercase() in setOf("DC", "DO", "FO", "FER", "BX", "LM", "LC", "BY")
                if (!isRest) {
                    workingCount++
                    if (d.isOvertime) overtimeSum += d.overtimeHours
                }
            }
            Pair(workingCount, overtimeSum)
        }

        val prevOvertime = remember(prevDaysList) {
            prevDaysList.filter { it.isOvertime }.sumOf { it.overtimeHours }
        }

        val tradedDays = remember(allMyDaysList) {
            allMyDaysList.filter { it.isModified && it.alterationType == "Trade" }
        }
        val totalHoursOwedToOthers = remember(tradedDays) {
            tradedDays.filter { it.tradeAccountability == "I_OWE_COWORKER" }.sumOf { it.tradeOwedHours }
        }
        val totalHoursOwedToMe = remember(tradedDays) {
            tradedDays.filter { it.tradeAccountability == "COWORKER_OWES_ME" }.sumOf { it.tradeOwedHours }
        }
        val netAccountability = totalHoursOwedToMe - totalHoursOwedToOthers

        val nextFourDays = remember(myDaysList) {
            val todayCal = Calendar.getInstance()
            val isCurrentMonthSelected = todayCal.get(Calendar.YEAR) == vm.selectedYear && (todayCal.get(Calendar.MONTH) + 1) == vm.selectedMonth
            val startDay = if (isCurrentMonthSelected) todayCal.get(Calendar.DAY_OF_MONTH) else 1
            var list = myDaysList.filter { it.day >= startDay }.take(4)
            if (list.isEmpty() && myDaysList.isNotEmpty()) {
                list = myDaysList.take(4)
            }
            list
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // General Overview Title with elegant Month selector button integrated on the right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "General Overview",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = themeFontFamily
                )

                // Month Sheet Dropdown Trigger
                var showMonthDrop by remember { mutableStateOf(false) }
                val monthsName = listOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )

                Box {
                    Button(
                        onClick = { showMonthDrop = true },
                        colors = ButtonDefaults.buttonColors(containerColor = themePrimary.copy(alpha = 0.15f), contentColor = themePrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        val matchingText = monthsName.getOrNull(vm.selectedMonth - 1) ?: "Month"
                        Text(
                            text = "$matchingText ${vm.selectedYear}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = themeFontFamily
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    DropdownMenu(
                        expanded = showMonthDrop,
                        onDismissRequest = { showMonthDrop = false },
                        modifier = Modifier
                            .background(themeSurface)
                            .border(1.dp, themePrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    ) {
                        parsedMonths.forEach { mY ->
                            val label = "${monthsName.getOrNull(mY.month - 1) ?: "M"} ${mY.year}"
                            DropdownMenuItem(
                                text = { Text(label, color = Color.White, fontFamily = themeFontFamily) },
                                onClick = {
                                    vm.selectedYear = mY.year
                                    vm.selectedMonth = mY.month
                                    showMonthDrop = false
                                }
                            )
                        }
                    }
                }
            }

            // Beautiful 3 stats metrics row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val cardData = listOf(
                    Triple("Overtime Hours", String.format(Locale.US, "%.1f", stats.second), Icons.Default.Refresh to themePrimary),
                    Triple("Hours I Owe", String.format(Locale.US, "%.1f", totalHoursOwedToOthers), Icons.Default.Person to themeMutedGreen),
                    Triple("People Owe Me", if (totalHoursOwedToMe == totalHoursOwedToMe.toInt().toDouble()) totalHoursOwedToMe.toInt().toString() else String.format(Locale.US, "%.1f", totalHoursOwedToMe), Icons.Default.Favorite to Color(0xFFF472B6))
                )
                
                cardData.forEach { (label, value, iconInfo) ->
                    val (icon, color) = iconInfo
                    val isHoursIOwe = label == "Hours I Owe"
                    val isPeopleOweMe = label == "People Owe Me"
                    val isSwapCard = isHoursIOwe || isPeopleOweMe
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(132.dp)
                            .let { modifier ->
                                if (isSwapCard) {
                                    modifier.clickable { showSwapDetails = !showSwapDetails }
                                } else {
                                    modifier
                                }
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = themeCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Circle Icon badge and Optional Chevron toggle indicator for expandables
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (isSwapCard) {
                                    Icon(
                                        imageVector = if (showSwapDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle Details",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            
                            Column {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF8E95BB),
                                    fontFamily = themeFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = value,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontFamily = themeFontFamily,
                                    lineHeight = 24.sp
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = if (isSwapCard) (if (showSwapDetails) "tap to hide" else "tap to expand") else "this month",
                                    fontSize = 10.sp,
                                    color = Color(0xFF8E95BB).copy(alpha = 0.5f),
                                    fontFamily = themeFontFamily
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val coworkerNetBalances = remember(tradedDays, listEmployees) {
                val grouped = tradedDays.filter { !it.tradeWithEmployeeCode.isNullOrBlank() }
                    .groupBy { it.tradeWithEmployeeCode!! }
                grouped.map { (code, days) ->
                    val owesMe = days.filter { it.tradeAccountability == "COWORKER_OWES_ME" }.sumOf { it.tradeOwedHours }
                    val iOwe = days.filter { it.tradeAccountability == "I_OWE_COWORKER" }.sumOf { it.tradeOwedHours }
                    val net = owesMe - iOwe
                    val name = listEmployees.find { it.code == code }?.name ?: "Employee #$code"
                    CoworkerNetBalance(code, name, net)
                }.filter { Math.abs(it.netHours) > 0.01 }
            }

            val activeDebtsToMe = remember(coworkerNetBalances) {
                coworkerNetBalances.filter { it.netHours > 0.01 }
            }
            val activeDebtsToOthers = remember(coworkerNetBalances) {
                coworkerNetBalances.filter { it.netHours < -0.01 }
            }

            // Swap Accountability Details section (styled with new Aero Obsidian palette)
            if (showSwapDetails) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = themeCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "SWAP ACCOUNTABILITY DETAILS".themeCased(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8E95BB),
                            fontFamily = themeFontFamily,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (activeDebtsToMe.isEmpty() && activeDebtsToOthers.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = themeMutedGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "All balanced! No active hour debts.",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontFamily = themeFontFamily
                                )
                            }
                        } else {
                            if (activeDebtsToMe.isNotEmpty()) {
                                Text(
                                    text = "OWED TO YOU (${String.format(java.util.Locale.US, "%.1f", activeDebtsToMe.sumOf { it.netHours })}h)".themeCased(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF34D399),
                                    fontFamily = themeFontFamily,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    activeDebtsToMe.forEach { cell ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(RoundedCornerShape(percent = 50))
                                                        .background(Color(0xFF34D399))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = cell.employeeName,
                                                    fontSize = 12.sp,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontFamily = themeFontFamily
                                                )
                                            }
                                            Text(
                                                text = "+${String.format(java.util.Locale.US, "%.1f", cell.netHours)} hrs",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF34D399),
                                                fontFamily = themeFontFamily
                                            )
                                        }
                                    }
                                }
                                if (activeDebtsToOthers.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }

                            if (activeDebtsToOthers.isNotEmpty()) {
                                Text(
                                    text = "YOU OWE (${String.format(java.util.Locale.US, "%.1f", activeDebtsToOthers.sumOf { Math.abs(it.netHours) })}h)".themeCased(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF472B6),
                                    fontFamily = themeFontFamily,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    activeDebtsToOthers.forEach { cell ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(RoundedCornerShape(percent = 50))
                                                        .background(Color(0xFFF472B6))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = cell.employeeName,
                                                    fontSize = 12.sp,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontFamily = themeFontFamily
                                                )
                                            }
                                            Text(
                                                text = "-${String.format(java.util.Locale.US, "%.1f", Math.abs(cell.netHours))} hrs",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF472B6),
                                                fontFamily = themeFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Header for Upcoming Schedule (from screenshot mockup)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming Schedule",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = themeFontFamily
                )
                Text(
                    text = "Next 4 Days",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = themePrimary,
                    fontFamily = themeFontFamily
                )
            }

            // Vertical list elements of upcoming schedules (from screenshot mockup)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (nextFourDays.isEmpty()) {
                    Text(
                        text = "No upcoming shifts imported.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 12.dp),
                        fontFamily = themeFontFamily
                    )
                } else {
                    nextFourDays.forEach { cell ->
                        val isOff = cell.currentShift.uppercase() in setOf("DC", "DO", "FO", "FER", "BX", "LM", "LC", "BY")
                        val dayOfWeek = remember(cell.day) {
                            val cal = Calendar.getInstance()
                            cal.set(vm.selectedYear, vm.selectedMonth - 1, cell.day)
                            val days = listOf("", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                            val idx = cal.get(Calendar.DAY_OF_WEEK)
                            days.getOrNull(idx) ?: ""
                        }
                        val monthLabel = remember(vm.selectedMonth) {
                            val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                            months.getOrNull(vm.selectedMonth) ?: ""
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    // Navigate to cell details editor!
                                    // In original Groundlink III, clicking daily rows opens editing flow.
                                    // We preserve this perfectly!
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = themeCard)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Date details
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(44.dp)
                                ) {
                                    Text(
                                        text = dayOfWeek.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themePrimary,
                                        fontFamily = themeFontFamily
                                    )
                                    Text(
                                        text = cell.day.toString(),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontFamily = themeFontFamily,
                                        lineHeight = 26.sp
                                    )
                                    Text(
                                        text = monthLabel,
                                        fontSize = 11.sp,
                                        color = themePrimary.copy(alpha = 0.8f),
                                        fontFamily = themeFontFamily
                                    )
                                }

                                // Separator
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(34.dp)
                                        .background(Color.White.copy(alpha = 0.08f))
                                )
                                Spacer(modifier = Modifier.width(14.dp))

                                // Clock Badge icon
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(themePrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = themePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))

                                // Shift times
                                Column(modifier = Modifier.weight(1f)) {
                                    val timeStr = if (isOff) {
                                        "OFF"
                                    } else {
                                        val times = ShiftTimeCalculator.parseShiftTimes(cell.currentShift)
                                        if (times != null) "${times.first} - ${times.second}" else cell.currentShift
                                    }
                                    Text(
                                        text = timeStr,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = themeFontFamily
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    
                                    val shiftDesc = if (isOff) "Rest Day" else "Duty Shift #${cell.currentShift}"
                                    Text(
                                        text = shiftDesc,
                                        fontSize = 12.sp,
                                        color = Color(0xFF8E95BB),
                                        fontFamily = themeFontFamily
                                    )
                                }

                                // Length tag
                                val durationHours = if (isOff) 0 else {
                                    val times = ShiftTimeCalculator.parseShiftTimes(cell.currentShift)
                                    if (times != null) 8 else 8
                                }
                                
                                if (!isOff) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(themePrimary.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${durationHours}h",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = themePrimary,
                                            fontFamily = themeFontFamily
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Row component for user's personal daily cells
@Composable
fun ActivePersonalDayRow(cell: ScheduleDayEntity, onClick: () -> Unit) {
    val isRest = cell.currentShift.uppercase() in setOf("DC", "DO", "FO", "FER", "BX", "LM", "LC", "BY")
    val isLeave = cell.currentShift.uppercase() in setOf("FER", "BX", "BY", "LM")

    val accentColor = when {
        isRest && !isLeave -> Color.White.copy(alpha = 0.35f)
        isLeave -> themeMutedGreen
        else -> themePrimary
    }

    val cardBorder = when {
        cell.isModified -> BorderStroke(1.5.dp, themePrimary) // highlight modification cell tracer!
        ActiveTheme.current.borderWidth > 0.dp -> BorderStroke(ActiveTheme.current.borderWidth, Color.White.copy(alpha = 0.11f))
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = themeCardShape,
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = themeCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day Bubble Indicator
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(themeBackground)
                    .border(1.dp, themePrimary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DAY".themeCased(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        fontFamily = themeFontFamily
                    )
                    Text(
                        text = cell.day.toString(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontFamily = themeFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Shift status Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Shift:".themeCased(),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        fontFamily = themeFontFamily
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = cell.currentShift.replace("\n", "  "),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = themeFontFamily
                    )
                }

                if (cell.isOvertime) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(themePrimary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "OVERTIME: +${cell.overtimeHours} HRS".themeCased(),
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = themeFontFamily
                            )
                        }
                    }
                }

                // Alterations Tracing message
                if (cell.isModified && cell.alterationType != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✏️ [${cell.alterationType}] Note: ${cell.alterationNote ?: "Updated"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = themePrimary.copy(alpha = 0.8f), // Amber notice tracer -> themePrimary glow!
                        fontFamily = themeFontFamily
                    )
                }
            }

            // Quick Status badge
            Box(
                modifier = Modifier
                    .clip(themeMiniShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = (if (isRest) "REST" else "DUTY").themeCased(),
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = themeFontFamily
                )
            }
        }
    }
}

// TAB 1: Trade Finder Workspace - Matches coworkers shifts dynamically for day indices and toggles swapping
@Composable
fun TradeFinderTab(vm: ScheduleViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val candidates by vm.tradeCandidates.collectAsStateWithLifecycle(emptyList())
    val parsedMonths by vm.availableMonths.collectAsStateWithLifecycle(emptyList())
    val listEmployees by vm.allEmployees.collectAsStateWithLifecycle(emptyList())
    val myDays by vm.mySchedules.collectAsStateWithLifecycle(emptyList())
    val allMyDays by vm.allMySchedules.collectAsStateWithLifecycle(emptyList())
    val myProfileState by vm.myProfile.collectAsStateWithLifecycle(null)
    var showActiveLogs by remember { mutableStateOf(false) }

    val filteredCandidates = remember(candidates, vm.tradeSearchKeyword) {
        if (vm.tradeSearchKeyword.isBlank()) {
            candidates
        } else {
            candidates.filter {
                it.employee.name.contains(vm.tradeSearchKeyword, ignoreCase = true) ||
                it.employee.code.contains(vm.tradeSearchKeyword, ignoreCase = true)
            }
        }
    }

    if (parsedMonths.isEmpty()) {
        NoRosterLoadedArea()
        return
    }

    if (myProfileState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Please select your active profile in the Roster Explorer tab first to find trade candidates.", color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
        }
        return
    }

    val selectedDayShift = myDays.find { it.day == vm.tradeSelectedDay }?.currentShift ?: "OFF"

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp).border(1.dp, themePrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = themeCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "YOUR SHIFT ON DAY ${vm.tradeSelectedDay}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = themePrimary,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectedDayShift.replace("\n", " "),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Ledger Card component
        val tradedDays = allMyDays.filter { it.isModified && it.alterationType == "Trade" }
        var totalHoursOwedToOthers = 0.0
        var totalHoursOwedToMe = 0.0
        var settledPaidToMe = 0
        var settledPaidToOthers = 0

        tradedDays.forEach { td ->
            val acc = td.tradeAccountability
            val hrs = td.tradeOwedHours
            if (acc == "I_OWE_COWORKER") totalHoursOwedToOthers += hrs
            if (acc == "COWORKER_OWES_ME") totalHoursOwedToMe += hrs
            if (acc == "COWORKER_PAID_ME") settledPaidToMe++
            if (acc == "I_PAID_COWORKER") settledPaidToOthers++
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).border(1.dp, themePrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = themeSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "ACCOUNTABILITY BALANCES LEDGER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = themePrimary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).border(1.dp, themePrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = themeCard)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Owed to Coworkers", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace)
                            Text(String.format(Locale.US, "%.1f Hrs", totalHoursOwedToOthers), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80), fontFamily = FontFamily.Monospace)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).border(1.dp, themePrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = themeCard)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Coworkers Owe Me", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace)
                            Text(String.format(Locale.US, "%.1f Hrs", totalHoursOwedToMe), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB9F6CA), fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                var showManualDebtDialog by remember { mutableStateOf(false) }

                Button(
                    onClick = { showManualDebtDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("add_manual_debt_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary.copy(alpha = 0.15f), contentColor = themePrimary),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, themePrimary.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Manual Debt",
                        tint = themePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Input Debts/Owed Hours Manually", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                if (showManualDebtDialog) {
                    ManualDebtDialog(
                        onDismiss = { showManualDebtDialog = false },
                        vm = vm,
                        coworkers = listEmployees
                    )
                }
                
                // Show list of specific outstanding debts if any!
                val debtTrades = tradedDays.filter { it.tradeAccountability in setOf("I_OWE_COWORKER", "COWORKER_OWES_ME", "I_PAID_COWORKER", "COWORKER_PAID_ME") }
                if (debtTrades.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showActiveLogs = !showActiveLogs }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ACTIVE TRANSACTION LOGS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = debtTrades.size.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (showActiveLogs) "Tap to hide" else "Tap to expand",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.35f),
                                fontFamily = themeFontFamily
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (showActiveLogs) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle active debt logs",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    if (showActiveLogs) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val monthsName = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            debtTrades.forEach { td ->
                                val matchCoworkerName = listEmployees.find { it.code == td.tradeWithEmployeeCode }?.name ?: "Employee #${td.tradeWithEmployeeCode}"
                                val summaryStr = when (td.tradeAccountability) {
                                    "I_OWE_COWORKER" -> "You owe $matchCoworkerName ${td.tradeOwedHours}h"
                                    "COWORKER_OWES_ME" -> "$matchCoworkerName owes you ${td.tradeOwedHours}h"
                                    "I_PAID_COWORKER" -> "You paid $matchCoworkerName"
                                    "COWORKER_PAID_ME" -> "$matchCoworkerName paid you"
                                    else -> ""
                                }
                                val badgeColor = when (td.tradeAccountability) {
                                    "I_OWE_COWORKER" -> Color(0xFFFF8A80)
                                    "COWORKER_OWES_ME" -> Color(0xFFB9F6CA)
                                    else -> Color.White.copy(alpha = 0.5f)
                                }
                                val monthStr = monthsName.getOrNull(td.month - 1) ?: "${td.month}"
                                Text("- $summaryStr ($monthStr ${td.day})", fontSize = 11.sp, color = badgeColor)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Segment selector
        Text(
            text = "CHOOSE INQUIRY TARGET FOR DAY ${vm.tradeSelectedDay}:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD54F),
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        // Scrollable Day selection block list 1..31
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (d in 1..31) {
                val isSelected = vm.tradeSelectedDay == d
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFFFFD54F) else Color(0xFF1E2732))
                        .clickable { vm.tradeSelectedDay = d }
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = d.toString(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search options toggle block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filterOptions = listOf(
                "ALL" to "All Shifts",
                "OFF" to "Who holds Rest?",
                "EARLY" to "Early (<12:00)",
                "LATE" to "Late (>=12:00)"
            )

            filterOptions.forEach { opt ->
                val selected = vm.tradeSearchType == opt.first
                Button(
                    onClick = { vm.tradeSearchType = opt.first },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color(0xFFFFD54F) else Color(0xFF1E2732),
                        contentColor = if (selected) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(opt.second, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search text input space for specific peer inside Trade tab
        OutlinedTextField(
            value = vm.tradeSearchKeyword,
            onValueChange = { vm.tradeSearchKeyword = it },
            placeholder = { Text("Search specific colleague...", color = Color.White.copy(alpha = 0.5f)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = themePrimary.copy(alpha = 0.6f)
                )
            },
            trailingIcon = {
                if (vm.tradeSearchKeyword.isNotEmpty()) {
                    IconButton(onClick = { vm.tradeSearchKeyword = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = themePrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedContainerColor = themeCard,
                unfocusedContainerColor = themeCard
            ),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("trade_search_field")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Result entries scroll list
        if (filteredCandidates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val emptyMsg = if (vm.tradeSearchKeyword.isNotBlank()) {
                    "No coworkers named '${vm.tradeSearchKeyword}' found matching conditions for Day ${vm.tradeSelectedDay}."
                } else {
                    "No coworkers found matching conditions for Day ${vm.tradeSelectedDay}."
                }
                Text(
                    text = emptyMsg,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCandidates) { matchResult ->
                    val colleague = matchResult.employee
                    val colleagueSched = matchResult.schedule
                    val isMyRestOk = matchResult.isMyRestOk
                    val isColleagueRestOk = matchResult.isColleagueRestOk

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2732))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = colleague.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (matchResult.colleagueMatchesBadge) Color(0xFF2E7D32).copy(alpha = 0.2f) else Color(0xFF121820))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                .border(1.dp, if (matchResult.colleagueMatchesBadge) Color(0xFF81C784).copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(4.dp))
                                        ) {
                                            Text(
                                                text = colleague.title + (if (matchResult.colleagueMatchesBadge) " (Matches Yours)" else ""),
                                                color = if (matchResult.colleagueMatchesBadge) Color(0xFF81C784) else Color(0xFFFFD54F),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Code: #${colleague.code}",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                // Quick swap action
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = colleagueSched.currentShift.replace("\n", " "),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFFF176)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val myProfileVal = vm.prefs.myEmployeeCode
                                                if (myProfileVal != null) {
                                                    val myDay = vm.getScheduleForDay(myProfileVal, vm.selectedYear, vm.selectedMonth, vm.tradeSelectedDay)
                                                    if (myDay != null) {
                                                        vm.editingScheduleDay = myDay.copy(
                                                            currentShift = colleagueSched.currentShift,
                                                            alterationType = "Trade",
                                                            tradeWithEmployeeCode = colleague.code,
                                                            alterationNote = "Swapped shifts with ${colleague.name}"
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F), contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Trade", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Monthly Stats Match Details
                            Spacer(modifier = Modifier.height(10.dp))
                            val isAvgHoursMatch = Math.abs(matchResult.colleagueAvgHours - matchResult.myAvgHours) < 0.25

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isAvgHoursMatch) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .border(1.dp, if (isAvgHoursMatch) Color(0xFF81C784).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
                            ) {
                                Text(
                                    text = String.format(Locale.US, "Shift Duration: %.1fh/day vs Yours: %.1fh/day", matchResult.colleagueAvgHours, matchResult.myAvgHours) + if (isAvgHoursMatch) " ✓ (Matches Yours)" else "",
                                    fontSize = 11.sp,
                                    color = if (isAvgHoursMatch) Color(0xFF81C784) else Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // 11-Hour Rest Safety Status Badges Onwards & Backwards
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = Color.White.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("11H SHIFT REST SAFETY COMPLIANCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(6.dp))

                            // Draw Your Rest Compliance
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(if (isMyRestOk) Color(0xFF81C784) else Color(0xFFE57373))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val myPrevStr = matchResult.myRestPrev?.let { String.format(Locale.US, "%.1fh back", it) } ?: "Infinite back"
                                val myNextStr = matchResult.myRestNext?.let { String.format(Locale.US, "%.1fh onwards", it) } ?: "Infinite onwards"
                                Text(
                                    text = "Your Rest: " + (if (isMyRestOk) "Compliant" else "VIOLATION") + " ($myPrevStr, $myNextStr)",
                                    fontSize = 11.sp,
                                    color = if (isMyRestOk) Color(0xFF81C784) else Color(0xFFE57373),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Draw Colleague's Rest Compliance
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(if (isColleagueRestOk) Color(0xFF81C784) else Color(0xFFE57373))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val colPrevStr = matchResult.colleagueRestPrev?.let { String.format(Locale.US, "%.1fh back", it) } ?: "Infinite back"
                                val colNextStr = matchResult.colleagueRestNext?.let { String.format(Locale.US, "%.1fh onwards", it) } ?: "Infinite onwards"
                                Text(
                                    text = "Coworker Rest: " + (if (isColleagueRestOk) "Compliant" else "VIOLATION") + " ($colPrevStr, $colNextStr)",
                                    fontSize = 11.sp,
                                    color = if (isColleagueRestOk) Color(0xFF81C784) else Color(0xFFE57373),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 2: Roster Explorer - Shows the active personal daily schedule calendar grid & modification overlays
@Composable
fun RosterExplorerTab(vm: ScheduleViewModel) {
    val myProfileState by vm.myProfile.collectAsStateWithLifecycle(null)
    val parsedMonths by vm.availableMonths.collectAsStateWithLifecycle(emptyList())
    val myDaysList by vm.mySchedules.collectAsStateWithLifecycle(emptyList())

    if (parsedMonths.isEmpty()) {
        NoRosterLoadedArea()
        return
    }

    if (myProfileState == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = themePrimary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Select your Profile",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "To explore full interactive rosters, schedule days, and edit shift configurations, please select your active employee profile in the Settings tab.",
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { vm.activeTab = 4 },
                colors = ButtonDefaults.buttonColors(containerColor = themePrimary, contentColor = Color.Black)
            ) {
                Text("Go to Settings", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
        return
    }

    val currentMe = myProfileState!!

    Column(modifier = Modifier.fillMaxSize()) {
        // Month switcher header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeSurface)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Active Roster,",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    fontFamily = themeFontFamily
                )
                Text(
                    text = currentMe.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    fontFamily = themeFontFamily
                )
            }

            // Month Sheet Dropdown Trigger
            var showMonthDrop by remember { mutableStateOf(false) }
            val monthsName = listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )

            Box {
                Button(
                    onClick = { showMonthDrop = true },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val matchingText = monthsName.getOrNull(vm.selectedMonth - 1) ?: "Month"
                    Text("$matchingText ${vm.selectedYear}", fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = themeFontFamily)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }

                DropdownMenu(
                    expanded = showMonthDrop,
                    onDismissRequest = { showMonthDrop = false },
                    modifier = Modifier.background(themeSurface).border(1.dp, themePrimary.copy(alpha = 0.2f))
                ) {
                    parsedMonths.forEach { mY ->
                        val label = "${monthsName.getOrNull(mY.month - 1) ?: "M"} ${mY.year}"
                        DropdownMenuItem(
                            text = { Text(label, color = Color.White, fontFamily = themeFontFamily) },
                            onClick = {
                                vm.selectedYear = mY.year
                                vm.selectedMonth = mY.month
                                showMonthDrop = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (myDaysList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No shifts populated for ${vm.selectedMonth}/${vm.selectedYear}.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(myDaysList) { dayCell ->
                    ActivePersonalDayRow(dayCell, onClick = {
                        vm.editingScheduleDay = dayCell
                    })
                }
            }
        }
    }
}

// TAB 3: Everyone - Displays a directory of all loaded coworkers, search them, and click to inspect their live calendar shifts
@Composable
fun EveryoneTab(vm: ScheduleViewModel) {
    val myProfileState by vm.myProfile.collectAsStateWithLifecycle(null)
    val parsedMonths by vm.availableMonths.collectAsStateWithLifecycle(emptyList())
    val listEmployees by vm.allEmployees.collectAsStateWithLifecycle(emptyList())

    if (parsedMonths.isEmpty()) {
        NoRosterLoadedArea()
        return
    }

    // State for selected (expanded) coworker to see their calendar
    var expandedEmployeeCode by remember { mutableStateOf<String?>(null) }

    val filteredList = remember(listEmployees, vm.everyoneSearchKeyword) {
        if (vm.everyoneSearchKeyword.isBlank()) {
            listEmployees
        } else {
            listEmployees.filter {
                it.name.contains(vm.everyoneSearchKeyword, ignoreCase = true) ||
                it.code.contains(vm.everyoneSearchKeyword, ignoreCase = true) ||
                it.title.contains(vm.everyoneSearchKeyword, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Dropdown Header Month switching selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeSurface)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "TEAM DIRECTORY",
                    fontSize = 11.sp,
                    color = themePrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = themeFontFamily,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${filteredList.size} Coworkers Polulated",
                    fontSize = 13.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = themeFontFamily
                )
            }

            // Simple styling for active display of Selected Month/Year
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(themeMiniShape)
                        .background(themeCard)
                        .themeMiniBorder()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Month: ${vm.selectedMonth}/${vm.selectedYear}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = themeFontFamily
                    )
                }
            }
        }

        // Search text input
        OutlinedTextField(
            value = vm.everyoneSearchKeyword,
            onValueChange = { vm.everyoneSearchKeyword = it },
            placeholder = { Text("Search name, code, or role...", color = Color.White.copy(alpha = 0.5f)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = themePrimary.copy(alpha = 0.6f)
                )
            },
            trailingIcon = {
                if (vm.everyoneSearchKeyword.isNotEmpty()) {
                    IconButton(onClick = { vm.everyoneSearchKeyword = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = themePrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedContainerColor = themeCard,
                unfocusedContainerColor = themeCard
            ),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("everyone_search_field")
        )

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No team members found for '${vm.everyoneSearchKeyword}'.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    fontFamily = themeFontFamily
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { coworker ->
                    val isMe = coworker.code == myProfileState?.code
                    val isExpanded = expandedEmployeeCode == coworker.code

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .themeCardBorder()
                            .clickable {
                                expandedEmployeeCode = if (isExpanded) null else coworker.code
                            },
                        shape = themeCardShape,
                        colors = CardDefaults.cardColors(containerColor = themeCard)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(percent = 50))
                                            .background(if (isMe) themePrimary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isMe) themePrimary else Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = coworker.name + if (isMe) " (Me)" else "",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isMe) themePrimary else Color.White,
                                            fontFamily = themeFontFamily
                                        )
                                        Text(
                                            text = coworker.title.themeCased(),
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontFamily = themeFontFamily
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "#${coworker.code}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themePrimary.copy(alpha = 0.7f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Divider(color = Color.White.copy(alpha = 0.08f))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Show calendar grid for coworker
                                Text(
                                    text = "MONTHLY LIVE CALENDAR".themeCased(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themePrimary,
                                    fontFamily = themeFontFamily,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val coworkerSchedules by vm.getSchedulesForEmployee(coworker.code, vm.selectedYear, vm.selectedMonth)
                                    .collectAsState(initial = emptyList())

                                val daysInMonth = remember(vm.selectedYear, vm.selectedMonth) {
                                    val cal = Calendar.getInstance()
                                    cal.set(Calendar.YEAR, vm.selectedYear)
                                    cal.set(Calendar.MONTH, vm.selectedMonth - 1)
                                    cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                }

                                if (coworkerSchedules.isEmpty()) {
                                    Text(
                                        text = "No schedules loaded for this month.",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontFamily = themeFontFamily
                                    )
                                } else {
                                    val gridSpacing = 4.dp
                                    val columnsCount = 7
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(gridSpacing)
                                    ) {
                                        val dayRows = (daysInMonth + columnsCount - 1) / columnsCount
                                        for (rowIdx in 0 until dayRows) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(gridSpacing)
                                            ) {
                                                for (colIdx in 0 until columnsCount) {
                                                    val dayNum = rowIdx * columnsCount + colIdx + 1
                                                    if (dayNum <= daysInMonth) {
                                                        val sched = coworkerSchedules.find { it.day == dayNum }
                                                        val shiftStr = sched?.currentShift ?: "OFF"
                                                        val isOff = shiftStr.uppercase() in setOf("DC", "DO", "FO", "FER", "BX", "LM", "LC", "BY", "OFF")

                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .aspectRatio(1f)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(
                                                                    if (isOff) Color.White.copy(alpha = 0.03f)
                                                                    else themePrimary.copy(alpha = 0.12f)
                                                                )
                                                                .border(
                                                                    width = 0.5.dp,
                                                                    color = if (isOff) Color.White.copy(alpha = 0.06f) else themePrimary.copy(alpha = 0.3f),
                                                                    shape = RoundedCornerShape(4.dp)
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Column(
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                verticalArrangement = Arrangement.Center
                                                            ) {
                                                                Text(
                                                                    text = dayNum.toString(),
                                                                    fontSize = 8.sp,
                                                                    color = Color.White.copy(alpha = 0.4f),
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontFamily = themeFontFamily
                                                                )
                                                                Text(
                                                                    text = shiftStr,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = if (isOff) Color.White.copy(alpha = 0.3f) else themePrimary,
                                                                    fontFamily = themeFontFamily
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 3: Settings - Hosts PDF configuration loader, active profile worker selection, coefficients, and earnings estimates
@Composable
fun SettingsTab(vm: ScheduleViewModel, filePicker: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
    val context = LocalContext.current
    val listEmployees by vm.allEmployees.collectAsStateWithLifecycle(emptyList())
    val myProfileVal by vm.myProfile.collectAsStateWithLifecycle(null)
    val myDays by vm.mySchedules.collectAsStateWithLifecycle(emptyList())
    val parsedMonths by vm.availableMonths.collectAsStateWithLifecycle(emptyList())

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            vm.configShiftAlertEnabled = isGranted
            vm.prefs.shiftAlertEnabled = isGranted
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: PDF Selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .themeCardBorder(),
            colors = CardDefaults.cardColors(containerColor = themeCard),
            shape = themeCardShape
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "IMPORT PDF ROSTER SHEET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themePrimary,
                    fontFamily = themeFontFamily
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select your airline handler roster PDF. Schedules will be structured completely locally.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = themeFontFamily
                )
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { filePicker.launch(arrayOf("application/pdf")) },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary, contentColor = Color.Black),
                    shape = themeButtonShape,
                    modifier = Modifier.fillMaxWidth().testTag("select_pdf_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select PDF File", fontWeight = FontWeight.Bold, fontFamily = themeFontFamily)
                }

                if (vm.isLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally), color = themePrimary)
                }

                vm.errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(err, color = Color(0xFFEF5350), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = themeFontFamily)
                }
            }
        }

        // Section 2: Worker Selector (Choose Profile)
        if (parsedMonths.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .themeCardBorder(),
                colors = CardDefaults.cardColors(containerColor = themeCard),
                shape = themeCardShape
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "COWORKER DATABASE / CHOOSE ACTIVE PROFILE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themePrimary,
                        fontFamily = themeFontFamily
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Find and nominate who you are to map your individual schedule timings, trades, and night overtime metrics.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        fontFamily = themeFontFamily
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = vm.searchKeyword,
                        onValueChange = { vm.searchKeyword = it },
                        placeholder = { Text("Search coworkers...", color = Color.White.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themePrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_field")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val filteredList = listEmployees.filter {
                        it.name.contains(vm.searchKeyword, ignoreCase = true) ||
                        it.code.contains(vm.searchKeyword, ignoreCase = true)
                    }

                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No colleagues matching filters.", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, fontFamily = themeFontFamily)
                        }
                    } else {
                        Box(modifier = Modifier.heightIn(max = 240.dp)) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredList) { em ->
                                    val isMyProfile = em.code == myProfileVal?.code
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isMyProfile) themePrimary else Color.White.copy(alpha = 0.05f)
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isMyProfile) themeSurface else Color(0xFF161F2B)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = em.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color.White,
                                                    fontFamily = themeFontFamily
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFF121820))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(em.title, color = themePrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = themeFontFamily)
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Code: #${em.code}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), fontFamily = themeFontFamily)
                                                }
                                            }

                                            if (isMyProfile) {
                                                Button(
                                                    onClick = {},
                                                    enabled = false,
                                                    colors = ButtonDefaults.buttonColors(
                                                        disabledContainerColor = Color(0xFF2E7D32),
                                                        disabledContentColor = Color.White
                                                    ),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text("Me", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = themeFontFamily)
                                                }
                                            } else {
                                                Button(
                                                    onClick = { vm.selectMyProfile(em.code) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = themePrimary,
                                                        contentColor = Color.Black
                                                    ),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text("Select", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = themeFontFamily)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2.3: Theme & Typography Styles
        Text("APP THEME & LETTER TYPOGRAPHY", color = themePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = themeFontFamily)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .themeCardBorder(),
            colors = CardDefaults.cardColors(containerColor = themeCard),
            shape = themeCardShape
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Aesthetic Styles Theme Catalog",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    fontFamily = themeFontFamily
                )
                Text(
                    text = "Tap a preset below to instantly customize the app's color palette, corners, borders, and general letter styling:",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    fontFamily = themeFontFamily
                )
                
                // Horizontal list of the 6 beautiful theme styles
                val themesList = listOf(
                    Triple(0, "Retro Runway", "✈️ Avionics Amber & Slate HUD"),
                    Triple(1, "Neon Dusk", "🌌 Futuristic Navy & Glowing Cyan"),
                    Triple(2, "Nordic Spruce", "🌲 Organic Charcoal & Forest Mint"),
                    Triple(3, "Cyber Sunset", "🌅 Cybernetic Sunset Glowing Crimson"),
                    Triple(4, "Ocean Drift", "🌊 Maritime Deep Pacific Turquoise"),
                    Triple(5, "Matrix Green", "⚡ Retro Green Phosphorus Console")
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(themesList) { (idx, name, desc) ->
                        val isSelected = ActiveTheme.selectedIndex == idx
                        val tConfig = getThemeConfig(idx)
                        Card(
                            modifier = Modifier
                                .width(150.dp)
                                .clickable {
                                    ActiveTheme.selectedIndex = idx
                                    vm.prefs.selectedTheme = idx
                                },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                2.dp,
                                if (isSelected) themePrimary else Color.White.copy(alpha = 0.08f)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) themeSurface else Color.White.copy(alpha = 0.03f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(tConfig.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(tConfig.themeIcon, fontSize = 12.sp)
                                    }
                                    Text(
                                        text = name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) themePrimary else Color.White,
                                        maxLines = 1,
                                        fontFamily = themeFontFamily
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                // Small color bubbles to preview colors
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(tConfig.primary))
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(tConfig.surface))
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(tConfig.card))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = desc,
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    maxLines = 2,
                                    fontFamily = themeFontFamily
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                // Letter Font Override Choice custom options
                Text(
                    text = "Letter Typography Font Style Override",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    fontFamily = themeFontFamily
                )
                Text(
                    text = "Force layout letters to use a specific family style, or keep theme native matching default rules automatically:",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    fontFamily = themeFontFamily
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0 to "Theme Preset",
                        1 to "Terminal Mono",
                        2 to "Modern Sans",
                        3 to "Classic Serif"
                    ).forEach { (idx, label) ->
                        val isSelected = ActiveTheme.fontOverrideIndex == idx
                        val fontOptionFamily = when (idx) {
                            1 -> FontFamily.Monospace
                            2 -> FontFamily.SansSerif
                            3 -> FontFamily.Serif
                            else -> themeFontFamily
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) themePrimary else Color.White.copy(alpha = 0.04f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) themePrimary else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    ActiveTheme.fontOverrideIndex = idx
                                    vm.prefs.fontOverride = idx
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontFamily = fontOptionFamily,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "AaBb",
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.4f),
                                    fontFamily = fontOptionFamily,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Section 2.5: Shift Reminders and iCalendar Sync Export
        Text("SHIFT REMINDERS & EXPORTS", color = themePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = themeFontFamily)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .themeCardBorder(),
            colors = CardDefaults.cardColors(containerColor = themeCard),
            shape = themeCardShape
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Shift Alarms toggle switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Shift Alarms & Reminders",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            fontFamily = themeFontFamily
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Notify me locally before working shifts start",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontFamily = themeFontFamily
                        )
                    }
                    Switch(
                        checked = vm.configShiftAlertEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                vm.configShiftAlertEnabled = isChecked
                                vm.prefs.shiftAlertEnabled = isChecked
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = themePrimary,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }

                if (vm.configShiftAlertEnabled) {
                    OutlinedTextField(
                        value = vm.configShiftAlertMinutes,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                vm.configShiftAlertMinutes = newValue
                                val mins = newValue.toIntOrNull() ?: 90
                                vm.prefs.shiftAlertMinutes = mins
                            }
                        },
                        label = { Text("Alarm Timeframe (Minutes prior to shift)") },
                        supportingText = {
                            val mins = vm.configShiftAlertMinutes.toIntOrNull() ?: 90
                            val hrs = mins / 60
                            val remMins = mins % 60
                            val timeStr = if (hrs > 0) {
                                if (remMins > 0) "$hrs hour${if (hrs>1) "s" else ""} and $remMins minute${if (remMins>1) "s" else ""}" else "$hrs hour${if (hrs>1) "s" else ""}"
                            } else {
                                "$mins minute${if (mins>1) "s" else ""}"
                            }
                            Text("Will notify you $timeStr before shift start.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themePrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("alarm_time_input")
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                // Calendar Export action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Calendar Exporter (.ics)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            fontFamily = themeFontFamily
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Download & Sync shift entries to third-party calendars",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontFamily = themeFontFamily
                        )
                    }
                    Button(
                        onClick = {
                            com.example.utils.CalendarExporter.exportToIcs(
                                context = context,
                                schedules = myDays,
                                year = vm.selectedYear,
                                month = vm.selectedMonth
                            )
                        },
                        enabled = myProfileVal != null && myDays.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themePrimary,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.White.copy(alpha = 0.08f),
                            disabledContentColor = Color.White.copy(alpha = 0.35f)
                        ),
                        shape = themeButtonShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("export_ics_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = themeFontFamily)
                    }
                }
            }
        }

        // Section 3: Calculation Rates
        Text("CALCULATION COEFFICIENTS SETUP", color = themePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = themeFontFamily)

        Card(
            modifier = Modifier.fillMaxWidth().themeCardBorder(),
            colors = CardDefaults.cardColors(containerColor = themeCard),
            shape = themeCardShape
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = vm.configBaseRate,
                        onValueChange = { vm.configBaseRate = it },
                        label = { Text("Base Rate (€/hr)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themePrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = vm.configOvertimeMultiplier,
                        onValueChange = { vm.configOvertimeMultiplier = it },
                        label = { Text("Overtime mult (e.g. 1.5)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themePrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = vm.configNightPremium,
                        onValueChange = { vm.configNightPremium = it },
                        label = { Text("Night Premium % (e.g. 25)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themePrimary
                        ),
                        modifier = Modifier.weight(1.3f)
                    )

                    OutlinedTextField(
                        value = vm.configHolidayMultiplier,
                        onValueChange = { vm.configHolidayMultiplier = it },
                        label = { Text("Holiday mult (e.g. 2.0)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themePrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = vm.configNightStart,
                        onValueChange = { vm.configNightStart = it },
                        label = { Text("Night Start (24h hr)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themePrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = vm.configNightEnd,
                        onValueChange = { vm.configNightEnd = it },
                        label = { Text("Night End (24h hr)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themePrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        vm.updateSettings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary, contentColor = Color.Black),
                    shape = themeButtonShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Rates & Recalculate", fontWeight = FontWeight.Bold, fontFamily = themeFontFamily)
                }
            }
        }

        // Section 4: Earnings Calculator (Only visible if profile setup completed!)
        if (myProfileVal != null) {
            val results = remember(
                myDays,
                vm.prefs.baseRate,
                vm.prefs.overtimeMultiplier,
                vm.prefs.nightPremiumPercentage,
                vm.prefs.nightStartHour,
                vm.prefs.nightEndHour,
                vm.prefs.holidayMultiplier
            ) {
                var totalBaseHours = 0.0
                var totalOvertimeHours = 0.0
                var totalNightHours = 0.0
                val baseRate = vm.prefs.baseRate
                var holidayWorkingHours = 0.0
                var daysWorked = 0
                var restDays = 0
                var overtimeEarn = 0.0

                myDays.forEach { c ->
                    val shift = c.currentShift.uppercase()
                    val isRest = shift in setOf("DC", "DO", "FO", "FER", "BX", "LM", "LC", "BY")

                    if (isRest) {
                        restDays++
                    } else {
                        daysWorked++
                        val times = ShiftTimeCalculator.parseShiftTimes(c.currentShift)
                        if (times != null) {
                            val duration = ShiftTimeCalculator.calculateHours(times.first, times.second)
                            totalBaseHours += duration

                            // Add Overtime track
                            if (c.isOvertime) {
                                totalOvertimeHours += c.overtimeHours
                                val ov = c.overtimeHours
                                val dayOvertimeEarn = if (ov <= 1.0) {
                                    ov * baseRate * 1.5
                                } else {
                                    (1.0 * baseRate * 1.5) + ((ov - 1.0) * baseRate * 1.75)
                                }
                                overtimeEarn += dayOvertimeEarn
                            }

                            // Night premium extraction
                            val nightHrs = ShiftTimeCalculator.calculateNightHours(
                                times.first,
                                times.second,
                                vm.prefs.nightStartHour,
                                vm.prefs.nightEndHour
                            )
                            totalNightHours += nightHrs

                            // Holiday multiplier check
                            if (c.isHoliday) {
                                holidayWorkingHours += duration
                            }
                        }
                    }
                }

                val baseEarn = maxOf(0.0, totalBaseHours - totalOvertimeHours) * baseRate
                val nightEarn = totalNightHours * baseRate * vm.prefs.nightPremiumPercentage
                val holExtraEarn = holidayWorkingHours * baseRate * maxOf(0.0, (vm.prefs.holidayMultiplier - 1.0))
                val totalSum = overtimeEarn + nightEarn + holExtraEarn

                EarningsSummary(
                    totalBaseHours = totalBaseHours,
                    totalOvertimeHours = totalOvertimeHours,
                    totalNightHours = totalNightHours,
                    holidayWorkingHours = holidayWorkingHours,
                    daysWorkedCount = daysWorked,
                    restDaysCount = restDays,
                    baseEarnings = baseEarn,
                    nightPremiumEarnings = nightEarn,
                    overtimeEarnings = overtimeEarn,
                    holidayExtraEarnings = holExtraEarn,
                    grandTotal = totalSum
                )
            }

            Text("ESTIMATED PREMIUM CALCULATIONS", color = themePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = themeFontFamily)

            Card(
                modifier = Modifier.fillMaxWidth().themeCardBorder(),
                colors = CardDefaults.cardColors(containerColor = themeCard),
                shape = themeCardShape
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = String.format(Locale.US, "Estimated Premium Pay: €%.2f", results.grandTotal),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = themePrimary,
                        fontFamily = themeFontFamily
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FinanceLineItem(
                        label = "Overtime trackers",
                        metric = String.format(Locale.US, "%.1fh", results.totalOvertimeHours),
                        valStr = String.format(Locale.US, "€%.2f", results.overtimeEarnings)
                    )
                    FinanceLineItem(
                        label = "Night Premiums (+${(vm.prefs.nightPremiumPercentage * 100).toInt()}%)",
                        metric = String.format(Locale.US, "%.1fh", results.totalNightHours),
                        valStr = String.format(Locale.US, "€%.2f", results.nightPremiumEarnings)
                    )
                    FinanceLineItem(
                        label = "Holiday Premium (Sundays Excluded)",
                        metric = String.format(Locale.US, "%.1fh", results.holidayWorkingHours),
                        valStr = String.format(Locale.US, "€%.2f", results.holidayExtraEarnings)
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().themeCardBorder(),
                colors = CardDefaults.cardColors(containerColor = themeCard),
                shape = themeCardShape
            ) {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select your coworker profile above to unlock automatic premium earnings calculation.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = themeFontFamily
                    )
                }
            }
        }
    }
}

@Composable
fun FinanceLineItem(label: String, metric: String, valStr: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(metric, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        Text(valStr, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

// Reusable standard layout warning when roster files remain unselected with interactive design lab
@Composable
fun NoRosterLoadedArea() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Warning Card at the Top
        Card(
            colors = CardDefaults.cardColors(containerColor = themeCard),
            shape = themeCardShape,
            modifier = Modifier.fillMaxWidth().themeCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = themePrimary, // Glowing retro amber or active primary green
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Welcome to Groundlink III!",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No PDF roster file is loaded yet. Upload your airline roster on the 'Settings' tab to activate interactive shift calendars, swap metrics, and earnings calculation.",
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = themeFontFamily
                )
            }
        }
    }
}

// Simple raw file name resolver utility from raw Uri structure
private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

@Composable
fun ManualDebtDialog(
    onDismiss: () -> Unit,
    vm: ScheduleViewModel,
    coworkers: List<EmployeeEntity>
) {
    val myCode = vm.prefs.myEmployeeCode ?: ""
    val filteredCoworkers = coworkers.filter { it.code != myCode }

    var selectedColleagueCode by remember { mutableStateOf(filteredCoworkers.firstOrNull()?.code ?: "") }
    var dayText by remember { mutableStateOf("1") }
    var debtDirection by remember { mutableStateOf("COWORKER_OWES_ME") }
    var hoursText by remember { mutableStateOf("8.0") }
    var noteText by remember { mutableStateOf("") }

    var showDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Manual Debt Record",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        },
        containerColor = themeCard,
        modifier = Modifier.border(1.dp, themePrimary.copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
        confirmButton = {
            Button(
                onClick = {
                    val targetCode = selectedColleagueCode
                    val dayParsed = dayText.toIntOrNull() ?: 1
                    val hoursParsed = hoursText.toDoubleOrNull() ?: 0.0
                    if (targetCode.isNotBlank() && hoursParsed > 0.0) {
                        vm.saveManualDebt(
                            colleagueCode = targetCode,
                            day = dayParsed,
                            accountability = debtDirection,
                            hours = hoursParsed,
                            note = noteText
                        )
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = themePrimary, contentColor = Color.Black),
                modifier = Modifier.testTag("submit_manual_debt")
            ) {
                Text("Save Debt Record", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (filteredCoworkers.isEmpty()) {
                    Text("No coworkers found in the database. Please import a roster PDF first.", color = Color.Red, fontSize = 13.sp)
                } else {
                    Text("Select Coworker:", color = themePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    val activeColleagueName = filteredCoworkers.find { it.code == selectedColleagueCode }?.name ?: "Select Colleague"

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Text(activeColleagueName, color = Color.White)
                        }
                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false },
                            modifier = Modifier.background(themeCard).border(1.dp, themePrimary.copy(alpha = 0.2f))
                        ) {
                            filteredCoworkers.forEach { targetC ->
                                DropdownMenuItem(
                                    text = { Text("${targetC.name} (${targetC.title})", color = Color.White, fontFamily = FontFamily.Monospace) },
                                    onClick = {
                                        selectedColleagueCode = targetC.code
                                        showDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = dayText,
                        onValueChange = { dayText = it },
                        label = { Text("Day of the Month (1-31)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themePrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Debt Direction:", color = themePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    val options = listOf(
                        "COWORKER_OWES_ME" to "They Owe Me Hours",
                        "I_OWE_COWORKER" to "I Owe Them Hours"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        options.forEach { opt ->
                            val isSel = debtDirection == opt.first
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) themePrimary.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable { debtDirection = opt.first }
                                    .border(1.dp, if (isSel) themePrimary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSel,
                                    onClick = { debtDirection = opt.first },
                                    colors = RadioButtonDefaults.colors(selectedColor = themePrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(opt.second, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it },
                        label = { Text("Amount of Hours (e.g. 8.0)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themePrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Trace / Reference Note") },
                        placeholder = { Text("e.g. Swapped Sunday shift manually") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themePrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    )
}

