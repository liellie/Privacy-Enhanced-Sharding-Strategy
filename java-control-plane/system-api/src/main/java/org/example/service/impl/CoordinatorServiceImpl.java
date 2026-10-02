package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Coordinator;
import org.example.enums.CoordinatorState;
import org.example.mapper.CoordinatorMapper;
import org.example.service.CoordinatorService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CoordinatorServiceImpl extends ServiceImpl<CoordinatorMapper, Coordinator>
        implements CoordinatorService {

    private static final String DEFAULT_COORDINATOR_ID = "coordinator-1";
     
    private static final long INITIAL_EPOCH = 1L;
     
    private static final Object START_LOCK = new Object();

    @Override
    public Coordinator start() {
        Coordinator coordinator = getGlobal();
        if (coordinator != null) {
            return coordinator;
        }
        synchronized (START_LOCK) {
             
            coordinator = getGlobal();
            if (coordinator != null) {
                return coordinator;
            }
            coordinator = Coordinator.builder()
                    .coordinatorId(DEFAULT_COORDINATOR_ID)
                    .currentEpoch(INITIAL_EPOCH)
                    .epochStartTime(System.currentTimeMillis())
                    .globalState(CoordinatorState.RUNNING)
                    .totalShards(0)
                    .build();
            try {
                save(coordinator);
            } catch (DuplicateKeyException e) {
                 
                log.warn("协调者记录已存在（并发创建），复用已有记录");
                coordinator = getGlobal();
            }
        }
        return coordinator;
    }

    @Override
    public Long advanceEpoch() {
        Coordinator coordinator = start();
        String coordinatorId = coordinator.getCoordinatorId();
        lambdaUpdate()
                .eq(Coordinator::getCoordinatorId, coordinatorId)
                .setSql("current_epoch = current_epoch + 1")
                .set(Coordinator::getEpochStartTime, System.currentTimeMillis())
                .update();
         
        Coordinator updated = getById(coordinatorId);
        return updated == null ? null : updated.getCurrentEpoch();
    }

    @Override
    public boolean updateGlobalState(CoordinatorState globalState) {
        Coordinator coordinator = start();
        return lambdaUpdate()
                .eq(Coordinator::getCoordinatorId, coordinator.getCoordinatorId())
                .set(Coordinator::getGlobalState, globalState)
                .update();
    }

    @Override
    public Long getCurrentEpoch() {
        Coordinator coordinator = getGlobal();
        return coordinator == null ? null : coordinator.getCurrentEpoch();
    }

    @Override
    public boolean updateShardCount(int totalShards) {
        Coordinator coordinator = start();
        return lambdaUpdate()
                .eq(Coordinator::getCoordinatorId, coordinator.getCoordinatorId())
                .set(Coordinator::getTotalShards, totalShards)
                .update();
    }

    @Override
    public boolean updateEpochDuration(Long epochDuration) {
        Coordinator coordinator = start();
        return lambdaUpdate()
                .eq(Coordinator::getCoordinatorId, coordinator.getCoordinatorId())
                .set(Coordinator::getEpochDuration, epochDuration)
                .update();
    }

    private Coordinator getGlobal() {
        return getOne(new LambdaQueryWrapper<Coordinator>()
                .orderByAsc(Coordinator::getCoordinatorId)
                .last("LIMIT 1"));
    }
}