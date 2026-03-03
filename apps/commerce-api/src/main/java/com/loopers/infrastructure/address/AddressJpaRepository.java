package com.loopers.infrastructure.address;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressJpaRepository extends JpaRepository<AddressEntity, Long> {

    List<AddressEntity> findByMemberIdOrderByIsDefaultDescIdAsc(Long memberId);

    Optional<AddressEntity> findByMemberIdAndIsDefaultTrue(Long memberId);
}
