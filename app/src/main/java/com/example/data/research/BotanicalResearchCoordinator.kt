package com.example.data.research

import android.util.LruCache
import com.example.model.ConfidenceTier
import com.example.model.DeepResearchBundle
import com.example.model.EvidenceQuality
import com.example.model.ResearchSourceRecord
import com.example.model.SourceAuthorityTier
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class BotanicalResearchCoordinator(
    private val gbifProvider: GbifSourceProvider = GbifSourceProvider(),
    // Modular registry: add or replace BotanicalSourceProvider adapters here in future updates.
    private val providers: List<BotanicalSourceProvider> = listOf(
        gbifProvider,
        KewPowoSourceProvider(),
        WorldFloraOnlineSourceProvider(),
        PubMedSourceProvider(),
        WikipediaSupplementaryProvider()
    )
) {
    private val cache = LruCache<String, DeepResearchBundle>(40)

    suspend fun verifySpecies(
        scientificName: String,
        commonName: String?,
        confidenceTier: ConfidenceTier
    ): DeepResearchBundle {
        val cleanSci = scientificName.trim()
        val nowIso = currentUtcIso()

        if (cleanSci.isBlank() || confidenceTier == ConfidenceTier.NON_PLANT) {
            return DeepResearchBundle(
                retrievedAtIso = nowIso,
                evidenceQuality = EvidenceQuality.INSUFFICIENT,
                evidenceRationale = "Identification did not yield a confident Latin binomial to verify against external botanical databases.",
                discrepanciesNote = "",
                sources = emptyList()
            )
        }

        val cacheKey = cleanSci.lowercase(Locale.US)
        cache.get(cacheKey)?.let { cached ->
            return cached
        }

        val results: List<ResearchSourceRecord> = supervisorScope {
            providers.map { provider ->
                async {
                    withTimeoutOrNull(9_000L) {
                        provider.querySpecies(cleanSci, commonName)
                    } ?: ResearchSourceRecord(
                        sourceId = provider.sourceId,
                        sourceName = provider.displayName,
                        authorityTier = provider.authorityTier.name,
                        retrieved = false,
                        statusLabel = "Timed out"
                    )
                }
            }.awaitAll()
        }

        val primaryVerified = results.filter {
            it.authorityTier == SourceAuthorityTier.PRIMARY_DATABASE.name &&
                it.retrieved &&
                it.acceptedScientificName.isNotBlank()
        }
        val pubMedHit = results.firstOrNull {
            it.sourceId == "pubmed" &&
                it.retrieved &&
                !it.statusLabel.startsWith("0 ")
        }

        val acceptedNames = primaryVerified.map {
            it.acceptedScientificName.lowercase(Locale.US).trim()
        }.distinct()

        val discrepanciesNote = when {
            acceptedNames.size > 1 ->
                "Taxonomic note: Primary authorities returned differing accepted names or synonym mappings (${primaryVerified.joinToString("; ") { "${it.sourceName}: ${it.acceptedScientificName}" }})."
            primaryVerified.any { it.taxonomicStatus.contains("SYNONYM", ignoreCase = true) } ->
                "Synonym note: One or more botanical databases treat \"$cleanSci\" as a synonym of ${acceptedNames.firstOrNull() ?: cleanSci}."
            else -> ""
        }

        val evidenceQuality = when {
            confidenceTier == ConfidenceTier.UNCERTAIN -> EvidenceQuality.LIMITED
            primaryVerified.size >= 2 && pubMedHit != null -> EvidenceQuality.STRONG
            primaryVerified.isNotEmpty() -> EvidenceQuality.MODERATE
            results.any { it.retrieved } -> EvidenceQuality.LIMITED
            else -> EvidenceQuality.INSUFFICIENT
        }

        val rationale = buildString {
            append("${primaryVerified.size} primary taxonomic database(s) verified \"$cleanSci\"")
            if (pubMedHit != null) {
                append("; peer-reviewed literature retrieved (${pubMedHit.statusLabel})")
            } else {
                append("; no PubMed literature index matched or query was unavailable")
            }
            if (confidenceTier == ConfidenceTier.UNCERTAIN) {
                append(". Visual identification from the photo remains uncertain.")
            } else {
                append(".")
            }
        }

        val bundle = DeepResearchBundle(
            retrievedAtIso = nowIso,
            evidenceQuality = evidenceQuality,
            evidenceRationale = rationale,
            discrepanciesNote = discrepanciesNote,
            sources = results
        )
        if (primaryVerified.isNotEmpty()) {
            cache.put(cacheKey, bundle)
        }
        return bundle
    }

    suspend fun searchLivePlantae(query: String): Result<List<GbifSearchEntry>> =
        gbifProvider.searchPlantae(query)

    private fun currentUtcIso(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}
