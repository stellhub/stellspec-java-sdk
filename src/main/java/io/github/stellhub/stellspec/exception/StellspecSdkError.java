package io.github.stellhub.stellspec.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** SDK 内部错误枚举。 */
@Getter
@RequiredArgsConstructor
public enum StellspecSdkError {
    INVALID_CONFIGURATION("INVALID_ARGUMENT", "stellspec.sdk", "INVALID_CONFIGURATION"),
    EXPORTER_BUILD_FAILED("FAILED_PRECONDITION", "stellspec.sdk", "EXPORTER_BUILD_FAILED"),
    EXPORT_FAILED("UNAVAILABLE", "stellspec.sdk", "EXPORT_FAILED"),
    RUNTIME_SHUTDOWN_FAILED("INTERNAL", "stellspec.sdk", "RUNTIME_SHUTDOWN_FAILED");

    private final String code;

    private final String domain;

    private final String reason;
}
