CREATE TABLE IF NOT EXISTS `shedlock` (
                                          `name` VARCHAR(64) NOT NULL COMMENT '스케줄러 락 이름 (@SchedulerLock의 name)',
    `lock_until` TIMESTAMP(3) NOT NULL COMMENT '락 유지 만료 시간',
    `locked_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '락을 획득한 시간',
    `locked_by` VARCHAR(255) NOT NULL COMMENT '락을 획득한 인스턴스/노드 이름',
    PRIMARY KEY (`name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;