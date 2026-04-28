package io.github.stellhub.stellspec.exception;

import lombok.Getter;

/**
 * SDK 对外异常。
 */
@Getter
public class StellspecException extends Exception {

    private final String code;

    private final String domain;

    private final String reason;

    public StellspecException(StellspecSdkError error, String message) {
        super(message);
        this.code = error.getCode();
        this.domain = error.getDomain();
        this.reason = error.getReason();
    }

    public StellspecException(StellspecSdkError error, String message, Throwable cause) {
        super(message, cause);
        this.code = error.getCode();
        this.domain = error.getDomain();
        this.reason = error.getReason();
    }
}
