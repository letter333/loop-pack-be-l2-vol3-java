package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductOption;
import com.loopers.domain.product.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductOptionRepositoryImpl implements ProductOptionRepository {

    private final ProductOptionJpaRepository productOptionJpaRepository;

    @Override
    public Optional<ProductOption> findById(Long id) {
        return productOptionJpaRepository.findById(id)
            .map(ProductOptionEntity::toDomain);
    }

    @Override
    public List<ProductOption> findAllByProductId(Long productId) {
        return productOptionJpaRepository.findByProductIdAndDeletedAtIsNull(productId)
            .stream()
            .map(ProductOptionEntity::toDomain)
            .toList();
    }

    @Override
    public ProductOption save(ProductOption productOption) {
        ProductOptionEntity entity = ProductOptionEntity.from(productOption);
        ProductOptionEntity saved = productOptionJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void delete(Long id) {
        productOptionJpaRepository.deleteById(id);
    }
}