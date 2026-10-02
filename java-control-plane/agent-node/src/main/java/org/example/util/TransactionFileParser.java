package org.example.util;

import org.example.entity.ChainRequest;
import org.example.enums.TxType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public final class TransactionFileParser {

    private TransactionFileParser() {
    }

    public static ChainRequest parseLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        Map<String, String> kv = splitKeyValues(line);
        String txHash = kv.get("Transaction Hash");
        String from = kv.get("From");
        if (txHash == null || txHash.isBlank() || from == null || from.isBlank()) {
            return null;
        }
        String to = kv.get("To");
         
        String requestId = txHash.startsWith("0x") ? txHash.substring(2) : txHash;
        BigDecimal amount = parseAmount(kv.get("Value"));
        if (amount == null) {
             
            return null;
        }
        return ChainRequest.builder()
                .requestId(requestId)
                .txType(TxType.TRANSFER)
                .senderAddress(from.trim())
                .receiverAddress(to == null || to.isBlank() ? null : to.trim())
                .amount(amount)
                .payload(txHash.trim())
                .build();
    }

    private static Map<String, String> splitKeyValues(String line) {
        Map<String, String> kv = new HashMap<>();
        for (String part : line.split(", ")) {
            int idx = part.indexOf(": ");
            if (idx <= 0) {
                continue;
            }
            kv.put(part.substring(0, idx), part.substring(idx + 2));
        }
        return kv;
    }

    private static BigDecimal parseAmount(String valueStr) {
        if (valueStr == null || valueStr.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(valueStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}