package com.loopers.application.coupon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.MemberCouponService;
import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import com.loopers.infrastructure.coupon.CouponIssueStatusRedisRepository;
import com.loopers.support.auth.AdminValidator;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("CouponFacade 비동기 발급 요청 단위 테스트")
@ExtendWith(MockitoExtension.class)
class CouponFacadeAsyncIssueTest {

    @InjectMocks
    private CouponFacade couponFacade;

    @Mock
    private CouponService couponService;

    @Mock
    private MemberCouponService memberCouponService;

    @Mock
    private MemberService memberService;

    @Mock
    private AdminValidator adminValidator;

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Mock
    private CouponIssueStatusRedisRepository couponIssueStatusRedisRepository;

    @Spy
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("쿠폰 발급 요청 시 Kafka로 메시지를 발행하고 requestId를 반환한다")
    void requestCouponIssueAsync_publishesToKafka() {
        // Arrange
        Member member = mock(Member.class);
        given(member.getId()).willReturn(1L);
        given(memberService.authenticate("user1", "pass")).willReturn(member);
        given(kafkaTemplate.send(eq("coupon-issue-requests"), eq("10"), any()))
            .willReturn(CompletableFuture.completedFuture(null));

        // Act
        CouponIssueRequestInfo result = couponFacade.requestCouponIssueAsync("user1", "pass", 10L);

        // Assert
        assertThat(result.requestId()).isNotNull();
        assertThat(result.couponId()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo("PENDING");
        verify(couponService).validateCouponExists(10L);
        verify(kafkaTemplate).send(eq("coupon-issue-requests"), eq("10"), any());
    }

    @Test
    @DisplayName("Kafka 발행 실패 시 CoreException(SERVICE_UNAVAILABLE)을 던진다")
    void requestCouponIssueAsync_throwsOnKafkaFailure() {
        // Arrange
        Member member = mock(Member.class);
        given(member.getId()).willReturn(1L);
        given(memberService.authenticate("user1", "pass")).willReturn(member);

        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka 연결 실패"));
        given(kafkaTemplate.send(any(), any(), any())).willReturn((CompletableFuture) failedFuture);

        // Act & Assert
        assertThatThrownBy(() ->
            couponFacade.requestCouponIssueAsync("user1", "pass", 10L)
        ).isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("Redis에 상태가 있으면 해당 상태를 반환한다")
    void getCouponIssueRequestStatus_returnsRedisStatus() {
        // Arrange
        Member member = mock(Member.class);
        given(memberService.authenticate("user1", "pass")).willReturn(member);
        given(couponIssueStatusRedisRepository.getRequestStatus("req-123"))
            .willReturn(Optional.of("SUCCESS"));

        // Act
        CouponIssueRequestStatusInfo result =
            couponFacade.getCouponIssueRequestStatus("user1", "pass", "req-123");

        // Assert
        assertThat(result.requestId()).isEqualTo("req-123");
        assertThat(result.status()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("Redis에 상태가 없으면 PENDING을 반환한다")
    void getCouponIssueRequestStatus_returnsPending_whenNotInRedis() {
        // Arrange
        Member member = mock(Member.class);
        given(memberService.authenticate("user1", "pass")).willReturn(member);
        given(couponIssueStatusRedisRepository.getRequestStatus("req-456"))
            .willReturn(Optional.empty());

        // Act
        CouponIssueRequestStatusInfo result =
            couponFacade.getCouponIssueRequestStatus("user1", "pass", "req-456");

        // Assert
        assertThat(result.requestId()).isEqualTo("req-456");
        assertThat(result.status()).isEqualTo("PENDING");
    }
}
