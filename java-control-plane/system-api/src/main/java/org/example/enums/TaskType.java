package org.example.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum TaskType {

    CROSS_SHARD_TX("CROSS_SHARD_TX"),

    STATE_SYNC("STATE_SYNC"),

    REBALANCE("REBALANCE");

    @EnumValue
    private final String code;

    TaskType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
