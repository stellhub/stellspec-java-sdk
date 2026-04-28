package io.github.stellhub.stellspec.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/** Java SDK 统一配置模型。 */
@Getter
@Builder(toBuilder = true)
public class StellspecConfig {

    private final String serviceName;

    private final String serviceNamespace;

    private final String serviceVersion;

    private final String serviceInstanceId;

    private final String environment;

    private final String cluster;

    private final String region;

    private final String zone;

    private final String idc;

    private final String hostName;

    private final String hostIp;

    private final String nodeName;

    private final String k8sNamespace;

    private final String podName;

    private final String podUid;

    private final String podIp;

    private final String containerName;

    private final String endpoint;

    @Builder.Default private final boolean insecure = true;

    @Builder.Default private final String protocol = "grpc";

    private final String format;

    private final String output;

    @Builder.Default private final String level = "info";

    @Builder.Default private final boolean development = true;

    @Builder.Default private final boolean enableCaller = true;

    @Builder.Default private final boolean enableStacktrace = true;

    @Builder.Default private final Duration batchTimeout = Duration.ofSeconds(5);

    @Builder.Default private final Duration exportTimeout = Duration.ofSeconds(3);

    @Builder.Default private final int maxBatchSize = 512;

    @Builder.Default private final int maxQueueSize = 2048;

    @Builder.Default private final String fallbackFilePath = "logs/stellspec-fallback.log";

    @Builder.Default private final RetryConfig retry = RetryConfig.builder().build();

    @Singular("header")
    private final Map<String, String> headers;

    @Singular("resourceAttribute")
    private final Map<String, String> resourceAttributes;

    /**
     * 返回一份始终非空的 Header 副本。
     *
     * @return Header 副本
     */
    public Map<String, String> headerSnapshot() {
        return headers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
    }

    /**
     * 返回一份始终非空的资源属性副本。
     *
     * @return 资源属性副本
     */
    public Map<String, String> resourceAttributeSnapshot() {
        return resourceAttributes == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(resourceAttributes);
    }
}
