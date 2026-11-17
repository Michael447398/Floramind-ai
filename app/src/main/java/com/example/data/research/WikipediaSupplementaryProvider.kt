package com.example.data.research

import com.example.model.ResearchSourceRecord
import com.example.model.SourceAuthorityTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class WikipediaSupplementaryProvider : BotanicalSourceProvider {
    override val sourceId: String = "wikipedia_supplementary"
    override val displayName: String = "Wikipedia (Supplementary Background Only)"
    override val authorityTier: SourceAuthorityTier = SourceAuthorityTier.SUPPLEMENTARY_ENCYCLOPEDIA

    override suspend fun querySpecies(
        scientificName: String,
        commonName: String?
    ): ResearchSourceRecord = withContext(Dispatchers.IO) {
        val target = scientificName.trim().ifBlank { commonName?.trim().orEmpty() }
        if (target.isBlank()) {
            return@withContext ResearchSourceRecord(
                sourceId = sourceId,
                sourceName = displayName,
                authorityTier = authorityTier.name,
                retrieved = false,
                statusLabel = "Skipped"
            )
        }

        val slug = URLEncoder.encode(target.replace(" ", "_"), "UTF-8")
        val apiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/$slug"

        try {
            val req = Request.Builder().url(apiUrl).get().build()
            BotanicalHttpClient.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext ResearchSourceRecord(
                        sourceId = sourceId,
                        sourceName = displayName,
                        authorityTier = authorityTier.name,
                        retrieved = false,
                        statusLabel = "Not found (HTTP ${resp.code})",
                        summary = "No supplementary Wikipedia summary page matched \"$target\".",
                        url = "https://en.wikipedia.org/wiki/Special:Search?search=$slug"
                    )
                }
                val json = JSONObject(resp.body?.string().orEmpty())
                val extract = json.optString("extract", "").take(380)
                val pageUrl = json.optJSONObject("content_urls")
                    ?.optJSONObject("desktop")
                    ?.optString("page")
                    ?: "https://en.wikipedia.org/wiki/$slug"
                val timestamp = json.optString("timestamp", "")

                ResearchSourceRecord(
                    sourceId = sourceId,
                    sourceName = displayName,
                    authorityTier = authorityTier.name,
                    retrieved = true,
                    statusLabel = "Supplementary Context",
                    summary = "[Not primary scientific authority] $extract",
                    url = pageUrl,
                    publicationDate = timestamp.take(10)
                )
            }
        } catch (e: Exception) {
            ResearchSourceRecord(
                sourceId = sourceId,
                sourceName = displayName,
                authorityTier = authorityTier.name,
                retrieved = false,
                statusLabel = "Unavailable",
                url = "https://en.wikipedia.org/wiki/$slug"
            )
        }
    }
}
