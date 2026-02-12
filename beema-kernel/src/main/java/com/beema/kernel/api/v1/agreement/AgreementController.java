package com.beema.kernel.api.v1.agreement;

import com.beema.kernel.api.v1.agreement.dto.*;
import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.base.TemporalKey;
import com.beema.kernel.domain.metadata.MarketContext;
import com.beema.kernel.service.agreement.AgreementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API for agreement management.
 *
 * Endpoints:
 * - POST /api/v1/agreements - Create agreement
 * - GET /api/v1/agreements/{id} - Get current version
 * - PUT /api/v1/agreements/{id} - Update (create new version)
 * - GET /api/v1/agreements/{id}/history - Get audit trail
 * - POST /api/v1/agreements/search - Search by attributes
 * - POST /api/v1/agreements/as-of - Point-in-time query
 */
@RestController
@RequestMapping("/api/v1/agreements")
@Tag(name = "Agreements", description = "Bitemporal agreement management")
public class AgreementController {

    private static final Logger log = LoggerFactory.getLogger(AgreementController.class);

    private final AgreementService agreementService;

    public AgreementController(AgreementService agreementService) {
        this.agreementService = agreementService;
    }

    // ========================================================================
    // CRUD Operations
    // ========================================================================

    /**
     * Create new agreement.
     *
     * @param request Agreement details
     * @return Created agreement
     */
    @PostMapping
    @Operation(summary = "Create agreement", description = "Create new agreement with schema validation")
    public ResponseEntity<AgreementResponse> createAgreement(
        @Valid @RequestBody AgreementRequest request
    ) {
        log.info("Creating agreement: {} for tenant: {}",
            request.agreementNumber(), request.tenantId());

        // Convert DTO to entity
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber(request.agreementNumber());
        agreement.setAgreementTypeCode(request.agreementTypeCode());
        agreement.setMarketContext(request.marketContext());
        agreement.setStatus(request.status());
        agreement.setAttributes(request.attributes());
        agreement.setDataResidencyRegion(request.dataResidencyRegion());
        agreement.setTenantId(request.tenantId());
        agreement.setCreatedBy(request.createdBy() != null ? request.createdBy() : "system");
        agreement.setUpdatedBy(request.updatedBy() != null ? request.updatedBy() : "system");

        // Set temporal key
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        agreement.setTemporalKey(new TemporalKey(id, now, now));

        // Create
        Agreement created = agreementService.createAgreement(agreement);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(AgreementResponse.from(created));
    }

    /**
     * Get current version of agreement.
     *
     * @param id Agreement ID
     * @return Current version
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get agreement", description = "Get current version of agreement")
    public ResponseEntity<AgreementResponse> getAgreement(
        @PathVariable UUID id
    ) {
        log.debug("Fetching current agreement: {}", id);

        return agreementService.getCurrentAgreement(id)
            .map(agreement -> ResponseEntity.ok(AgreementResponse.from(agreement)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update agreement (creates new temporal version).
     *
     * @param id Agreement ID
     * @param request Update details
     * @return New version
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update agreement", description = "Create new temporal version with updates")
    public ResponseEntity<AgreementResponse> updateAgreement(
        @PathVariable UUID id,
        @Valid @RequestBody AgreementUpdateRequest request
    ) {
        log.info("Updating agreement: {} effective from: {}", id, request.effectiveFrom());

        // Build updates map
        Map<String, Object> updates = new HashMap<>();
        if (request.status() != null) {
            updates.put("status", request.status());
        }
        if (request.attributes() != null) {
            updates.put("attributes", request.attributes());
        }

        // Update
        Agreement updated = agreementService.updateAgreement(
            id,
            updates,
            request.effectiveFrom(),
            request.updatedBy()
        );

        return ResponseEntity.ok(AgreementResponse.from(updated));
    }

    /**
     * Get agreement by number.
     *
     * @param agreementNumber Agreement number
     * @param tenantId Tenant ID
     * @return Current version
     */
    @GetMapping("/by-number/{agreementNumber}")
    @Operation(summary = "Get by number", description = "Get agreement by business number")
    public ResponseEntity<AgreementResponse> getByNumber(
        @PathVariable String agreementNumber,
        @RequestParam String tenantId
    ) {
        log.debug("Fetching agreement by number: {} for tenant: {}", agreementNumber, tenantId);

        return agreementService.getAgreementByNumber(agreementNumber, tenantId)
            .map(agreement -> ResponseEntity.ok(AgreementResponse.from(agreement)))
            .orElse(ResponseEntity.notFound().build());
    }

    // ========================================================================
    // Temporal Queries
    // ========================================================================

    /**
     * Get agreement as of a specific point in time.
     *
     * @param id Agreement ID
     * @param validTime Valid time (when data was effective)
     * @param transactionTime Transaction time (when we knew about it)
     * @return Version at that point in time
     */
    @GetMapping("/{id}/as-of")
    @Operation(summary = "Point-in-time query", description = "Get agreement as of specific valid time and transaction time")
    public ResponseEntity<AgreementResponse> getAgreementAsOf(
        @PathVariable UUID id,
        @RequestParam @Parameter(description = "Valid time (ISO 8601)") OffsetDateTime validTime,
        @RequestParam @Parameter(description = "Transaction time (ISO 8601)") OffsetDateTime transactionTime
    ) {
        log.debug("Fetching agreement: {} as of valid: {} transaction: {}",
            id, validTime, transactionTime);

        return agreementService.getAgreementAsOf(id, validTime, transactionTime)
            .map(agreement -> ResponseEntity.ok(AgreementResponse.from(agreement)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get complete audit trail for agreement.
     *
     * @param id Agreement ID
     * @return All temporal versions
     */
    @GetMapping("/{id}/history")
    @Operation(summary = "Get history", description = "Get complete audit trail for agreement")
    public ResponseEntity<List<AgreementResponse>> getHistory(
        @PathVariable UUID id
    ) {
        log.debug("Fetching history for agreement: {}", id);

        List<Agreement> history = agreementService.getAgreementHistory(id);

        if (history.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<AgreementResponse> response = history.stream()
            .map(AgreementResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // Search Operations
    // ========================================================================

    /**
     * Find agreements by tenant and market context.
     *
     * @param tenantId Tenant ID
     * @param marketContext Market context
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Page of agreements
     */
    @GetMapping
    @Operation(summary = "List agreements", description = "Find agreements by tenant and market context")
    public ResponseEntity<Page<AgreementSummaryResponse>> listAgreements(
        @RequestParam String tenantId,
        @RequestParam MarketContext marketContext,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        log.debug("Listing agreements for tenant: {} market: {} page: {} size: {}",
            tenantId, marketContext, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Agreement> agreements = agreementService.findAgreementsByTenantAndContext(
            tenantId,
            marketContext,
            pageable
        );

        Page<AgreementSummaryResponse> response = agreements.map(AgreementResponse::toSummary);

        return ResponseEntity.ok(response);
    }

    /**
     * Find agreements by status.
     *
     * @param tenantId Tenant ID
     * @param status Agreement status
     * @return List of agreements
     */
    @GetMapping("/by-status")
    @Operation(summary = "Find by status", description = "Find agreements by status")
    public ResponseEntity<List<AgreementSummaryResponse>> findByStatus(
        @RequestParam String tenantId,
        @RequestParam AgreementStatus status
    ) {
        log.debug("Finding agreements for tenant: {} status: {}", tenantId, status);

        List<Agreement> agreements = agreementService.findAgreementsByStatus(tenantId, status);

        List<AgreementSummaryResponse> response = agreements.stream()
            .map(AgreementResponse::toSummary)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Search agreements by JSONB attributes.
     *
     * @param request Search criteria
     * @return List of matching agreements
     */
    @PostMapping("/search")
    @Operation(summary = "Search by attributes", description = "Find agreements by JSONB attribute containment")
    public ResponseEntity<List<AgreementSummaryResponse>> searchByAttributes(
        @Valid @RequestBody AttributeSearchRequest request
    ) {
        log.debug("Searching agreements for tenant: {} with attributes: {}",
            request.tenantId(), request.attributes());

        List<Agreement> agreements = agreementService.findByAttribute(
            request.tenantId(),
            request.attributes()
        );

        List<AgreementSummaryResponse> response = agreements.stream()
            .map(AgreementResponse::toSummary)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // Status Management
    // ========================================================================

    /**
     * Change agreement status.
     *
     * @param id Agreement ID
     * @param newStatus New status
     * @param effectiveFrom When status change is effective
     * @param updatedBy User making the change
     * @return Updated agreement
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Change status", description = "Change agreement status (creates new version)")
    public ResponseEntity<AgreementResponse> changeStatus(
        @PathVariable UUID id,
        @RequestParam AgreementStatus newStatus,
        @RequestParam OffsetDateTime effectiveFrom,
        @RequestParam String updatedBy
    ) {
        log.info("Changing status for agreement: {} to: {} effective: {}",
            id, newStatus, effectiveFrom);

        Agreement updated = agreementService.changeStatus(
            id,
            newStatus,
            effectiveFrom,
            updatedBy
        );

        return ResponseEntity.ok(AgreementResponse.from(updated));
    }

    // ========================================================================
    // Statistics
    // ========================================================================

    /**
     * Get agreement count by market context.
     *
     * @param tenantId Tenant ID
     * @param marketContext Market context
     * @return Count
     */
    @GetMapping("/count")
    @Operation(summary = "Count agreements", description = "Count agreements by tenant and market")
    public ResponseEntity<Map<String, Object>> countAgreements(
        @RequestParam String tenantId,
        @RequestParam MarketContext marketContext
    ) {
        long count = agreementService.countByTenantAndContext(tenantId, marketContext);

        return ResponseEntity.ok(Map.of(
            "tenantId", tenantId,
            "marketContext", marketContext,
            "count", count
        ));
    }
}
