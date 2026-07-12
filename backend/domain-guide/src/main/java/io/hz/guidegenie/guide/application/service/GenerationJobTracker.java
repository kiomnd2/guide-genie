package io.hz.guidegenie.guide.application.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * 일괄 생성 진행 상황 추적(인메모리). 템플릿 실행 시 total을 등록하고, 항목 생성이 끝날 때마다 카운트한다.
 * 단일 app-api 인스턴스에서 @Async 생성이 도는 전제. (다중 인스턴스면 공유 저장소로 교체)
 */
@Component
public class GenerationJobTracker {

    public record Progress(int total, int done, int failed, boolean finished) {}

    private static final class Counter {
        final int total;
        final AtomicInteger done = new AtomicInteger();
        final AtomicInteger failed = new AtomicInteger();
        Counter(int total) { this.total = total; }
    }

    private final Map<String, Counter> jobs = new ConcurrentHashMap<>();

    public void start(String jobId, int total) {
        jobs.put(jobId, new Counter(total));
    }

    public void success(String jobId) {
        Counter c = jobs.get(jobId);
        if (c != null) {
            c.done.incrementAndGet();
        }
    }

    public void failure(String jobId) {
        Counter c = jobs.get(jobId);
        if (c != null) {
            c.failed.incrementAndGet();
        }
    }

    public Progress snapshot(String jobId) {
        Counter c = jobs.get(jobId);
        if (c == null) {
            return new Progress(0, 0, 0, true); // 없음 = 이미 끝났거나 만료
        }
        int done = c.done.get();
        int failed = c.failed.get();
        boolean finished = done + failed >= c.total;
        if (finished) {
            jobs.remove(jobId); // 완료된 job은 정리(마지막 조회에서 finished 반환)
        }
        return new Progress(c.total, done, failed, finished);
    }
}
