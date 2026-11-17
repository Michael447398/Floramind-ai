package com.example.data.research

import com.example.model.ResearchSourceRecord
import com.example.model.SourceAuthorityTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class KewPowoSourceProvider : BotanicalSourceProvider {
    override val sourceId: String = "kew_powo"
    override val displayName: String = "Plants of the World Online (RBG Kew)"
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
        val apiUrl = "https://powo.science.kew.org/api/2/search?q=$encoded"
        val webSearchUrl = "https://powo.science.kew.org/?q=$encoded"

        try {
            val request = Request.Builder().url(apiUrl).get().build()
            BotanicalHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ResearchSourceRecord(
                        sourceId = sourceId,
                        sourceName = displayName,
                        authorityTier = authorityTier.name,
                        retrieved = false,
                        statusLabel = "HTTP ${response.code}",
                        summary = "Kew POWO search returned HTTP ${response.code}; use link to inspect manually.",
                        url = webSearchUrl
                    )
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val results = json.optJSONArray("results")
                if (results == null || results.length() == 0) {
                    return@withContext ResearchSourceRecord(
                        sourceId = sourceId,
                        sourceName = displayName,
                        authorityTier = authorityTier.name,
                        retrieved = true,
                        statusLabel = "No POWO record found",
                        summary = "No vascular plant entry returned by Kew POWO for \"$cleanName\".",
                        url = webSearchUrl
                    )
                }

                // Prefer exact name match if present
                var chosen = results.optJSONObject(0)
                for (i in 0 until results.length()) {
                    val candidate = results.optJSONObject(i) ?: continue
                    if (candidate.optString("name", "").equals(cleanName, ignoreCase = true)) {
                        chosen = candidate
                        break
                    }
                }

                val name = chosen?.optString("name", cleanName) ?: cleanName
                val author = chosen?.optString("author", "") ?: ""
                val family = chosen?.optString("family", "") ?: ""
                val accepted = chosen?.optBoolean("accepted", true) ?: true
                val fqId = chosen?.optString("fqId", "") ?: ""
                val relUrl = chosen?.optString("url", "") ?: ""
                val fullUrl = when {
                    relUrl.startsWith("http") -> relUrl
                    relUrl.isNotBlank() -> "https://powo.science.kew.org$relUrl"
                    fqId.isNotBlank() -> "https://powo.science.kew.org/taxon/$fqId"
                    else -> webSearchUrl
                }

                val synObj = chosen?.optJSONObject("synonymOf")
                val acceptedName = if (!accepted && synObj != null) {
                    synObj.optString("name", name)
                } else {
                    name
                }
                val taxStatus = if (accepted) "ACCEPTED" else "SYNONYM of $acceptedName"

                ResearchSourceRecord(
                    sourceId = sourceId,
                    sourceName = displayName,
                    authorityTier = authorityTier.name,
                    retrieved = true,
                    statusLabel = if (accepted) "Verified Accepted" else "Synonym Mapped",
                    acceptedScientificName = acceptedName,
                    taxonomicStatus = taxStatus,
                    family = family,
                    summary = "Kew POWO: $name $author (Family: ${family.ifBlank { "Unknown" }}, Status: $taxStatus).".trim(),
                    url = fullUrl
                )
            }
        } catch (e: Exception) {
            ResearchSourceRecord(
                sourceId = sourceId,
                sourceName = displayName,
                authorityTier = authorityTier.name,
                retrieved = false,
                statusLabel = "Lookup unavailable",
                summary = e.localizedMessage ?: "Network timeout querying Kew POWO.",
                url = webSearchUrl
            )
        }
    }
}
