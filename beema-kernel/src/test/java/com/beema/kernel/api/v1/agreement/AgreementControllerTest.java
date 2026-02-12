package com.beema.kernel.api.v1.agreement;

import com.beema.kernel.api.v1.agreement.dto.AgreementRequest;
import com.beema.kernel.api.v1.agreement.dto.AgreementResponse;
import com.beema.kernel.api.v1.agreement.dto.AgreementUpdateRequest;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.metadata.MarketContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for AgreementController REST API.
 *
 * Tests:
 * - CRUD operations
 * - Temporal queries
 * - Attribute searches
 * - Status changes
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AgreementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/agreements";
    private static final String TENANT_ID = "tenant-test";

    @Test
    void shouldCreateAgreement() throws Exception {
        // Given: Agreement request
        AgreementRequest request = new AgreementRequest(
            "TEST-API-001",
            "AUTO_POLICY",
            MarketContext.RETAIL,
            AgreementStatus.DRAFT,
            Map.of(
                "vehicle_vin", "1HGCM82633A123456",
                "vehicle_year", 2024,
                "vehicle_make", "Honda",
                "vehicle_model", "Accord",
                "primary_driver_age", 35
            ),
            "US",
            TENANT_ID,
            "api-test-user",
            "api-test-user"
        );

        // When: POST /api/v1/agreements
        MvcResult result = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.agreementNumber").value("TEST-API-001"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.isCurrent").value(true))
            .andExpect(jsonPath("$.id").exists())
            .andReturn();

        // Then: Response contains created agreement
        String responseBody = result.getResponse().getContentAsString();
        AgreementResponse response = objectMapper.readValue(responseBody, AgreementResponse.class);

        assertThat(response.id()).isNotNull();
        assertThat(response.agreementNumber()).isEqualTo("TEST-API-001");
        assertThat(response.attributes()).containsEntry("vehicle_vin", "1HGCM82633A123456");
    }

    @Test
    void shouldGetAgreementById() throws Exception {
        // Given: Created agreement
        String agreementId = createTestAgreement("TEST-GET-001");

        // When: GET /api/v1/agreements/{id}
        mockMvc.perform(get(BASE_URL + "/" + agreementId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(agreementId))
            .andExpect(jsonPath("$.agreementNumber").value("TEST-GET-001"))
            .andExpect(jsonPath("$.isCurrent").value(true));
    }

    @Test
    void shouldUpdateAgreement() throws Exception {
        // Given: Created agreement
        String agreementId = createTestAgreement("TEST-UPDATE-001");

        // And: Update request (change driver age)
        AgreementUpdateRequest updateRequest = new AgreementUpdateRequest(
            null,
            Map.of(
                "vehicle_vin", "1HGCM82633A123456",
                "vehicle_year", 2024,
                "vehicle_make", "Honda",
                "vehicle_model", "Accord",
                "primary_driver_age", 36  // Changed
            ),
            OffsetDateTime.now(),
            "api-test-user"
        );

        // When: PUT /api/v1/agreements/{id}
        mockMvc.perform(put(BASE_URL + "/" + agreementId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(agreementId))
            .andExpect(jsonPath("$.attributes.primary_driver_age").value(36))
            .andExpect(jsonPath("$.isCurrent").value(true));
    }

    @Test
    void shouldGetAgreementHistory() throws Exception {
        // Given: Agreement with updates
        String agreementId = createTestAgreement("TEST-HISTORY-001");

        // Update twice
        updateAgreementAge(agreementId, 36);
        Thread.sleep(50);
        updateAgreementAge(agreementId, 37);

        // When: GET /api/v1/agreements/{id}/history
        mockMvc.perform(get(BASE_URL + "/" + agreementId + "/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3))  // Original + 2 updates
            .andExpect(jsonPath("$[0].attributes.primary_driver_age").value(35))
            .andExpect(jsonPath("$[1].attributes.primary_driver_age").value(36))
            .andExpect(jsonPath("$[2].attributes.primary_driver_age").value(37))
            .andExpect(jsonPath("$[2].isCurrent").value(true));
    }

    @Test
    void shouldGetAgreementAsOf() throws Exception {
        // Given: Agreement
        String agreementId = createTestAgreement("TEST-ASOF-001");
        OffsetDateTime t1 = OffsetDateTime.now();

        Thread.sleep(100);

        // Update
        updateAgreementAge(agreementId, 36);
        OffsetDateTime t2 = OffsetDateTime.now();

        // When: GET /api/v1/agreements/{id}/as-of?validTime=t1&transactionTime=t1
        mockMvc.perform(get(BASE_URL + "/" + agreementId + "/as-of")
                .param("validTime", t1.toString())
                .param("transactionTime", t1.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.attributes.primary_driver_age").value(35));

        // And: Query at t2 returns updated version
        mockMvc.perform(get(BASE_URL + "/" + agreementId + "/as-of")
                .param("validTime", t2.toString())
                .param("transactionTime", t2.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.attributes.primary_driver_age").value(36));
    }

    @Test
    void shouldListAgreementsByTenantAndMarket() throws Exception {
        // Given: Multiple agreements
        createTestAgreement("TEST-LIST-001");
        createTestAgreement("TEST-LIST-002");
        createTestAgreement("TEST-LIST-003");

        // When: GET /api/v1/agreements?tenantId=...&marketContext=RETAIL
        mockMvc.perform(get(BASE_URL)
                .param("tenantId", TENANT_ID)
                .param("marketContext", "RETAIL")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(3))
            .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void shouldFindByStatus() throws Exception {
        // Given: Agreements with different statuses
        String draftId = createTestAgreement("TEST-STATUS-DRAFT");
        String quotedId = createTestAgreement("TEST-STATUS-QUOTED");

        // Change one to QUOTED
        changeStatus(quotedId, AgreementStatus.QUOTED);

        // When: GET /api/v1/agreements/by-status?status=QUOTED
        mockMvc.perform(get(BASE_URL + "/by-status")
                .param("tenantId", TENANT_ID)
                .param("status", "QUOTED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("QUOTED"));
    }

    @Test
    void shouldChangeStatus() throws Exception {
        // Given: DRAFT agreement
        String agreementId = createTestAgreement("TEST-CHANGE-STATUS");

        // When: PATCH /api/v1/agreements/{id}/status
        mockMvc.perform(patch(BASE_URL + "/" + agreementId + "/status")
                .param("newStatus", "QUOTED")
                .param("effectiveFrom", OffsetDateTime.now().toString())
                .param("updatedBy", "underwriter"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("QUOTED"))
            .andExpect(jsonPath("$.updatedBy").value("underwriter"));
    }

    @Test
    void shouldCountAgreements() throws Exception {
        // Given: Multiple agreements
        createTestAgreement("TEST-COUNT-001");
        createTestAgreement("TEST-COUNT-002");

        // When: GET /api/v1/agreements/count
        mockMvc.perform(get(BASE_URL + "/count")
                .param("tenantId", TENANT_ID)
                .param("marketContext", "RETAIL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(2))
            .andExpect(jsonPath("$.tenantId").value(TENANT_ID))
            .andExpect(jsonPath("$.marketContext").value("RETAIL"));
    }

    @Test
    void shouldReturn404ForNonExistentAgreement() throws Exception {
        // When: GET /api/v1/agreements/{nonExistentId}
        mockMvc.perform(get(BASE_URL + "/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldValidateRequiredFields() throws Exception {
        // Given: Invalid request (missing required fields)
        AgreementRequest invalidRequest = new AgreementRequest(
            null,  // Missing agreement number
            "AUTO_POLICY",
            MarketContext.RETAIL,
            AgreementStatus.DRAFT,
            Map.of("vehicle_vin", "TEST"),
            "US",
            TENANT_ID,
            "test-user",
            "test-user"
        );

        // When: POST /api/v1/agreements
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String createTestAgreement(String agreementNumber) throws Exception {
        AgreementRequest request = new AgreementRequest(
            agreementNumber,
            "AUTO_POLICY",
            MarketContext.RETAIL,
            AgreementStatus.DRAFT,
            Map.of(
                "vehicle_vin", "1HGCM82633A123456",
                "vehicle_year", 2024,
                "vehicle_make", "Honda",
                "vehicle_model", "Accord",
                "primary_driver_age", 35
            ),
            "US",
            TENANT_ID,
            "api-test-user",
            "api-test-user"
        );

        MvcResult result = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AgreementResponse response = objectMapper.readValue(responseBody, AgreementResponse.class);

        return response.id().toString();
    }

    private void updateAgreementAge(String agreementId, int age) throws Exception {
        AgreementUpdateRequest updateRequest = new AgreementUpdateRequest(
            null,
            Map.of(
                "vehicle_vin", "1HGCM82633A123456",
                "vehicle_year", 2024,
                "vehicle_make", "Honda",
                "vehicle_model", "Accord",
                "primary_driver_age", age
            ),
            OffsetDateTime.now(),
            "api-test-user"
        );

        mockMvc.perform(put(BASE_URL + "/" + agreementId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk());
    }

    private void changeStatus(String agreementId, AgreementStatus newStatus) throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + agreementId + "/status")
                .param("newStatus", newStatus.toString())
                .param("effectiveFrom", OffsetDateTime.now().toString())
                .param("updatedBy", "api-test-user"))
            .andExpect(status().isOk());
    }
}
