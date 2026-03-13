package com.loopers.domain.address;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressService addressService;

    private static final Long MEMBER_ID = 1L;

    @DisplayName("배송지 목록 조회")
    @Nested
    class GetAddresses {

        @Test
        @DisplayName("회원의 배송지 목록을 조회한다")
        void returnsAddresses_forMember() {
            // arrange
            Address address1 = createAddress(1L, MEMBER_ID, true);
            Address address2 = createAddress(2L, MEMBER_ID, false);
            given(addressRepository.findByMemberId(MEMBER_ID)).willReturn(List.of(address1, address2));

            // act
            List<Address> result = addressService.getAddresses(MEMBER_ID);

            // assert
            assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0).getId()).isEqualTo(1L),
                () -> assertThat(result.get(1).getId()).isEqualTo(2L)
            );
        }

        @Test
        @DisplayName("배송지가 없으면 빈 목록을 반환한다")
        void returnsEmptyList_whenNoAddresses() {
            // arrange
            given(addressRepository.findByMemberId(MEMBER_ID)).willReturn(Collections.emptyList());

            // act
            List<Address> result = addressService.getAddresses(MEMBER_ID);

            // assert
            assertThat(result).isEmpty();
        }
    }

    @DisplayName("배송지 등록")
    @Nested
    class RegisterAddress {

        @Test
        @DisplayName("첫 번째 배송지는 자동으로 기본 배송지로 설정된다")
        void setsAsDefault_whenFirstAddress() {
            // arrange
            Address newAddress = new Address(MEMBER_ID, "홍길동", "010-1234-5678", null, "서울시", null);
            given(addressRepository.findByMemberId(MEMBER_ID)).willReturn(Collections.emptyList());
            given(addressRepository.save(any(Address.class))).willAnswer(invocation -> invocation.getArgument(0));

            // act
            Address result = addressService.register(newAddress);

            // assert
            assertThat(result.isDefault()).isTrue();
        }

        @Test
        @DisplayName("두 번째 이후 배송지는 기본 배송지로 설정되지 않는다")
        void doesNotSetAsDefault_whenNotFirstAddress() {
            // arrange
            Address existingAddress = createAddress(1L, MEMBER_ID, true);
            Address newAddress = new Address(MEMBER_ID, "홍길동", "010-1234-5678", null, "서울시", null);
            given(addressRepository.findByMemberId(MEMBER_ID)).willReturn(List.of(existingAddress));
            given(addressRepository.save(any(Address.class))).willAnswer(invocation -> invocation.getArgument(0));

            // act
            Address result = addressService.register(newAddress);

            // assert
            assertThat(result.isDefault()).isFalse();
        }

        @Test
        @DisplayName("배송지 개수가 최대치(5개)를 초과하면 예외가 발생한다")
        void throwsException_whenExceedsMaxAddresses() {
            // arrange
            List<Address> existingAddresses = List.of(
                createAddress(1L, MEMBER_ID, true),
                createAddress(2L, MEMBER_ID, false),
                createAddress(3L, MEMBER_ID, false),
                createAddress(4L, MEMBER_ID, false),
                createAddress(5L, MEMBER_ID, false)
            );
            Address newAddress = new Address(MEMBER_ID, "홍길동", "010-1234-5678", null, "서울시", null);
            given(addressRepository.findByMemberId(MEMBER_ID)).willReturn(existingAddresses);

            // act & assert
            assertThatThrownBy(() -> addressService.register(newAddress))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("배송지 수정")
    @Nested
    class UpdateAddress {

        @Test
        @DisplayName("배송지 정보를 수정할 수 있다")
        void updatesAddress() {
            // arrange
            Address address = createAddress(1L, MEMBER_ID, false);
            given(addressRepository.findById(1L)).willReturn(Optional.of(address));
            given(addressRepository.save(any(Address.class))).willAnswer(invocation -> invocation.getArgument(0));

            // act
            Address result = addressService.update(MEMBER_ID, 1L, "김철수", "010-9999-8888", "12345", "부산시", "201호");

            // assert
            assertAll(
                () -> assertThat(result.getRecipientName()).isEqualTo("김철수"),
                () -> assertThat(result.getPhone()).isEqualTo("010-9999-8888"),
                () -> assertThat(result.getZipCode()).isEqualTo("12345"),
                () -> assertThat(result.getAddress()).isEqualTo("부산시"),
                () -> assertThat(result.getAddressDetail()).isEqualTo("201호")
            );
        }

        @Test
        @DisplayName("존재하지 않는 배송지를 수정하면 예외가 발생한다")
        void throwsException_whenAddressNotFound() {
            // arrange
            given(addressRepository.findById(999L)).willReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> addressService.update(MEMBER_ID, 999L, "홍길동", "010-1234-5678", null, "서울시", null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("다른 회원의 배송지를 수정하면 예외가 발생한다")
        void throwsException_whenNotOwner() {
            // arrange
            Address address = createAddress(1L, 2L, false);  // 다른 회원의 배송지
            given(addressRepository.findById(1L)).willReturn(Optional.of(address));

            // act & assert
            assertThatThrownBy(() -> addressService.update(MEMBER_ID, 1L, "홍길동", "010-1234-5678", null, "서울시", null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.FORBIDDEN);
        }
    }

    @DisplayName("배송지 삭제")
    @Nested
    class DeleteAddress {

        @Test
        @DisplayName("배송지를 삭제할 수 있다")
        void deletesAddress() {
            // arrange
            Address address = createAddress(1L, MEMBER_ID, false);
            given(addressRepository.findById(1L)).willReturn(Optional.of(address));

            // act
            addressService.delete(MEMBER_ID, 1L);

            // assert
            verify(addressRepository).delete(address);
        }

        @Test
        @DisplayName("존재하지 않는 배송지를 삭제하면 예외가 발생한다")
        void throwsException_whenAddressNotFound() {
            // arrange
            given(addressRepository.findById(999L)).willReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> addressService.delete(MEMBER_ID, 999L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("다른 회원의 배송지를 삭제하면 예외가 발생한다")
        void throwsException_whenNotOwner() {
            // arrange
            Address address = createAddress(1L, 2L, false);  // 다른 회원의 배송지
            given(addressRepository.findById(1L)).willReturn(Optional.of(address));

            // act & assert
            assertThatThrownBy(() -> addressService.delete(MEMBER_ID, 1L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.FORBIDDEN);

            verify(addressRepository, never()).delete(any());
        }

        @Test
        @DisplayName("삭제 후 남은 배송지가 1개이면 자동으로 기본 배송지로 설정된다")
        void setsAsDefault_whenOneAddressRemains() {
            // arrange
            Address defaultAddress = createAddress(1L, MEMBER_ID, true);
            Address remainingAddress = createAddress(2L, MEMBER_ID, false);
            given(addressRepository.findById(1L)).willReturn(Optional.of(defaultAddress));
            given(addressRepository.findByMemberId(MEMBER_ID)).willReturn(List.of(remainingAddress));
            given(addressRepository.save(any(Address.class))).willAnswer(invocation -> invocation.getArgument(0));

            // act
            addressService.delete(MEMBER_ID, 1L);

            // assert
            assertAll(
                () -> verify(addressRepository).delete(defaultAddress),
                () -> verify(addressRepository).save(remainingAddress),
                () -> assertThat(remainingAddress.isDefault()).isTrue()
            );
        }

        @Test
        @DisplayName("삭제 후 남은 배송지가 2개 이상이면 기본 배송지를 변경하지 않는다")
        void doesNotChangeDefault_whenMultipleAddressesRemain() {
            // arrange
            Address addressToDelete = createAddress(1L, MEMBER_ID, false);
            Address defaultAddress = createAddress(2L, MEMBER_ID, true);
            Address anotherAddress = createAddress(3L, MEMBER_ID, false);
            given(addressRepository.findById(1L)).willReturn(Optional.of(addressToDelete));
            given(addressRepository.findByMemberId(MEMBER_ID)).willReturn(List.of(defaultAddress, anotherAddress));

            // act
            addressService.delete(MEMBER_ID, 1L);

            // assert
            assertAll(
                () -> verify(addressRepository).delete(addressToDelete),
                () -> verify(addressRepository, never()).save(any()),
                () -> assertThat(defaultAddress.isDefault()).isTrue(),
                () -> assertThat(anotherAddress.isDefault()).isFalse()
            );
        }

        @Test
        @DisplayName("삭제 후 남은 배송지가 없으면 아무 동작도 하지 않는다")
        void doesNothing_whenNoAddressesRemain() {
            // arrange
            Address onlyAddress = createAddress(1L, MEMBER_ID, true);
            given(addressRepository.findById(1L)).willReturn(Optional.of(onlyAddress));
            given(addressRepository.findByMemberId(MEMBER_ID)).willReturn(Collections.emptyList());

            // act
            addressService.delete(MEMBER_ID, 1L);

            // assert
            assertAll(
                () -> verify(addressRepository).delete(onlyAddress),
                () -> verify(addressRepository, never()).save(any())
            );
        }

        @Test
        @DisplayName("삭제 후 남은 배송지가 1개이고 이미 기본 배송지이면 변경하지 않는다")
        void doesNotChange_whenRemainingAddressIsAlreadyDefault() {
            // arrange
            Address addressToDelete = createAddress(1L, MEMBER_ID, false);
            Address remainingDefaultAddress = createAddress(2L, MEMBER_ID, true);
            given(addressRepository.findById(1L)).willReturn(Optional.of(addressToDelete));
            given(addressRepository.findByMemberId(MEMBER_ID)).willReturn(List.of(remainingDefaultAddress));

            // act
            addressService.delete(MEMBER_ID, 1L);

            // assert
            assertAll(
                () -> verify(addressRepository).delete(addressToDelete),
                () -> verify(addressRepository, never()).save(any())
            );
        }
    }

    @DisplayName("기본 배송지 설정")
    @Nested
    class SetDefaultAddress {

        @Test
        @DisplayName("기본 배송지로 설정하면 기존 기본 배송지가 해제된다")
        void unsetsExistingDefault_whenSettingNewDefault() {
            // arrange
            Address existingDefault = createAddress(1L, MEMBER_ID, true);
            Address newDefault = createAddress(2L, MEMBER_ID, false);
            given(addressRepository.findById(2L)).willReturn(Optional.of(newDefault));
            given(addressRepository.findDefaultByMemberId(MEMBER_ID)).willReturn(Optional.of(existingDefault));
            given(addressRepository.save(any(Address.class))).willAnswer(invocation -> invocation.getArgument(0));

            // act
            Address result = addressService.setDefault(MEMBER_ID, 2L);

            // assert
            assertAll(
                () -> assertThat(result.isDefault()).isTrue(),
                () -> assertThat(existingDefault.isDefault()).isFalse()
            );
        }

        @Test
        @DisplayName("기존 기본 배송지가 없어도 새 기본 배송지를 설정할 수 있다")
        void setsNewDefault_whenNoExistingDefault() {
            // arrange
            Address newDefault = createAddress(2L, MEMBER_ID, false);
            given(addressRepository.findById(2L)).willReturn(Optional.of(newDefault));
            given(addressRepository.findDefaultByMemberId(MEMBER_ID)).willReturn(Optional.empty());
            given(addressRepository.save(any(Address.class))).willAnswer(invocation -> invocation.getArgument(0));

            // act
            Address result = addressService.setDefault(MEMBER_ID, 2L);

            // assert
            assertThat(result.isDefault()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 배송지를 기본으로 설정하면 예외가 발생한다")
        void throwsException_whenAddressNotFound() {
            // arrange
            given(addressRepository.findById(999L)).willReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> addressService.setDefault(MEMBER_ID, 999L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("다른 회원의 배송지를 기본으로 설정하면 예외가 발생한다")
        void throwsException_whenNotOwner() {
            // arrange
            Address address = createAddress(1L, 2L, false);  // 다른 회원의 배송지
            given(addressRepository.findById(1L)).willReturn(Optional.of(address));

            // act & assert
            assertThatThrownBy(() -> addressService.setDefault(MEMBER_ID, 1L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.FORBIDDEN);
        }
    }

    private Address createAddress(Long id, Long memberId, boolean isDefault) {
        return new Address(id, memberId, "홍길동", "010-1234-5678", "06234", "서울시", "101호", isDefault);
    }
}
