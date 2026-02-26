package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Brand 도메인 단위 테스트")
class BrandTest {

    @Nested
    @DisplayName("Brand 생성")
    class Create {

        @Test
        @DisplayName("모든 값이 유효하면 정상적으로 생성된다")
        void createsBrand_whenAllFieldsAreValid() {
            // Arrange & Act
            Brand brand = new Brand("Nike", "스포츠 브랜드", "https://example.com/nike-logo.png");

            // Assert
            assertAll(
                () -> assertThat(brand.getName()).isEqualTo("Nike"),
                () -> assertThat(brand.getDescription()).isEqualTo("스포츠 브랜드"),
                () -> assertThat(brand.getLogoImageUrl()).isEqualTo("https://example.com/nike-logo.png"),
                () -> assertThat(brand.isDeleted()).isFalse()
            );
        }

        @Test
        @DisplayName("name이 null이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenNameIsNull() {
            // Arrange & Act & Assert
            assertThatThrownBy(() -> new Brand(null, "설명", "https://example.com/logo.png"))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("name이 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenNameIsEmpty() {
            // Arrange & Act & Assert
            assertThatThrownBy(() -> new Brand("", "설명", "https://example.com/logo.png"))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("name이 공백 문자열이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenNameIsBlank() {
            // Arrange & Act & Assert
            assertThatThrownBy(() -> new Brand("   ", "설명", "https://example.com/logo.png"))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("description과 logoImageUrl이 null이어도 생성된다")
        void createsBrand_whenOptionalFieldsAreNull() {
            // Arrange & Act
            Brand brand = new Brand("Nike", null, null);

            // Assert
            assertAll(
                () -> assertThat(brand.getName()).isEqualTo("Nike"),
                () -> assertThat(brand.getDescription()).isNull(),
                () -> assertThat(brand.getLogoImageUrl()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("Brand update")
    class Update {

        @Test
        @DisplayName("모든 필드를 정상적으로 업데이트한다")
        void updatesAllFields() {
            // Arrange
            Brand brand = new Brand("Nike", "스포츠 브랜드", "https://example.com/nike-logo.png");

            // Act
            brand.update("Adidas", "독일 스포츠 브랜드", "https://example.com/adidas-logo.png");

            // Assert
            assertAll(
                () -> assertThat(brand.getName()).isEqualTo("Adidas"),
                () -> assertThat(brand.getDescription()).isEqualTo("독일 스포츠 브랜드"),
                () -> assertThat(brand.getLogoImageUrl()).isEqualTo("https://example.com/adidas-logo.png")
            );
        }

        @Test
        @DisplayName("name을 null로 업데이트하면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenUpdateNameToNull() {
            // Arrange
            Brand brand = new Brand("Nike", "스포츠 브랜드", "https://example.com/nike-logo.png");

            // Act & Assert
            assertThatThrownBy(() -> brand.update(null, "설명", "https://example.com/logo.png"))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("name을 빈 문자열로 업데이트하면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenUpdateNameToEmpty() {
            // Arrange
            Brand brand = new Brand("Nike", "스포츠 브랜드", "https://example.com/nike-logo.png");

            // Act & Assert
            assertThatThrownBy(() -> brand.update("", "설명", "https://example.com/logo.png"))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @Nested
    @DisplayName("Brand delete")
    class Delete {

        @Test
        @DisplayName("delete 호출 시 deletedAt이 설정된다")
        void setsDeletedAt_whenDeleteCalled() {
            // Arrange
            Brand brand = new Brand("Nike", "스포츠 브랜드", "https://example.com/nike-logo.png");

            // Act
            brand.delete();

            // Assert
            assertThat(brand.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("이미 삭제된 상태에서 delete 호출해도 예외가 발생하지 않는다 (멱등성)")
        void doesNotThrow_whenDeleteCalledTwice() {
            // Arrange
            Brand brand = new Brand("Nike", "스포츠 브랜드", "https://example.com/nike-logo.png");
            brand.delete();

            // Act & Assert (멱등성 - 예외 없이 정상 실행)
            brand.delete();
            assertThat(brand.isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("likeCount - 좋아요 수 관리")
    class LikeCount {

        @Test
        @DisplayName("생성 시 likeCount 기본값은 0이다")
        void likeCountIsZero_whenCreated() {
            // Arrange & Act
            Brand brand = new Brand("Nike", "스포츠 브랜드", "https://example.com/nike-logo.png");

            // Assert
            assertThat(brand.getLikeCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("increaseLikeCount 호출 시 likeCount가 1 증가한다")
        void increasesLikeCount() {
            // Arrange
            Brand brand = new Brand("Nike", "스포츠 브랜드", "https://example.com/nike-logo.png");

            // Act
            brand.increaseLikeCount();

            // Assert
            assertThat(brand.getLikeCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("decreaseLikeCount 호출 시 likeCount가 1 감소한다")
        void decreasesLikeCount() {
            // Arrange
            Brand brand = new Brand("Nike", "스포츠 브랜드", "https://example.com/nike-logo.png");
            brand.increaseLikeCount();
            brand.increaseLikeCount();

            // Act
            brand.decreaseLikeCount();

            // Assert
            assertThat(brand.getLikeCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("likeCount가 0일 때 decreaseLikeCount 호출해도 0 미만이 되지 않는다")
        void doesNotGoBelowZero_whenDecreaseCalledAtZero() {
            // Arrange
            Brand brand = new Brand("Nike", "스포츠 브랜드", "https://example.com/nike-logo.png");

            // Act
            brand.decreaseLikeCount();

            // Assert
            assertThat(brand.getLikeCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("DB 조회 데이터 복원 (toDomain 용)")
    class RestoreFromDatabase {

        @Test
        @DisplayName("DB에서 조회한 데이터로 Brand 도메인 객체를 복원한다")
        void restoresBrandFromDatabaseRecord() {
            // Arrange
            LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 2, 10, 0);

            // Act
            Brand brand = new Brand(1L, "Nike", "스포츠 브랜드", "https://example.com/nike-logo.png", 0L,
                createdAt, updatedAt, null);

            // Assert
            assertAll(
                () -> assertThat(brand.getId()).isEqualTo(1L),
                () -> assertThat(brand.getName()).isEqualTo("Nike"),
                () -> assertThat(brand.getCreatedAt()).isEqualTo(createdAt),
                () -> assertThat(brand.getUpdatedAt()).isEqualTo(updatedAt),
                () -> assertThat(brand.isDeleted()).isFalse()
            );
        }

        @Test
        @DisplayName("삭제된 브랜드 데이터를 복원하면 isDeleted가 true를 반환한다")
        void returnsTrue_whenRestoredBrandWasDeleted() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();

            // Act
            Brand brand = new Brand(1L, "Nike", "스포츠 브랜드", "https://example.com/nike-logo.png", 0L,
                now, now, now);

            // Assert
            assertThat(brand.isDeleted()).isTrue();
        }
    }
}