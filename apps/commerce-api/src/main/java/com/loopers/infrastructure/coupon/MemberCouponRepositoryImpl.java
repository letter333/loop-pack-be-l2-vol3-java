package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.MemberCoupon;
import com.loopers.domain.coupon.MemberCouponRepository;
import com.loopers.domain.coupon.MemberCouponStatus;
import com.loopers.domain.coupon.MemberCouponStatusCounts;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberCouponRepositoryImpl implements MemberCouponRepository {

    private final MemberCouponJpaRepository memberCouponJpaRepository;

    @Override
    public Optional<MemberCoupon> findById(Long id) {
        return memberCouponJpaRepository.findById(id)
            .map(MemberCouponEntity::toDomain);
    }

    @Override
    public Optional<MemberCoupon> findByIdWithCoupon(Long id) {
        return memberCouponJpaRepository.findByIdWithCoupon(id)
            .map(MemberCouponEntity::toDomainWithCoupon);
    }

    @Override
    public Optional<MemberCoupon> findByMemberIdAndCouponId(Long memberId, Long couponId) {
        return memberCouponJpaRepository.findByMemberIdAndCouponId(memberId, couponId)
            .map(MemberCouponEntity::toDomain);
    }

    @Override
    public Optional<MemberCoupon> findByUsedOrderId(Long orderId) {
        return memberCouponJpaRepository.findByUsedOrderId(orderId)
            .map(MemberCouponEntity::toDomain);
    }

    @Override
    public List<MemberCoupon> findAllByMemberId(Long memberId) {
        return memberCouponJpaRepository.findAllByMemberIdWithCoupon(memberId).stream()
            .map(MemberCouponEntity::toDomainWithCoupon)
            .toList();
    }

    @Override
    public List<MemberCoupon> findAllByMemberIdAndStatus(Long memberId, MemberCouponStatus status) {
        return memberCouponJpaRepository.findAllByMemberIdAndStatusWithCoupon(memberId, status).stream()
            .map(MemberCouponEntity::toDomainWithCoupon)
            .toList();
    }

    @Override
    public List<Long> findIssuedCouponIdsByMemberId(Long memberId) {
        return memberCouponJpaRepository.findCouponIdsByMemberId(memberId);
    }

    @Override
    public Page<MemberCoupon> findAllByMemberId(Long memberId, Pageable pageable) {
        return memberCouponJpaRepository.findAllByMemberIdWithCoupon(memberId, pageable)
            .map(MemberCouponEntity::toDomainWithCoupon);
    }

    @Override
    public Page<MemberCoupon> findAllByMemberIdAndStatus(Long memberId, MemberCouponStatus status, Pageable pageable) {
        return memberCouponJpaRepository.findAllByMemberIdAndStatusWithCoupon(memberId, status, pageable)
            .map(MemberCouponEntity::toDomainWithCoupon);
    }

    @Override
    public long countAvailableByMemberId(Long memberId) {
        return memberCouponJpaRepository.countAvailableByMemberId(memberId);
    }

    @Override
    public long countByMemberIdAndStatus(Long memberId, MemberCouponStatus status) {
        return memberCouponJpaRepository.countByMemberIdAndStatus(memberId, status);
    }

    @Override
    public long countExpiredByMemberId(Long memberId) {
        return memberCouponJpaRepository.countExpiredByMemberId(memberId);
    }

    @Override
    public MemberCouponStatusCounts countStatusesByMemberId(Long memberId) {
        List<Object[]> results = memberCouponJpaRepository.countStatusesByMemberId(memberId);
        Object[] row = results.get(0);
        long available = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        long used = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        long expired = row[2] != null ? ((Number) row[2]).longValue() : 0L;
        return new MemberCouponStatusCounts(available, used, expired);
    }

    @Override
    public MemberCoupon save(MemberCoupon memberCoupon) {
        MemberCouponEntity entity;
        if (memberCoupon.getId() != null) {
            entity = memberCouponJpaRepository.findById(memberCoupon.getId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "발급된 쿠폰을 찾을 수 없습니다."));
            entity.update(
                memberCoupon.getStatus(),
                memberCoupon.getUsedOrderId(),
                memberCoupon.getUsedAt()
            );
        } else {
            entity = MemberCouponEntity.from(memberCoupon);
        }
        MemberCouponEntity saved = memberCouponJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public boolean existsByMemberIdAndCouponId(Long memberId, Long couponId) {
        return memberCouponJpaRepository.existsByMemberIdAndCouponId(memberId, couponId);
    }

    @Override
    public Page<MemberCoupon> findAllByCouponId(Long couponId, Pageable pageable) {
        return memberCouponJpaRepository.findAllByCouponId(couponId, pageable)
            .map(MemberCouponEntity::toDomain);
    }
}
