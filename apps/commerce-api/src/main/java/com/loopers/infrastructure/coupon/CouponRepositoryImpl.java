package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.IssuableCoupon;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponJpaRepository couponJpaRepository;

    @Override
    public Optional<Coupon> findById(Long id) {
        return couponJpaRepository.findById(id)
            .map(CouponEntity::toDomain);
    }

    @Override
    public List<Coupon> findAllIssuable() {
        return couponJpaRepository.findAllIssuable(LocalDateTime.now()).stream()
            .map(CouponEntity::toDomain)
            .toList();
    }

    @Override
    public List<IssuableCoupon> findAllIssuableWithIssuedFlag(Long memberId) {
        return couponJpaRepository.findAllIssuableWithIssuedFlag(memberId, LocalDateTime.now()).stream()
            .map(row -> {
                CouponEntity entity = (CouponEntity) row[0];
                boolean issued = Boolean.TRUE.equals(row[1]);
                return new IssuableCoupon(entity.toDomain(), issued);
            })
            .toList();
    }

    @Override
    public Page<Coupon> findAllActive(Pageable pageable) {
        return couponJpaRepository.findAllByDeletedAtIsNull(pageable)
            .map(CouponEntity::toDomain);
    }

    @Override
    public Coupon save(Coupon coupon) {
        CouponEntity entity;
        if (coupon.getId() != null) {
            entity = couponJpaRepository.findById(coupon.getId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));
            entity.update(
                coupon.getName(),
                coupon.getDescription(),
                coupon.getCouponType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getMaxDiscountAmount(),
                coupon.getTotalQuantity(),
                coupon.getIssuedQuantity(),
                coupon.getValidFrom(),
                coupon.getValidUntil()
            );
            if (coupon.isDeleted()) {
                entity.delete();
            }
        } else {
            entity = CouponEntity.from(coupon);
        }
        CouponEntity saved = couponJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void delete(Long id) {
        couponJpaRepository.deleteById(id);
    }
}
