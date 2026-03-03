package com.loopers.domain.address;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class AddressTest {

    @DisplayName("Address 생성")
    @Nested
    class Create {

        @Test
        @DisplayName("필수 필드로 Address를 생성할 수 있다")
        void createsAddress_withRequiredFields() {
            // arrange
            Long memberId = 1L;
            String recipientName = "홍길동";
            String phone = "010-1234-5678";
            String address = "서울시 강남구 테헤란로 123";

            // act
            Address result = new Address(memberId, recipientName, phone, null, address, null);

            // assert
            assertAll(
                () -> assertThat(result.getMemberId()).isEqualTo(memberId),
                () -> assertThat(result.getRecipientName()).isEqualTo(recipientName),
                () -> assertThat(result.getPhone()).isEqualTo(phone),
                () -> assertThat(result.getAddress()).isEqualTo(address),
                () -> assertThat(result.isDefault()).isFalse()
            );
        }

        @Test
        @DisplayName("모든 필드로 Address를 생성할 수 있다")
        void createsAddress_withAllFields() {
            // arrange
            Long memberId = 1L;
            String recipientName = "홍길동";
            String phone = "010-1234-5678";
            String zipCode = "06234";
            String address = "서울시 강남구 테헤란로 123";
            String addressDetail = "5층 501호";

            // act
            Address result = new Address(memberId, recipientName, phone, zipCode, address, addressDetail);

            // assert
            assertAll(
                () -> assertThat(result.getMemberId()).isEqualTo(memberId),
                () -> assertThat(result.getRecipientName()).isEqualTo(recipientName),
                () -> assertThat(result.getPhone()).isEqualTo(phone),
                () -> assertThat(result.getZipCode()).isEqualTo(zipCode),
                () -> assertThat(result.getAddress()).isEqualTo(address),
                () -> assertThat(result.getAddressDetail()).isEqualTo(addressDetail),
                () -> assertThat(result.isDefault()).isFalse()
            );
        }

        @Test
        @DisplayName("memberId가 null이면 예외가 발생한다")
        void throwsException_whenMemberIdIsNull() {
            // act & assert
            assertThatThrownBy(() -> new Address(null, "홍길동", "010-1234-5678", null, "서울시", null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("수령인 이름이 null이면 예외가 발생한다")
        void throwsException_whenRecipientNameIsNull() {
            // act & assert
            assertThatThrownBy(() -> new Address(1L, null, "010-1234-5678", null, "서울시", null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("수령인 이름이 빈 문자열이면 예외가 발생한다")
        void throwsException_whenRecipientNameIsEmpty() {
            // act & assert
            assertThatThrownBy(() -> new Address(1L, "  ", "010-1234-5678", null, "서울시", null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("전화번호가 null이면 예외가 발생한다")
        void throwsException_whenPhoneIsNull() {
            // act & assert
            assertThatThrownBy(() -> new Address(1L, "홍길동", null, null, "서울시", null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("주소가 null이면 예외가 발생한다")
        void throwsException_whenAddressIsNull() {
            // act & assert
            assertThatThrownBy(() -> new Address(1L, "홍길동", "010-1234-5678", null, null, null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("주소가 빈 문자열이면 예외가 발생한다")
        void throwsException_whenAddressIsEmpty() {
            // act & assert
            assertThatThrownBy(() -> new Address(1L, "홍길동", "010-1234-5678", null, "  ", null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("Address 수정")
    @Nested
    class Update {

        @Test
        @DisplayName("Address 정보를 수정할 수 있다")
        void updatesAddress() {
            // arrange
            Address address = new Address(1L, "홍길동", "010-1234-5678", "06234", "서울시 강남구", "101호");

            // act
            address.update("김철수", "010-9999-8888", "12345", "부산시 해운대구", "201호");

            // assert
            assertAll(
                () -> assertThat(address.getRecipientName()).isEqualTo("김철수"),
                () -> assertThat(address.getPhone()).isEqualTo("010-9999-8888"),
                () -> assertThat(address.getZipCode()).isEqualTo("12345"),
                () -> assertThat(address.getAddress()).isEqualTo("부산시 해운대구"),
                () -> assertThat(address.getAddressDetail()).isEqualTo("201호")
            );
        }

        @Test
        @DisplayName("수정 시 수령인 이름이 null이면 예외가 발생한다")
        void throwsException_whenUpdatingWithNullRecipientName() {
            // arrange
            Address address = new Address(1L, "홍길동", "010-1234-5678", null, "서울시", null);

            // act & assert
            assertThatThrownBy(() -> address.update(null, "010-1234-5678", null, "서울시", null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("기본 배송지 설정")
    @Nested
    class SetDefault {

        @Test
        @DisplayName("기본 배송지로 설정할 수 있다")
        void setsAsDefault() {
            // arrange
            Address address = new Address(1L, "홍길동", "010-1234-5678", null, "서울시", null);

            // act
            address.setAsDefault();

            // assert
            assertThat(address.isDefault()).isTrue();
        }

        @Test
        @DisplayName("기본 배송지를 해제할 수 있다")
        void unsetsAsDefault() {
            // arrange
            Address address = new Address(1L, "홍길동", "010-1234-5678", null, "서울시", null);
            address.setAsDefault();

            // act
            address.unsetDefault();

            // assert
            assertThat(address.isDefault()).isFalse();
        }
    }

    @DisplayName("소유권 검증")
    @Nested
    class Ownership {

        @Test
        @DisplayName("소유자가 일치하면 true를 반환한다")
        void returnsTrue_whenOwnerMatches() {
            // arrange
            Address address = new Address(1L, "홍길동", "010-1234-5678", null, "서울시", null);

            // act & assert
            assertThat(address.isOwnedBy(1L)).isTrue();
        }

        @Test
        @DisplayName("소유자가 일치하지 않으면 false를 반환한다")
        void returnsFalse_whenOwnerDoesNotMatch() {
            // arrange
            Address address = new Address(1L, "홍길동", "010-1234-5678", null, "서울시", null);

            // act & assert
            assertThat(address.isOwnedBy(2L)).isFalse();
        }
    }
}
