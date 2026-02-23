package com.loopers.infrastructure.address;

import com.loopers.domain.address.Address;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "member_addresses",
    indexes = {
        @Index(name = "idx_member_addresses_member_id", columnList = "member_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "recipient_name", nullable = false, length = 50)
    private String recipientName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static AddressEntity from(Address address) {
        AddressEntity entity = new AddressEntity();
        entity.memberId = address.getMemberId();
        entity.recipientName = address.getRecipientName();
        entity.phone = address.getPhone();
        entity.zipCode = address.getZipCode();
        entity.address = address.getAddress();
        entity.addressDetail = address.getAddressDetail();
        entity.isDefault = address.isDefault();
        return entity;
    }

    public Address toDomain() {
        return new Address(
            id,
            memberId,
            recipientName,
            phone,
            zipCode,
            address,
            addressDetail,
            isDefault
        );
    }

    public void update(String recipientName, String phone, String zipCode, String address, String addressDetail, boolean isDefault) {
        this.recipientName = recipientName;
        this.phone = phone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.isDefault = isDefault;
    }
}
