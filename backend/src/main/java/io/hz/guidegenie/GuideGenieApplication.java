package io.hz.guidegenie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class GuideGenieApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuideGenieApplication.class, args);
    }
}
