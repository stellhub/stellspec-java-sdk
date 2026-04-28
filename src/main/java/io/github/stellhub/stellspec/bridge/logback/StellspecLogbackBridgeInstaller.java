package io.github.stellhub.stellspec.bridge.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import io.github.stellhub.stellspec.sdk.StellspecRuntime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Logback 桥接安装器。
 */
public final class StellspecLogbackBridgeInstaller {

    private static final String APPENDER_NAME_PREFIX = "stellspec-logback-";

    private StellspecLogbackBridgeInstaller() {}

    /**
     * 将 stellspec appender 安装到 Logback Root Logger。
     *
     * @param runtime stellspec 运行时
     * @param instrumentationScopeName scope 名称
     * @return 是否安装成功
     */
    public static boolean install(StellspecRuntime runtime, String instrumentationScopeName) {
        return install(runtime, instrumentationScopeName, false);
    }

    /**
     * 将 stellspec appender 安装到 Logback Root Logger。
     *
     * @param runtime stellspec 运行时
     * @param instrumentationScopeName scope 名称
     * @param replaceExistingRootAppenders 是否替换现有 root appenders
     * @return 是否安装成功
     */
    public static boolean install(
            StellspecRuntime runtime,
            String instrumentationScopeName,
            boolean replaceExistingRootAppenders) {
        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        if (!(factory instanceof LoggerContext context)) {
            return false;
        }

        Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        String appenderName = APPENDER_NAME_PREFIX + instrumentationScopeName;
        if (root.getAppender(appenderName) != null) {
            return true;
        }
        if (replaceExistingRootAppenders) {
            detachAllAppenders(root);
        }

        StellspecLogbackAppender appender =
                new StellspecLogbackAppender(runtime.createAppender(instrumentationScopeName));
        appender.setContext(context);
        appender.setName(appenderName);
        appender.start();
        root.addAppender(appender);
        return true;
    }

    private static void detachAllAppenders(Logger root) {
        List<Appender<ch.qos.logback.classic.spi.ILoggingEvent>> appenders = new ArrayList<>();
        Iterator<Appender<ch.qos.logback.classic.spi.ILoggingEvent>> iterator =
                root.iteratorForAppenders();
        while (iterator.hasNext()) {
            appenders.add(iterator.next());
        }
        for (Appender<ch.qos.logback.classic.spi.ILoggingEvent> appender : appenders) {
            root.detachAppender(appender);
            appender.stop();
        }
    }
}
