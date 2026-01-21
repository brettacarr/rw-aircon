package com.rw.aircon.config

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.Instant

/**
 * JPA converter to store Instant as ISO 8601 strings in SQLite.
 * This ensures timestamps are stored in a format compatible with:
 * - Lexicographic comparison for date filtering
 * - SQLite substr() for hour grouping in aggregation queries
 */
@Converter(autoApply = true)
class InstantConverter : AttributeConverter<Instant?, String?> {

    override fun convertToDatabaseColumn(attribute: Instant?): String? {
        return attribute?.toString()
    }

    override fun convertToEntityAttribute(dbData: String?): Instant? {
        return dbData?.let { Instant.parse(it) }
    }
}
