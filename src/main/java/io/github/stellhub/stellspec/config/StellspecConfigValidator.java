package io.github.stellhub.stellspec.config;

import io.github.stellhub.stellspec.exception.StellspecException;
import io.github.stellhub.stellspec.exception.StellspecSdkError;

/** 配置校验器。 */
public final class StellspecConfigValidator {

    private StellspecConfigValidator() {}

    /**
     * 校验配置合法性。
     *
     * @param config 配置
     * @throws StellspecException 配置非法时抛出
     */
    public static void validate(StellspecConfig config) throws StellspecException {
        if (config == null) {
            throw new StellspecException(StellspecSdkError.INVALID_CONFIGURATION, "config is null");
        }
        if (isBlank(config.getServiceName())) {
            throw new StellspecException(
                    StellspecSdkError.INVALID_CONFIGURATION, "serviceName must not be blank");
        }
        if (!config.isDevelopment()
                && "otlp".equalsIgnoreCase(config.getOutput())
                && isBlank(config.getEndpoint())) {
            throw new StellspecException(
                    StellspecSdkError.INVALID_CONFIGURATION,
                    "endpoint must not be blank when output is otlp");
        }
        if (config.getMaxBatchSize() <= 0 || config.getMaxQueueSize() <= 0) {
            throw new StellspecException(
                    StellspecSdkError.INVALID_CONFIGURATION,
                    "maxBatchSize and maxQueueSize must be positive");
        }
        if (config.getMaxBatchSize() > config.getMaxQueueSize()) {
            throw new StellspecException(
                    StellspecSdkError.INVALID_CONFIGURATION, "maxBatchSize must not exceed maxQueueSize");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
