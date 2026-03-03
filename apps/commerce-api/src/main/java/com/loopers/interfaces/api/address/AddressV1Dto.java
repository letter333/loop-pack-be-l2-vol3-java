package com.loopers.interfaces.api.address;

import com.loopers.application.address.AddressCommand;
import com.loopers.application.address.AddressInfo;
import jakarta.validation.constraints.NotBlank;

public class AddressV1Dto {

    public record RegisterRequest(
        @NotBlank(message = "수령인 이름은 필수입니다.")
        String recipientName,
        @NotBlank(message = "전화번호는 필수입니다.")
        String phone,
        String zipCode,
        @NotBlank(message = "주소는 필수입니다.")
        String address,
        String addressDetail
    ) {
        public AddressCommand.Create toCommand() {
            return new AddressCommand.Create(recipientName, phone, zipCode, address, addressDetail);
        }
    }

    public record UpdateRequest(
        @NotBlank(message = "수령인 이름은 필수입니다.")
        String recipientName,
        @NotBlank(message = "전화번호는 필수입니다.")
        String phone,
        String zipCode,
        @NotBlank(message = "주소는 필수입니다.")
        String address,
        String addressDetail
    ) {
        public AddressCommand.Update toCommand() {
            return new AddressCommand.Update(recipientName, phone, zipCode, address, addressDetail);
        }
    }

    public record AddressResponse(
        Long id,
        String recipientName,
        String phone,
        String zipCode,
        String address,
        String addressDetail,
        boolean isDefault
    ) {
        public static AddressResponse from(AddressInfo info) {
            return new AddressResponse(
                info.id(),
                info.recipientName(),
                info.phone(),
                info.zipCode(),
                info.address(),
                info.addressDetail(),
                info.isDefault()
            );
        }
    }
}
