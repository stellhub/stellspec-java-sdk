package io.github.stellhub.stellspec.bridge;

import lombok.Builder;
import lombok.Getter;

/**
 * 结构化错误描述，遵循错误码规范。
 */
@Getter
@Builder(toBuilder = true)
public class StellspecErrorDescriptor {

    private final String code;

    private final String domain;

    private final String reason;

    private final Boolean retryable;

    /**
     * 创建一个仅包含基础规范码的错误描述。
     *
     * @param code 基础规范码
     * @return 错误描述
     */
    public static StellspecErrorDescriptor ofCode(String code) {
        return StellspecErrorDescriptor.builder().code(code).build();
    }
}
