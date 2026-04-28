package io.github.stellhub.stellspec.sdk;

import io.github.stellhub.stellspec.bridge.StellspecAppender;
import io.github.stellhub.stellspec.bridge.StellspecLogger;
import io.github.stellhub.stellspec.config.StellspecConfig;
import io.github.stellhub.stellspec.exception.StellspecException;
import io.github.stellhub.stellspec.exception.StellspecSdkError;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import java.util.concurrent.TimeUnit;
import lombok.Getter;

/**
 * SDK 运行时，负责 Provider 生命周期。
 */
@Getter
public class StellspecRuntime {

    private final StellspecConfig config;

    private final OpenTelemetrySdk openTelemetrySdk;

    private final SdkLoggerProvider loggerProvider;

    public StellspecRuntime(
            StellspecConfig config, OpenTelemetrySdk openTelemetrySdk, SdkLoggerProvider loggerProvider) {
        this.config = config;
        this.openTelemetrySdk = openTelemetrySdk;
        this.loggerProvider = loggerProvider;
    }

    /**
     * 创建指定名称的追加器。
     *
     * @param instrumentationScopeName instrumentation scope
     * @return 追加器
     */
    public StellspecAppender createAppender(String instrumentationScopeName) {
        Logger logger = loggerProvider.loggerBuilder(instrumentationScopeName).build();
        return new OtelLogAppender(logger, config);
    }

    /**
     * 创建指定名称的轻量 Logger。
     *
     * @param instrumentationScopeName scope name
     * @return 轻量 Logger
     */
    public StellspecLogger createLogger(String instrumentationScopeName) {
        return new StellspecLogger(instrumentationScopeName, createAppender(instrumentationScopeName));
    }

    /**
     * 主动刷新日志缓冲。
     *
     * @throws StellspecException 刷新失败时抛出
     */
    public void flush() throws StellspecException {
        if (!loggerProvider.forceFlush().join(10, TimeUnit.SECONDS).isSuccess()) {
            throw new StellspecException(StellspecSdkError.EXPORT_FAILED, "force flush timeout");
        }
    }

    /**
     * 优雅关闭 SDK。
     *
     * @throws StellspecException 关闭失败时抛出
     */
    public void shutdown() throws StellspecException {
        if (!openTelemetrySdk.shutdown().join(10, TimeUnit.SECONDS).isSuccess()) {
            throw new StellspecException(
                    StellspecSdkError.RUNTIME_SHUTDOWN_FAILED, "shutdown timeout");
        }
    }
}
