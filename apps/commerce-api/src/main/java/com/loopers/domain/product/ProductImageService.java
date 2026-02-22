package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageRepository productImageRepository;

    @Transactional(readOnly = true)
    public ProductImage getProductImage(Long imageId) {
        return productImageRepository.findById(imageId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품 이미지를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<ProductImage> getProductImages(Long productId) {
        return productImageRepository.findAllByProductId(productId);
    }

    @Transactional
    public ProductImage createProductImage(Long productId, ImageType type, String url, String altText) {
        ProductImage productImage = new ProductImage(productId, type, url, altText);
        return productImageRepository.save(productImage);
    }

    @Transactional
    public void deleteProductImage(Long imageId) {
        getProductImage(imageId);
        productImageRepository.delete(imageId);
    }
}