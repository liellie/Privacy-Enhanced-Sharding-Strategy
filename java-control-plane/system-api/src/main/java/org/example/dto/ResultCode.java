package org.example.dto;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "success"),

    BAD_REQUEST(400, "bad request"),

    UNAUTHORIZED(401, "unauthorized"),

    NOT_FOUND(404, "not found"),

    INTERNAL_ERROR(500, "internal server error");

    private final int code;

    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
