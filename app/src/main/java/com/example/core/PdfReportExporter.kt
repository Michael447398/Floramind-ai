package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.BotanicalReport
import java.io.File
import java.io.FileOutputStream

object PdfReportExporter {

    fun buildPrivacySafeShareSummary(report: BotanicalReport): String = buildString {
        appendLine("FloraMind AI — Botanical Research Summary")
        appendLine("==========================================")
        appendLine("Common Name: ${report.identity.commonName}")
        if (report.identity.scientificName.isNotBlank()) {
            appendLine("Scientific Name: ${report.identity.scientificName}")
        }
        if (report.identity.family.isNotBlank()) {
            appendLine("Family: ${report.identity.family}")
        }
        appendLine("Identification Confidence: ${report.confidenceTier.name}")
        appendLine("Safety Status: ${report.safetyLevel.name}")
        if (report.safetyHeadline.isNotBlank()) {
            appendLine("Safety Notice: ${report.safetyHeadline}")
        }
        appendLine()
        appendLine("Summary:")
        appendLine(report.summary)
        appendLine()
        if (report.deepResearch.sources.isNotEmpty()) {
            appendLine("Deep Research Sources (${report.deepResearch.retrievedAtIso}):")
            report.deepResearch.sources.filter { it.url.isNotBlank() }.forEach { src ->
                appendLine("- ${src.sourceName} [${src.statusLabel}]: ${src.url}")
            }
            appendLine()
        }
        appendLine(BotanicalConstants.SAFETY_DISCLAIMER)
    }

    fun shareSummaryText(context: Context, report: BotanicalReport) {
        val text = buildPrivacySafeShareSummary(report)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "FloraMind AI Report: ${report.identity.commonName}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Botanical Report"))
    }

    fun exportAndSharePdf(context: Context, report: BotanicalReport): Result<File> = runCatching {
        val document = PdfDocument()
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points
        val margin = 42f
        val maxWidth = pageWidth - (margin * 2)

        val titlePaint = Paint().apply {
            color = android.graphics.Color.rgb(20, 61, 43)
            textSize = 19f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val headingPaint = Paint().apply {
            color = android.graphics.Color.rgb(20, 61, 43)
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = android.graphics.Color.rgb(32, 38, 34)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val alertPaint = Paint().apply {
            color = android.graphics.Color.rgb(166, 43, 43)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin + 16f

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - margin) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = margin + 10f
            }
        }

        fun drawWrapped(text: String, paint: Paint, lineSpacing: Float = 15f) {
            if (text.isBlank()) return
            val words = text.split(" ")
            var line = StringBuilder()
            for (word in words) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    line = StringBuilder(candidate)
                } else {
                    ensureSpace(lineSpacing)
                    canvas.drawText(line.toString(), margin, y, paint)
                    y += lineSpacing
                    line = StringBuilder(word)
                }
            }
            if (line.isNotEmpty()) {
                ensureSpace(lineSpacing)
                canvas.drawText(line.toString(), margin, y, paint)
                y += lineSpacing
            }
        }

        fun drawSection(title: String, lines: List<Pair<String, String>>) {
            val nonEmpty = lines.filter { it.second.isNotBlank() }
            if (nonEmpty.isEmpty()) return
            y += 8f
            ensureSpace(24f)
            canvas.drawText(title, margin, y, headingPaint)
            y += 16f
            for ((label, value) in nonEmpty) {
                drawWrapped("$label: $value", bodyPaint)
            }
        }

        canvas.drawText("FloraMind AI — Botanical Research Dossier", margin, y, titlePaint)
        y += 22f
        drawWrapped("${report.identity.commonName} (${report.identity.scientificName})", headingPaint)
        drawWrapped("Confidence: ${report.confidenceTier.name} | Safety: ${report.safetyLevel.name} | Evidence: ${report.deepResearch.evidenceQuality.name}", bodyPaint)
        y += 6f
        if (report.safetyHeadline.isNotBlank()) {
            drawWrapped("SAFETY NOTICE: ${report.safetyHeadline}", alertPaint)
        }
        drawWrapped(report.summary, bodyPaint)

        drawSection(
            "A. Identity & Classification",
            listOf(
                "Family" to report.identity.family,
                "Genus / Species" to "${report.identity.genus} ${report.identity.species}".trim(),
                "Taxonomy" to listOf(report.identity.kingdom, report.identity.phylum, report.identity.order).filter { it.isNotBlank() }.joinToString(" > "),
                "Regional Names" to report.identity.regionalNames.joinToString(", "),
                "Synonyms" to report.identity.synonyms.joinToString(", ")
            )
        )
        drawSection(
            "B. Physical Characteristics",
            listOf(
                "Observed in Photo" to report.physical.observedInPhoto,
                "General Appearance" to report.physical.generalAppearance,
                "Leaves" to report.physical.leafMorphology,
                "Flowers" to report.physical.flowersAndSeason,
                "Distinguishing Traits" to report.physical.distinguishingTraits
            )
        )
        drawSection(
            "C. Habitat & Distribution",
            listOf(
                "Native Range" to report.habitat.nativeRange,
                "Regions" to report.habitat.countriesAndRegions,
                "Climate & Soil" to "${report.habitat.climateAndTemperature} ${report.habitat.soilRequirements}".trim()
            )
        )
        drawSection(
            "D & E. Uses & Benefits",
            listOf(
                "Food & Agriculture" to "${report.uses.foodAndNutrition} ${report.uses.agricultureAndHorticulture}".trim(),
                "Traditional & Scientific" to "${report.uses.traditionalUses} ${report.benefits.establishedScientific}".trim()
            )
        )
        drawSection(
            "F & G. Toxicity, Safety & Pharmacology",
            listOf(
                "Human Toxicity" to report.safety.humanToxicity,
                "Pets & Livestock" to report.safety.petsAndLivestockToxicity,
                "Toxic Parts" to report.safety.toxicPartsAndCompounds,
                "Clinical Evidence" to report.medicinal.humanClinicalEvidence,
                "Contraindications" to report.safety.contraindications
            )
        )
        drawSection(
            "H & I. Cultivation & Ecology",
            listOf(
                "Cultivation" to "${report.cultivation.plantingAndPropagation} ${report.cultivation.wateringAndLight}".trim(),
                "Conservation & Ecology" to "${report.ecology.conservationStatus} ${report.ecology.pollinatorsAndWildlife}".trim()
            )
        )
        drawSection(
            "K. Deep Research Sources (${report.deepResearch.retrievedAtIso})",
            report.deepResearch.sources.map { it.sourceName to "${it.statusLabel} ${it.url}".trim() }
        )

        y += 10f
        drawWrapped(BotanicalConstants.SAFETY_DISCLAIMER, alertPaint)

        document.finishPage(page)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = report.identity.scientificName.ifBlank { report.identity.commonName }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(40)
        val outFile = File(exportDir, "FloraMind_${safeName}.pdf")
        FileOutputStream(outFile).use { stream ->
            document.writeTo(stream)
        }
        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "FloraMind AI Botanical Report: ${report.identity.commonName}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Botanical Report PDF"))
        outFile
    }
}
