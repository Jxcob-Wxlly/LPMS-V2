package com.teampointblank.lpms.ui.report

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.teampointblank.lpms.data.LearnerProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Builds the PDF and opens the Android share sheet (save to Files/Drive, WhatsApp, email, print...).
 * Returns null on success or a friendly error message; it never throws.
 */
suspend fun exportAndShareReport(context: Context, progress: LearnerProgress, target: Int): String? {
    return try {
        val file = withContext(Dispatchers.IO) { ReportPdf.create(context, progress, target) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "LPMS progress report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share or save the report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        null
    } catch (e: Exception) {
        "Could not create the report. Please try again."
    }
}
