package io.github.stellhub.stellspec.bridge;

import io.github.stellhub.stellspec.model.StellspecSeverity;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 提供给业务和自研框架的轻量日志门面。
 */
@RequiredArgsConstructor
public class StellspecLogger {

    private final String loggerName;

    private final StellspecAppender appender;

    /**
     * 输出调试日志。
     *
     * @param message 日志正文
     */
    public void debug(String message) {
        log(StellspecSeverity.DEBUG, message, Map.of(), null, null);
    }

    /**
     * 输出信息日志。
     *
     * @param message 日志正文
     */
    public void info(String message) {
        log(StellspecSeverity.INFO, message, Map.of(), null, null);
    }

    /**
     * 输出告警日志。
     *
     * @param message 日志正文
     */
    public void warn(String message) {
        log(StellspecSeverity.WARN, message, Map.of(), null, null);
    }

    /**
     * 输出错误日志。
     *
     * @param message 日志正文
     * @param throwable 异常对象
     */
    public void error(String message, Throwable throwable) {
        log(StellspecSeverity.ERROR, message, Map.of(), throwable, null);
    }

    /**
     * 输出结构化日志。
     *
     * @param severity 严重级别
     * @param message 日志正文
     * @param attributes 扩展属性
     * @param throwable 异常对象
     * @param error 错误描述
     */
    public void log(
            StellspecSeverity severity,
            String message,
            Map<String, ?> attributes,
            Throwable throwable,
            StellspecErrorDescriptor error) {
        StellspecLogEvent.StellspecLogEventBuilder builder =
                StellspecLogEvent.builder()
                        .severity(severity)
                        .loggerName(loggerName)
                        .threadName(Thread.currentThread().getName())
                        .message(message)
                        .throwable(throwable)
                        .error(error);
        if (attributes != null) {
            attributes.forEach(builder::attribute);
        }
        appender.append(builder.build());
    }
}
