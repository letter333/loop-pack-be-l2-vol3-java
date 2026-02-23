package com.loopers.application.address;

import com.loopers.domain.address.Address;
import com.loopers.domain.address.AddressService;
import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AddressFacadeTest {

    @Mock
    private AddressService addressService;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private AddressFacade addressFacade;

    private static final String LOGIN_ID = "testuser";
    private static final String PASSWORD = "Password123!";
    private static final Long MEMBER_ID = 1L;

    @DisplayName("배송지 목록 조회")
    @Nested
    class GetAddresses {

        @Test
        @DisplayName("인증 후 회원의 배송지 목록을 조회한다")
        void returnsAddresses_afterAuthentication() {
            // arrange
            Member member = createMember();
            Address address1 = createAddress(1L, MEMBER_ID, true);
            Address address2 = createAddress(2L, MEMBER_ID, false);
            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(addressService.getAddresses(MEMBER_ID)).willReturn(List.of(address1, address2));

            // act
            List<AddressInfo> result = addressFacade.getAddresses(LOGIN_ID, PASSWORD);

            // assert
            assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0).id()).isEqualTo(1L),
                () -> assertThat(result.get(1).id()).isEqualTo(2L)
            );
        }
    }

    @DisplayName("배송지 등록")
    @Nested
    class RegisterAddress {

        @Test
        @DisplayName("인증 후 배송지를 등록한다")
        void registersAddress_afterAuthentication() {
            // arrange
            Member member = createMember();
            AddressCommand.Create command = new AddressCommand.Create(
                "홍길동", "010-1234-5678", "06234", "서울시 강남구", "101호"
            );
            Address savedAddress = createAddress(1L, MEMBER_ID, true);
            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(addressService.register(any(Address.class))).willReturn(savedAddress);

            // act
            AddressInfo result = addressFacade.register(LOGIN_ID, PASSWORD, command);

            // assert
            assertAll(
                () -> assertThat(result.id()).isEqualTo(1L),
                () -> assertThat(result.recipientName()).isEqualTo("홍길동"),
                () -> assertThat(result.isDefault()).isTrue()
            );
        }
    }

    @DisplayName("배송지 수정")
    @Nested
    class UpdateAddress {

        @Test
        @DisplayName("인증 후 배송지를 수정한다")
        void updatesAddress_afterAuthentication() {
            // arrange
            Member member = createMember();
            AddressCommand.Update command = new AddressCommand.Update(
                "김철수", "010-9999-8888", "12345", "부산시", "201호"
            );
            Address updatedAddress = new Address(1L, MEMBER_ID, "김철수", "010-9999-8888", "12345", "부산시", "201호", false);
            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(addressService.update(eq(MEMBER_ID), eq(1L), eq("김철수"), eq("010-9999-8888"), eq("12345"), eq("부산시"), eq("201호")))
                .willReturn(updatedAddress);

            // act
            AddressInfo result = addressFacade.update(LOGIN_ID, PASSWORD, 1L, command);

            // assert
            assertAll(
                () -> assertThat(result.recipientName()).isEqualTo("김철수"),
                () -> assertThat(result.phone()).isEqualTo("010-9999-8888")
            );
        }
    }

    @DisplayName("배송지 삭제")
    @Nested
    class DeleteAddress {

        @Test
        @DisplayName("인증 후 배송지를 삭제한다")
        void deletesAddress_afterAuthentication() {
            // arrange
            Member member = createMember();
            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);

            // act
            addressFacade.delete(LOGIN_ID, PASSWORD, 1L);

            // assert
            verify(addressService).delete(MEMBER_ID, 1L);
        }
    }

    @DisplayName("기본 배송지 설정")
    @Nested
    class SetDefaultAddress {

        @Test
        @DisplayName("인증 후 기본 배송지를 설정한다")
        void setsDefaultAddress_afterAuthentication() {
            // arrange
            Member member = createMember();
            Address updatedAddress = createAddress(1L, MEMBER_ID, true);
            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(addressService.setDefault(MEMBER_ID, 1L)).willReturn(updatedAddress);

            // act
            AddressInfo result = addressFacade.setDefault(LOGIN_ID, PASSWORD, 1L);

            // assert
            assertThat(result.isDefault()).isTrue();
        }
    }

    private Member createMember() {
        return new Member(MEMBER_ID, LOGIN_ID, "encodedPassword", "테스트", LocalDate.of(1990, 1, 1), "test@example.com");
    }

    private Address createAddress(Long id, Long memberId, boolean isDefault) {
        return new Address(id, memberId, "홍길동", "010-1234-5678", "06234", "서울시", "101호", isDefault);
    }
}
