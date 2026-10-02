package org.example.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum TaskStatus {

    PENDING("PENDING"),

    PROCESSING("PROCESSING"),

    COMPLETED("COMPLETED"),

    FAILED("FAILED");

    @EnumValue
    private final String code;

    TaskStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
