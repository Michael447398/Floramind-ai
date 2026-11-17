package com.example.data.research

import com.example.model.ResearchSourceRecord
import com.example.model.SourceAuthorityTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class PubMedSourceProvider : BotanicalSourceProvider {
    override val sourceId: String = "pubmed"
    override val displayName: String = "NCBI PubMed Peer-Reviewed Literature"
    override val authorityTier: SourceAuthorityTier = SourceAuthorityTier.PEER_REVIEWED_LITERATURE

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

        val queryTerm = "\"$cleanName\"[Title/Abstract]"
        val encodedTerm = URLEncoder.encode(queryTerm, "UTF-8")
        val searchUrl =
            "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term=$encodedTerm&retmode=json&retmax=3&sort=relevance&tool=FloraMindAI"
        val webSearchUrl = "https://pubmed.ncbi.nlm.nih.gov/?term=${URLEncoder.encode(cleanName, "UTF-8")}"

        try {
            val searchReq = Request.Builder().url(searchUrl).get().build()
            val pmids = BotanicalHttpClient.client.newCall(searchReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext ResearchSourceRecord(
                        sourceId = sourceId,
                        sourceName = displayName,
                        authorityTier = authorityTier.name,
                        retrieved = false,
                        statusLabel = "HTTP ${resp.code}",
                        url = webSearchUrl
                    )
                }
                val json = JSONObject(resp.body?.string().orEmpty())
                val esearch = json.optJSONObject("esearchresult")
                val idList = esearch?.optJSONArray("idlist")
                val totalCount = esearch?.optString("count", "0") ?: "0"
                val ids = (0 until (idList?.length() ?: 0)).mapNotNull { idList?.optString(it) }
                Pair(totalCount, ids)
            }

            if (pmids.second.isEmpty()) {
                return@withContext ResearchSourceRecord(
                    sourceId = sourceId,
                    sourceName = displayName,
                    authorityTier = authorityTier.name,
                    retrieved = true,
                    statusLabel = "0 indexed PubMed hits",
                    summary = "No PubMed articles found with \"$cleanName\" in title/abstract.",
                    url = webSearchUrl
                )
            }

            val joinedIds = pmids.second.joinToString(",")
            val summaryUrl =
                "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id=$joinedIds&retmode=json&tool=FloraMindAI"

            val summaryReq = Request.Builder().url(summaryUrl).get().build()
            BotanicalHttpClient.client.newCall(summaryReq).execute().use { sumResp ->
                if (!sumResp.isSuccessful) {
                    return@withContext ResearchSourceRecord(
                        sourceId = sourceId,
                        sourceName = displayName,
                        authorityTier = authorityTier.name,
                        retrieved = true,
                        statusLabel = "${pmids.first} PubMed records",
                        summary = "Found ${pmids.first} peer-reviewed records; open PubMed link to browse.",
                        url = webSearchUrl
                    )
                }
                val sumJson = JSONObject(sumResp.body?.string().orEmpty())
                val resultObj = sumJson.optJSONObject("result")
                val topId = pmids.second.first()
                val paperSummaries = pmids.second.mapNotNull { id ->
                    val p = resultObj?.optJSONObject(id) ?: return@mapNotNull null
                    val title = p.optString("title", "").trim().trimEnd('.')
                    val journal = p.optString("source", "")
                    val pubDate = p.optString("pubdate", "")
                    if (title.isBlank()) null else "• $title ($journal, $pubDate) [PMID:$id]"
                }
                val topPaperDate = resultObj?.optJSONObject(topId)?.optString("pubdate", "") ?: ""

                ResearchSourceRecord(
                    sourceId = sourceId,
                    sourceName = displayName,
                    authorityTier = authorityTier.name,
                    retrieved = true,
                    statusLabel = "${pmids.first} indexed studies",
                    summary = paperSummaries.joinToString("\n"),
                    url = "https://pubmed.ncbi.nlm.nih.gov/$topId/",
                    publicationDate = topPaperDate
                )
            }
        } catch (e: Exception) {
            ResearchSourceRecord(
                sourceId = sourceId,
                sourceName = displayName,
                authorityTier = authorityTier.name,
                retrieved = false,
                statusLabel = "Lookup unavailable",
                summary = e.localizedMessage ?: "Network timeout querying PubMed.",
                url = webSearchUrl
            )
        }
    }
}
