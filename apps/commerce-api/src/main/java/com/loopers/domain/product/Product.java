package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
    private Long likeCount;
    private List<ProductOption> options;
    private List<ProductImage> images;
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
        this.likeCount = 0L;
        this.options = new ArrayList<>();
        this.images = new ArrayList<>();
    }

    public Product(String name, Long brandId, Long categoryId, Long basePrice,
                   List<ProductOption> options, List<ProductImage> images) {
        this(name, brandId, categoryId, basePrice);
        this.options = options != null ? new ArrayList<>(options) : new ArrayList<>();
        this.images = images != null ? new ArrayList<>(images) : new ArrayList<>();
    }

    public Product(Long id, String name, String productCode, Long brandId, Long categoryId, Long basePrice,
                   ProductStatus status, Long discount, DiscountType discountType, Long likeCount,
                   LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.name = name;
        this.productCode = productCode;
        this.brandId = brandId;
        this.categoryId = categoryId;
        this.basePrice = basePrice;
        this.status = status;
        this.discount = discount;
        this.discountType = discountType;
        this.likeCount = likeCount != null ? likeCount : 0L;
        this.options = new ArrayList<>();
        this.images = new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public Product(Long id, String name, String productCode, Long brandId, Long categoryId, Long basePrice,
                   ProductStatus status, Long discount, DiscountType discountType, Long likeCount,
                   List<ProductOption> options, List<ProductImage> images,
                   LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.name = name;
        this.productCode = productCode;
        this.brandId = brandId;
        this.categoryId = categoryId;
        this.basePrice = basePrice;
        this.status = status;
        this.discount = discount;
        this.discountType = discountType;
        this.likeCount = likeCount != null ? likeCount : 0L;
        this.options = options != null ? new ArrayList<>(options) : new ArrayList<>();
        this.images = images != null ? new ArrayList<>(images) : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public Long calculateDiscountedPrice() {
        if (discount == null || discountType == null) {
            return basePrice;
        }

        long discountedPrice = switch (discountType) {
            case PRICE -> basePrice - discount;
            case RATE -> basePrice - (basePrice * discount / 100);
        };

        return Math.max(0L, discountedPrice);
    }

    public void applyDiscount(Long discount, DiscountType discountType) {
        ProductValidator.validateDiscount(discount, discountType, this.basePrice);
        this.discount = discount;
        this.discountType = discountType;
    }

    public void removeDiscount() {
        this.discount = null;
        this.discountType = null;
    }

    public void update(String name, Long categoryId, Long basePrice,
                       Long discount, DiscountType discountType, ProductStatus status) {
        ProductValidator.validateName(name);
        ProductValidator.validateCategoryId(categoryId);
        ProductValidator.validateBasePrice(basePrice);
        ProductValidator.validateDiscount(discount, discountType, basePrice);
        ProductValidator.validateStatus(status);
        this.name = name;
        this.categoryId = categoryId;
        this.basePrice = basePrice;
        this.discount = discount;
        this.discountType = discountType;
        this.status = status;
    }

    public boolean isAvailable() {
        return status == ProductStatus.SALE && !isDeleted();
    }

    public void delete() {
        if (deletedAt == null) {
            this.deletedAt = LocalDateTime.now();
        }
    }

    public void addOption(ProductOption option) {
        if (option == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "옵션은 null일 수 없습니다.");
        }
        this.options.add(option);
    }

    public void removeOption(Long optionId) {
        this.options.removeIf(opt -> opt.getId().equals(optionId));
    }

    public ProductOption getOption(Long optionId) {
        return options.stream()
            .filter(opt -> opt.getId().equals(optionId))
            .findFirst()
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품 옵션을 찾을 수 없습니다."));
    }

    public void decreaseStock(Long optionId, int quantity) {
        ProductOption option = getOption(optionId);
        option.decreaseStock(quantity);
        checkAndUpdateSoldoutStatus();
    }

    public void increaseStock(Long optionId, int quantity) {
        ProductOption option = getOption(optionId);
        option.increaseStock(quantity);
        checkAndUpdateSoldoutStatus();
    }

    public int getTotalStockQuantity() {
        if (options == null || options.isEmpty()) {
            return 0;
        }
        return options.stream()
            .mapToInt(ProductOption::getStockQuantity)
            .sum();
    }

    public void checkAndUpdateSoldoutStatus() {
        if (this.status == ProductStatus.STOP) {
            return;
        }
        int totalStock = getTotalStockQuantity();
        if (totalStock == 0 && this.status == ProductStatus.SALE) {
            this.status = ProductStatus.SOLDOUT;
        } else if (totalStock > 0 && this.status == ProductStatus.SOLDOUT) {
            this.status = ProductStatus.SALE;
        }
    }

    public void addImage(ProductImage image) {
        if (image == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미지는 null일 수 없습니다.");
        }
        this.images.add(image);
    }

    public void removeImage(Long imageId) {
        this.images.removeIf(img -> img.getId().equals(imageId));
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    private String generateProductCode() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomSuffix = ThreadLocalRandom.current().nextInt(0, 100000);
        return String.format("%s-%05d", datePrefix, randomSuffix);
    }
}