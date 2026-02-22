package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductImage;
import com.loopers.domain.product.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductImageRepositoryImpl implements ProductImageRepository {

    private final ProductImageJpaRepository productImageJpaRepository;

    @Override
    public Optional<ProductImage> findById(Long id) {
        return productImageJpaRepository.findById(id)
            .map(ProductImageEntity::toDomain);
    }

    @Override
    public List<ProductImage> findAllByProductId(Long productId) {
        return productImageJpaRepository.findByProductId(productId)
            .stream()
            .map(ProductImageEntity::toDomain)
            .toList();
    }

    @Override
    public ProductImage save(ProductImage productImage) {
        ProductImageEntity entity = ProductImageEntity.from(productImage);
        ProductImageEntity saved = productImageJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void delete(Long id) {
        productImageJpaRepository.deleteById(id);
    }

    @Override
    public void deleteAllByProductId(Long productId) {
        productImageJpaRepository.deleteByProductId(productId);
    }
}