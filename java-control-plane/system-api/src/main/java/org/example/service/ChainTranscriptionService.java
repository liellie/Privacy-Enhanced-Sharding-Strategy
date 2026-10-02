package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.ChainTranscription;

import java.util.List;

public interface ChainTranscriptionService extends IService<ChainTranscription> {

    ChainTranscription createTranscription(ChainTranscription transcription);

    ChainTranscription getByRequestId(String requestId);

    void markRecordedOnChain(List<String> requestIds);
}