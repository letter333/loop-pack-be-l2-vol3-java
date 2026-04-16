package com.loopers;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class CommerceBatchApplication {

    @PostConstruct
    public void started() {
        // set timezone
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CommerceBatchApplication.class, args);

        String jobName = context.getEnvironment().getProperty("spring.batch.job.name", "NONE");
        if (!"NONE".equals(jobName)) {
            int exitCode = SpringApplication.exit(context);
            System.exit(exitCode);
        }
    }
}
