# springboot-like-demo

这是一个不引入 Spring 依赖、但模拟 Spring Boot 自动装配入口的独立示例。

它演示了 `StellspecSpringBootAdapter` 的三段优先级：

1. `LOG_STDOUT=true` 或命令行参数显式要求本地标准输出
2. 存在 `logback.configurationFile` 或 `logback.xml` 时优先本地 Logback
3. 否则降级走 `otel -> log-agent`

## 预备步骤

先在仓库根目录执行：

```bash
mvn install
```

## 运行方式

### 1. 默认运行，走 OTel

```bash
mvn -f examples/springboot-like-demo/pom.xml exec:java -Dstellspec_EXPORT_TIMEOUT=200ms -Dstellspec_RETRY_MAX_ELAPSED_TIME=1s
```

### 2. 显式强制 stdout

```bash
mvn -f examples/springboot-like-demo/pom.xml exec:java -Dstellspec_EXPORT_TIMEOUT=200ms -Dstellspec_RETRY_MAX_ELAPSED_TIME=1s -Dexec.args=--LOG_STDOUT=true
```

### 3. 指定本地 logback 配置

```bash
mvn -f examples/springboot-like-demo/pom.xml exec:java -Dstellspec_EXPORT_TIMEOUT=200ms -Dstellspec_RETRY_MAX_ELAPSED_TIME=1s -Dlogback.configurationFile=src/main/resources/logback-local.xml
```

## 说明

- 默认 `OTEL` 模式下，示例会安装 `slf4j/logback -> stellspec` bridge
- 如果本机没有 `log-agent`，日志最终会按 SDK 兜底策略写入本地 fallback 文件
- 本示例会在启动时打印当前解析到的模式，便于验证优先级是否符合预期
