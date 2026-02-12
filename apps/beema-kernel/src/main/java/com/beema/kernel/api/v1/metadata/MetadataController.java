package com.beema.kernel.api.v1.metadata;

import com.beema.kernel.api.v1.metadata.dto.MetadataTypeRequest;
import com.beema.kernel.api.v1.metadata.dto.MetadataTypeResponse;
import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.domain.metadata.MetadataAgreementType;
import com.beema.kernel.service.metadata.MetadataService;
import com.beema.kernel.util.SchemaValidator.ValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metadata/agreement-types")
@Tag(name = "Metadata", description = "Agreement type metadata management")
public class MetadataController {

    private final MetadataService metadataService;

    public MetadataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @PostMapping
    @Operation(summary = "Register a new agreement type", description = "Creates a new metadata-driven agreement type definition")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Agreement type registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "409", description = "Agreement type already exists for this tenant and market context")
    })
    public ResponseEntity<MetadataTypeResponse> registerAgreementType(
            @Valid @RequestBody MetadataTypeRequest request) {
        MetadataAgreementType entity = toEntity(request);
        MetadataAgreementType saved = metadataService.registerAgreementType(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(MetadataTypeResponse.fromEntity(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an agreement type", description = "Updates an existing agreement type definition, incrementing schema version")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agreement type updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Agreement type not found")
    })
    public ResponseEntity<MetadataTypeResponse> updateAgreementType(
            @Parameter(description = "Agreement type ID") @PathVariable UUID id,
            @Valid @RequestBody MetadataTypeRequest request) {
        MetadataAgreementType entity = toEntity(request);
        MetadataAgreementType updated = metadataService.updateAgreementType(id, entity);
        return ResponseEntity.ok(MetadataTypeResponse.fromEntity(updated));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get agreement type by ID", description = "Retrieves a single agreement type by its unique identifier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agreement type found"),
            @ApiResponse(responseCode = "404", description = "Agreement type not found")
    })
    public ResponseEntity<MetadataTypeResponse> getAgreementType(
            @Parameter(description = "Agreement type ID") @PathVariable UUID id) {
        return metadataService.getAgreementType(id)
                .map(entity -> ResponseEntity.ok(MetadataTypeResponse.fromEntity(entity)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-code")
    @Operation(summary = "Get agreement type by code", description = "Retrieves an agreement type by tenant, type code, and market context")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agreement type found"),
            @ApiResponse(responseCode = "404", description = "Agreement type not found")
    })
    public ResponseEntity<MetadataTypeResponse> getAgreementTypeByCode(
            @Parameter(description = "Tenant ID", required = true) @RequestParam UUID tenantId,
            @Parameter(description = "Type code", required = true) @RequestParam String typeCode,
            @Parameter(description = "Market context", required = true) @RequestParam MarketContext marketContext) {
        return metadataService.getAgreementTypeByCode(tenantId, typeCode, marketContext)
                .map(entity -> ResponseEntity.ok(MetadataTypeResponse.fromEntity(entity)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "List agreement types for a tenant", description = "Retrieves all active agreement types for a tenant, optionally filtered by market context")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of agreement types")
    })
    public ResponseEntity<List<MetadataTypeResponse>> listAgreementTypes(
            @Parameter(description = "Tenant ID", required = true) @RequestParam UUID tenantId,
            @Parameter(description = "Filter by market context") @RequestParam(required = false) MarketContext marketContext) {
        List<MetadataAgreementType> types;
        if (marketContext != null) {
            types = metadataService.getActiveAgreementTypesByTenantAndContext(tenantId, marketContext);
        } else {
            types = metadataService.getAgreementTypesByTenant(tenantId);
        }
        List<MetadataTypeResponse> responses = types.stream()
                .map(MetadataTypeResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Validate attributes against schema", description = "Validates a set of attributes against the agreement type's JSON Schema")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation result returned"),
            @ApiResponse(responseCode = "404", description = "Agreement type not found")
    })
    public ResponseEntity<Map<String, Object>> validateAttributes(
            @Parameter(description = "Agreement type ID") @PathVariable UUID id,
            @RequestBody Map<String, Object> attributes) {
        ValidationResult result = metadataService.validateAgainstSchema(id, attributes);
        Map<String, Object> response = new HashMap<>();
        response.put("valid", result.valid());
        response.put("errors", result.errors());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate an agreement type", description = "Soft-deletes an agreement type by setting it as inactive")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Agreement type deactivated"),
            @ApiResponse(responseCode = "404", description = "Agreement type not found")
    })
    public ResponseEntity<Void> deactivateAgreementType(
            @Parameter(description = "Agreement type ID") @PathVariable UUID id) {
        metadataService.deactivateAgreementType(id);
        return ResponseEntity.noContent().build();
    }

    private MetadataAgreementType toEntity(MetadataTypeRequest request) {
        MetadataAgreementType entity = new MetadataAgreementType();
        entity.setTenantId(request.getTenantId());
        entity.setTypeCode(request.getTypeCode());
        entity.setTypeName(request.getTypeName());
        entity.setDescription(request.getDescription());
        entity.setMarketContext(request.getMarketContext());
        entity.setAttributeSchema(request.getAttributeSchema());
        entity.setValidationRules(request.getValidationRules() != null ? request.getValidationRules() : Map.of());
        entity.setUiConfiguration(request.getUiConfiguration() != null ? request.getUiConfiguration() : Map.of());
        entity.setUpdatedBy(request.getUpdatedBy());
        return entity;
    }
}
