package com.loopers.domain.product;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class Product {

    private Long id;
    private String name;
    private String productCode;
    private Long brandId;
    private Long categoryId;
    private Long basePrice;
    private ProductStatus status;
    private Long discount;
    private DiscountType discountType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public Product(String name, Long brandId, Long categoryId, Long basePrice) {
        ProductValidator.validateName(name);
        ProductValidator.validateBrandId(brandId);
        ProductValidator.validateCategoryId(categoryId);
        ProductValidator.validateBasePrice(basePrice);

        this.name = name;
        this.brandId = brandId;
        this.categoryId = categoryId;
        this.basePrice = basePrice;
        this.status = ProductStatus.SALE;
        this.productCode = generateProductCode();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private String generateProductCode() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomSuffix = ThreadLocalRandom.current().nextInt(0, 100000);
        return String.format("%s-%05d", datePrefix, randomSuffix);
    }
}