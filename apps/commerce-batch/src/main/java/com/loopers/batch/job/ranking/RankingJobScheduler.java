package com.loopers.batch.job.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingJobScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier(WeeklyRankingJobConfig.JOB_NAME)
    private final Job weeklyRankingJob;

    @Qualifier(MonthlyRankingJobConfig.JOB_NAME)
    private final Job monthlyRankingJob;

    @Scheduled(cron = "0 0 1 * * MON")
    public void runWeeklyRankingJob() {
        String targetDate = LocalDate.now().minusDays(1).toString();
        log.info("주간 랭킹 배치 스케줄 실행: targetDate={}", targetDate);
        launchJob(weeklyRankingJob, targetDate);
    }

    @Scheduled(cron = "0 0 2 1 * *")
    public void runMonthlyRankingJob() {
        String targetDate = LocalDate.now().minusDays(1).toString();
        log.info("월간 랭킹 배치 스케줄 실행: targetDate={}", targetDate);
        launchJob(monthlyRankingJob, targetDate);
    }

    private void launchJob(Job job, String targetDate) {
        try {
            JobParameters params = new JobParametersBuilder()
                .addString("targetDate", targetDate)
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
            jobLauncher.run(job, params);
        } catch (Exception e) {
            log.error("랭킹 배치 실행 실패: job={}, targetDate={}", job.getName(), targetDate, e);
        }
    }
}
