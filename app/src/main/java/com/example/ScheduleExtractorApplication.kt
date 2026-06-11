package com.example

import android.app.Application
import com.example.db.AppDatabase
import com.example.db.RosterRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class ScheduleExtractorApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { RosterRepository(database.employeeDao(), database.scheduleDayDao()) }

    override fun onCreate() {
        super.onCreate()
        // Initialize the PDFBox-Android context right at app startup
        PDFBoxResourceLoader.init(this)
    }
}

