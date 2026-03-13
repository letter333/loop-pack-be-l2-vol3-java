package com.loopers.application.address;

import com.loopers.domain.address.Address;

public record AddressInfo(
    Long id,
    String recipientName,
    String phone,
    String zipCode,
    String address,
    String addressDetail,
    boolean isDefault
) {

    public static AddressInfo from(Address address) {
        return new AddressInfo(
            address.getId(),
            address.getRecipientName(),
            address.getPhone(),
            address.getZipCode(),
            address.getAddress(),
            address.getAddressDetail(),
            address.isDefault()
        );
    }
}
