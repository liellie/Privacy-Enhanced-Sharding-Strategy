package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ChainReceipt;
import org.example.enums.ReceiptStatus;
import org.example.mapper.ChainReceiptMapper;
import org.example.service.ChainReceiptService;
import org.springframework.stereotype.Service;

@Service
public class ChainReceiptServiceImpl extends ServiceImpl<ChainReceiptMapper, ChainReceipt>
        implements ChainReceiptService {

    @Override
    public ChainReceipt createReceipt(ChainReceipt receipt) {
        if (receipt.getTimestamp() == null) {
            receipt.setTimestamp(System.currentTimeMillis());
        }
        save(receipt);
        return receipt;
    }

    @Override
    public ChainReceipt getByRequestId(String requestId) {
        return getById(requestId);
    }

    @Override
    public ChainReceipt getByTxHash(String txHash) {
        return getOne(new LambdaQueryWrapper<ChainReceipt>()
                .eq(ChainReceipt::getTxHash, txHash)
                .last("LIMIT 1"));
    }

    @Override
    public long countByStatus(ReceiptStatus status) {
        return count(new LambdaQueryWrapper<ChainReceipt>()
                .eq(ChainReceipt::getStatus, status));
    }
}
