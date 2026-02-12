package com.beema.kernel.ai.tools;

import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.service.metadata.MetadataService;
import com.beema.kernel.service.expression.JexlExpressionEngine;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * AI-callable tools for insurance domain operations.
 * These tools expose existing Beema services to LLM function calling.
 */
@Component
public class InsuranceTools {

    private final MetadataService metadataService;
    private final JexlExpressionEngine expressionEngine;

    public InsuranceTools(MetadataService metadataService,
                          JexlExpressionEngine expressionEngine) {
        this.metadataService = metadataService;
        this.expressionEngine = expressionEngine;
    }

    /**
     * Tool 1: Get field metadata for a given agreement type
     */
    public record GetFieldMetadataRequest(
        @JsonProperty(required = true)
        @JsonPropertyDescription("The agreement type code (e.g., 'motor_comprehensive', 'property_all_risk')")
        String agreementType,

        @JsonProperty(required = true)
        @JsonPropertyDescription("Market context: RETAIL, COMMERCIAL, or LONDON_MARKET")
        String marketContext,

        @JsonProperty(required = true)
        @JsonPropertyDescription("Tenant ID")
        String tenantId
    ) {}

    public Function<GetFieldMetadataRequest, String> getFieldMetadata() {
        return request -> {
            try {
                MarketContext context = MarketContext.valueOf(request.marketContext());
                UUID tenantUuid = UUID.fromString(request.tenantId());

                var metadataOpt = metadataService.getAgreementTypeByCode(
                    tenantUuid,
                    request.agreementType(),
                    context
                );

                if (metadataOpt.isEmpty()) {
                    return "Error: Agreement type not found for code: " + request.agreementType();
                }

                var metadata = metadataOpt.get();
                Map<String, Object> result = new HashMap<>();
                result.put("typeCode", metadata.getTypeCode());
                result.put("typeName", metadata.getTypeName());
                result.put("description", metadata.getDescription());
                result.put("marketContext", metadata.getMarketContext().name());
                result.put("attributeSchema", metadata.getAttributeSchema());
                result.put("validationRules", metadata.getValidationRules());
                result.put("calculationRules", metadata.getCalculationRules());

                return toJson(result);
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        };
    }

    /**
     * Tool 2: Evaluate a JEXL expression against claim data
     */
    public record EvaluateExpressionRequest(
        @JsonProperty(required = true)
        @JsonPropertyDescription("JEXL expression to evaluate (e.g., 'claimAmount > 10000')")
        String expression,

        @JsonProperty(required = true)
        @JsonPropertyDescription("Context data as JSON object containing claim, policy, etc.")
        Map<String, Object> context
    ) {}

    public Function<EvaluateExpressionRequest, String> evaluateExpression() {
        return request -> {
            try {
                Object result = expressionEngine.evaluate(
                    request.context(),
                    request.expression()
                );

                return String.format(
                    "Expression: %s\nResult: %s\nType: %s",
                    request.expression(),
                    result,
                    result != null ? result.getClass().getSimpleName() : "null"
                );
            } catch (Exception e) {
                return "Expression evaluation failed: " + e.getMessage();
            }
        };
    }

    /**
     * Tool 3: Validate claim data against schema
     */
    public record ValidateSchemaRequest(
        @JsonProperty(required = true)
        @JsonPropertyDescription("Agreement type ID to validate against")
        String agreementTypeId,

        @JsonProperty(required = true)
        @JsonPropertyDescription("Claim attributes to validate")
        Map<String, Object> attributes
    ) {}

    public Function<ValidateSchemaRequest, String> validateSchema() {
        return request -> {
            try {
                UUID agreementTypeUuid = UUID.fromString(request.agreementTypeId());
                var validationResult = metadataService.validateAgainstSchema(
                    agreementTypeUuid,
                    request.attributes()
                );

                return String.format(
                    "Valid: %s\nErrors: %s",
                    validationResult.isValid(),
                    validationResult.errors()
                );
            } catch (Exception e) {
                return "Schema validation failed: " + e.getMessage();
            }
        };
    }

    /**
     * Tool 4: Get validation rules for an agreement type
     */
    public record GetValidationRulesRequest(
        @JsonProperty(required = true)
        @JsonPropertyDescription("Agreement type code")
        String agreementType,

        @JsonProperty(required = true)
        @JsonPropertyDescription("Market context")
        String marketContext,

        @JsonProperty(required = true)
        @JsonPropertyDescription("Tenant ID")
        String tenantId
    ) {}

    public Function<GetValidationRulesRequest, String> getValidationRules() {
        return request -> {
            try {
                MarketContext context = MarketContext.valueOf(request.marketContext());
                UUID tenantUuid = UUID.fromString(request.tenantId());

                var metadataOpt = metadataService.getAgreementTypeByCode(
                    tenantUuid,
                    request.agreementType(),
                    context
                );

                if (metadataOpt.isEmpty()) {
                    return "Error: Agreement type not found";
                }

                var validationRules = metadataOpt.get().getValidationRules();
                return toJson(validationRules);
            } catch (Exception e) {
                return "Error retrieving validation rules: " + e.getMessage();
            }
        };
    }

    /**
     * Tool 5: Calculate claim metrics
     */
    public record CalculateClaimMetricsRequest(
        @JsonProperty(required = true)
        @JsonPropertyDescription("Claim amount")
        Double claimAmount,

        @JsonProperty(required = true)
        @JsonPropertyDescription("Policy sum insured")
        Double sumInsured,

        @JsonProperty(required = false)
        @JsonPropertyDescription("Policy excess/deductible")
        Double excess
    ) {}

    public Function<CalculateClaimMetricsRequest, String> calculateClaimMetrics() {
        return request -> {
            try {
                double claimRatio = (request.claimAmount() / request.sumInsured()) * 100;
                double excessAmount = request.excess() != null ? request.excess() : 0.0;
                double payableAmount = Math.max(0, request.claimAmount() - excessAmount);

                String severity;
                if (claimRatio > 80) {
                    severity = "HIGH";
                } else if (claimRatio > 50) {
                    severity = "MEDIUM";
                } else {
                    severity = "LOW";
                }

                return String.format(
                    "Claim Amount: %.2f\nSum Insured: %.2f\nClaim Ratio: %.2f%%\nExcess: %.2f\nPayable Amount: %.2f\nSeverity: %s",
                    request.claimAmount(),
                    request.sumInsured(),
                    claimRatio,
                    excessAmount,
                    payableAmount,
                    severity
                );
            } catch (Exception e) {
                return "Calculation failed: " + e.getMessage();
            }
        };
    }

    // Helper method to serialize objects to JSON
    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
