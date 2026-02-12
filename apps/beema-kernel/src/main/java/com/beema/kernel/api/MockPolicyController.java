package com.beema.kernel.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock Policy API Controller
 *
 * Simulates an external policy management system.
 * Demonstrates how Temporal workflows can integrate with external systems
 * via REST APIs.
 */
@RestController
@RequestMapping("/mock-policy-api")
@Tag(name = "Mock Policy API", description = "Simulated external policy management system")
public class MockPolicyController {

    private static final Logger log = LoggerFactory.getLogger(MockPolicyController.class);

    // In-memory storage for snapshots
    private final Map<String, Map<String, Object>> snapshots = new ConcurrentHashMap<>();
    private int versionCounter = 1;

    /**
     * Capture policy snapshot
     *
     * POST /mock-policy-api/snapshots
     *
     * Simulates capturing the current state of a policy/agreement.
     * In a real system, this would:
     * - Store the snapshot in a database
     * - Create an immutable record of the policy state
     * - Enable time-travel queries and audit trails
     */
    @PostMapping("/snapshots")
    @Operation(summary = "Capture policy snapshot",
               description = "Captures current state of an agreement for audit and compliance")
    public ResponseEntity<Map<String, Object>> captureSnapshot(@RequestBody Map<String, Object> request) {
        log.info("Capturing policy snapshot for request: {}", request);

        try {
            // Extract agreement information
            Object agreementId = request.get("agreementId");
            Object agreementType = request.get("agreementType");
            Map<String, Object> agreementData = (Map<String, Object>) request.get("agreementData");
            Boolean includeEndorsement = (Boolean) request.getOrDefault("includeEndorsement", false);

            // Generate snapshot ID
            String snapshotId = UUID.randomUUID().toString();
            Instant timestamp = Instant.now();

            // Create snapshot record
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("snapshotId", snapshotId);
            snapshot.put("agreementId", agreementId);
            snapshot.put("agreementType", agreementType);
            snapshot.put("agreementData", agreementData);
            snapshot.put("includeEndorsement", includeEndorsement);
            snapshot.put("timestamp", timestamp.toString());
            snapshot.put("version", versionCounter++);
            snapshot.put("status", "CAPTURED");
            snapshot.put("capturedBy", "mock-policy-api");

            // Store snapshot
            snapshots.put(snapshotId, snapshot);

            log.info("Policy snapshot captured successfully: snapshotId={}, agreementId={}, version={}",
                    snapshotId, agreementId, snapshot.get("version"));

            // Return snapshot response
            Map<String, Object> response = new HashMap<>();
            response.put("snapshotId", snapshotId);
            response.put("agreementId", agreementId);
            response.put("timestamp", timestamp.toString());
            response.put("version", snapshot.get("version"));
            response.put("status", "CAPTURED");
            response.put("message", "Policy snapshot captured successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error capturing policy snapshot", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", Instant.now().toString());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get snapshot by ID
     *
     * GET /mock-policy-api/snapshots/{snapshotId}
     */
    @GetMapping("/snapshots/{snapshotId}")
    @Operation(summary = "Get policy snapshot",
               description = "Retrieves a previously captured policy snapshot")
    public ResponseEntity<Map<String, Object>> getSnapshot(@PathVariable String snapshotId) {
        log.info("Retrieving policy snapshot: snapshotId={}", snapshotId);

        Map<String, Object> snapshot = snapshots.get(snapshotId);

        if (snapshot == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "NOT_FOUND");
            errorResponse.put("error", "Snapshot not found: " + snapshotId);
            errorResponse.put("timestamp", Instant.now().toString());

            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(snapshot);
    }

    /**
     * List all snapshots for an agreement
     *
     * GET /mock-policy-api/snapshots/agreement/{agreementId}
     */
    @GetMapping("/snapshots/agreement/{agreementId}")
    @Operation(summary = "List snapshots for agreement",
               description = "Lists all snapshots for a specific agreement")
    public ResponseEntity<Map<String, Object>> listSnapshotsByAgreement(@PathVariable Long agreementId) {
        log.info("Listing snapshots for agreement: agreementId={}", agreementId);

        var agreementSnapshots = snapshots.values().stream()
                .filter(s -> agreementId.equals(s.get("agreementId")))
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("agreementId", agreementId);
        response.put("snapshotCount", agreementSnapshots.size());
        response.put("snapshots", agreementSnapshots);

        return ResponseEntity.ok(response);
    }

    /**
     * Get snapshot statistics
     *
     * GET /mock-policy-api/snapshots/stats
     */
    @GetMapping("/snapshots/stats")
    @Operation(summary = "Get snapshot statistics",
               description = "Returns statistics about captured snapshots")
    public ResponseEntity<Map<String, Object>> getSnapshotStats() {
        log.info("Retrieving snapshot statistics");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSnapshots", snapshots.size());
        stats.put("currentVersion", versionCounter - 1);
        stats.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(stats);
    }

    /**
     * Delete all snapshots (for testing)
     *
     * DELETE /mock-policy-api/snapshots
     */
    @DeleteMapping("/snapshots")
    @Operation(summary = "Delete all snapshots",
               description = "Deletes all snapshots (for testing only)")
    public ResponseEntity<Map<String, Object>> deleteAllSnapshots() {
        log.info("Deleting all snapshots");

        int count = snapshots.size();
        snapshots.clear();
        versionCounter = 1;

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("deletedCount", count);
        response.put("message", "All snapshots deleted");

        return ResponseEntity.ok(response);
    }
}
