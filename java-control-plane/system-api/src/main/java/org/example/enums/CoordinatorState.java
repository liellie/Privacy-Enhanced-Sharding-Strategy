package org.example.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum CoordinatorState {

    RUNNING("RUNNING"),

    PAUSED("PAUSED"),

    REBALANCING("REBALANCING"),

    FINISHED("FINISHED");

    @EnumValue
    private final String code;

    CoordinatorState(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}