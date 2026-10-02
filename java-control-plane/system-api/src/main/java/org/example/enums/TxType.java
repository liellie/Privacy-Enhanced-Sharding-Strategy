package org.example.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum TxType {

    TRANSFER("TRANSFER"),

    LOCK("LOCK"),

    QUERY("QUERY");

    @EnumValue
    private final String code;

    TxType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
