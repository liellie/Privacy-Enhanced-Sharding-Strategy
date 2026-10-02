package org.example.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ReceiptStatus {

    FAILED(0),

    SUCCESS(1);

    @EnumValue
    private final int code;

    ReceiptStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
