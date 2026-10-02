package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.dto.RoutingResult;
import org.example.entity.ChainRequest;

import java.util.List;

public interface ChainRequestService extends IService<ChainRequest> {

    ChainRequest submit(ChainRequest request);

    boolean isCrossShard(ChainRequest request);

    Integer routeToShard(ChainRequest request);

    RoutingResult route(ChainRequest request);

    ChainRequest getByRequestId(String requestId);

    List<ChainRequest> listTimeoutRequests(long timeoutMillis);
}