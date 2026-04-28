package io.github.stellhub.stellspec.adapter.springboot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class StellspecBootstrapModeResolverTest {

    @Test
    void shouldPreferStdoutWhenEnvEnabled() {
        StellspecBootstrapMode mode =
                StellspecBootstrapModeResolver.resolve(
                        Map.of("LOG_STDOUT", "true"),
                        new Properties(),
                        new String[0],
                        getClass().getClassLoader());
        assertEquals(StellspecBootstrapMode.STDOUT, mode);
    }

    @Test
    void shouldPreferStdoutWhenCommandLineEnabled() {
        StellspecBootstrapMode mode =
                StellspecBootstrapModeResolver.resolve(
                        Map.of(),
                        new Properties(),
                        new String[] {"--LOG_STDOUT=true"},
                        getClass().getClassLoader());
        assertEquals(StellspecBootstrapMode.STDOUT, mode);
    }

    @Test
    void shouldPreferLocalLogbackWhenConfigExists() throws Exception {
        Path tempDir = Files.createTempDirectory("stellspec-logback-test");
        Files.writeString(tempDir.resolve("logback.xml"), "<configuration />");
        try (URLClassLoader classLoader =
                new URLClassLoader(new URL[] {tempDir.toUri().toURL()}, null)) {
            StellspecBootstrapMode mode =
                    StellspecBootstrapModeResolver.resolve(
                            Map.of(), new Properties(), new String[0], classLoader);
            assertEquals(StellspecBootstrapMode.LOCAL_LOGBACK, mode);
        }
    }

    @Test
    void shouldFallbackToOtel() {
        StellspecBootstrapMode mode =
                StellspecBootstrapModeResolver.resolve(
                        Map.of(), new Properties(), new String[0], ClassLoader.getSystemClassLoader());
        assertEquals(StellspecBootstrapMode.OTEL, mode);
    }
}
