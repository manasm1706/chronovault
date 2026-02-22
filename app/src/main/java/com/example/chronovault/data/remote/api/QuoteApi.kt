package com.example.chronovault.data.remote.api

import retrofit2.http.GET

/**
 * API service for fetching daily quotes
 */
interface QuoteApi {

    @GET("random")
    suspend fun getRandomQuote(): QuoteResponse
}

data class QuoteResponse(
    val content: String,
    val author: String
)

