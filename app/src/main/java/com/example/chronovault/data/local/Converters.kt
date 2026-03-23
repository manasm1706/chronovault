package com.example.chronovault.data.local

import androidx.room.TypeConverter

/**
 * Room TypeConverters for ChronoVaultDatabase
 * Handles conversion of non-primitive types to/from storable formats
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",")
    }
}

