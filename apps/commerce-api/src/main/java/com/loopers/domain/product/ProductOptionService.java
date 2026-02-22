package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductOptionService {

    private final ProductOptionRepository productOptionRepository;

    @Transactional(readOnly = true)
    public ProductOption getProductOption(Long optionId) {
        return productOptionRepository.findById(optionId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품 옵션을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<ProductOption> getProductOptions(Long productId) {
        return productOptionRepository.findAllByProductId(productId);
    }

    @Transactional
    public ProductOption createProductOption(Long productId, String optionValue, String displayName,
                                              Long extraPrice, Integer stockQuantity) {
        ProductOption productOption = new ProductOption(productId, optionValue, displayName, extraPrice, stockQuantity);
        return productOptionRepository.save(productOption);
    }

    @Transactional
    public void decreaseStock(Long optionId, int quantity) {
        getProductOption(optionId);
        productOptionRepository.decreaseStock(optionId, quantity);
    }

    @Transactional
    public void increaseStock(Long optionId, int quantity) {
        getProductOption(optionId);
        productOptionRepository.increaseStock(optionId, quantity);
    }
}