package com.loopers.application;

import com.loopers.domain.coupon.CouponCodeGenerator;
import com.loopers.domain.coupon.CouponIssueDomain;
import com.loopers.domain.coupon.CouponIssueRepository;
import com.loopers.domain.coupon.MemberCouponIssueRepository;
import com.loopers.infrastructure.redis.CouponIssueRedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("CouponIssueService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class CouponIssueServiceTest {

    private CouponIssueService couponIssueService;

    @Mock
    private CouponIssueRepository couponIssueRepository;

    @Mock
    private MemberCouponIssueRepository memberCouponIssueRepository;

    @Mock
    private CouponIssueRedisRepository couponIssueRedisRepository;

    @Mock
    private CouponCodeGenerator couponCodeGenerator;

    @Mock
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        couponIssueService = new CouponIssueService(
            couponIssueRepository, memberCouponIssueRepository,
            couponIssueRedisRepository, couponCodeGenerator, transactionTemplate
        );
    }

    @Test
    @DisplayName("수량 초과 시 EXHAUSTED 상태를 기록하고 DECR 한다")
    void setsExhaustedStatus_whenQuantityExceeded() {
        // Arrange
        given(couponIssueRedisRepository.incrementIssuedCount(1L)).willReturn(51L);
        given(couponIssueRedisRepository.getTotalQuantity(1L)).willReturn(Optional.of(50));

        // Act
        couponIssueService.processCouponIssue("req-1", 10L, 1L);

        // Assert
        verify(couponIssueRedisRepository).decrementIssuedCount(1L);
        verify(couponIssueRedisRepository).setRequestStatus("req-1", "EXHAUSTED");
        verify(transactionTemplate, never()).executeWithoutResult(any());
    }

    @Test
    @DisplayName("수량 이내이면 DB 발급을 시도한다")
    void attemptsDbIssue_whenWithinQuantity() {
        // Arrange
        given(couponIssueRedisRepository.incrementIssuedCount(1L)).willReturn(1L);
        given(couponIssueRedisRepository.getTotalQuantity(1L)).willReturn(Optional.of(50));

        doAnswer(invocation -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        CouponIssueDomain coupon = new CouponIssueDomain(
            1L, 50, 0,
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), null
        );
        given(couponIssueRepository.findByIdForUpdate(1L)).willReturn(Optional.of(coupon));
        given(couponCodeGenerator.generate()).willReturn("ABCD-EFGH-1234");

        // Act
        couponIssueService.processCouponIssue("req-1", 10L, 1L);

        // Assert
        verify(couponIssueRepository).save(any(CouponIssueDomain.class));
        verify(memberCouponIssueRepository).save(eq(10L), eq(1L), eq("ABCD-EFGH-1234"), eq("AVAILABLE"), any());
        verify(couponIssueRedisRepository).setRequestStatus("req-1", "SUCCESS");
    }

    @Test
    @DisplayName("중복 발급 시 DUPLICATE 상태를 기록하고 DECR 한다")
    void setsDuplicateStatus_whenMemberAlreadyIssued() {
        // Arrange
        given(couponIssueRedisRepository.incrementIssuedCount(1L)).willReturn(1L);
        given(couponIssueRedisRepository.getTotalQuantity(1L)).willReturn(Optional.of(50));

        doThrow(new DataIntegrityViolationException("Duplicate entry"))
            .when(transactionTemplate).executeWithoutResult(any());

        // Act
        couponIssueService.processCouponIssue("req-1", 10L, 1L);

        // Assert
        verify(couponIssueRedisRepository).decrementIssuedCount(1L);
        verify(couponIssueRedisRepository).setRequestStatus("req-1", "DUPLICATE");
    }
}
