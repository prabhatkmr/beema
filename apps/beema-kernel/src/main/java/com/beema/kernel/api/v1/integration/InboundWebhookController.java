package com.beema.kernel.api.v1.integration;

import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.agreement.MarketContext;
import com.beema.kernel.domain.integration.InboundHook;
import com.beema.kernel.domain.integration.InboundHookLog;
import com.beema.kernel.repository.integration.InboundHookLogRepository;
import com.beema.kernel.repository.integration.InboundHookRepository;
import com.beema.kernel.service.agreement.AgreementService;
import com.beema.kernel.service.integration.SignatureVerificationService;
import com.beema.kernel.service.integration.WebhookTransformationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hooks")
@Tag(name = "Inbound Webhooks", description = "Generic webhook entry point for external partner integrations")
public class InboundWebhookController {

    private static final Logger log = LoggerFactory.getLogger(InboundWebhookController.class);

    private final InboundHookRepository hookRepository;
    private final InboundHookLogRepository hookLogRepository;
    private final SignatureVerificationService signatureService;
    private final WebhookTransformationService transformationService;
    private final AgreementService agreementService;

    public InboundWebhookController(InboundHookRepository hookRepository,
                                     InboundHookLogRepository hookLogRepository,
                                     SignatureVerificationService signatureService,
                                     WebhookTransformationService transformationService,
                                     AgreementService agreementService) {
        this.hookRepository = hookRepository;
        this.hookLogRepository = hookLogRepository;
        this.signatureService = signatureService;
        this.transformationService = transformationService;
        this.agreementService = agreementService;
    }

    @PostMapping("/{hookId}")
    @Operation(summary = "Receive inbound webhook",
            description = "Generic webhook endpoint. Looks up hook config, verifies HMAC signature, " +
                    "transforms payload via JEXL mapping, and persists to the target entity.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Webhook accepted and processed"),
            @ApiResponse(responseCode = "401", description = "Invalid signature"),
            @ApiResponse(responseCode = "404", description = "Hook not found or inactive"),
            @ApiResponse(responseCode = "400", description = "Transformation failed"),
            @ApiResponse(responseCode = "500", description = "Internal processing error")
    })
    public ResponseEntity<Map<String, String>> receiveWebhook(
            @PathVariable String hookId,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {

        log.info("Received webhook for hookId={}", hookId);

        InboundHookLog logEntry = new InboundHookLog();
        logEntry.setHookId(hookId);
        logEntry.setReceivedAt(OffsetDateTime.now());
        logEntry.setSourceIp(request.getRemoteAddr());
        logEntry.setRequestHeaders(new HashMap<>(headers));
        logEntry.setRequestBody(payload);

        // 1. Lookup hook config
        InboundHook hook = hookRepository.findByHookIdAndIsActive(hookId, true)
                .orElse(null);

        if (hook == null) {
            log.warn("Webhook hook not found or inactive: {}", hookId);
            logEntry.setStatus("NOT_FOUND");
            hookLogRepository.save(logEntry);
            throw new EntityNotFoundException("Webhook hook not found or inactive: " + hookId);
        }

        // 2. Verify HMAC signature
        String signatureHeaderName = hook.getSignatureHeader() != null
                ? hook.getSignatureHeader().toLowerCase()
                : "x-signature";
        String signature = headers.get(signatureHeaderName);

        if (!signatureService.verify(payload, signature, hook.getSignatureSecret())) {
            log.warn("Invalid signature for hookId={}", hookId);
            logEntry.setStatus("SIGNATURE_FAILED");
            hookLogRepository.save(logEntry);
            throw new InvalidSignatureException("Invalid webhook signature for hook: " + hookId);
        }

        // 3. Transform payload using JEXL mapping script
        Map<String, Object> transformed;
        try {
            transformed = transformationService.transform(payload, hook.getMappingScript());
            logEntry.setTransformedBody(transformed);
        } catch (WebhookTransformationService.WebhookTransformationException e) {
            log.error("Transformation failed for hookId={}: {}", hookId, e.getMessage());
            logEntry.setStatus("TRANSFORM_FAILED");
            logEntry.setErrorMessage(e.getMessage());
            hookLogRepository.save(logEntry);
            throw new WebhookTransformationException("Payload transformation failed: " + e.getMessage(), e);
        }

        // 4. Save to database based on target object type
        String entityId;
        try {
            entityId = persistTransformedData(hook, transformed);
        } catch (Exception e) {
            log.error("Failed to save webhook data for hookId={}: {}", hookId, e.getMessage(), e);
            logEntry.setStatus("SAVE_FAILED");
            logEntry.setErrorMessage(e.getMessage());
            hookLogRepository.save(logEntry);
            throw e;
        }

        // 5. Log success
        logEntry.setStatus("SUCCESS");
        logEntry.setEntityId(entityId);
        hookLogRepository.save(logEntry);

        log.info("Webhook processed successfully: hookId={}, entityId={}", hookId, entityId);

        return ResponseEntity.accepted()
                .body(Map.of(
                        "status", "accepted",
                        "hookId", hookId,
                        "entityId", entityId,
                        "message", "Webhook processed successfully"
                ));
    }

    private String persistTransformedData(InboundHook hook, Map<String, Object> transformed) {
        return switch (hook.getTargetObjectType()) {
            case "AGREEMENT" -> createAgreementFromWebhook(hook, transformed);
            default -> throw new UnsupportedOperationException(
                    "Unsupported target object type: " + hook.getTargetObjectType());
        };
    }

    private String createAgreementFromWebhook(InboundHook hook, Map<String, Object> data) {
        Agreement agreement = new Agreement();

        agreement.setAgreementNumber(getStringOrDefault(data, "agreementNumber",
                "WH-" + System.currentTimeMillis()));
        agreement.setExternalReference(getString(data, "externalReference"));
        agreement.setMarketContext(MarketContext.valueOf(
                getStringOrDefault(data, "marketContext", "RETAIL")));

        String typeId = getString(data, "agreementTypeId");
        if (typeId != null) {
            agreement.setAgreementTypeId(UUID.fromString(typeId));
        }

        String statusStr = getString(data, "status");
        agreement.setStatus(statusStr != null ? AgreementStatus.valueOf(statusStr) : AgreementStatus.DRAFT);

        agreement.setTenantId(UUID.fromString(hook.getTenantId()));
        agreement.setCurrencyCode(getStringOrDefault(data, "currencyCode", "GBP"));

        String inception = getString(data, "inceptionDate");
        if (inception != null) {
            agreement.setInceptionDate(LocalDate.parse(inception));
        }

        String expiry = getString(data, "expiryDate");
        if (expiry != null) {
            agreement.setExpiryDate(LocalDate.parse(expiry));
        }

        Object premium = data.get("totalPremium");
        if (premium != null) {
            agreement.setTotalPremium(new BigDecimal(premium.toString()));
        }

        Object sumInsured = data.get("totalSumInsured");
        if (sumInsured != null) {
            agreement.setTotalSumInsured(new BigDecimal(sumInsured.toString()));
        }

        // Any additional fields go into the JSONB attributes
        Map<String, Object> attributes = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!isAgreementCoreField(entry.getKey())) {
                attributes.put(entry.getKey(), entry.getValue());
            }
        }
        attributes.put("_webhookSource", "hook:" + data.getOrDefault("_hookPartner", "unknown"));
        agreement.setAttributes(attributes);
        agreement.setUpdatedBy("webhook:" + (data.getOrDefault("_hookPartner", "system")));

        Agreement saved = agreementService.createAgreement(agreement);
        return saved.getTemporalKey().getId().toString();
    }

    private boolean isAgreementCoreField(String field) {
        return switch (field) {
            case "agreementNumber", "externalReference", "marketContext",
                 "agreementTypeId", "status", "tenantId", "currencyCode",
                 "inceptionDate", "expiryDate", "totalPremium", "totalSumInsured" -> true;
            default -> false;
        };
    }

    private String getString(Map<String, Object> data, String key) {
        Object val = data.get(key);
        return val != null ? val.toString() : null;
    }

    private String getStringOrDefault(Map<String, Object> data, String key, String defaultValue) {
        String val = getString(data, key);
        return val != null ? val : defaultValue;
    }

    // Custom exceptions for webhook-specific error handling

    public static class InvalidSignatureException extends RuntimeException {
        public InvalidSignatureException(String message) {
            super(message);
        }
    }

    public static class WebhookTransformationException extends RuntimeException {
        public WebhookTransformationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
