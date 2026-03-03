package com.loopers.application.address;

public class AddressCommand {

    public record Create(
        String recipientName,
        String phone,
        String zipCode,
        String address,
        String addressDetail
    ) {
    }

    public record Update(
        String recipientName,
        String phone,
        String zipCode,
        String address,
        String addressDetail
    ) {
    }
}
