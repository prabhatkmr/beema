package com.beema.kernel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic smoke test to ensure application context loads.
 */
@SpringBootTest
@ActiveProfiles("test")
class KernelApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, Spring Boot context loaded successfully
        // with all beans and configurations
    }
}
