package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MemberCoupon {

    private Long id;
    private Long memberId;
    private Long couponId;
    private String couponCode;
    private MemberCouponStatus status;
    private Long usedOrderId;
    private LocalDateTime usedAt;
    private LocalDateTime issuedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Coupon coupon;

    public MemberCoupon(Long memberId, Long couponId, String couponCode, LocalDateTime expiredAt) {
        validateMemberId(memberId);
        validateCouponId(couponId);
        validateCouponCode(couponCode);
        validateExpiredAt(expiredAt);

        this.memberId = memberId;
        this.couponId = couponId;
        this.couponCode = couponCode;
        this.status = MemberCouponStatus.AVAILABLE;
        this.issuedAt = LocalDateTime.now();
        this.expiredAt = expiredAt;
    }

    public MemberCoupon(Long id, Long memberId, Long couponId, String couponCode,
                        MemberCouponStatus status, Long usedOrderId, LocalDateTime usedAt,
                        LocalDateTime issuedAt, LocalDateTime expiredAt,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.memberId = memberId;
        this.couponId = couponId;
        this.couponCode = couponCode;
        this.status = status;
        this.usedOrderId = usedOrderId;
        this.usedAt = usedAt;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void setCoupon(Coupon coupon) {
        this.coupon = coupon;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }

    public boolean isAvailable() {
        return status == MemberCouponStatus.AVAILABLE && !isExpired();
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public void use(Long orderId) {
        if (!isAvailable()) {
            if (status == MemberCouponStatus.USED) {
                throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
            }
            if (isExpired() || status == MemberCouponStatus.EXPIRED) {
                throw new CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.");
            }
            throw new CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.");
        }
        this.status = MemberCouponStatus.USED;
        this.usedOrderId = orderId;
        this.usedAt = LocalDateTime.now();
    }

    public void cancelUse() {
        if (status != MemberCouponStatus.USED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용된 쿠폰만 사용 취소할 수 있습니다.");
        }
        this.status = MemberCouponStatus.AVAILABLE;
        this.usedOrderId = null;
        this.usedAt = null;
    }

    public void expire() {
        if (status == MemberCouponStatus.AVAILABLE) {
            this.status = MemberCouponStatus.EXPIRED;
        }
    }

    private void validateMemberId(Long memberId) {
        if (memberId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "회원 ID는 필수입니다.");
        }
    }

    private void validateCouponId(Long couponId) {
        if (couponId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 ID는 필수입니다.");
        }
    }

    private void validateCouponCode(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 코드는 필수입니다.");
        }
    }

    private void validateExpiredAt(LocalDateTime expiredAt) {
        if (expiredAt == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "만료일은 필수입니다.");
        }
    }
}
