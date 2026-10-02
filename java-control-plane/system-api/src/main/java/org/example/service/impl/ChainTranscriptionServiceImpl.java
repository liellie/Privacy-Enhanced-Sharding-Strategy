package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.ChainTranscription;
import org.example.mapper.ChainTranscriptionMapper;
import org.example.service.ChainTranscriptionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ChainTranscriptionServiceImpl extends ServiceImpl<ChainTranscriptionMapper, ChainTranscription>
        implements ChainTranscriptionService {

    @Override
    public ChainTranscription createTranscription(ChainTranscription transcription) {
        if (transcription.getCreateTime() == null) {
            transcription.setCreateTime(System.currentTimeMillis());
        }
        save(transcription);
        return transcription;
    }

    @Override
    public ChainTranscription getByRequestId(String requestId) {
        return getById(requestId);
    }

    @Override
    public void markRecordedOnChain(List<String> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            return;
        }
        lambdaUpdate()
                .in(ChainTranscription::getRequestId, requestIds)
                .set(ChainTranscription::getRecordedOnChain, true)
                .update();
    }
}