package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public final class ProductImageValidator {

    private ProductImageValidator() {
        // 인스턴스화 방지
    }

    public static void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미지 URL은 필수입니다.");
        }
    }

    public static void validateType(ImageType type) {
        if (type == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미지 타입은 필수입니다.");
        }
    }
}