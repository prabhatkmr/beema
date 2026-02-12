package com.beema.kernel.api.v1.agreement;

import com.beema.kernel.api.v1.agreement.dto.AgreementRequest;
import com.beema.kernel.api.v1.agreement.dto.AgreementResponse;
import com.beema.kernel.api.v1.agreement.dto.TemporalQueryParams;
import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.service.agreement.AgreementService;
import com.beema.kernel.service.security.WriteShieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/agreements")
@Tag(name = "Agreements", description = "Agreement management with bitemporal versioning")
public class AgreementController {

    private final AgreementService agreementService;
    private final WriteShieldService writeShieldService;

    public AgreementController(AgreementService agreementService, WriteShieldService writeShieldService) {
        this.agreementService = agreementService;
        this.writeShieldService = writeShieldService;
    }

    @PostMapping
    @Operation(summary = "Create a new agreement", description = "Creates a new agreement with schema validation against its metadata type")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Agreement created"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<AgreementResponse> createAgreement(
            @Valid @RequestBody AgreementRequest request,
            @Parameter(description = "User role for write permissions (CUSTOMER, BROKER, UNDERWRITER, ADMIN)")
            @RequestParam(defaultValue = "CUSTOMER") String userRole) {
        Agreement entity = toEntity(request, userRole);
        Agreement saved = agreementService.createAgreement(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(AgreementResponse.fromEntity(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an agreement", description = "Creates a new bitemporal version of the agreement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agreement updated"),
            @ApiResponse(responseCode = "404", description = "Agreement not found"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<AgreementResponse> updateAgreement(
            @Parameter(description = "Agreement ID") @PathVariable UUID id,
            @Valid @RequestBody AgreementRequest request,
            @Parameter(description = "User role for write permissions (CUSTOMER, BROKER, UNDERWRITER, ADMIN)")
            @RequestParam(defaultValue = "CUSTOMER") String userRole) {
        Agreement entity = toEntity(request, userRole);
        Agreement updated = agreementService.updateAgreement(id, entity);
        return ResponseEntity.ok(AgreementResponse.fromEntity(updated));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get current agreement", description = "Retrieves the current version of an agreement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agreement found"),
            @ApiResponse(responseCode = "404", description = "Agreement not found")
    })
    public ResponseEntity<AgreementResponse> getAgreement(
            @Parameter(description = "Agreement ID") @PathVariable UUID id) {
        Agreement agreement = agreementService.getCurrentAgreement(id)
                .orElseThrow(() -> new EntityNotFoundException("Agreement not found: " + id));
        return ResponseEntity.ok(AgreementResponse.fromEntity(agreement));
    }

    @GetMapping("/by-number")
    @Operation(summary = "Get agreement by number", description = "Retrieves the current version of an agreement by its number")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agreement found"),
            @ApiResponse(responseCode = "404", description = "Agreement not found")
    })
    public ResponseEntity<AgreementResponse> getAgreementByNumber(
            @Parameter(description = "Agreement number", required = true) @RequestParam String agreementNumber,
            @Parameter(description = "Tenant ID", required = true) @RequestParam String tenantId) {
        Agreement agreement = agreementService.getCurrentAgreementByNumber(agreementNumber, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Agreement not found: " + agreementNumber));
        return ResponseEntity.ok(AgreementResponse.fromEntity(agreement));
    }

    @GetMapping("/{id}/as-of")
    @Operation(summary = "Get agreement as-of a point in time", description = "Retrieves the agreement version valid at a specific business time")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agreement version found"),
            @ApiResponse(responseCode = "404", description = "No version found for the given time")
    })
    public ResponseEntity<AgreementResponse> getAgreementAsOf(
            @Parameter(description = "Agreement ID") @PathVariable UUID id,
            @Parameter(description = "Tenant ID", required = true) @RequestParam String tenantId,
            @Valid TemporalQueryParams temporal) {
        if (temporal.getAsOf() == null) {
            throw new IllegalArgumentException("asOf parameter is required for temporal queries");
        }
        Agreement agreement = agreementService.getAgreementAsOf(id, tenantId, temporal.getAsOf())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No agreement version found at " + temporal.getAsOf()));
        return ResponseEntity.ok(AgreementResponse.fromEntity(agreement));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get agreement history", description = "Retrieves the full bitemporal history of an agreement")
    @ApiResponse(responseCode = "200", description = "Agreement history")
    public ResponseEntity<List<AgreementResponse>> getAgreementHistory(
            @Parameter(description = "Agreement ID") @PathVariable UUID id,
            @Parameter(description = "Tenant ID", required = true) @RequestParam String tenantId) {
        List<AgreementResponse> history = agreementService.getAgreementHistory(id, tenantId).stream()
                .map(AgreementResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(history);
    }

    @GetMapping
    @Operation(summary = "List agreements", description = "Retrieves current agreements filtered by tenant and market context or status")
    @ApiResponse(responseCode = "200", description = "Page of agreements")
    public ResponseEntity<Page<AgreementResponse>> listAgreements(
            @Parameter(description = "Tenant ID", required = true) @RequestParam String tenantId,
            @Parameter(description = "Filter by market context") @RequestParam(required = false) MarketContext marketContext,
            @Parameter(description = "Filter by status") @RequestParam(required = false) AgreementStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AgreementResponse> page;
        if (marketContext != null) {
            page = agreementService.getAgreementsByTenantAndContext(tenantId, marketContext, pageable)
                    .map(AgreementResponse::fromEntity);
        } else if (status != null) {
            page = agreementService.getAgreementsByTenantAndStatus(tenantId, status, pageable)
                    .map(AgreementResponse::fromEntity);
        } else {
            page = agreementService.getAgreementsByTenantAndContext(tenantId, null, pageable)
                    .map(AgreementResponse::fromEntity);
        }
        return ResponseEntity.ok(page);
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Validate agreement attributes", description = "Validates attributes against the agreement type schema without persisting")
    @ApiResponse(responseCode = "200", description = "Validation result")
    public ResponseEntity<Map<String, Object>> validateAgreement(
            @Parameter(description = "Agreement ID") @PathVariable UUID id) {
        Agreement agreement = agreementService.getCurrentAgreement(id)
                .orElseThrow(() -> new EntityNotFoundException("Agreement not found: " + id));
        var result = agreementService.validateAgreement(agreement);
        Map<String, Object> response = new HashMap<>();
        response.put("valid", result.valid());
        response.put("errors", result.errors());
        return ResponseEntity.ok(response);
    }

    private Agreement toEntity(AgreementRequest request, String userRole) {
        Agreement entity = new Agreement();
        entity.setAgreementNumber(request.getAgreementNumber());
        entity.setExternalReference(request.getExternalReference());
        entity.setMarketContext(request.getMarketContext());
        entity.setAgreementTypeId(request.getAgreementTypeId());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : AgreementStatus.DRAFT);
        entity.setTenantId(UUID.fromString(request.getTenantId()));
        entity.setDataResidencyRegion(request.getDataResidencyRegion() != null ? request.getDataResidencyRegion() : "EU");
        entity.setInceptionDate(request.getInceptionDate());
        entity.setExpiryDate(request.getExpiryDate());
        entity.setCurrencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "GBP");
        entity.setTotalPremium(request.getTotalPremium());
        entity.setTotalSumInsured(request.getTotalSumInsured());

        // SECURITY: Apply WriteShield to prevent mass assignment attacks
        if (request.getAttributes() != null) {
            Map<String, Object> sanitizedAttributes = writeShieldService.sanitize(
                    request.getAttributes(),
                    request.getAgreementTypeId(),
                    request.getMarketContext(),
                    userRole,
                    UUID.fromString(request.getTenantId())
            );
            entity.setAttributes(sanitizedAttributes);
        }

        entity.setUpdatedBy(request.getUpdatedBy());
        return entity;
    }
}
