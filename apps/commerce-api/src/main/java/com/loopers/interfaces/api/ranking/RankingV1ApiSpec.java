package com.loopers.interfaces.api.ranking;

import com.loopers.domain.ranking.RankingPeriod;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Ranking V1 API", description = "상품 랭킹 API")
public interface RankingV1ApiSpec {

    @Operation(
        summary = "랭킹 조회",
        description = "일간/주간/월간 상품 랭킹을 조회합니다."
    )
    ApiResponse<List<RankingV1Dto.RankingResponse>> getRankings(
        @Parameter(description = "조회 기간 (DAILY, WEEKLY, MONTHLY)") RankingPeriod period,
        @Parameter(description = "기준 날짜 (yyyyMMdd)") String date,
        @Parameter(description = "페이지 번호 (1부터)") int page,
        @Parameter(description = "페이지 크기") int size
    );
}
