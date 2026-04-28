package io.github.stellhub.stellspec.bridge.jul;

import io.github.stellhub.stellspec.bridge.StellspecAppender;
import io.github.stellhub.stellspec.bridge.StellspecLogEvent;
import io.github.stellhub.stellspec.model.StellspecSeverity;
import java.time.Instant;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/** JUL 适配器，可直接挂载到自研框架或标准 JUL。 */
public class StellspecJulHandler extends Handler {

    private final StellspecAppender appender;

    public StellspecJulHandler(StellspecAppender appender) {
        this.appender = appender;
    }

    @Override
    public void publish(LogRecord record) {
        if (record == null || !isLoggable(record)) {
            return;
        }
        appender.append(
                StellspecLogEvent.builder()
                        .timestamp(Instant.ofEpochMilli(record.getMillis()))
                        .severity(StellspecSeverity.fromJulLevel(record.getLevel()))
                        .loggerName(record.getLoggerName())
                        .threadName(String.valueOf(record.getLongThreadID()))
                        .message(record.getMessage())
                        .throwable(record.getThrown())
                        .attribute("source.class", record.getSourceClassName())
                        .attribute("source.method", record.getSourceMethodName())
                        .build());
    }

    @Override
    public void flush() {
        // No buffering in handler.
    }

    @Override
    public void close() {
        // Nothing to close.
    }
}
