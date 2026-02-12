package com.beema.kernel.service.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Service
public class SignatureVerificationService {

    private static final Logger log = LoggerFactory.getLogger(SignatureVerificationService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;

    public SignatureVerificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Verifies the HMAC SHA-256 signature of a webhook payload.
     *
     * @param payload   the raw request body as a Map
     * @param signature the signature value from the request header
     * @param secret    the shared secret for this hook
     * @return true if signature is valid, false otherwise
     */
    public boolean verify(Map<String, Object> payload, String signature, String secret) {
        if (signature == null || signature.isBlank()) {
            log.warn("Webhook signature is missing");
            return false;
        }
        if (secret == null || secret.isBlank()) {
            log.warn("Webhook secret is not configured");
            return false;
        }

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String computed = computeHmac(payloadJson, secret);

            // Strip common prefixes (e.g., "sha256=")
            String cleanSignature = signature.startsWith("sha256=")
                    ? signature.substring(7)
                    : signature;

            // Constant-time comparison to prevent timing attacks
            boolean valid = MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    cleanSignature.getBytes(StandardCharsets.UTF_8)
            );

            if (!valid) {
                log.warn("Webhook signature mismatch");
            }
            return valid;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload for signature verification", e);
            return false;
        }
    }

    private String computeHmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }
}
