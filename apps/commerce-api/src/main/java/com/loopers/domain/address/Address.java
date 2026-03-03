package com.loopers.domain.address;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

@Getter
public class Address {

    private Long id;
    private Long memberId;
    private String recipientName;
    private String phone;
    private String zipCode;
    private String address;
    private String addressDetail;
    private boolean isDefault;

    public Address(Long memberId, String recipientName, String phone, String zipCode, String address, String addressDetail) {
        validateMemberId(memberId);
        validateRecipientName(recipientName);
        validatePhone(phone);
        validateAddress(address);

        this.memberId = memberId;
        this.recipientName = recipientName;
        this.phone = phone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.isDefault = false;
    }

    public Address(Long id, Long memberId, String recipientName, String phone, String zipCode, String address, String addressDetail, boolean isDefault) {
        this.id = id;
        this.memberId = memberId;
        this.recipientName = recipientName;
        this.phone = phone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.isDefault = isDefault;
    }

    public void update(String recipientName, String phone, String zipCode, String address, String addressDetail) {
        validateRecipientName(recipientName);
        validatePhone(phone);
        validateAddress(address);

        this.recipientName = recipientName;
        this.phone = phone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
    }

    public void setAsDefault() {
        this.isDefault = true;
    }

    public void unsetDefault() {
        this.isDefault = false;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    private void validateMemberId(Long memberId) {
        if (memberId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "회원 ID는 필수입니다.");
        }
    }

    private void validateRecipientName(String recipientName) {
        if (recipientName == null || recipientName.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수령인 이름은 필수입니다.");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "전화번호는 필수입니다.");
        }
    }

    private void validateAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주소는 필수입니다.");
        }
    }
}
