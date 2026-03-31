package com.loopers.infrastructure.actionlog;

import com.loopers.domain.actionlog.UserActionLog;
import com.loopers.domain.actionlog.UserActionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserActionLogRepositoryImpl implements UserActionLogRepository {

    private final UserActionLogJpaRepository userActionLogJpaRepository;

    @Override
    public void save(UserActionLog userActionLog) {
        userActionLogJpaRepository.save(UserActionLogEntity.from(userActionLog));
    }
}
