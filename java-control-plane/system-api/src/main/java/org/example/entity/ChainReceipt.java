package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import org.example.enums.ReceiptStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "chain_receipt", autoResultMap = true)
public class ChainReceipt {
     
    @TableId(value = "request_id", type = IdType.INPUT)
    private String requestId;

    private String txHash;

    private ReceiptStatus status;

    private Long blockNumber;

    private Long gasUsed;

    private String errorMessage;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> logs;

    private Long timestamp;
}