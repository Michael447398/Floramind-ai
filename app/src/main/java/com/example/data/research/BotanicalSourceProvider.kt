package com.example.data.research

import com.example.model.ResearchSourceRecord
import com.example.model.SourceAuthorityTier
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

interface BotanicalSourceProvider {
    val sourceId: String
    val displayName: String
    val authorityTier: SourceAuthorityTier
    suspend fun querySpecies(
        scientificName: String,
        commonName: String? = null
    ): ResearchSourceRecord
}

object BotanicalHttpClient {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(7, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "FloraMindAI/1.0 (Botanical Research Client)")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .build()
    }
}
