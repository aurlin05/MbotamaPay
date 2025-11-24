package com.mbotamapay.backend.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify Flyway configuration is properly set up
 */
@SpringBootTest
@ActiveProfiles("test")
class FlywayConfigurationTest {

    @Autowired(required = false)
    private Flyway flyway;

    @Test
    void testFlywayIsDisabledInTestProfile() {
        // In test profile, Flyway should be disabled as we use H2 with create-drop
        assertThat(flyway).isNull();
    }
}
