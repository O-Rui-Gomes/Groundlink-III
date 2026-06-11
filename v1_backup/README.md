# Schedule Extractor - Version 1.0 Backup

This directory contains a pristine backup of Version 1 of the Roster Schedule Extractor.

If you ever wish to restore the app to this exact version, simply copy these files over their respective original locations in the main project structure:

*   **`MainActivity.kt`** -> Copy to `/app/src/main/java/com/example/MainActivity.kt`
*   **`PDFParser.kt`** -> Copy to `/app/src/main/java/com/example/utils/PDFParser.kt`

## Key Capabilities of Version 1

1.  **High-Performance OCR & Text Alignment Grid System**: Implemented entirely offline using Android's PDFBox layout engine to map daily columns mathematically and read shift cells correctly.
2.  **Clean Material 3 Jetpack Compose Dashboard**: Displays full employee lists with filterable names, colorful role indicators, and styled scrollable calendars showing shift details.
3.  **Local CSV Report Generation & Share Sharing Sheet Integration**: High-fidelity CSV export with full formatting validation.
