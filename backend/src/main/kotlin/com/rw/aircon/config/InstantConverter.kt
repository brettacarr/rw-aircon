package com.rw.aircon.config

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * JPA converter to store Instant as ISO 8601 strings in SQLite.
 * This ensures timestamps are stored in a format compatible with:
 * - Lexicographic comparison for date filtering
 * - SQLite substr() for hour grouping in aggregation queries
 */
@Converter(autoApply = true)
class InstantConverter : AttributeConverter<Instant?, String?> {

    companion object {
        private val LEGACY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    override fun convertToDatabaseColumn(attribute: Instant?): String? {
        return attribute?.toString()
    }

    override fun convertToEntityAttribute(dbData: String?): Instant? {
        return dbData?.let {
            try {
                // Try ISO-8601 format first (current format)
                Instant.parse(it)
            } catch (e: Exception) {
                // Fall back to legacy format: "yyyy-MM-dd HH:mm:ss"
                LocalDateTime.parse(it, LEGACY_FORMATTER).toInstant(ZoneOffset.UTC)
            }
        }
    }
}
