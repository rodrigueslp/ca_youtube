package com.nextpost.ca_youtube.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class IntMapConverter : AttributeConverter<Map<Int, Int>, String> {
    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: Map<Int, Int>?): String {
        return try {
            if (attribute == null) return "{}"
            objectMapper.writeValueAsString(attribute)
        } catch (e: Exception) {
            "{}"
        }
    }

    override fun convertToEntityAttribute(dbData: String?): Map<Int, Int> {
        return try {
            if (dbData.isNullOrBlank()) return emptyMap()
            objectMapper.readValue(dbData, object : TypeReference<Map<Int, Int>>() {})
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
