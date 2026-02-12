package com.beema.kernel.service.openapi;

import com.beema.kernel.service.metadata.model.CompiledFieldDefinition;
import com.beema.kernel.service.metadata.model.CompiledObjectDefinition;
import io.swagger.v3.oas.models.media.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates OpenAPI Schema objects from Beema metadata definitions.
 *
 * This service converts our metadata-driven field definitions into valid
 * OpenAPI v3.0 schemas that can be used for API documentation and client generation.
 *
 * Key Features:
 * - Maps Beema data types to OpenAPI types
 * - Adds bitemporal system fields automatically
 * - Respects security (excludes internal-only fields)
 * - Handles validation constraints (min/max, patterns, enums)
 * - Supports nested objects and complex types
 */
@Service
public class DynamicSchemaGenerator {

    private static final Logger log = LoggerFactory.getLogger(DynamicSchemaGenerator.class);

    /**
     * Generates a complete OpenAPI Schema for an object definition.
     *
     * @param objectDef The compiled object definition
     * @param includeReadOnlyFields Whether to include read-only bitemporal fields
     * @return OpenAPI Schema object
     */
    public Schema<?> generateSchema(CompiledObjectDefinition objectDef, boolean includeReadOnlyFields) {
        log.debug("Generating schema for object: {} ({}:{})",
                objectDef.typeCode(), objectDef.tenantId(), objectDef.marketContext());

        ObjectSchema schema = new ObjectSchema();
        schema.setTitle(objectDef.displayName());
        schema.setDescription(objectDef.description());

        // Add standard fields from metadata
        List<String> requiredFields = new ArrayList<>();
        Map<String, Schema> properties = new LinkedHashMap<>();

        for (CompiledFieldDefinition field : objectDef.allFields()) {
            // Skip calculated/derived fields in request schemas
            if (!includeReadOnlyFields && (field.isCalculated() || field.isDerived())) {
                continue;
            }

            // Add field to schema
            Schema<?> fieldSchema = generateFieldSchema(field);
            properties.put(field.attributeName(), fieldSchema);

            if (field.isRequired() && !field.isCalculated()) {
                requiredFields.add(field.attributeName());
            }
        }

        // Add bitemporal system fields if requested (read-only)
        if (includeReadOnlyFields) {
            addBitemporalFields(properties);
        }

        schema.setProperties(properties);
        if (!requiredFields.isEmpty()) {
            schema.setRequired(requiredFields);
        }

        return schema;
    }

    /**
     * Generates a Schema for a single field definition.
     *
     * @param field The field definition
     * @return OpenAPI Schema for the field
     */
    public Schema<?> generateFieldSchema(CompiledFieldDefinition field) {
        Schema<?> fieldSchema = createSchemaByDataType(field.dataType());

        // Set metadata
        fieldSchema.setDescription(field.description());

        // Mark calculated/derived fields as read-only
        if (field.isCalculated() || field.isDerived()) {
            fieldSchema.setReadOnly(true);
        }

        // Add validation constraints
        applyValidationConstraints(fieldSchema, field);

        // Add enum values if defined
        if (field.allowedValues() != null && !field.allowedValues().isEmpty()) {
            addEnumValues(fieldSchema, field.allowedValues());
        }

        // Add default value if defined
        if (field.defaultValue() != null && !field.defaultValue().isEmpty()) {
            Object defaultVal = field.defaultValue().get("value");
            if (defaultVal != null) {
                fieldSchema.setDefault(defaultVal);
            }
        }

        return fieldSchema;
    }

    /**
     * Creates the appropriate Schema type based on the data type string.
     *
     * Maps Beema data types to OpenAPI types:
     * - STRING -> StringSchema
     * - CURRENCY, DECIMAL -> NumberSchema (format: double)
     * - INTEGER -> IntegerSchema
     * - BOOLEAN -> BooleanSchema
     * - DATE, DATETIME -> DateTimeSchema
     * - UUID -> UUIDSchema (StringSchema with format: uuid)
     */
    private Schema<?> createSchemaByDataType(String dataType) {
        if (dataType == null) {
            return new StringSchema();
        }

        return switch (dataType.toUpperCase()) {
            case "STRING", "TEXT" -> new StringSchema();
            case "CURRENCY", "DECIMAL", "DOUBLE", "FLOAT" -> {
                NumberSchema schema = new NumberSchema();
                schema.setFormat("double");
                yield schema;
            }
            case "INTEGER", "INT", "LONG" -> new IntegerSchema();
            case "BOOLEAN", "BOOL" -> new BooleanSchema();
            case "DATE" -> {
                DateSchema schema = new DateSchema();
                schema.setFormat("date");
                yield schema;
            }
            case "DATETIME", "TIMESTAMP" -> {
                DateTimeSchema schema = new DateTimeSchema();
                schema.setFormat("date-time");
                yield schema;
            }
            case "UUID" -> {
                UUIDSchema schema = new UUIDSchema();
                schema.setFormat("uuid");
                yield schema;
            }
            case "JSON", "JSONB", "OBJECT" -> new ObjectSchema();
            case "ARRAY" -> new ArraySchema();
            default -> {
                log.warn("Unknown data type: {}, defaulting to StringSchema", dataType);
                yield new StringSchema();
            }
        };
    }

    /**
     * Applies validation constraints from field definition to schema.
     */
    private void applyValidationConstraints(Schema<?> schema, CompiledFieldDefinition field) {
        // Min/Max for numbers
        if (schema instanceof NumberSchema numSchema) {
            if (field.minValue() != null) {
                numSchema.setMinimum(field.minValue());
            }
            if (field.maxValue() != null) {
                numSchema.setMaximum(field.maxValue());
            }
        }

        // Min/Max for integers
        if (schema instanceof IntegerSchema intSchema) {
            if (field.minValue() != null) {
                intSchema.setMinimum(field.minValue());
            }
            if (field.maxValue() != null) {
                intSchema.setMaximum(field.maxValue());
            }
        }

        // Pattern validation for strings
        if (schema instanceof StringSchema strSchema && field.validationPattern() != null) {
            strSchema.setPattern(field.validationPattern());
        }
    }

    /**
     * Adds enum values to a schema.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addEnumValues(Schema<?> schema, Map<String, Object> allowedValues) {
        // Check if allowedValues contains a list of options
        Object valuesObj = allowedValues.get("values");
        if (valuesObj instanceof List) {
            List enumValues = (List) valuesObj;
            schema.setEnum(enumValues);
        }
    }

    /**
     * Adds standard bitemporal system fields to the schema.
     * These fields are always read-only and managed by the system.
     */
    private void addBitemporalFields(Map<String, Schema> properties) {
        // id (UUID)
        UUIDSchema idSchema = new UUIDSchema();
        idSchema.setDescription("Unique identifier for this object");
        idSchema.setReadOnly(true);
        idSchema.setFormat("uuid");
        properties.put("id", idSchema);

        // valid_from (DateTime)
        DateTimeSchema validFromSchema = new DateTimeSchema();
        validFromSchema.setDescription("Start of validity period");
        validFromSchema.setReadOnly(true);
        validFromSchema.setFormat("date-time");
        properties.put("valid_from", validFromSchema);

        // valid_to (DateTime)
        DateTimeSchema validToSchema = new DateTimeSchema();
        validToSchema.setDescription("End of validity period");
        validToSchema.setReadOnly(true);
        validToSchema.setFormat("date-time");
        properties.put("valid_to", validToSchema);

        // transaction_time (DateTime)
        DateTimeSchema transactionTimeSchema = new DateTimeSchema();
        transactionTimeSchema.setDescription("Transaction timestamp");
        transactionTimeSchema.setReadOnly(true);
        transactionTimeSchema.setFormat("date-time");
        properties.put("transaction_time", transactionTimeSchema);

        // is_current (Boolean)
        BooleanSchema isCurrentSchema = new BooleanSchema();
        isCurrentSchema.setDescription("Whether this is the current version");
        isCurrentSchema.setReadOnly(true);
        properties.put("is_current", isCurrentSchema);

        // version (Integer)
        IntegerSchema versionSchema = new IntegerSchema();
        versionSchema.setDescription("Version number");
        versionSchema.setReadOnly(true);
        properties.put("version", versionSchema);

        // tenant_id (String)
        StringSchema tenantIdSchema = new StringSchema();
        tenantIdSchema.setDescription("Tenant identifier");
        tenantIdSchema.setReadOnly(true);
        properties.put("tenant_id", tenantIdSchema);

        // created_by (String)
        StringSchema createdBySchema = new StringSchema();
        createdBySchema.setDescription("User who created this record");
        createdBySchema.setReadOnly(true);
        properties.put("created_by", createdBySchema);

        // updated_by (String)
        StringSchema updatedBySchema = new StringSchema();
        updatedBySchema.setDescription("User who last updated this record");
        updatedBySchema.setReadOnly(true);
        properties.put("updated_by", updatedBySchema);
    }

    /**
     * Generates a request schema (excludes read-only fields).
     */
    public Schema<?> generateRequestSchema(CompiledObjectDefinition objectDef) {
        return generateSchema(objectDef, false);
    }

    /**
     * Generates a response schema (includes all fields including read-only).
     */
    public Schema<?> generateResponseSchema(CompiledObjectDefinition objectDef) {
        return generateSchema(objectDef, true);
    }

    /**
     * Generates an array schema for list responses.
     */
    public ArraySchema generateArraySchema(CompiledObjectDefinition objectDef) {
        ArraySchema arraySchema = new ArraySchema();
        arraySchema.setItems(generateResponseSchema(objectDef));
        return arraySchema;
    }
}
