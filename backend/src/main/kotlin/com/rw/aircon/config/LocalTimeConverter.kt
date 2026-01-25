package com.rw.aircon.config

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.LocalTime

/**
 * JPA converter to store LocalTime as HH:MM strings in SQLite.
 * This format supports lexicographic comparison for time-based queries.
 */
@Converter(autoApply = true)
class LocalTimeConverter : AttributeConverter<LocalTime?, String?> {

    override fun convertToDatabaseColumn(attribute: LocalTime?): String? {
        return attribute?.let {
            String.format("%02d:%02d", it.hour, it.minute)
        }
    }

    override fun convertToEntityAttribute(dbData: String?): LocalTime? {
        return dbData?.let {
            val parts = it.split(":")
            LocalTime.of(parts[0].toInt(), parts[1].toInt())
        }
    }
}
