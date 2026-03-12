package com.loopers.batch.job.seedproduct;

import com.loopers.batch.job.seedproduct.step.SeedProductTasklet;
import com.loopers.batch.listener.JobListener;
import com.loopers.batch.listener.StepMonitorListener;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = SeedProductJobConfig.JOB_NAME)
@RequiredArgsConstructor
@Configuration
public class SeedProductJobConfig {
    public static final String JOB_NAME = "seedProductJob";
    private static final String STEP_SEED_PRODUCTS = "seedProductsStep";

    private final JobRepository jobRepository;
    private final JobListener jobListener;
    private final StepMonitorListener stepMonitorListener;
    private final SeedProductTasklet seedProductTasklet;
    private final PlatformTransactionManager transactionManager;

    @Bean(JOB_NAME)
    public Job seedProductJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(seedProductsStep())
                .listener(jobListener)
                .build();
    }

    @JobScope
    @Bean(STEP_SEED_PRODUCTS)
    public Step seedProductsStep() {
        return new StepBuilder(STEP_SEED_PRODUCTS, jobRepository)
                .tasklet(seedProductTasklet, transactionManager)
                .listener(stepMonitorListener)
                .build();
    }
}
