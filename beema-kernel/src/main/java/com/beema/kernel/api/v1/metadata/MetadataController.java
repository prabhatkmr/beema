package com.beema.kernel.api.v1.metadata;

import com.beema.kernel.api.v1.metadata.dto.*;
import com.beema.kernel.domain.metadata.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAgreementType;
import com.beema.kernel.service.metadata.MetadataService;
import com.beema.kernel.util.SchemaValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for metadata management.
 *
 * Endpoints:
 * - POST /api/v1/metadata/agreement-types - Register agreement type
 * - GET /api/v1/metadata/agreement-types - List all types
 * - GET /api/v1/metadata/agreement-types/{typeCode} - Get specific type
 * - POST /api/v1/metadata/validate - Validate attributes
 */
@RestController
@RequestMapping("/api/v1/metadata")
@Tag(name = "Metadata", description = "Metadata management and schema validation")
public class MetadataController {

    private static final Logger log = LoggerFactory.getLogger(MetadataController.class);
    private final MetadataService metadataService;

    public MetadataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    // ========================================================================
    // Agreement Type Endpoints
    // ========================================================================

    /**
     * Register a new agreement type with schema.
     *
     * @param request Agreement type details
     * @return Created agreement type
     */
    @PostMapping("/agreement-types")
    @Operation(summary = "Register agreement type", description = "Create a new agreement type with JSON schema")
    public ResponseEntity<MetadataTypeResponse> registerAgreementType(
        @Valid @RequestBody MetadataTypeRequest request
    ) {
        log.info("Registering agreement type: {} for market: {}",
            request.typeCode(), request.marketContext());

        // Convert DTO to entity
        MetadataAgreementType entity = new MetadataAgreementType();
        entity.setTypeCode(request.typeCode());
        entity.setMarketContext(request.marketContext());
        entity.setDisplayName(request.displayName());
        entity.setDescription(request.description());
        entity.setAttributeSchema(request.attributeSchema());
        entity.setValidationRules(request.validationRules());
        entity.setIsActive(request.isActive());
        entity.setCreatedBy(request.createdBy() != null ? request.createdBy() : "system");
        entity.setUpdatedBy(request.updatedBy() != null ? request.updatedBy() : "system");

        // Save
        MetadataAgreementType saved = metadataService.registerAgreementType(entity);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(MetadataTypeResponse.from(saved));
    }

    /**
     * Get all agreement types.
     *
     * @param marketContext Optional market context filter
     * @return List of agreement types
     */
    @GetMapping("/agreement-types")
    @Operation(summary = "List agreement types", description = "Get all active agreement types")
    public ResponseEntity<List<MetadataTypeResponse>> getAllAgreementTypes(
        @RequestParam(required = false) MarketContext marketContext
    ) {
        log.debug("Fetching agreement types for market: {}", marketContext);

        List<MetadataAgreementType> types = marketContext != null
            ? metadataService.getAgreementTypesByMarket(marketContext)
            : metadataService.getAllAgreementTypes();

        List<MetadataTypeResponse> response = types.stream()
            .map(MetadataTypeResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get specific agreement type.
     *
     * @param typeCode Type code
     * @param marketContext Market context
     * @param schemaVersion Optional schema version (defaults to latest)
     * @return Agreement type if found
     */
    @GetMapping("/agreement-types/{typeCode}")
    @Operation(summary = "Get agreement type", description = "Get specific agreement type by code and market")
    public ResponseEntity<MetadataTypeResponse> getAgreementType(
        @PathVariable String typeCode,
        @RequestParam MarketContext marketContext,
        @RequestParam(required = false) Integer schemaVersion
    ) {
        log.debug("Fetching agreement type: {} for market: {} version: {}",
            typeCode, marketContext, schemaVersion);

        var typeOpt = schemaVersion != null
            ? metadataService.getAgreementType(typeCode, marketContext, schemaVersion)
            : metadataService.getAgreementType(typeCode, marketContext);

        return typeOpt
            .map(type -> ResponseEntity.ok(MetadataTypeResponse.from(type)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deactivate an agreement type.
     *
     * @param typeCode Type code
     * @param marketContext Market context
     * @return No content
     */
    @DeleteMapping("/agreement-types/{typeCode}")
    @Operation(summary = "Deactivate agreement type", description = "Mark agreement type as inactive")
    public ResponseEntity<Void> deactivateAgreementType(
        @PathVariable String typeCode,
        @RequestParam MarketContext marketContext
    ) {
        log.info("Deactivating agreement type: {} for market: {}", typeCode, marketContext);
        metadataService.deactivateAgreementType(typeCode, marketContext);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // Validation Endpoints
    // ========================================================================

    /**
     * Validate attributes against schema.
     *
     * @param request Validation request with type code and attributes
     * @return Validation result
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate attributes", description = "Validate agreement attributes against JSON schema")
    public ResponseEntity<ValidationResponse> validateAttributes(
        @Valid @RequestBody ValidationRequest request
    ) {
        log.debug("Validating attributes for type: {} market: {}",
            request.typeCode(), request.marketContext());

        SchemaValidator.ValidationResult result = metadataService.validateAttributes(
            request.typeCode(),
            request.marketContext(),
            request.attributes()
        );

        return ResponseEntity.ok(ValidationResponse.from(result));
    }

    // ========================================================================
    // Health Check
    // ========================================================================

    /**
     * Health check for metadata service.
     *
     * @return Service status
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check metadata service health")
    public ResponseEntity<MetadataHealthResponse> health() {
        long totalTypes = metadataService.getAllAgreementTypes().size();
        long totalAttributes = metadataService.getAllAttributes().size();

        return ResponseEntity.ok(new MetadataHealthResponse(
            "UP",
            totalTypes,
            totalAttributes
        ));
    }

    /**
     * Health response DTO.
     */
    public record MetadataHealthResponse(
        String status,
        long agreementTypesCount,
        long attributesCount
    ) {
    }
}
