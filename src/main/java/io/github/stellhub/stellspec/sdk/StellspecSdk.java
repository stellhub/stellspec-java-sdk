package io.github.stellhub.stellspec.sdk;

import io.github.stellhub.stellspec.config.StellspecConfig;
import io.github.stellhub.stellspec.config.StellspecConfigLoader;
import io.github.stellhub.stellspec.config.StellspecConfigValidator;
import io.github.stellhub.stellspec.exception.StellspecException;
import io.github.stellhub.stellspec.exception.StellspecSdkError;
import io.github.stellhub.stellspec.exporter.ConsoleLogRecordExporter;
import io.github.stellhub.stellspec.exporter.FallbackLogRecordExporter;
import io.github.stellhub.stellspec.exporter.RetryingLogRecordExporter;
import io.github.stellhub.stellspec.internal.ResourceBuilderFactory;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;

/** SDK 入口。 */
public final class StellspecSdk {

    private StellspecSdk() {}

    /**
     * 从环境变量创建运行时。
     *
     * @return 运行时
     * @throws StellspecException 初始化失败时抛出
     */
    public static StellspecRuntime create() throws StellspecException {
        return create(StellspecConfigLoader.load());
    }

    /**
     * 从显式配置创建运行时。
     *
     * @param config 显式配置
     * @return 运行时
     * @throws StellspecException 初始化失败时抛出
     */
    public static StellspecRuntime create(StellspecConfig config) throws StellspecException {
        StellspecConfigValidator.validate(config);
        Resource resource = ResourceBuilderFactory.create(config);
        LogRecordExporter exporter = createExporter(config);
        SdkLoggerProvider loggerProvider =
                SdkLoggerProvider.builder()
                        .setResource(resource)
                        .addLogRecordProcessor(
                                BatchLogRecordProcessor.builder(exporter)
                                        .setScheduleDelay(config.getBatchTimeout())
                                        .setExporterTimeout(config.getExportTimeout())
                                        .setMaxExportBatchSize(config.getMaxBatchSize())
                                        .setMaxQueueSize(config.getMaxQueueSize())
                                        .build())
                        .build();
        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().setLoggerProvider(loggerProvider).build();
        return new StellspecRuntime(config, sdk, loggerProvider);
    }

    private static LogRecordExporter createExporter(StellspecConfig config)
            throws StellspecException {
        if (config.isDevelopment() || !"otlp".equalsIgnoreCase(config.getOutput())) {
            return new ConsoleLogRecordExporter(config.getFormat());
        }
        LogRecordExporter delegate =
                switch (config.getProtocol().toLowerCase()) {
                    case "http", "http/protobuf" -> buildHttpExporter(config);
                    case "grpc" -> buildGrpcExporter(config);
                    default ->
                            throw new StellspecException(
                                    StellspecSdkError.INVALID_CONFIGURATION,
                                    "unsupported protocol: " + config.getProtocol());
                };
        LogRecordExporter exporter = new RetryingLogRecordExporter(delegate, config.getRetry());
        return new FallbackLogRecordExporter(exporter, config.getFallbackFilePath());
    }

    private static LogRecordExporter buildGrpcExporter(StellspecConfig config) {
        OtlpGrpcLogRecordExporterBuilder builder =
                OtlpGrpcLogRecordExporter.builder()
                        .setEndpoint(config.getEndpoint())
                        .setTimeout(config.getExportTimeout());
        config.headerSnapshot().forEach(builder::addHeader);
        return builder.build();
    }

    private static LogRecordExporter buildHttpExporter(StellspecConfig config) {
        OtlpHttpLogRecordExporterBuilder builder =
                OtlpHttpLogRecordExporter.builder()
                        .setEndpoint(config.getEndpoint())
                        .setTimeout(config.getExportTimeout());
        config.headerSnapshot().forEach(builder::addHeader);
        return builder.build();
    }
}
