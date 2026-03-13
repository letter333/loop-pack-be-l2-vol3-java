package com.loopers.application.address;

import com.loopers.domain.address.Address;
import com.loopers.domain.address.AddressService;
import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AddressFacade {

    private final AddressService addressService;
    private final MemberService memberService;

    @Transactional(readOnly = true)
    public List<AddressInfo> getAddresses(String loginId, String password) {
        Member member = memberService.authenticate(loginId, password);
        return addressService.getAddresses(member.getId())
            .stream()
            .map(AddressInfo::from)
            .toList();
    }

    @Transactional
    public AddressInfo register(String loginId, String password, AddressCommand.Create command) {
        Member member = memberService.authenticate(loginId, password);
        Address address = new Address(
            member.getId(),
            command.recipientName(),
            command.phone(),
            command.zipCode(),
            command.address(),
            command.addressDetail()
        );
        Address savedAddress = addressService.register(address);
        return AddressInfo.from(savedAddress);
    }

    @Transactional
    public AddressInfo update(String loginId, String password, Long addressId, AddressCommand.Update command) {
        Member member = memberService.authenticate(loginId, password);
        Address updatedAddress = addressService.update(
            member.getId(),
            addressId,
            command.recipientName(),
            command.phone(),
            command.zipCode(),
            command.address(),
            command.addressDetail()
        );
        return AddressInfo.from(updatedAddress);
    }

    @Transactional
    public void delete(String loginId, String password, Long addressId) {
        Member member = memberService.authenticate(loginId, password);
        addressService.delete(member.getId(), addressId);
    }

    @Transactional
    public AddressInfo setDefault(String loginId, String password, Long addressId) {
        Member member = memberService.authenticate(loginId, password);
        Address updatedAddress = addressService.setDefault(member.getId(), addressId);
        return AddressInfo.from(updatedAddress);
    }
}
