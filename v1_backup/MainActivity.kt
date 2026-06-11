package com.example

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.EmployeeSchedule
import com.example.utils.PDFParser
import java.io.File
import java.io.FileOutputStream

// State manager holding parse and sharing logic
class ScheduleViewModel : ViewModel() {
    var searchKeyword by mutableStateOf("")
    var parsedEmployees by mutableStateOf<List<EmployeeSchedule>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isSharing by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var selectedFileName by mutableStateOf<String?>(null)

    private val job = kotlinx.coroutines.SupervisorJob()
    private val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + job)

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

    fun parsePdf(context: Context, uri: Uri, fileName: String) {
        coroutineScope.launch(Dispatchers.IO) {
            isLoading = true
            errorMessage = null
            selectedFileName = fileName
            try {
                val results = PDFParser.parseRosterPdf(context, uri)
                parsedEmployees = results
                if (results.isEmpty()) {
                    errorMessage = "No schedule rows could be extracted. Please make sure the PDF contains columns of Title, Sequence indicator, Employee Name, 4-digit Code, and Calendar dates."
                }
            } catch (e: Exception) {
                errorMessage = "Failed parsing the document programmatically: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun shareCsv(context: Context) {
        if (parsedEmployees.isEmpty()) return
        isSharing = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val csvContent = PDFParser.convertToCsv(parsedEmployees)
                val cacheFile = File(context.cacheDir, "schedule_report.csv")
                FileOutputStream(cacheFile).use { fos ->
                    fos.write(csvContent.toByteArray())
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cacheFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Roster Schedule CSV Report")
                    putExtra(Intent.EXTRA_TEXT, "Parsed schedule report extracted using Schedule Extractor app.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Share CSV")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSharing = false
            }
        }
    }

    fun clearData() {
        parsedEmployees = emptyList()
        selectedFileName = null
        errorMessage = null
        searchKeyword = ""
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    ScheduleExtractorDashboard(
                        paddingValues = innerPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleExtractorDashboard(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    vm: ScheduleViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // File picker contract launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                // Quick resolution of readable display name
                val name = getFileNameFromUri(context, it) ?: "Roster.pdf"
                vm.parsePdf(context, it, name)
            }
        }
    )

    Column(
        modifier = modifier
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Schedule Extractor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            actions = {
                if (vm.parsedEmployees.isNotEmpty()) {
                    IconButton(
                        onClick = { vm.clearData() },
                        modifier = Modifier.testTag("clear_data")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear and parse new file",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )

        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

        if (vm.parsedEmployees.isEmpty()) {
            // Roster Landing Area (when no file is loaded yet)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header graphic styling
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Roster input",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Convert Company Roster to CSV",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Import multi-page PDF shift rosters containing employee rows (Title, Sequence, Name, 4-digit Code, and shift schedule cells). All extraction works programmatically offline with standard high-performance parsing.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Interactive Primary Call to Action Button
                Button(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp)
                        .testTag("select_pdf_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Select PDF File", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (vm.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }

                vm.errorMessage?.let { errorMsg ->
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error message",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = errorMsg,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Dashboard with schedule details loaded
            Column(modifier = Modifier.fillMaxSize()) {
                // Loaded metadata overview file header card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Checked file icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ROSTER LOADED SUCCESSFULLY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = vm.selectedFileName ?: "Selected PDF Roster",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Extracted ${vm.parsedEmployees.size} worker schedule entries perfectly.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // Interactive Filters and Options Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = vm.searchKeyword,
                        onValueChange = { vm.searchKeyword = it },
                        placeholder = { Text("Filter by worker name...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (vm.searchKeyword.isNotEmpty()) {
                                IconButton(onClick = { vm.searchKeyword = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Reset filter", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_field")
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Secondary action to export & share CSV report
                    Button(
                        onClick = { vm.shareCsv(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("export_csv_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export CSV", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable details list representing workers
                val filteredList = vm.parsedEmployees.filter {
                    it.name.contains(vm.searchKeyword, ignoreCase = true) ||
                    it.title.contains(vm.searchKeyword, ignoreCase = true) ||
                    it.code.contains(vm.searchKeyword, ignoreCase = true)
                }

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No worker matches found for: '${vm.searchKeyword}'",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList) { worker ->
                            WorkerScheduleRow(worker)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerScheduleRow(worker: EmployeeSchedule) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Worker Header (Role badges, index tracking and Names)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Role branding badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(getRoleColor(worker.title))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = worker.title,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Index sequence ID track
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = worker.sequence,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Employee Name
                Text(
                    text = worker.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Employee Code tracker badge
                Text(
                    text = "#${worker.code}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Calendar daily schedule blocks scrollable row
            Text(
                text = "SHIFT SCHEDULES (${worker.dailySchedules.size} DAYS):",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                worker.dailySchedules.forEachIndexed { index, schedule ->
                    DayScheduleBlock(dayIndex = index + 1, schedule = schedule)
                }
            }
        }
    }
}

// Adaptive widget visual block for each day
@Composable
fun DayScheduleBlock(dayIndex: Int, schedule: String) {
    // Coloring code categorization: rest hours (DC, DO, FO) vs leaves vs active hours
    val isRest = schedule.uppercase() in setOf("DC", "DO", "FO")
    val isLeave = schedule.uppercase() in setOf("FER", "BX", "LM", "LC", "BY")
    
    val containerColor = when {
        isRest -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        isLeave -> Color(0xFFE8F5E9) // soft light green background
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    }

    val contentColor = when {
        isRest -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        isLeave -> Color(0xFF2E7D32) // deep forest green text
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    val outlineColor = when {
        isRest -> MaterialTheme.colorScheme.outlineVariant
        isLeave -> Color(0xFFC8E6C9)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    }

    val linesCount = schedule.count { it == '\n' } + 1
    val calculatedHeight = when (linesCount) {
        1 -> 58.dp
        2 -> 72.dp
        else -> 86.dp
    }

    Column(
        modifier = Modifier
            .width(76.dp)
            .height(calculatedHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .border(1.dp, outlineColor, RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Day index indicator
        Text(
            text = "Day $dayIndex",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.61f)
        )

        // Shift block hours or category text
        Text(
            text = schedule,
            fontSize = if (linesCount > 1) 9.sp else 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = contentColor,
            maxLines = 4,
            lineHeight = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Distinct badge colors per employee operational roles
private fun getRoleColor(title: String): Color {
    val up = title.uppercase()
    return when {
        up.contains("TDA") -> Color(0xFF1E88E5) // beautiful blue
        up.contains("OPS") -> Color(0xFFD81B60) // beautiful pinkish red
        up.contains("LST") -> Color(0xFF8E24AA) // purple
        up.contains("SUP") -> Color(0xFFF57C00) // warning amber orange
        up.contains("CSA") -> Color(0xFF00897B) // charming teal
        else -> Color(0xFF5D4037) // brown
    }
}

// Simple query content helper to fetch the raw file name from an SAF Uri
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
