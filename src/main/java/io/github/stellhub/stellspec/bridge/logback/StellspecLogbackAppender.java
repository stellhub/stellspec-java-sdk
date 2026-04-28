package io.github.stellhub.stellspec.bridge.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import io.github.stellhub.stellspec.bridge.StellspecAppender;
import io.github.stellhub.stellspec.bridge.StellspecLogEvent;
import io.github.stellhub.stellspec.model.StellspecSeverity;
import java.time.Instant;
import java.util.Map;

/**
 * Logback 到 stellspec 的桥接 Appender。
 */
public class StellspecLogbackAppender extends AppenderBase<ILoggingEvent> {

    private final StellspecAppender appender;

    public StellspecLogbackAppender(StellspecAppender appender) {
        this.appender = appender;
    }

    /**
     * 将 Logback 事件转发给 stellspec。
     *
     * @param eventObject Logback 事件
     */
    @Override
    protected void append(ILoggingEvent eventObject) {
        if (eventObject == null) {
            return;
        }
        StellspecLogEvent.StellspecLogEventBuilder builder =
                StellspecLogEvent.builder()
                        .timestamp(Instant.ofEpochMilli(eventObject.getTimeStamp()))
                        .severity(mapSeverity(eventObject.getLevel()))
                        .loggerName(eventObject.getLoggerName())
                        .threadName(eventObject.getThreadName())
                        .message(eventObject.getFormattedMessage())
                        .throwable(extractThrowable(eventObject.getThrowableProxy()));

        for (Map.Entry<String, String> entry : eventObject.getMDCPropertyMap().entrySet()) {
            builder.attribute("mdc." + entry.getKey(), entry.getValue());
        }
        StackTraceElement[] callerData = eventObject.getCallerData();
        if (callerData != null && callerData.length > 0) {
            StackTraceElement caller = callerData[0];
            builder.attribute("log.origin.class", caller.getClassName());
            builder.attribute("log.origin.method", caller.getMethodName());
            builder.attribute("log.origin.file", caller.getFileName());
            builder.attribute("log.origin.line", caller.getLineNumber());
        }
        appender.append(builder.build());
    }

    private StellspecSeverity mapSeverity(Level level) {
        if (level == null) {
            return StellspecSeverity.INFO;
        }
        return switch (level.toInt()) {
            case Level.TRACE_INT -> StellspecSeverity.TRACE;
            case Level.DEBUG_INT -> StellspecSeverity.DEBUG;
            case Level.INFO_INT -> StellspecSeverity.INFO;
            case Level.WARN_INT -> StellspecSeverity.WARN;
            case Level.ERROR_INT -> StellspecSeverity.ERROR;
            default -> StellspecSeverity.INFO;
        };
    }

    private Throwable extractThrowable(IThrowableProxy throwableProxy) {
        if (throwableProxy instanceof ThrowableProxy proxy && proxy.getThrowable() != null) {
            return proxy.getThrowable();
        }
        if (throwableProxy == null) {
            return null;
        }
        return new LogbackProxyException(throwableProxy);
    }

    private static final class LogbackProxyException extends RuntimeException {

        LogbackProxyException(IThrowableProxy proxy) {
            super(proxy.getClassName() + ": " + proxy.getMessage());
            StackTraceElementProxy[] stackTrace = proxy.getStackTraceElementProxyArray();
            if (stackTrace == null) {
                return;
            }
            StackTraceElement[] converted = new StackTraceElement[stackTrace.length];
            for (int index = 0; index < stackTrace.length; index++) {
                converted[index] = stackTrace[index].getStackTraceElement();
            }
            setStackTrace(converted);
        }
    }
}
