package com.aptech.aptechMall.model.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA Converter for OAuth data
 * Converts Map<String, Object> to JSON string for database storage
 */
@Converter
public class OAuthConverter implements AttributeConverter<Map<String, Object>, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) return "{}";
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert OAuth map to JSON string", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(dbData, new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to convert JSON string to OAuth map", e);
        }
    }
}
