package com.loopers.domain.category;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Category 도메인 단위 테스트")
class CategoryTest {

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Test
        @DisplayName("정상적인 값으로 루트 카테고리를 생성한다")
        void createsRootCategory_whenValidValues() {
            // Act
            Category category = new Category("전자제품");

            // Assert
            assertAll(
                () -> assertThat(category.getName()).isEqualTo("전자제품"),
                () -> assertThat(category.getParentId()).isNull(),
                () -> assertThat(category.getDepth()).isEqualTo(0),
                () -> assertThat(category.isRoot()).isTrue()
            );
        }

        @Test
        @DisplayName("부모 카테고리를 지정하여 하위 카테고리를 생성한다")
        void createsChildCategory_whenParentProvided() {
            // Arrange
            Long parentId = 1L;
            String parentPath = "1";
            Integer parentDepth = 0;

            // Act
            Category category = new Category("휴대폰", parentId, parentPath, parentDepth);

            // Assert
            assertAll(
                () -> assertThat(category.getName()).isEqualTo("휴대폰"),
                () -> assertThat(category.getParentId()).isEqualTo(1L),
                () -> assertThat(category.getDepth()).isEqualTo(1),
                () -> assertThat(category.isRoot()).isFalse()
            );
        }

        @Test
        @DisplayName("name이 null이면 예외가 발생한다")
        void throwsException_whenNameIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> new Category(null))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("name이 빈 문자열이면 예외가 발생한다")
        void throwsException_whenNameIsEmpty() {
            // Act & Assert
            assertThatThrownBy(() -> new Category(""))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("name이 공백 문자열이면 예외가 발생한다")
        void throwsException_whenNameIsBlank() {
            // Act & Assert
            assertThatThrownBy(() -> new Category("   "))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @Nested
    @DisplayName("isRoot")
    class IsRoot {

        @Test
        @DisplayName("parentId가 null이면 true를 반환한다")
        void returnsTrue_whenParentIdIsNull() {
            // Arrange
            Category category = new Category("전자제품");

            // Act & Assert
            assertThat(category.isRoot()).isTrue();
        }

        @Test
        @DisplayName("parentId가 존재하면 false를 반환한다")
        void returnsFalse_whenParentIdExists() {
            // Arrange
            Category category = new Category("휴대폰", 1L, "1", 0);

            // Act & Assert
            assertThat(category.isRoot()).isFalse();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("삭제하면 deletedAt이 설정된다")
        void setsDeletedAt_whenDeleted() {
            // Arrange
            Category category = new Category("전자제품");

            // Act
            category.delete();

            // Assert
            assertAll(
                () -> assertThat(category.isDeleted()).isTrue(),
                () -> assertThat(category.getDeletedAt()).isNotNull()
            );
        }

        @Test
        @DisplayName("삭제되지 않은 카테고리는 isDeleted가 false다")
        void returnsFalse_whenNotDeleted() {
            // Arrange
            Category category = new Category("전자제품");

            // Act & Assert
            assertThat(category.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("카테고리명을 수정한다")
        void updatesName() {
            // Arrange
            Category category = new Category(1L, null, "전자제품", "1", 0, null, null, null);

            // Act
            category.update("가전제품");

            // Assert
            assertThat(category.getName()).isEqualTo("가전제품");
        }

        @Test
        @DisplayName("수정 시 name이 null이면 예외가 발생한다")
        void throwsException_whenUpdateNameIsNull() {
            // Arrange
            Category category = new Category(1L, null, "전자제품", "1", 0, null, null, null);

            // Act & Assert
            assertThatThrownBy(() -> category.update(null))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }
}