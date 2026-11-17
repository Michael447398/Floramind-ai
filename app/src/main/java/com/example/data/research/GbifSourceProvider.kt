package com.example.data.research

import com.example.model.ResearchSourceRecord
import com.example.model.SourceAuthorityTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

data class GbifSearchEntry(
    val usageKey: Long,
    val scientificName: String,
    val canonicalName: String,
    val vernacularName: String,
    val family: String,
    val rank: String,
    val taxonomicStatus: String
)

class GbifSourceProvider : BotanicalSourceProvider {
    override val sourceId: String = "gbif"
    override val displayName: String = "GBIF Backbone Taxonomy"
    override val authorityTier: SourceAuthorityTier = SourceAuthorityTier.PRIMARY_DATABASE

    override suspend fun querySpecies(
        scientificName: String,
        commonName: String?
    ): ResearchSourceRecord = withContext(Dispatchers.IO) {
        val cleanName = scientificName.trim()
        if (cleanName.isBlank()) {
            return@withContext ResearchSourceRecord(
                sourceId = sourceId,
                sourceName = displayName,
                authorityTier = authorityTier.name,
                retrieved = false,
                statusLabel = "Skipped (no scientific name)"
            )
        }

        val encoded = URLEncoder.encode(cleanName, "UTF-8")
        val url = "https://api.gbif.org/v1/species/match?name=$encoded&kingdom=Plantae&verbose=true"
        val fallbackSearchUrl = "https://www.gbif.org/species/search?q=$encoded&highertaxon_key=6"

        try {
            val request = Request.Builder().url(url).get().build()
            BotanicalHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ResearchSourceRecord(
                        sourceId = sourceId,
                        sourceName = displayName,
                        authorityTier = authorityTier.name,
                        retrieved = false,
                        statusLabel = "HTTP ${response.code}",
                        url = fallbackSearchUrl
                    )
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val matchType = json.optString("matchType", "NONE")
                val usageKey = json.optLong("usageKey", 0L)
                if (matchType == "NONE" || usageKey == 0L) {
                    return@withContext ResearchSourceRecord(
                        sourceId = sourceId,
                        sourceName = displayName,
                        authorityTier = authorityTier.name,
                        retrieved = true,
                        statusLabel = "No exact GBIF match",
                        summary = "GBIF Backbone returned no Plantae match for \"$cleanName\".",
                        url = fallbackSearchUrl
                    )
                }

                val canonical = json.optString("canonicalName", cleanName)
                val fullSciName = json.optString("scientificName", canonical)
                val status = json.optString("status", "UNKNOWN")
                val family = json.optString("family", "")
                val order = json.optString("order", "")
                val confidence = json.optInt("confidence", 0)
                val rank = json.optString("rank", "")
                val speciesUrl = "https://www.gbif.org/species/$usageKey"

                val synonyms = fetchSynonyms(usageKey)

                ResearchSourceRecord(
                    sourceId = sourceId,
                    sourceName = displayName,
                    authorityTier = authorityTier.name,
                    retrieved = true,
                    statusLabel = "$matchType ($status, $confidence% match)",
                    acceptedScientificName = canonical,
                    taxonomicStatus = status,
                    family = family,
                    synonyms = synonyms,
                    summary = "GBIF matched \"$fullSciName\" (Rank: $rank, Order: $order, Family: $family, Status: $status).",
                    url = speciesUrl
                )
            }
        } catch (e: Exception) {
            ResearchSourceRecord(
                sourceId = sourceId,
                sourceName = displayName,
                authorityTier = authorityTier.name,
                retrieved = false,
                statusLabel = "Lookup unavailable",
                summary = e.localizedMessage ?: "Network timeout querying GBIF.",
                url = fallbackSearchUrl
            )
        }
    }

    private fun fetchSynonyms(usageKey: Long): List<String> {
        return try {
            val req = Request.Builder()
                .url("https://api.gbif.org/v1/species/$usageKey/synonyms?limit=5")
                .get()
                .build()
            BotanicalHttpClient.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val json = JSONObject(resp.body?.string().orEmpty())
                val arr = json.optJSONArray("results") ?: return emptyList()
                (0 until arr.length()).mapNotNull { idx ->
                    arr.optJSONObject(idx)?.optString("canonicalName")?.takeIf { it.isNotBlank() }
                }.distinct()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun searchPlantae(query: String): Result<List<GbifSearchEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = query.trim()
            if (clean.isBlank()) return@runCatching emptyList()
            val encoded = URLEncoder.encode(clean, "UTF-8")
            val req = Request.Builder()
                .url("https://api.gbif.org/v1/species/search?q=$encoded&highertaxonKey=6&status=ACCEPTED&limit=18")
                .get()
                .build()
            BotanicalHttpClient.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("GBIF search failed with HTTP ${resp.code}")
                }
                val json = JSONObject(resp.body?.string().orEmpty())
                val arr = json.optJSONArray("results") ?: return@use emptyList()
                (0 until arr.length()).mapNotNull { i ->
                    val item = arr.optJSONObject(i) ?: return@mapNotNull null
                    val key = item.optLong("key", 0L)
                    val canonical = item.optString("canonicalName", "").ifBlank {
                        item.optString("scientificName", "")
                    }
                    if (key == 0L || canonical.isBlank()) return@mapNotNull null
                    val vernacularArr = item.optJSONArray("vernacularNames")
                    val vernacular = (0 until (vernacularArr?.length() ?: 0))
                        .mapNotNull { vIdx -> vernacularArr?.optJSONObject(vIdx)?.optString("vernacularName") }
                        .firstOrNull { it.isNotBlank() }
                        ?: ""
                    GbifSearchEntry(
                        usageKey = key,
                        scientificName = item.optString("scientificName", canonical),
                        canonicalName = canonical,
                        vernacularName = vernacular,
                        family = item.optString("family", ""),
                        rank = item.optString("rank", "SPECIES"),
                        taxonomicStatus = item.optString("taxonomicStatus", "ACCEPTED")
                    )
                }.distinctBy { it.canonicalName }
            }
        }
    }
}
