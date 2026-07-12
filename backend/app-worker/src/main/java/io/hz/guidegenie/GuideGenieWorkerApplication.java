package io.hz.guidegenie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * guide-genie 워커. 소스 증분 동기화 스케줄러(인바운드 어댑터)를 구동하는 비웹 배포 단위.
 * 스키마는 app-api(Flyway)가 소유하므로 워커는 ddl-auto=none, flyway 비활성으로 접속한다.
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class GuideGenieWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuideGenieWorkerApplication.class, args);
    }
}
