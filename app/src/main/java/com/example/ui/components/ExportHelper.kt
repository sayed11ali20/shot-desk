package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.ProjectWithShots
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.localizeShotType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    fun buildPlainTextScript(
        projectWithShots: ProjectWithShots,
        strings: AppStrings = EnglishStrings,
    ): String {
        val project = projectWithShots.project
        val shots = projectWithShots.activeShots
        val locale = if (strings.isArabic) Locale("ar") else Locale.US
        val dateStr = SimpleDateFormat("MMM dd, yyyy", locale).format(Date(project.createdAtMillis))

        return buildString {
            appendLine("==========================================")
            appendLine(if (strings.isArabic) "تفصيل إنتاج SHOTDESK" else "SHOTDESK PRODUCTION BREAKDOWN")
            appendLine("==========================================")
            appendLine("${strings.projectTitleLabel}: ${project.title}")
            appendLine(strings.createdDateLabel(dateStr))
            appendLine(strings.shotsCountPill(shots.size))
            appendLine(strings.estRuntimeLabel(formatDuration(projectWithShots.totalDurationSeconds)))
            appendLine("${strings.filmedCountLabel(projectWithShots.completedCount)} / ${shots.size}")
            appendLine()

            if (shots.isEmpty()) {
                appendLine(strings.emptyBreakdownText)
            } else {
                shots.forEachIndexed { index, shot ->
                    val statusTag = if (shot.isCompleted) "[${strings.filmedBadge}]" else "[${strings.statusInProgress}]"
                    val shotTypeLabel = localizeShotType(shot.shotType, strings)
                    appendLine("------------------------------------------")
                    appendLine("${index + 1}. ${shot.shotNumber}  |  $shotTypeLabel  |  ${shot.durationSeconds}s  $statusTag")
                    if (shot.visualDescription.isNotBlank()) {
                        appendLine("   ${strings.fieldVisuals}: ${shot.visualDescription}")
                    }
                    if (shot.dialogueScript.isNotBlank()) {
                        appendLine("   ${strings.fieldScript}: ${shot.dialogueScript}")
                    }
                    if (shot.vfxNotes.isNotBlank()) {
                        appendLine("   ${strings.fieldVfx}: ${shot.vfxNotes}")
                    }
                    if (shot.sfxMusicNotes.isNotBlank()) {
                        appendLine("   ${strings.fieldSfx}: ${shot.sfxMusicNotes}")
                    }
                    if (shot.directorsNotes.isNotBlank()) {
                        appendLine("   ${strings.fieldNotes}: ${shot.directorsNotes}")
                    }
                }
            }
            appendLine("------------------------------------------")
        }
    }

    fun shareTextScript(
        context: Context,
        projectWithShots: ProjectWithShots,
        strings: AppStrings = EnglishStrings,
    ) {
        val text = buildPlainTextScript(projectWithShots, strings)
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = sanitizeFilename(projectWithShots.project.title)
        val txtFile = File(exportsDir, "${safeName}_shot_list.txt")
        txtFile.writeText(text, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            txtFile,
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${projectWithShots.project.title} - ShotDesk")
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, strings.exportTextAction))
    }

    fun exportAndSharePdf(
        context: Context,
        projectWithShots: ProjectWithShots,
        strings: AppStrings = EnglishStrings,
    ) {
        val project = projectWithShots.project
        val shots = projectWithShots.activeShots
        val locale = if (strings.isArabic) Locale("ar") else Locale.US
        val dateStr = SimpleDateFormat("MMM dd, yyyy", locale).format(Date(project.createdAtMillis))

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val contentWidth = pageWidth - margin * 2

        val titlePaint = Paint().apply {
            color = Color.rgb(20, 23, 26)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(90, 98, 108)
            textSize = 11f
            isAntiAlias = true
        }
        val shotHeaderPaint = Paint().apply {
            color = Color.rgb(154, 116, 20)
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.rgb(45, 52, 60)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(45, 52, 60)
            textSize = 10.5f
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            color = Color.rgb(215, 212, 202)
            strokeWidth = 1f
        }

        var pageNum = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas
        var y = margin + 12f

        fun startNewPage() {
            document.finishPage(page)
            pageNum++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            canvas = page.canvas
            y = margin + 12f
        }

        fun drawWrappedLine(prefix: String, text: String, paint: Paint) {
            val fullText = if (prefix.isNotBlank()) "$prefix: $text" else text
            val words = fullText.split(Regex("\\s+"))
            var line = ""
            for (word in words) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) > contentWidth) {
                    if (y > pageHeight - margin - 20f) startNewPage()
                    canvas.drawText(line, margin, y, paint)
                    y += 15f
                    line = word
                } else {
                    line = candidate
                }
            }
            if (line.isNotEmpty()) {
                if (y > pageHeight - margin - 20f) startNewPage()
                canvas.drawText(line, margin, y, paint)
                y += 15f
            }
        }

        canvas.drawText(project.title, margin, y, titlePaint)
        y += 18f
        canvas.drawText(
            "ShotDesk  •  ${strings.createdDateLabel(dateStr)}  •  ${strings.shotsAndDurationSummary(shots.size, formatDuration(projectWithShots.totalDurationSeconds))}",
            margin,
            y,
            subtitlePaint,
        )
        y += 12f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 20f

        if (shots.isEmpty()) {
            canvas.drawText(strings.emptyBreakdownText, margin, y, bodyPaint)
        } else {
            shots.forEachIndexed { idx, shot ->
                if (y > pageHeight - margin - 100f) startNewPage()
                val statusLabel = if (shot.isCompleted) " [${strings.filmedBadge}]" else ""
                val shotTypeLabel = localizeShotType(shot.shotType, strings)
                canvas.drawText(
                    "${idx + 1}. ${shot.shotNumber}  —  $shotTypeLabel (${shot.durationSeconds}s)$statusLabel",
                    margin,
                    y,
                    shotHeaderPaint,
                )
                y += 16f

                if (shot.visualDescription.isNotBlank()) drawWrappedLine(strings.fieldVisuals, shot.visualDescription, bodyPaint)
                if (shot.dialogueScript.isNotBlank()) drawWrappedLine(strings.fieldScript, shot.dialogueScript, bodyPaint)
                if (shot.vfxNotes.isNotBlank()) drawWrappedLine(strings.fieldVfx, shot.vfxNotes, bodyPaint)
                if (shot.sfxMusicNotes.isNotBlank()) drawWrappedLine(strings.fieldSfx, shot.sfxMusicNotes, bodyPaint)
                if (shot.directorsNotes.isNotBlank()) drawWrappedLine(strings.fieldNotes, shot.directorsNotes, labelPaint)

                y += 6f
                canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                y += 16f
            }
        }

        document.finishPage(page)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = sanitizeFilename(project.title)
        val pdfFile = File(exportsDir, "${safeName}_production_sheet.pdf")
        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile,
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, "${project.title} - ShotDesk PDF")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, strings.exportPdfAction))
    }

    private fun sanitizeFilename(raw: String): String {
        return raw.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "shotdesk_project" }
    }
}
