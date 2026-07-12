package io.hz.guidegenie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/** guide-genie API 서버. 모든 domain-* 을 조합하는 인바운드(REST) 진입점. */
@EnableAsync
@SpringBootApplication
public class GuideGenieApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuideGenieApplication.class, args);
    }
}
