package com.loopers.batch.job.ranking;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class RankingJobParametersValidator implements JobParametersValidator {

    @Override
    public void validate(JobParameters parameters) throws JobParametersInvalidException {
        String targetDate = parameters.getString("targetDate");

        if (targetDate == null || targetDate.isBlank()) {
            throw new JobParametersInvalidException("targetDate 파라미터는 필수입니다.");
        }

        LocalDate parsed;
        try {
            parsed = LocalDate.parse(targetDate);
        } catch (DateTimeParseException e) {
            throw new JobParametersInvalidException(
                "targetDate 형식이 올바르지 않습니다. (yyyy-MM-dd): " + targetDate
            );
        }

        if (parsed.isAfter(LocalDate.now())) {
            throw new JobParametersInvalidException(
                "targetDate는 미래 날짜일 수 없습니다: " + targetDate
            );
        }
    }
}
