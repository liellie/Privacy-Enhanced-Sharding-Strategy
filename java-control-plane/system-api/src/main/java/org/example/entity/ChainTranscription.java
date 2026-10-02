package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.enums.ReceiptStatus;
import org.example.enums.TxType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chain_transcription")
public class ChainTranscription {

    @TableId(value = "request_id", type = IdType.INPUT)
    private String requestId;

    private String txHash;

    private TxType txType;

    private String senderAddress;

    private String receiverAddress;

    private String assetId;

    private BigDecimal amount;

    private Integer senderShardId;

    private Integer receiverShardId;

    private Integer targetShardId;

    private Boolean crossShard;

    private String routeSource;

    private String source;

    private String processor;

    private Long submitTime;

    private Long routeTime;

    private Long crossCheckTime;

    private Long receiptTime;

    private Long totalLatency;

    private Boolean recordedOnChain;

    private String onChainTxHash;

    private Long blockNumber;

    private Long gasUsed;

    private ReceiptStatus status;

    private String errorMessage;

    private String logs;

    private Long createTime;
}