# stellspec-java-sdk

`stellspec-java-sdk` 是 `stellspec · 星谱` 面向 Java 生态的日志 SDK。它基于 OpenTelemetry Logs，对自研框架暴露一组不依赖 Spring 的通用接入能力：

- 开发环境默认输出到 `stdout` / `stderr`
- 非开发环境默认通过 OTLP 将日志发送到本机 `log-agent`
- 统一映射 `service.*`、`k8s.*`、`host.*` 等资源语义
- 支持 exporter 重试、最终失败本地落盘、超长正文截断
- 支持结构化错误属性 `error.type`、`stellar.error.*`

## 设计原则

- 不引入任何 Spring 相关依赖
- 以 OpenTelemetry 标准语义为主，`STELLAR_*` 与 `stellspec_*` 仅作为输入桥接
- 保持和 Go SDK 一致的环境行为：开发环境看控制台，非开发环境走本机 `log-agent`
- 给自研框架暴露通用 `Appender` / `Logger` API，而不是强绑定某个日志实现

## 当前能力

### 1. 统一配置模型

核心入口配置为 `io.github.stellhub.stellspec.config.StellspecConfig`，包含：

- 服务身份：`serviceName`、`serviceNamespace`、`serviceVersion`、`serviceInstanceId`
- 部署拓扑：`environment`、`cluster`、`region`、`zone`、`hostName`、`k8sNamespace`、`podName`
- 导出行为：`endpoint`、`protocol`、`output`、`format`
- 可靠性：`retry`、`fallbackFilePath`、`batchTimeout`、`exportTimeout`

环境变量读取优先级：

1. 代码显式传入 `StellspecConfig`
2. `stellspec_*`
3. `OTEL_SERVICE_NAME` / `OTEL_RESOURCE_ATTRIBUTES`
4. `STELLAR_*`
5. SDK 默认值

### 2. 环境行为

默认环境识别规则：

- `dev` / `local` / `development`：视为开发环境
- 其它环境：视为非开发环境

默认输出行为：

| 环境 | 默认输出 | 默认格式 | 默认端点 |
| :--- | :--- | :--- | :--- |
| 开发环境 | `stdout` | `console` | 无 |
| 非开发环境 | `otlp` | `json` | `http://localhost:4317` |

### 3. 可靠性保护

- OTLP exporter 默认重试：
  - 首次重试间隔 `5s`
  - 最大重试间隔 `30s`
  - 总重试窗口 `1m`
- 导出最终失败后写入本地兜底文件：
  - 默认路径：`logs/stellspec-fallback.log`
- 超长日志正文在 SDK 内统一截断到 `32 KiB`
  - 自动补充 `log.body_truncated`
  - 自动补充 `log.body_original_size`
  - 自动补充 `log.body_max_size`

### 4. Spring Boot 自动装配友好适配

仓库新增了一个不依赖 Spring 的适配器：

- `io.github.stellhub.stellspec.adapter.springboot.StellspecSpringBootAdapter`

它适合由你的自研框架在自动装配阶段直接调用，内部已经固化好启动期优先级：

1. 如果环境变量 `LOG_STDOUT=true`，或命令行参数传入 `--LOG_STDOUT=true`
2. 如果存在 `logback.configurationFile`，或类路径下存在 `logback.xml` / `logback-spring.xml`
3. 否则自动降级到 `otel`

### 5. slf4j/logback bridge

仓库新增了：

- `io.github.stellhub.stellspec.bridge.logback.StellspecLogbackAppender`
- `io.github.stellhub.stellspec.bridge.logback.StellspecLogbackBridgeInstaller`
- `io.github.stellhub.stellspec.bridge.logback.LogbackConfigurationDetector`

在 `OTEL` 模式下，适配器会自动把 bridge 安装到 Logback Root Logger。

## 快速开始

### 方式一：直接使用 SDK Logger

```java
package demo;

import io.github.stellhub.stellspec.bridge.StellspecErrorDescriptor;
import io.github.stellhub.stellspec.bridge.StellspecLogger;
import io.github.stellhub.stellspec.config.RetryConfig;
import io.github.stellhub.stellspec.config.StellspecConfig;
import io.github.stellhub.stellspec.model.StellspecSeverity;
import io.github.stellhub.stellspec.sdk.StellspecRuntime;
import io.github.stellhub.stellspec.sdk.StellspecSdk;
import java.time.Duration;
import java.util.Map;

public final class DemoApplication {

    private DemoApplication() {}

    public static void main(String[] args) throws Exception {
        StellspecConfig config =
                StellspecConfig.builder()
                        .serviceName("order-service")
                        .serviceNamespace("stellar.trade")
                        .serviceVersion("1.0.0")
                        .environment("prod")
                        .development(false)
                        .output("otlp")
                        .protocol("grpc")
                        .endpoint("http://localhost:4317")
                        .fallbackFilePath("logs/stellspec-fallback.log")
                        .batchTimeout(Duration.ofSeconds(5))
                        .exportTimeout(Duration.ofSeconds(3))
                        .retry(
                                RetryConfig.builder()
                                        .enabled(true)
                                        .initialInterval(Duration.ofSeconds(5))
                                        .maxInterval(Duration.ofSeconds(30))
                                        .maxElapsedTime(Duration.ofMinutes(1))
                                        .build())
                        .build();

        StellspecRuntime runtime = StellspecSdk.create(config);
        try {
            StellspecLogger logger = runtime.createLogger("order-service");
            logger.info("order created");
            logger.log(
                    StellspecSeverity.ERROR,
                    "cancel order failed",
                    Map.of("order.id", "o-1001", "tenant.id", "t-01"),
                    new IllegalStateException("order already settled"),
                    StellspecErrorDescriptor.builder()
                            .code("FAILED_PRECONDITION")
                            .domain("trade.order")
                            .reason("ORDER_NOT_CANCELLABLE")
                            .retryable(false)
                            .build());
            runtime.flush();
        } finally {
            runtime.shutdown();
        }
    }
}
```

### 方式二：给自研框架接 `Appender`

如果你的框架已经有自己的日志门面，只需要拿 `Appender`：

```java
StellspecRuntime runtime = StellspecSdk.create();
var appender = runtime.createAppender("custom-framework");

appender.append(
        io.github.stellhub.stellspec.bridge.StellspecLogEvent.builder()
                .severity(io.github.stellhub.stellspec.model.StellspecSeverity.INFO)
                .loggerName("custom-framework")
                .message("request handled")
                .attribute("http.request.method", "GET")
                .attribute("http.response.status_code", 200)
                .build());
```

### 方式三：接入 JUL

```java
java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger("demo");
StellspecRuntime runtime = StellspecSdk.create();
julLogger.addHandler(new io.github.stellhub.stellspec.bridge.jul.StellspecJulHandler(
        runtime.createAppender("jul-demo")));
```

### 方式四：Spring Boot 自动装配友好入口

```java
package demo;

import io.github.stellhub.stellspec.adapter.springboot.StellspecSpringBootAdapter;

public final class DemoBootstrap {

    private DemoBootstrap() {}

    public static void main(String[] args) throws Exception {
        try (StellspecSpringBootAdapter adapter = StellspecSpringBootAdapter.initialize(args)) {
            if (adapter.isOtelEnabled()) {
                System.out.println("stellspec otel bridge enabled");
            }
        }
    }
}
```

## 公开 API

### `StellspecSdk`

- `StellspecRuntime create()`
- `StellspecRuntime create(StellspecConfig config)`

### `StellspecRuntime`

- `StellspecAppender createAppender(String instrumentationScopeName)`
- `StellspecLogger createLogger(String instrumentationScopeName)`
- `void flush()`
- `void shutdown()`

### `StellspecLogger`

- `debug(String message)`
- `info(String message)`
- `warn(String message)`
- `error(String message, Throwable throwable)`
- `log(StellspecSeverity severity, String message, Map<String, ?> attributes, Throwable throwable, StellspecErrorDescriptor error)`

## 关键环境变量

### 全局基础变量

- `STELLAR_APP_NAME`
- `STELLAR_APP_NAMESPACE`
- `STELLAR_APP_VERSION`
- `STELLAR_APP_INSTANCE_ID`
- `STELLAR_ENV`
- `STELLAR_CLUSTER`
- `STELLAR_REGION`
- `STELLAR_ZONE`
- `STELLAR_IDC`
- `STELLAR_HOST_NAME`
- `STELLAR_HOST_IP`
- `STELLAR_NODE_NAME`
- `STELLAR_K8S_NAMESPACE`
- `STELLAR_POD_NAME`
- `STELLAR_POD_IP`
- `STELLAR_CONTAINER_NAME`

### stellspec 覆盖变量

- `stellspec_SERVICE_NAME`
- `stellspec_SERVICE_NAMESPACE`
- `stellspec_SERVICE_VERSION`
- `stellspec_SERVICE_INSTANCE_ID`
- `stellspec_ENVIRONMENT`
- `stellspec_ENDPOINT`
- `stellspec_PROTOCOL`
- `stellspec_OUTPUT`
- `stellspec_FORMAT`
- `stellspec_LEVEL`
- `stellspec_INSECURE`
- `stellspec_DEVELOPMENT`
- `stellspec_ENABLE_CALLER`
- `stellspec_ENABLE_STACKTRACE`
- `stellspec_BATCH_TIMEOUT`
- `stellspec_EXPORT_TIMEOUT`
- `stellspec_MAX_BATCH_SIZE`
- `stellspec_MAX_QUEUE_SIZE`
- `stellspec_FALLBACK_FILE_PATH`
- `stellspec_RETRY_ENABLED`
- `stellspec_RETRY_INITIAL_INTERVAL`
- `stellspec_RETRY_MAX_INTERVAL`
- `stellspec_RETRY_MAX_ELAPSED_TIME`
- `stellspec_HEADERS`
- `stellspec_RESOURCE_ATTRIBUTES`

## 包结构

```text
src/main/java/io/github/stellhub/stellspec
├── bridge
│   ├── jul
│   ├── StellspecAppender.java
│   ├── StellspecErrorDescriptor.java
│   ├── StellspecLogEvent.java
│   └── StellspecLogger.java
├── config
├── exception
├── exporter
├── internal
├── model
└── sdk
```

## 本地验证

```bash
mvn test
```

当前仓库已通过 `mvn test`，包含：

- Spring Boot 模式优先级测试
- 配置优先级测试
- 正文截断测试
- 初始化链路 smoke test

## Examples

独立示例位于：

- [examples/springboot-like-demo/README.md](/E:/PersonalCode/JavaProject/stellspec-java-sdk/examples/springboot-like-demo/README.md)
