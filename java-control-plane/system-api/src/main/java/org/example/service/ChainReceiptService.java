package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.ChainReceipt;
import org.example.enums.ReceiptStatus;

public interface ChainReceiptService extends IService<ChainReceipt> {

    ChainReceipt createReceipt(ChainReceipt receipt);

    ChainReceipt getByRequestId(String requestId);

    ChainReceipt getByTxHash(String txHash);

    long countByStatus(ReceiptStatus status);
}
