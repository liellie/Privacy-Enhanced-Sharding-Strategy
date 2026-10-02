package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import org.example.enums.TxType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chain_request")
public class ChainRequest {
     
    @TableId(value = "request_id", type = IdType.INPUT)
    private String requestId;

    private TxType txType;

    private String senderAddress;

    private String receiverAddress;

    private String assetId;

    private BigDecimal amount;

    private Integer targetShardId;

    private Long timestamp;

    private String signature;

    private String payload;
}