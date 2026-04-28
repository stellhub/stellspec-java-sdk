package io.github.stellhub.stellspec.sdk;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.github.stellhub.stellspec.bridge.StellspecLogger;
import io.github.stellhub.stellspec.config.StellspecConfig;
import org.junit.jupiter.api.Test;

class StellspecSdkSmokeTest {

    @Test
    void shouldCreateRuntimeAndEmitLog() {
        assertDoesNotThrow(
                () -> {
                    StellspecConfig config =
                            StellspecConfig.builder()
                                    .serviceName("smoke-service")
                                    .serviceNamespace("stellar.demo")
                                    .serviceVersion("1.0.0")
                                    .environment("dev")
                                    .development(true)
                                    .output("stdout")
                                    .format("console")
                                    .build();
                    StellspecRuntime runtime = StellspecSdk.create(config);
                    try {
                        StellspecLogger logger = runtime.createLogger("smoke-service");
                        logger.info("smoke test log");
                        runtime.flush();
                    } finally {
                        runtime.shutdown();
                    }
                });
    }
}
