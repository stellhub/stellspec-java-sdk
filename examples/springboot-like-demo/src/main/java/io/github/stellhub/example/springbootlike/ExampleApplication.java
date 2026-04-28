package io.github.stellhub.example.springbootlike;

import io.github.stellhub.stellspec.adapter.springboot.StellspecBootstrapResult;
import io.github.stellhub.stellspec.adapter.springboot.StellspecSpringBootAdapter;
import io.github.stellhub.stellspec.exception.StellspecException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Boot 风格独立示例。
 */
public final class ExampleApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleApplication.class);

    private ExampleApplication() {}

    /**
     * 应用入口。
     *
     * @param args 启动参数
     * @throws Exception 运行异常
     */
    public static void main(String[] args) throws Exception {
        StellspecSpringBootAdapter adapter = StellspecSpringBootAdapter.initialize(args);
        try {
            StellspecBootstrapResult result = adapter.getBootstrapResult();
            System.out.println("stellspec bootstrap mode=" + result.getMode());
            LOGGER.info("stellspec bootstrap mode={}", result.getMode());
            LOGGER.info("service.name={}", result.getConfig().getServiceName());
            LOGGER.info("environment={}", result.getConfig().getEnvironment());
            LOGGER.warn("this is a warning from slf4j/logback bridge");
            LOGGER.error("this is an error from slf4j/logback bridge");

            if (result.getRuntime() != null) {
                try {
                    result.getRuntime().flush();
                } catch (StellspecException exception) {
                    System.err.println("flush skipped: " + exception.getMessage());
                }
            }
        } finally {
            try {
                adapter.close();
            } catch (StellspecException exception) {
                System.err.println("adapter close skipped: " + exception.getMessage());
            }
        }
    }
}
