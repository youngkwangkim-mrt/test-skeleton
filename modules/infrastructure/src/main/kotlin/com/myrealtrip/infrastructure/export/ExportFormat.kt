package com.myrealtrip.infrastructure.export

/**
 * Export 포맷
 */
enum class ExportFormat(
    val extension: String,
    val contentType: String,
) {
    /** Excel 2007+ (.xlsx) */
    EXCEL(
        extension = "xlsx",
        contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    ),

    /** CSV (.csv) */
    CSV(
        extension = "csv",
        contentType = "text/csv",
    ),

    /** CSV with BOM for Excel compatibility (.csv) */
    CSV_EXCEL(
        extension = "csv",
        contentType = "text/csv; charset=UTF-8",
    ),
}
