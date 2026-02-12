package com.beema.kernel.api.v1.layout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LayoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getLayout_shouldReturnMotorPolicyLayout() throws Exception {
        mockMvc.perform(get("/api/v1/layouts/policy/motor_comprehensive")
                .param("marketContext", "RETAIL")
                .header("X-Tenant-ID", "default")
                .header("X-User-Role", "user"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Motor Policy"))
            .andExpect(jsonPath("$.sections").isArray())
            .andExpect(jsonPath("$.sections", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$._metadata.layoutName").value("motor-policy-form"))
            .andExpect(jsonPath("$._metadata.context").value("policy"))
            .andExpect(jsonPath("$._metadata.objectType").value("motor_comprehensive"));
    }

    @Test
    void getLayout_shouldReturnDefaultLayoutWhenNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/layouts/unknown/unknown_type")
                .param("marketContext", "RETAIL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._metadata.default").value(true))
            .andExpect(jsonPath("$._metadata.context").value("unknown"));
    }

    @Test
    void getAllLayouts_shouldReturnLayoutsList() throws Exception {
        mockMvc.perform(get("/api/v1/layouts/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").isNumber())
            .andExpect(jsonPath("$.layouts").isArray())
            .andExpect(jsonPath("$.layouts[*].layoutName").exists());
    }

    @Test
    void getAllLayouts_shouldFilterByContext() throws Exception {
        mockMvc.perform(get("/api/v1/layouts/all")
                .param("context", "policy"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.layouts").isArray())
            .andExpect(jsonPath("$.layouts[*].context").value(everyItem(equalTo("policy"))));
    }

    @Test
    void healthCheck_shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/v1/layouts/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("layout-resolution"));
    }

    @Test
    void getLayout_shouldUseDefaultHeaderValues() throws Exception {
        mockMvc.perform(get("/api/v1/layouts/policy/motor_comprehensive"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Motor Policy"));
    }
}
