package com.beema.kernel.batch.export;

import com.beema.kernel.domain.agreement.Agreement;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Converts Agreement entities to Avro GenericRecords.
 *
 * Handles dynamic JSONB attributes by building an Avro schema that includes
 * both fixed agreement fields and flattened attribute fields.
 * The schema is built dynamically based on the first record's attributes,
 * then cached for the remainder of the batch.
 */
public class JsonToAvroProcessor implements ItemProcessor<Agreement, GenericRecord> {

    private static final Logger log = LoggerFactory.getLogger(JsonToAvroProcessor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentHashMap<String, Schema> schemaCache = new ConcurrentHashMap<>();
    private volatile Schema currentSchema;

    @Override
    public GenericRecord process(Agreement agreement) throws Exception {
        Schema schema = getOrBuildSchema(agreement);
        GenericRecord record = new GenericData.Record(schema);

        // Fixed fields
        record.put("id", agreement.getId().toString());
        record.put("agreement_number", agreement.getAgreementNumber());
        record.put("agreement_type_code", agreement.getAgreementTypeCode());
        record.put("market_context", agreement.getMarketContext().name());
        record.put("status", agreement.getStatus().name());
        record.put("valid_from", agreement.getValidFrom().toString());
        record.put("valid_to", agreement.getValidTo().toString());
        record.put("transaction_time", agreement.getTransactionTime().toString());
        record.put("tenant_id", agreement.getTenantId());
        record.put("data_residency_region",
                agreement.getDataResidencyRegion() != null ? agreement.getDataResidencyRegion() : "");
        record.put("created_by", agreement.getCreatedBy());
        record.put("updated_by", agreement.getUpdatedBy());
        record.put("version", agreement.getVersion());
        record.put("created_at", agreement.getCreatedAt().toString());
        record.put("updated_at", agreement.getUpdatedAt().toString());

        // Flatten JSONB attributes
        Map<String, Object> attributes = agreement.getAttributes();
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String fieldName = "attr_" + entry.getKey();
                if (schema.getField(fieldName) != null) {
                    record.put(fieldName, convertValue(entry.getValue()));
                }
            }
        }

        return record;
    }

    public Schema getSchema() {
        return currentSchema;
    }

    private Schema getOrBuildSchema(Agreement agreement) {
        if (currentSchema != null) {
            return currentSchema;
        }
        currentSchema = buildSchema(agreement);
        return currentSchema;
    }

    private Schema buildSchema(Agreement agreement) {
        SchemaBuilder.FieldAssembler<Schema> fields = SchemaBuilder.record("Agreement")
                .namespace("com.beema.kernel.export")
                .fields();

        // Fixed fields
        fields.requiredString("id");
        fields.requiredString("agreement_number");
        fields.requiredString("agreement_type_code");
        fields.requiredString("market_context");
        fields.requiredString("status");
        fields.requiredString("valid_from");
        fields.requiredString("valid_to");
        fields.requiredString("transaction_time");
        fields.requiredString("tenant_id");
        fields.requiredString("data_residency_region");
        fields.requiredString("created_by");
        fields.requiredString("updated_by");
        fields.requiredLong("version");
        fields.requiredString("created_at");
        fields.requiredString("updated_at");

        // Dynamic attribute fields (flattened from JSONB)
        Map<String, Object> attributes = agreement.getAttributes();
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String fieldName = "attr_" + entry.getKey();
                // All attribute values stored as strings for schema stability
                fields.requiredString(fieldName);
            }
        }

        Schema schema = fields.endRecord();
        log.info("Built Avro schema with {} fields ({} from attributes)",
                schema.getFields().size(),
                attributes != null ? attributes.size() : 0);
        return schema;
    }

    private String convertValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Collection || value instanceof Map) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (Exception e) {
                log.warn("Failed to serialize attribute value, using toString: {}", e.getMessage());
                return value.toString();
            }
        }
        return value.toString();
    }
}
