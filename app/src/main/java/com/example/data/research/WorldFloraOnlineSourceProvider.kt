package com.example.data.research

import com.example.model.ResearchSourceRecord
import com.example.model.SourceAuthorityTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class WorldFloraOnlineSourceProvider : BotanicalSourceProvider {
    override val sourceId: String = "wfo"
    override val displayName: String = "World Flora Online (WFO)"
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
        val matchUrl = "https://list.worldfloraonline.org/matching_rest.php?input_string=$encoded"
        val webSearchUrl = "https://www.worldfloraonline.org/search?query=$encoded"

        try {
            val request = Request.Builder().url(matchUrl).get().build()
            BotanicalHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ResearchSourceRecord(
                        sourceId = sourceId,
                        sourceName = displayName,
                        authorityTier = authorityTier.name,
                        retrieved = false,
                        statusLabel = "HTTP ${response.code}",
                        summary = "WFO REST matching returned HTTP ${response.code}. Open link to verify directly.",
                        url = webSearchUrl
                    )
                }
                val body = response.body?.string().orEmpty().trim()
                if (!body.startsWith("{")) {
                    return@withContext ResearchSourceRecord(
                        sourceId = sourceId,
                        sourceName = displayName,
                        authorityTier = authorityTier.name,
                        retrieved = false,
                        statusLabel = "Non-JSON WFO response",
                        summary = "WFO endpoint did not return structured JSON; manual check link provided.",
                        url = webSearchUrl
                    )
                }

                val json = JSONObject(body)
                val match = json.optJSONObject("match")
                val wfoId = match?.optString("wfo_id", "")
                    ?: json.optString("wfo_id", "")
                val fullName = match?.optString("full_name_plain", "")
                    ?: json.optString("full_name_plain", cleanName)
                val placement = match?.optString("placement", "") ?: ""
                val role = match?.optString("role", "") ?: ""

                if (wfoId.isBlank()) {
                    return@withContext ResearchSourceRecord(
                        sourceId = sourceId,
                        sourceName = displayName,
                        authorityTier = authorityTier.name,
                        retrieved = true,
                        statusLabel = "Ambiguous or no WFO match",
                        summary = "WFO Plant List returned no unambiguous WFO ID for \"$cleanName\".",
                        url = webSearchUrl
                    )
                }

                val taxonUrl = "https://www.worldfloraonline.org/taxon/$wfoId"
                ResearchSourceRecord(
                    sourceId = sourceId,
                    sourceName = displayName,
                    authorityTier = authorityTier.name,
                    retrieved = true,
                    statusLabel = "Matched ($wfoId)",
                    acceptedScientificName = fullName.ifBlank { cleanName },
                    taxonomicStatus = role.ifBlank { "MATCHED" },
                    summary = "WFO Plant List matched $fullName ($wfoId)${if (placement.isNotBlank()) " • $placement" else ""}.",
                    url = taxonUrl
                )
            }
        } catch (e: Exception) {
            ResearchSourceRecord(
                sourceId = sourceId,
                sourceName = displayName,
                authorityTier = authorityTier.name,
                retrieved = false,
                statusLabel = "Lookup unavailable",
                summary = e.localizedMessage ?: "Network timeout querying World Flora Online.",
                url = webSearchUrl
            )
        }
    }
}
