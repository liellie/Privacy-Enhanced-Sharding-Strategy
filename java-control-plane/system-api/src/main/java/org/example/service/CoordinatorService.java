package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.Coordinator;
import org.example.enums.CoordinatorState;

public interface CoordinatorService extends IService<Coordinator> {

    Coordinator start();

    Long advanceEpoch();

    boolean updateGlobalState(CoordinatorState globalState);

    Long getCurrentEpoch();

    boolean updateShardCount(int totalShards);

    boolean updateEpochDuration(Long epochDuration);
}
