package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("ProductImage 도메인 단위 테스트")
class ProductImageTest {

    @Nested
    @DisplayName("ProductImage 생성")
    class Create {

        @Test
        @DisplayName("모든 필수값이 유효하면 정상적으로 생성된다")
        void createsProductImage_whenAllFieldsAreValid() {
            // Arrange & Act
            ProductImage image = new ProductImage(1L, ImageType.MAIN, "https://example.com/image.jpg", "상품 이미지");

            // Assert
            assertAll(
                () -> assertThat(image.getProductId()).isEqualTo(1L),
                () -> assertThat(image.getType()).isEqualTo(ImageType.MAIN),
                () -> assertThat(image.getUrl()).isEqualTo("https://example.com/image.jpg"),
                () -> assertThat(image.getAltText()).isEqualTo("상품 이미지")
            );
        }

        @Test
        @DisplayName("productId가 null이면 정상 생성된다 (애그리거트 루트를 통해 설정)")
        void createsProductImage_whenProductIdIsNull() {
            // Arrange & Act
            ProductImage image = new ProductImage(null, ImageType.MAIN, "https://example.com/image.jpg", "상품 이미지");

            // Assert
            assertThat(image.getProductId()).isNull();
        }

        @Test
        @DisplayName("url이 null이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenUrlIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> new ProductImage(1L, ImageType.MAIN, null, "상품 이미지"))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("url이 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenUrlIsEmpty() {
            // Act & Assert
            assertThatThrownBy(() -> new ProductImage(1L, ImageType.MAIN, "", "상품 이미지"))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("altText가 null이면 정상 생성된다")
        void createsProductImage_whenAltTextIsNull() {
            // Arrange & Act
            ProductImage image = new ProductImage(1L, ImageType.MAIN, "https://example.com/image.jpg", null);

            // Assert
            assertThat(image.getAltText()).isNull();
        }

        @Test
        @DisplayName("type이 null이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenTypeIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> new ProductImage(1L, null, "https://example.com/image.jpg", "상품 이미지"))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }
}