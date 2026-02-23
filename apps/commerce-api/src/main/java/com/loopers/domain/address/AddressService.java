package com.loopers.domain.address;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private static final int MAX_ADDRESS_COUNT = 5;

    private final AddressRepository addressRepository;

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Address> getAddresses(Long memberId) {
        return addressRepository.findByMemberId(memberId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Address register(Address address) {
        List<Address> existingAddresses = addressRepository.findByMemberId(address.getMemberId());

        if (existingAddresses.size() >= MAX_ADDRESS_COUNT) {
            throw new CoreException(ErrorType.BAD_REQUEST, "배송지는 최대 " + MAX_ADDRESS_COUNT + "개까지 등록할 수 있습니다.");
        }

        if (existingAddresses.isEmpty()) {
            address.setAsDefault();
        }

        return addressRepository.save(address);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Address update(Long memberId, Long addressId, String recipientName, String phone, String zipCode, String address, String addressDetail) {
        Address existingAddress = getAddressOrThrow(addressId);
        validateOwnership(memberId, existingAddress);

        existingAddress.update(recipientName, phone, zipCode, address, addressDetail);
        return addressRepository.save(existingAddress);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void delete(Long memberId, Long addressId) {
        Address address = getAddressOrThrow(addressId);
        validateOwnership(memberId, address);

        addressRepository.delete(address);

        List<Address> remainingAddresses = addressRepository.findByMemberId(memberId);
        if (remainingAddresses.size() == 1) {
            Address remainingAddress = remainingAddresses.get(0);
            if (!remainingAddress.isDefault()) {
                remainingAddress.setAsDefault();
                addressRepository.save(remainingAddress);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Address setDefault(Long memberId, Long addressId) {
        Address address = getAddressOrThrow(addressId);
        validateOwnership(memberId, address);

        addressRepository.findDefaultByMemberId(memberId)
            .ifPresent(existingDefault -> {
                existingDefault.unsetDefault();
                addressRepository.save(existingDefault);
            });

        address.setAsDefault();
        return addressRepository.save(address);
    }

    private Address getAddressOrThrow(Long addressId) {
        return addressRepository.findById(addressId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "배송지를 찾을 수 없습니다."));
    }

    private void validateOwnership(Long memberId, Address address) {
        if (!address.isOwnedBy(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN, "해당 배송지에 대한 권한이 없습니다.");
        }
    }
}
