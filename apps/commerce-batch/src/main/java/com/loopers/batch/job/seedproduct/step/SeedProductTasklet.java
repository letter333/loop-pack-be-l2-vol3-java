package com.loopers.batch.job.seedproduct.step;

import com.loopers.batch.job.seedproduct.SeedProductDataDictionary;
import com.loopers.batch.job.seedproduct.SeedProductJobConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@StepScope
@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = SeedProductJobConfig.JOB_NAME)
@RequiredArgsConstructor
@Component
public class SeedProductTasklet implements Tasklet {

    private static final int PRODUCT_COUNT = 1_000_000;
    private static final int BATCH_SIZE = 5_000;
    private static final double BRAND_ZIPF_ALPHA = 1.2;

    private final DataSource dataSource;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            createTablesIfNotExist(conn);
            disableChecks(conn);
            truncateTables(conn);

            List<Long> brandIds = insertBrands(conn);
            log.info("브랜드 {}건 삽입 완료", brandIds.size());

            List<CategoryInfo> categories = insertCategories(conn);
            log.info("카테고리 {}건 삽입 완료", categories.size());

            insertProducts(conn, brandIds, categories);

            enableChecks(conn);
            conn.commit();

            log.info("상품 시딩 완료: 총 {}건", PRODUCT_COUNT);
        }

        return RepeatStatus.FINISHED;
    }

    private void createTablesIfNotExist(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS brands (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    name VARCHAR(50) NOT NULL,
                    description TEXT,
                    logo_image_url VARCHAR(512),
                    like_count BIGINT NOT NULL DEFAULT 0,
                    version BIGINT NOT NULL DEFAULT 0,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    deleted_at DATETIME(6),
                    PRIMARY KEY (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    parent_id BIGINT,
                    name VARCHAR(20) NOT NULL,
                    path VARCHAR(255),
                    depth INT,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    deleted_at DATETIME(6),
                    PRIMARY KEY (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL,
                    product_code VARCHAR(20) NOT NULL,
                    brand_id BIGINT NOT NULL,
                    category_id BIGINT NOT NULL,
                    base_price BIGINT NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    discount BIGINT,
                    discount_type VARCHAR(20),
                    like_count BIGINT NOT NULL DEFAULT 0,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    deleted_at DATETIME(6),
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_products_product_code (product_code)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS product_options (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    product_id BIGINT NOT NULL,
                    option_value VARCHAR(50) NOT NULL,
                    display_name VARCHAR(255),
                    extra_price BIGINT,
                    stock_quantity INT,
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    deleted_at DATETIME(6),
                    PRIMARY KEY (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS product_images (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    product_id BIGINT NOT NULL,
                    type VARCHAR(20),
                    url VARCHAR(512) NOT NULL,
                    alt_text VARCHAR(255),
                    created_at DATETIME(6) NOT NULL,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

            conn.commit();
            log.info("테이블 생성/확인 완료");
        }
    }

    private void truncateTables(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE product_images");
            stmt.execute("TRUNCATE TABLE product_options");
            stmt.execute("TRUNCATE TABLE products");
            stmt.execute("TRUNCATE TABLE categories");
            stmt.execute("TRUNCATE TABLE brands");
        }
        log.info("기존 시드 데이터 삭제 완료");
    }

    private void disableChecks(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            stmt.execute("SET UNIQUE_CHECKS = 0");
        }
    }

    private void enableChecks(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET UNIQUE_CHECKS = 1");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    // ==================== Brand ====================

    private List<Long> insertBrands(Connection conn) throws Exception {
        String sql = "INSERT INTO brands (name, description, logo_image_url, like_count, version, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, 0, NOW(), NOW())";

        List<Long> brandIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String[] names = SeedProductDataDictionary.BRAND_NAMES;
            String[] descs = SeedProductDataDictionary.BRAND_DESCRIPTIONS;
            ThreadLocalRandom random = ThreadLocalRandom.current();

            for (String name : names) {
                ps.setString(1, name);
                ps.setString(2, descs[random.nextInt(descs.length)]);
                ps.setString(3, "https://example.com/brands/" + name.hashCode() + "/logo.png");
                ps.setLong(4, random.nextLong(0, 5001));
                ps.addBatch();
            }
            ps.executeBatch();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                while (rs.next()) {
                    brandIds.add(rs.getLong(1));
                }
            }
        }
        return brandIds;
    }

    // ==================== Category ====================

    private List<CategoryInfo> insertCategories(Connection conn) throws Exception {
        String sql = "INSERT INTO categories (parent_id, name, path, depth, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, NOW(), NOW())";
        String updatePathSql = "UPDATE categories SET path = CAST(id AS CHAR) WHERE id = ?";

        List<CategoryInfo> allCategories = new ArrayList<>();
        Map<String, List<String>> hierarchy = SeedProductDataDictionary.CATEGORY_HIERARCHY;

        for (Map.Entry<String, List<String>> entry : hierarchy.entrySet()) {
            String rootName = entry.getKey();

            // 루트 카테고리 삽입
            long rootId;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setNull(1, java.sql.Types.BIGINT);
                ps.setString(2, rootName);
                ps.setString(3, "temp");
                ps.setInt(4, 0);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    rootId = rs.getLong(1);
                }
            }

            // path를 자기 자신의 ID로 업데이트
            try (PreparedStatement ps = conn.prepareStatement(updatePathSql)) {
                ps.setLong(1, rootId);
                ps.executeUpdate();
            }

            // 하위 카테고리 삽입
            List<String> children = entry.getValue();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (String childName : children) {
                    ps.setLong(1, rootId);
                    ps.setString(2, childName);
                    ps.setString(3, "temp");
                    ps.setInt(4, 1);
                    ps.addBatch();
                }
                ps.executeBatch();

                String updateChildPathSql = "UPDATE categories SET path = CONCAT(?, '/', CAST(id AS CHAR)) WHERE id = ?";
                try (ResultSet rs = ps.getGeneratedKeys();
                     PreparedStatement updatePs = conn.prepareStatement(updateChildPathSql)) {
                    int childIdx = 0;
                    while (rs.next()) {
                        long childId = rs.getLong(1);
                        updatePs.setString(1, String.valueOf(rootId));
                        updatePs.setLong(2, childId);
                        updatePs.addBatch();

                        allCategories.add(new CategoryInfo(childId, children.get(childIdx), rootName));
                        childIdx++;
                    }
                    updatePs.executeBatch();
                }
            }
        }

        return allCategories;
    }

    // ==================== Product + Option + Image ====================

    private void insertProducts(Connection conn, List<Long> brandIds, List<CategoryInfo> categories) throws Exception {
        String productSql = "INSERT INTO products (name, product_code, brand_id, category_id, base_price, status, discount, discount_type, like_count, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        String optionSql = "INSERT INTO product_options (product_id, option_value, display_name, extra_price, stock_quantity, created_at, updated_at) " +
                           "VALUES (?, ?, ?, ?, ?, NOW(), NOW())";
        String imageSql = "INSERT INTO product_images (product_id, type, url, alt_text, created_at, updated_at) " +
                          "VALUES (?, ?, ?, ?, NOW(), NOW())";

        ThreadLocalRandom random = ThreadLocalRandom.current();
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int productCodeSeq = 0;
        double[] brandCdf = buildBrandCdf(brandIds.size());

        String[] statuses = {"SALE", "SALE", "SALE", "SALE", "SALE", "SALE", "SALE", "STOP", "STOP", "SOLDOUT"};
        String[] imageTypes = {"MAIN", "SUB", "DETAIL"};

        for (int offset = 0; offset < PRODUCT_COUNT; offset += BATCH_SIZE) {
            int batchEnd = Math.min(offset + BATCH_SIZE, PRODUCT_COUNT);
            int currentBatchSize = batchEnd - offset;

            // 배치에 필요한 데이터 준비
            List<ProductSeed> seeds = new ArrayList<>(currentBatchSize);
            for (int i = 0; i < currentBatchSize; i++) {
                CategoryInfo cat = categories.get(random.nextInt(categories.size()));
                seeds.add(generateProductSeed(random, cat, brandIds, brandCdf, statuses, datePrefix, ++productCodeSeq));
            }

            // 상품 batch insert
            List<Long> productIds = new ArrayList<>(currentBatchSize);
            try (PreparedStatement ps = conn.prepareStatement(productSql, Statement.RETURN_GENERATED_KEYS)) {
                for (ProductSeed seed : seeds) {
                    ps.setString(1, seed.name);
                    ps.setString(2, seed.productCode);
                    ps.setLong(3, seed.brandId);
                    ps.setLong(4, seed.categoryId);
                    ps.setLong(5, seed.basePrice);
                    ps.setString(6, seed.status);
                    if (seed.discount != null) {
                        ps.setLong(7, seed.discount);
                        ps.setString(8, seed.discountType);
                    } else {
                        ps.setNull(7, java.sql.Types.BIGINT);
                        ps.setNull(8, java.sql.Types.VARCHAR);
                    }
                    ps.setLong(9, seed.likeCount);
                    ps.addBatch();
                }
                ps.executeBatch();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    while (rs.next()) {
                        productIds.add(rs.getLong(1));
                    }
                }
            }

            // 옵션 batch insert
            try (PreparedStatement ps = conn.prepareStatement(optionSql)) {
                for (int i = 0; i < productIds.size(); i++) {
                    long productId = productIds.get(i);
                    ProductSeed seed = seeds.get(i);
                    String[][] optionSets = SeedProductDataDictionary.CATEGORY_OPTIONS.get(seed.subCategoryName);

                    if (optionSets != null && optionSets.length > 0) {
                        // 첫 번째 옵션 세트에서 랜덤 개수 선택
                        String[] optionValues = optionSets[0];
                        int optionCount = Math.min(random.nextInt(1, 6), optionValues.length);
                        for (int j = 0; j < optionCount; j++) {
                            ps.setLong(1, productId);
                            ps.setString(2, optionValues[j]);
                            ps.setString(3, seed.name + " - " + optionValues[j]);
                            ps.setLong(4, j == 0 ? 0 : random.nextLong(1000, 50001));
                            ps.setInt(5, "SOLDOUT".equals(seed.status) ? 0 : random.nextInt(10, 1001));
                            ps.addBatch();
                        }
                    } else {
                        // 옵션 매핑이 없는 카테고리는 기본 옵션 1~3개
                        int optionCount = random.nextInt(1, 4);
                        for (int j = 0; j < optionCount; j++) {
                            ps.setLong(1, productId);
                            ps.setString(2, "옵션" + (j + 1));
                            ps.setString(3, seed.name + " - 옵션" + (j + 1));
                            ps.setLong(4, j == 0 ? 0 : random.nextLong(1000, 30001));
                            ps.setInt(5, "SOLDOUT".equals(seed.status) ? 0 : random.nextInt(10, 1001));
                            ps.addBatch();
                        }
                    }
                }
                ps.executeBatch();
            }

            // 이미지 batch insert
            try (PreparedStatement ps = conn.prepareStatement(imageSql)) {
                for (int i = 0; i < productIds.size(); i++) {
                    long productId = productIds.get(i);
                    ProductSeed seed = seeds.get(i);
                    int imageCount = random.nextInt(1, 6);

                    for (int j = 0; j < imageCount; j++) {
                        ps.setLong(1, productId);
                        ps.setString(2, j == 0 ? "MAIN" : imageTypes[random.nextInt(1, imageTypes.length)]);
                        ps.setString(3, "https://example.com/products/" + productId + "/" + (j == 0 ? "main" : "sub_" + j) + ".jpg");
                        ps.setString(4, seed.name + " 이미지 " + (j + 1));
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }

            conn.commit();
            log.info("상품 시딩 진행: {}/{}", batchEnd, PRODUCT_COUNT);
        }
    }

    private double[] buildBrandCdf(int brandCount) {
        double[] cdf = new double[brandCount];
        double total = 0;
        for (int i = 0; i < brandCount; i++) {
            total += 1.0 / Math.pow(i + 1, BRAND_ZIPF_ALPHA);
            cdf[i] = total;
        }
        for (int i = 0; i < brandCount; i++) {
            cdf[i] /= total;
        }
        return cdf;
    }

    private int pickBrandIndex(ThreadLocalRandom random, double[] cdf) {
        double r = random.nextDouble();
        int idx = Arrays.binarySearch(cdf, r);
        return idx >= 0 ? idx : Math.min(-(idx + 1), cdf.length - 1);
    }

    private ProductSeed generateProductSeed(ThreadLocalRandom random, CategoryInfo cat,
                                            List<Long> brandIds, double[] brandCdf, String[] statuses,
                                            String datePrefix, int seq) {
        String subCat = cat.subCategoryName;
        String rootCat = cat.rootCategoryName;

        // 상품명 생성
        String[] prefixes = SeedProductDataDictionary.PRODUCT_PREFIXES.get(subCat);
        String prefix = (prefixes != null) ? prefixes[random.nextInt(prefixes.length)] : "기본";

        Map<String, String[]> suffixMap = SeedProductDataDictionary.PRODUCT_SUFFIXES;
        String[] suffixes = suffixMap.get(subCat);
        String suffix = (suffixes != null) ? suffixes[random.nextInt(suffixes.length)] : subCat;

        String name = prefix + " " + suffix;

        // 상품 코드
        String productCode;
        if (seq <= 99999) {
            productCode = datePrefix + "-" + String.format("%05d", seq);
        } else {
            int extraDays = (seq - 1) / 99999;
            int remainder = ((seq - 1) % 99999) + 1;
            LocalDate adjustedDate = LocalDate.now().minusDays(extraDays);
            productCode = adjustedDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + String.format("%05d", remainder);
        }

        // 가격
        long[] priceRange = SeedProductDataDictionary.PRICE_RANGES.getOrDefault(rootCat, new long[]{10000, 500000});
        long basePrice = roundToThousand(random.nextLong(priceRange[0], priceRange[1] + 1));

        // 상태
        String status = statuses[random.nextInt(statuses.length)];

        // 할인
        Long discount = null;
        String discountType = null;
        if (random.nextInt(100) < 60) {
            if (random.nextBoolean()) {
                discountType = "RATE";
                discount = random.nextLong(5, 51);
            } else {
                discountType = "PRICE";
                long maxDiscount = Math.max(1000, basePrice / 2);
                discount = roundToThousand(random.nextLong(1000, maxDiscount + 1));
            }
        }

        long likeCount = random.nextLong(0, 10001);

        return new ProductSeed(name, productCode, brandIds.get(pickBrandIndex(random, brandCdf)),
                cat.categoryId, basePrice, status, discount, discountType, likeCount, subCat);
    }

    private long roundToThousand(long value) {
        return (value / 1000) * 1000;
    }

    // ==================== Inner DTOs ====================

    record CategoryInfo(long categoryId, String subCategoryName, String rootCategoryName) {}

    record ProductSeed(String name, String productCode, long brandId, long categoryId,
                       long basePrice, String status, Long discount, String discountType,
                       long likeCount, String subCategoryName) {}
}
