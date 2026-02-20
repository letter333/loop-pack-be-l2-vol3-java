package com.loopers.support.auth;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("AdminValidator 단위 테스트")
class AdminValidatorTest {

    private final AdminValidator adminValidator = new AdminValidator();

    @Nested
    @DisplayName("validate 메서드")
    class Validate {

        @Test
        @DisplayName("올바른 LDAP 값이면 예외가 발생하지 않는다")
        void success_withValidLdap() {
            // Arrange
            String validLdap = "loopers.admin";

            // Act & Assert
            assertThatCode(() -> adminValidator.validate(validLdap))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("잘못된 LDAP 값이면 FORBIDDEN 예외가 발생한다")
        void fail_withInvalidLdap() {
            // Arrange
            String invalidLdap = "invalid.ldap";

            // Act & Assert
            assertThatThrownBy(() -> adminValidator.validate(invalidLdap))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> {
                    CoreException coreException = (CoreException) ex;
                    assert coreException.getErrorType() == ErrorType.FORBIDDEN;
                });
        }

        @Test
        @DisplayName("null 값이면 FORBIDDEN 예외가 발생한다")
        void fail_withNullLdap() {
            // Arrange
            String nullLdap = null;

            // Act & Assert
            assertThatThrownBy(() -> adminValidator.validate(nullLdap))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> {
                    CoreException coreException = (CoreException) ex;
                    assert coreException.getErrorType() == ErrorType.FORBIDDEN;
                });
        }

        @Test
        @DisplayName("빈 문자열이면 FORBIDDEN 예외가 발생한다")
        void fail_withEmptyLdap() {
            // Arrange
            String emptyLdap = "";

            // Act & Assert
            assertThatThrownBy(() -> adminValidator.validate(emptyLdap))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> {
                    CoreException coreException = (CoreException) ex;
                    assert coreException.getErrorType() == ErrorType.FORBIDDEN;
                });
        }
    }
}