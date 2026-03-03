package com.loopers.domain.address;

import java.util.List;
import java.util.Optional;

public interface AddressRepository {

    List<Address> findByMemberId(Long memberId);

    Optional<Address> findById(Long id);

    Optional<Address> findDefaultByMemberId(Long memberId);

    Address save(Address address);

    void delete(Address address);
}
