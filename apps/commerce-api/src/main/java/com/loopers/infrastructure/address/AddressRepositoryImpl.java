package com.loopers.infrastructure.address;

import com.loopers.domain.address.Address;
import com.loopers.domain.address.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AddressRepositoryImpl implements AddressRepository {

    private final AddressJpaRepository addressJpaRepository;

    @Override
    public List<Address> findByMemberId(Long memberId) {
        return addressJpaRepository.findByMemberIdOrderByIsDefaultDescIdAsc(memberId)
            .stream()
            .map(AddressEntity::toDomain)
            .toList();
    }

    @Override
    public Optional<Address> findById(Long id) {
        return addressJpaRepository.findById(id)
            .map(AddressEntity::toDomain);
    }

    @Override
    public Optional<Address> findDefaultByMemberId(Long memberId) {
        return addressJpaRepository.findByMemberIdAndIsDefaultTrue(memberId)
            .map(AddressEntity::toDomain);
    }

    @Override
    public Address save(Address address) {
        AddressEntity entity;
        if (address.getId() != null) {
            entity = addressJpaRepository.findById(address.getId())
                .orElseGet(() -> AddressEntity.from(address));
            entity.update(
                address.getRecipientName(),
                address.getPhone(),
                address.getZipCode(),
                address.getAddress(),
                address.getAddressDetail(),
                address.isDefault()
            );
        } else {
            entity = AddressEntity.from(address);
        }
        return addressJpaRepository.save(entity).toDomain();
    }

    @Override
    public void delete(Address address) {
        addressJpaRepository.deleteById(address.getId());
    }
}
