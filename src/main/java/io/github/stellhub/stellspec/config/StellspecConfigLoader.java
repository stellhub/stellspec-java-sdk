package io.github.stellhub.stellspec.config;

import io.github.stellhub.stellspec.internal.EnvParsers;
import java.util.Map;

/**
 * 从环境变量装配配置。
 */
public final class StellspecConfigLoader {

    private StellspecConfigLoader() {}

    /**
     * 按规范优先级读取环境变量。
     *
     * @return 解析后的配置
     */
    public static StellspecConfig load() {
        return load(System.getenv());
    }

    /**
     * 按规范优先级读取环境变量。
     *
     * @param env 环境变量
     * @return 解析后的配置
     */
    public static StellspecConfig load(Map<String, String> env) {
        Map<String, String> otelResourceAttributes =
                EnvParsers.parseOtelResourceAttributes(env.get("OTEL_RESOURCE_ATTRIBUTES"));
        String environment =
                firstNonBlank(
                        env.get("stellspec_ENVIRONMENT"),
                        otelResourceAttributes.get("deployment.environment.name"),
                        env.get("STELLAR_ENV"),
                        "dev");
        boolean development =
                EnvParsers.parseBoolean(
                        env.get("stellspec_DEVELOPMENT"),
                        isDevelopmentEnvironment(environment));
        String output =
                firstNonBlank(
                        env.get("stellspec_OUTPUT"),
                        development ? "stdout" : "otlp");
        String format =
                firstNonBlank(
                        env.get("stellspec_FORMAT"),
                        development ? "console" : "json");

        RetryConfig retryConfig =
                RetryConfig.builder()
                        .enabled(
                                EnvParsers.parseBoolean(
                                        env.get("stellspec_RETRY_ENABLED"), true))
                        .initialInterval(
                                EnvParsers.parseDuration(
                                        env.get("stellspec_RETRY_INITIAL_INTERVAL"),
                                        RetryConfig.builder().build().getInitialInterval()))
                        .maxInterval(
                                EnvParsers.parseDuration(
                                        env.get("stellspec_RETRY_MAX_INTERVAL"),
                                        RetryConfig.builder().build().getMaxInterval()))
                        .maxElapsedTime(
                                EnvParsers.parseDuration(
                                        env.get("stellspec_RETRY_MAX_ELAPSED_TIME"),
                                        RetryConfig.builder().build().getMaxElapsedTime()))
                        .build();

        StellspecConfig.StellspecConfigBuilder builder =
                StellspecConfig.builder()
                        .serviceName(
                                firstNonBlank(
                                        env.get("stellspec_SERVICE_NAME"),
                                        env.get("OTEL_SERVICE_NAME"),
                                        env.get("STELLAR_APP_NAME"),
                                        "unknown-service"))
                        .serviceNamespace(
                                firstNonBlank(
                                        env.get("stellspec_SERVICE_NAMESPACE"),
                                        env.get("STELLAR_APP_NAMESPACE"),
                                        "default"))
                        .serviceVersion(
                                firstNonBlank(
                                        env.get("stellspec_SERVICE_VERSION"),
                                        env.get("STELLAR_APP_VERSION"),
                                        "unknown"))
                        .serviceInstanceId(
                                firstNonBlank(
                                        env.get("stellspec_SERVICE_INSTANCE_ID"),
                                        env.get("STELLAR_APP_INSTANCE_ID")))
                        .environment(environment)
                        .cluster(
                                firstNonBlank(
                                        env.get("stellspec_CLUSTER"), env.get("STELLAR_CLUSTER")))
                        .region(
                                firstNonBlank(
                                        env.get("stellspec_REGION"), env.get("STELLAR_REGION")))
                        .zone(
                                firstNonBlank(
                                        env.get("stellspec_ZONE"), env.get("STELLAR_ZONE")))
                        .idc(firstNonBlank(env.get("stellspec_IDC"), env.get("STELLAR_IDC")))
                        .hostName(
                                firstNonBlank(
                                        env.get("stellspec_HOST_NAME"),
                                        env.get("STELLAR_HOST_NAME")))
                        .hostIp(
                                firstNonBlank(
                                        env.get("stellspec_HOST_IP"), env.get("STELLAR_HOST_IP")))
                        .nodeName(
                                firstNonBlank(
                                        env.get("stellspec_NODE_NAME"),
                                        env.get("STELLAR_NODE_NAME")))
                        .k8sNamespace(
                                firstNonBlank(
                                        env.get("stellspec_K8S_NAMESPACE"),
                                        env.get("STELLAR_K8S_NAMESPACE")))
                        .podName(
                                firstNonBlank(
                                        env.get("stellspec_POD_NAME"), env.get("STELLAR_POD_NAME")))
                        .podUid(firstNonBlank(env.get("stellspec_POD_UID")))
                        .podIp(
                                firstNonBlank(
                                        env.get("stellspec_POD_IP"), env.get("STELLAR_POD_IP")))
                        .containerName(
                                firstNonBlank(
                                        env.get("stellspec_CONTAINER_NAME"),
                                        env.get("STELLAR_CONTAINER_NAME")))
                        .endpoint(
                                firstNonBlank(
                                        env.get("stellspec_ENDPOINT"),
                                        development ? null : "http://localhost:4317"))
                        .insecure(EnvParsers.parseBoolean(env.get("stellspec_INSECURE"), true))
                        .protocol(firstNonBlank(env.get("stellspec_PROTOCOL"), "grpc"))
                        .format(format)
                        .output(output)
                        .level(firstNonBlank(env.get("stellspec_LEVEL"), "info"))
                        .development(development)
                        .enableCaller(
                                EnvParsers.parseBoolean(
                                        env.get("stellspec_ENABLE_CALLER"), true))
                        .enableStacktrace(
                                EnvParsers.parseBoolean(
                                        env.get("stellspec_ENABLE_STACKTRACE"), true))
                        .batchTimeout(
                                EnvParsers.parseDuration(
                                        env.get("stellspec_BATCH_TIMEOUT"),
                                        StellspecConfig.builder().build().getBatchTimeout()))
                        .exportTimeout(
                                EnvParsers.parseDuration(
                                        env.get("stellspec_EXPORT_TIMEOUT"),
                                        StellspecConfig.builder().build().getExportTimeout()))
                        .maxBatchSize(
                                EnvParsers.parseInt(
                                        env.get("stellspec_MAX_BATCH_SIZE"),
                                        StellspecConfig.builder().build().getMaxBatchSize()))
                        .maxQueueSize(
                                EnvParsers.parseInt(
                                        env.get("stellspec_MAX_QUEUE_SIZE"),
                                        StellspecConfig.builder().build().getMaxQueueSize()))
                        .fallbackFilePath(
                                firstNonBlank(
                                        env.get("stellspec_FALLBACK_FILE_PATH"),
                                        StellspecConfig.builder().build().getFallbackFilePath()))
                        .retry(retryConfig);

        EnvParsers.parseKeyValuePairs(env.get("stellspec_HEADERS")).forEach(builder::header);
        EnvParsers.parseKeyValuePairs(env.get("stellspec_RESOURCE_ATTRIBUTES"))
                .forEach(builder::resourceAttribute);
        otelResourceAttributes.forEach(builder::resourceAttribute);
        return builder.build();
    }

    /**
     * 判断是否属于开发环境。
     *
     * @param environment 环境名
     * @return 是否开发环境
     */
    public static boolean isDevelopmentEnvironment(String environment) {
        if (environment == null) {
            return true;
        }
        return switch (environment.trim().toLowerCase()) {
            case "dev", "local", "development" -> true;
            default -> false;
        };
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
