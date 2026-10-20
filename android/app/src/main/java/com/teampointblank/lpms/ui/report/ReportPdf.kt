package com.teampointblank.lpms.ui.report

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.teampointblank.lpms.data.LearnerProgress
import com.teampointblank.lpms.domain.Formatters
import com.teampointblank.lpms.domain.ReportLogic
import java.io.File
import java.io.FileOutputStream

/** Draws a learner progress report onto A4 pages with Android's built-in PdfDocument (no extra libraries). */
object ReportPdf {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40f
    private const val BOTTOM = PAGE_H - 50f

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(20, 70, 120)
    }
    private val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(20, 70, 120)
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.BLACK }
    private val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK
    }
    private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = Color.DKGRAY }
    private val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

    /** Creates the PDF in the app's cache folder and returns the file. Throws on I/O failure. */
    fun create(context: Context, progress: LearnerProgress, target: Int, today: String = Formatters.today().toString()): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }           // keep the cache small
        val learner = progress.learner
        val file = File(dir, ReportLogic.fileName(learner.firstName, learner.lastName, today))

        val doc = PdfDocument()
        try {
            val writer = PageWriter(doc)
            writeContent(writer, progress, target, today)
            writer.finish()
            FileOutputStream(file).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
        return file
    }

    private fun writeContent(w: PageWriter, progress: LearnerProgress, target: Int, today: String) {
        val learner = progress.learner
        w.line("LPMS Learner Progress Report", titlePaint, gap = 6f)
        w.line("READ Educational Trust - Literacy Programme Management System", mutedPaint, gap = 14f)

        w.pair("Learner", Formatters.fullName(learner.firstName, learner.lastName))
        w.pair("Grade", learner.grade)
        w.pair("School", learner.schoolName)
        w.pair("Report date", Formatters.shortDate(today))
        w.space(10f)

        w.line("Summary", headingPaint, gap = 6f)
        w.pair("Average score", Formatters.score(learner.averageScore))
        w.pair("Progress", progress.progressLabel.ifBlank { "-" })
        w.pair("Assessments recorded", learner.assessmentCount.toString())
        w.pair("Attendance rate", Formatters.percent(learner.attendanceRate))
        w.pair("Your attendance expectation", "$target%")
        w.wrapped(ReportLogic.targetMessage(learner.attendanceRate, target), boldPaint)
        w.space(10f)

        w.line("Assessments", headingPaint, gap = 6f)
        if (progress.assessments.isEmpty()) {
            w.line("No assessments have been recorded yet.", bodyPaint)
        } else {
            w.row(listOf("Date" to 0f, "Score" to 100f, "Recorded by" to 160f, "Notes" to 290f), boldPaint)
            w.rule()
            progress.assessments.forEach { a ->
                w.row(
                    listOf(
                        Formatters.shortDate(a.assessmentDate) to 0f,
                        Formatters.score(a.assessmentScore) to 100f,
                        a.recordedBy to 160f,
                        (a.notes ?: "") to 290f
                    ),
                    bodyPaint,
                    clipWidths = listOf(95f, 55f, 125f, PAGE_W - MARGIN * 2 - 290f)
                )
            }
        }
        w.space(10f)

        w.line("Attendance history (most recent first)", headingPaint, gap = 6f)
        if (progress.attendance.isEmpty()) {
            w.line("No attendance has been recorded yet.", bodyPaint)
        } else {
            w.row(listOf("Date" to 0f, "Programme" to 100f, "Status" to 400f), boldPaint)
            w.rule()
            progress.attendance.forEach { a ->
                w.row(
                    listOf(
                        Formatters.shortDate(a.sessionDate) to 0f,
                        a.programmeName to 100f,
                        a.status to 400f
                    ),
                    bodyPaint,
                    clipWidths = listOf(95f, 290f, 100f)
                )
            }
        }
        w.space(14f)
        w.wrapped(
            "Generated by the LPMS app. Figures come from the attendance and assessment records held by READ Educational Trust.",
            mutedPaint
        )
    }

    /** Small helper that lays text down the page and starts a new page when it runs out of room. */
    private class PageWriter(private val doc: PdfDocument) {
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var y = MARGIN

        init { newPage() }

        private fun newPage() {
            finishPage()
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
            y = MARGIN
        }

        private fun finishPage() {
            val current = page ?: return
            current.canvas.drawText("Page $pageNumber", PAGE_W - MARGIN - 40f, PAGE_H - 24f, mutedPaint)
            doc.finishPage(current)
            page = null
        }

        fun finish() = finishPage()

        private fun ensure(height: Float) {
            if (y + height > BOTTOM) newPage()
        }

        fun space(h: Float) { y += h }

        fun line(text: String, paint: Paint, gap: Float = 4f) {
            val h = paint.textSize + gap
            ensure(h)
            y += paint.textSize
            page?.canvas?.drawText(text, MARGIN, y, paint)
            y += gap
        }

        fun pair(label: String, value: String) {
            ensure(bodyPaint.textSize + 4f)
            y += bodyPaint.textSize
            page?.canvas?.drawText(label, MARGIN, y, boldPaint)
            page?.canvas?.drawText(clip(value, bodyPaint, PAGE_W - MARGIN * 2 - 180f), MARGIN + 180f, y, bodyPaint)
            y += 4f
        }

        fun wrapped(text: String, paint: Paint) {
            val width = PAGE_W - MARGIN * 2
            for (part in wrap(text, paint, width)) line(part, paint)
        }

        fun row(cells: List<Pair<String, Float>>, paint: Paint, clipWidths: List<Float>? = null) {
            ensure(paint.textSize + 5f)
            y += paint.textSize
            cells.forEachIndexed { i, (text, x) ->
                val shown = if (clipWidths != null) clip(text, paint, clipWidths[i]) else text
                page?.canvas?.drawText(shown, MARGIN + x, y, paint)
            }
            y += 5f
        }

        fun rule() {
            page?.canvas?.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
            y += 4f
        }

        private fun clip(text: String, paint: Paint, width: Float): String {
            if (paint.measureText(text) <= width) return text
            var end = text.length
            while (end > 0 && paint.measureText(text.substring(0, end) + "...") > width) end--
            return text.substring(0, end) + "..."
        }

        private fun wrap(text: String, paint: Paint, width: Float): List<String> {
            val lines = mutableListOf<String>()
            var current = ""
            for (word in text.split(" ")) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= width) {
                    current = candidate
                } else {
                    if (current.isNotEmpty()) lines.add(current)
                    current = word
                }
            }
            if (current.isNotEmpty()) lines.add(current)
            return lines
        }
    }
}
