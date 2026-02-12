package com.beema.kernel.service.layout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LayoutResolutionServiceTest {

    @Autowired
    private LayoutResolutionService layoutResolutionService;

    @Test
    void resolveLayout_shouldReturnMotorPolicyLayout() {
        // When
        Map<String, Object> layout = layoutResolutionService.resolveLayout(
            "policy",
            "motor_comprehensive",
            "RETAIL",
            "default",
            "user"
        );

        // Then
        assertThat(layout).isNotNull();
        assertThat(layout.get("title")).isEqualTo("Motor Policy");
        assertThat(layout.get("sections")).isInstanceOf(List.class);

        // Verify metadata
        assertThat(layout).containsKey("_metadata");
        Map<String, Object> metadata = (Map<String, Object>) layout.get("_metadata");
        assertThat(metadata.get("layoutName")).isEqualTo("motor-policy-form");
        assertThat(metadata.get("context")).isEqualTo("policy");
        assertThat(metadata.get("objectType")).isEqualTo("motor_comprehensive");
    }

    @Test
    void resolveLayout_shouldReturnDefaultWhenNotFound() {
        // When
        Map<String, Object> layout = layoutResolutionService.resolveLayout(
            "unknown",
            "unknown_type",
            "RETAIL",
            "default",
            "user"
        );

        // Then
        assertThat(layout).isNotNull();
        assertThat(layout.get("_metadata")).isInstanceOf(Map.class);
        Map<String, Object> metadata = (Map<String, Object>) layout.get("_metadata");
        assertThat(metadata.get("default")).isEqualTo(true);
        assertThat(metadata.get("context")).isEqualTo("unknown");
    }

    @Test
    void resolveLayout_shouldHaveVehicleInfoSection() {
        // When
        Map<String, Object> layout = layoutResolutionService.resolveLayout(
            "policy",
            "motor_comprehensive",
            "RETAIL",
            "default",
            "user"
        );

        // Then
        List<Map<String, Object>> sections = (List<Map<String, Object>>) layout.get("sections");
        assertThat(sections).isNotEmpty();

        Map<String, Object> vehicleSection = sections.stream()
            .filter(s -> "vehicle-info".equals(s.get("id")))
            .findFirst()
            .orElse(null);

        assertThat(vehicleSection).isNotNull();
        assertThat(vehicleSection.get("title")).isEqualTo("Vehicle Information");
        assertThat(vehicleSection.get("columns")).isEqualTo(2);
    }

    @Test
    void resolveLayout_shouldHaveDriverInfoSection() {
        // When
        Map<String, Object> layout = layoutResolutionService.resolveLayout(
            "policy",
            "motor_comprehensive",
            "RETAIL",
            "default",
            "user"
        );

        // Then
        List<Map<String, Object>> sections = (List<Map<String, Object>>) layout.get("sections");
        Map<String, Object> driverSection = sections.stream()
            .filter(s -> "driver-info".equals(s.get("id")))
            .findFirst()
            .orElse(null);

        assertThat(driverSection).isNotNull();
        assertThat(driverSection.get("title")).isEqualTo("Driver Information");

        List<Map<String, Object>> fields = (List<Map<String, Object>>) driverSection.get("fields");
        assertThat(fields).isNotEmpty();
        assertThat(fields).anyMatch(f -> "driver_name".equals(f.get("id")));
    }

    @Test
    void getAllLayouts_shouldReturnAllEnabledLayouts() {
        // When
        List<Map<String, Object>> layouts = layoutResolutionService.getAllLayouts(null);

        // Then
        assertThat(layouts).isNotEmpty();
        assertThat(layouts).anyMatch(l -> "motor-policy-form".equals(l.get("layoutName")));
    }

    @Test
    void getAllLayouts_shouldFilterByContext() {
        // When
        List<Map<String, Object>> layouts = layoutResolutionService.getAllLayouts("policy");

        // Then
        assertThat(layouts).isNotEmpty();
        assertThat(layouts).allMatch(l -> "policy".equals(l.get("context")));
    }
}
