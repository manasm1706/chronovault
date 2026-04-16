package com.example.chronovault.data.repository

import com.example.chronovault.data.remote.api.QuoteApiService
import com.example.chronovault.data.remote.api.QuoteResponse
import com.example.chronovault.utils.PreferencesManager

/**
 * Repository for daily motivational quotes with lightweight local caching.
 */
class QuoteRepository(
    private val api: QuoteApiService,
    private val preferencesManager: PreferencesManager
) {

    suspend fun fetchQuote(): Result<QuoteResponse> {
        return try {
            val response = api.getRandomQuote()
            val quote = response.firstOrNull()
                ?: return Result.failure(IllegalStateException("Empty quote response"))
            cacheQuote(quote)
            Result.success(quote)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCachedQuote(): QuoteResponse? {
        val quote = preferencesManager.getLastQuoteText().orEmpty().trim()
        val author = preferencesManager.getLastQuoteAuthor().orEmpty().trim()
        if (quote.isEmpty() || author.isEmpty()) return null
        return QuoteResponse(q = quote, a = author)
    }

    private fun cacheQuote(quote: QuoteResponse) {
        preferencesManager.setLastQuoteText(quote.q)
        preferencesManager.setLastQuoteAuthor(quote.a)
        preferencesManager.setLastQuoteTimestamp(System.currentTimeMillis())
    }
}

