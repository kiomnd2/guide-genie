package io.hz.guidegenie.worker;

import io.hz.guidegenie.source.application.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 인바운드 어댑터 — 증분 동기화 스케줄 잡. 도메인 유스케이스(SyncService)를 트리거한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {

    private final SyncService syncService;

    @Scheduled(cron = "${guidegenie.sync.cron}")
    public void incrementalSync() {
        log.info("[Worker] incremental sync tick");
        syncService.syncAllIncremental();
    }
}
