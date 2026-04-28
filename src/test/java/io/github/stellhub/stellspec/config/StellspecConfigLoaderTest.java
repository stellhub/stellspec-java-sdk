package io.github.stellhub.stellspec.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class StellspecConfigLoaderTest {

    @Test
    void shouldPreferProductOverrides() {
        StellspecConfig config =
                StellspecConfigLoader.load(
                        Map.of(
                                "stellspec_SERVICE_NAME", "java-order-service",
                                "STELLAR_APP_NAME", "stellar-order-service",
                                "STELLAR_ENV", "prod"));
        assertEquals("java-order-service", config.getServiceName());
        assertEquals("prod", config.getEnvironment());
        assertFalse(config.isDevelopment());
        assertEquals("otlp", config.getOutput());
    }

    @Test
    void shouldUseDevelopmentDefaults() {
        StellspecConfig config =
                StellspecConfigLoader.load(Map.of("STELLAR_APP_NAME", "demo", "STELLAR_ENV", "dev"));
        assertTrue(config.isDevelopment());
        assertEquals("stdout", config.getOutput());
        assertEquals("console", config.getFormat());
    }
}
