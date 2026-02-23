package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Product getActiveProduct(Long productId) {
        Product product = getProduct(productId);
        if (product.isDeleted()) {
            throw new CoreException(ErrorType.NOT_FOUND, "삭제된 상품입니다.");
        }
        return product;
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Product> getAllActiveProducts() {
        return productRepository.findAllActive();
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Product> getActiveProductsByCategoryId(Long categoryId) {
        return productRepository.findAllActiveByCategoryId(categoryId);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Product> getProducts(Long categoryId, String keyword, ProductSortType sort, Pageable pageable) {
        return productRepository.findProducts(categoryId, keyword, sort, pageable);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Product createProduct(String name, Long brandId, Long categoryId, Long basePrice,
                                  List<ProductOption> options, List<ProductImage> images) {
        Product product = new Product(name, brandId, categoryId, basePrice, options, images);
        return productRepository.save(product);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Product createProduct(String name, Long brandId, Long categoryId, Long basePrice) {
        return createProduct(name, brandId, categoryId, basePrice, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Product updateProduct(Long productId, String name, Long categoryId, Long basePrice,
                                  Long discount, DiscountType discountType, ProductStatus status) {
        Product product = getProduct(productId);
        product.update(name, categoryId, basePrice, discount, discountType, status);
        return productRepository.save(product);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteProduct(Long productId) {
        Product product = getProduct(productId);
        productRepository.delete(product.getId());
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Product validateProduct(Long productId) {
        return getActiveProduct(productId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void decreaseStock(Long productId, Long optionId, int quantity) {
        Product product = getProduct(productId);
        product.decreaseStock(optionId, quantity);
        productRepository.save(product);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void increaseStock(Long productId, Long optionId, int quantity) {
        Product product = getProduct(productId);
        product.increaseStock(optionId, quantity);
        productRepository.save(product);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public ProductOption getProductOption(Long productId, Long optionId) {
        Product product = getProduct(productId);
        return product.getOption(optionId);
    }
}