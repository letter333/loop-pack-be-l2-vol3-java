package com.loopers.application;

import com.loopers.domain.coupon.CouponCodeGenerator;
import com.loopers.domain.coupon.CouponIssueDomain;
import com.loopers.domain.coupon.CouponIssueRepository;
import com.loopers.domain.coupon.MemberCouponIssueRepository;
import com.loopers.infrastructure.redis.CouponIssueRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final CouponIssueRepository couponIssueRepository;
    private final MemberCouponIssueRepository memberCouponIssueRepository;
    private final CouponIssueRedisRepository couponIssueRedisRepository;
    private final CouponCodeGenerator couponCodeGenerator;
    private final TransactionTemplate transactionTemplate;

    public void processCouponIssue(String requestId, Long memberId, Long couponId) {
        // 1. Redis INCR pre-check
        Long currentCount = couponIssueRedisRepository.incrementIssuedCount(couponId);

        // 2. totalQuantity 조회 (Redis → DB lazy loading)
        Optional<Integer> totalQuantityOpt = couponIssueRedisRepository.getTotalQuantity(couponId);
        int totalQuantity;

        if (totalQuantityOpt.isPresent()) {
            totalQuantity = totalQuantityOpt.get();
        } else {
            totalQuantity = loadTotalQuantityFromDb(couponId);
        }

        // 3. 수량 초과 체크
        if (currentCount > totalQuantity) {
            couponIssueRedisRepository.decrementIssuedCount(couponId);
            couponIssueRedisRepository.setRequestStatus(requestId, "EXHAUSTED");
            log.info("쿠폰 수량 소진: couponId={}, requestId={}", couponId, requestId);
            return;
        }

        // 4. DB 비관적 락으로 실제 발급
        try {
            transactionTemplate.executeWithoutResult(status -> {
                CouponIssueDomain coupon = couponIssueRepository.findByIdForUpdate(couponId)
                    .orElseThrow(() -> new IllegalStateException("쿠폰을 찾을 수 없습니다: " + couponId));

                if (!coupon.canIssue()) {
                    log.info("쿠폰 발급 불가: couponId={}, deleted={}, period={}, remaining={}",
                        couponId, coupon.isDeleted(), coupon.isWithinIssuePeriod(), coupon.hasRemainingQuantity());
                    couponIssueRedisRepository.setRequestStatus(requestId, "FAILED");
                    return;
                }

                coupon.issue();
                couponIssueRepository.save(coupon);

                String couponCode = couponCodeGenerator.generate();
                memberCouponIssueRepository.save(memberId, couponId, couponCode, "AVAILABLE", coupon.getValidUntil());

                couponIssueRedisRepository.setRequestStatus(requestId, "SUCCESS");
                log.debug("쿠폰 발급 성공: couponId={}, memberId={}, requestId={}", couponId, memberId, requestId);
            });
        } catch (DataIntegrityViolationException e) {
            couponIssueRedisRepository.decrementIssuedCount(couponId);
            couponIssueRedisRepository.setRequestStatus(requestId, "DUPLICATE");
            log.info("중복 쿠폰 발급 시도: couponId={}, memberId={}, requestId={}", couponId, memberId, requestId);
        } catch (Exception e) {
            couponIssueRedisRepository.decrementIssuedCount(couponId);
            couponIssueRedisRepository.setRequestStatus(requestId, "FAILED");
            log.error("쿠폰 발급 실패: couponId={}, memberId={}, requestId={}, error={}",
                couponId, memberId, requestId, e.getMessage());
        }
    }

    private int loadTotalQuantityFromDb(Long couponId) {
        return transactionTemplate.execute(status -> {
            CouponIssueDomain coupon = couponIssueRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new IllegalStateException("쿠폰을 찾을 수 없습니다: " + couponId));

            int total = coupon.getTotalQuantity();
            couponIssueRedisRepository.setTotalQuantity(couponId, total, coupon.getValidUntil());
            couponIssueRedisRepository.initIssuedCountIfAbsent(couponId, coupon.getIssuedQuantity(), coupon.getValidUntil());

            return total;
        });
    }
}
