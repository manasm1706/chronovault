package com.example.chronovault.data.remote.api

import retrofit2.http.GET

/**
 * ZenQuotes API for motivational quotes.
 */
interface QuoteApiService {

    @GET("random")
    suspend fun getRandomQuote(): List<QuoteResponse>
}

data class QuoteResponse(
    val q: String,
    val a: String
)

