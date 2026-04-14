package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingFacade;
import com.loopers.application.ranking.RankingInfo;
import com.loopers.domain.ranking.RankingPeriod;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rankings")
public class RankingV1Controller implements RankingV1ApiSpec {

    private final RankingFacade rankingFacade;

    @GetMapping
    @Override
    public ApiResponse<List<RankingV1Dto.RankingResponse>> getRankings(
        @RequestParam(defaultValue = "DAILY") RankingPeriod period,
        @RequestParam(required = false) String date,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        List<RankingInfo> rankings = rankingFacade.getRankings(period, date, page, size);
        List<RankingV1Dto.RankingResponse> response = rankings.stream()
            .map(RankingV1Dto.RankingResponse::from)
            .toList();
        return ApiResponse.success(response);
    }
}
