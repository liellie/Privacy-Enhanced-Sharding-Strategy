package org.example.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum AgentStatus {

    ONLINE("ONLINE"),

    OFFLINE("OFFLINE");

    @EnumValue
    private final String code;

    AgentStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
