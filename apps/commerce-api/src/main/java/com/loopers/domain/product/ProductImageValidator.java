package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public class ProductImageValidator {

    public static void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미지 URL은 필수입니다.");
        }
    }
}