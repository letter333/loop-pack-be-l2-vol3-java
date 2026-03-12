-- =============================================================================
-- 상품 목록 조회 필터/정렬 조합별 쿼리 정리
-- Source: ProductJpaRepositoryCustomImpl.findProducts()
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 필터 없음 (기본 정렬: 최신순)
-- -----------------------------------------------------------------------------
-- 데이터 조회
SELECT * FROM products WHERE deleted_at IS NULL
ORDER BY created_at DESC
LIMIT :limit OFFSET :offset;

-- -----------------------------------------------------------------------------
-- 2. 카테고리 필터 단독 (기본 정렬: 최신순)
-- -----------------------------------------------------------------------------
-- 데이터 조회
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = :categoryId
ORDER BY created_at DESC
LIMIT :limit OFFSET :offset;

-- -----------------------------------------------------------------------------
-- 3. 브랜드 필터 단독 (기본 정렬: 최신순)
-- -----------------------------------------------------------------------------
-- 데이터 조회
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = :brandId
ORDER BY created_at DESC
LIMIT :limit OFFSET :offset;

-- -----------------------------------------------------------------------------
-- 4. 키워드 검색 단독 (기본 정렬: 최신순)
-- -----------------------------------------------------------------------------
-- 데이터 조회
SELECT * FROM products WHERE deleted_at IS NULL
  AND MATCH(name) AGAINST(:keyword IN BOOLEAN MODE)
ORDER BY created_at DESC
LIMIT :limit OFFSET :offset;

-- -----------------------------------------------------------------------------
-- 5. 가격순 정렬 단독 (필터 없음)
-- -----------------------------------------------------------------------------
-- 데이터 조회
SELECT * FROM products WHERE deleted_at IS NULL
ORDER BY base_price ASC
LIMIT :limit OFFSET :offset;

-- -----------------------------------------------------------------------------
-- 6. 좋아요순 정렬 단독 (필터 없음)
-- -----------------------------------------------------------------------------
-- 데이터 조회
SELECT * FROM products WHERE deleted_at IS NULL
ORDER BY like_count DESC, created_at DESC
LIMIT :limit OFFSET :offset;

-- -----------------------------------------------------------------------------
-- 7. 브랜드 필터 + 가격순 정렬
-- -----------------------------------------------------------------------------
-- 데이터 조회
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = :brandId
ORDER BY base_price ASC
LIMIT :limit OFFSET :offset;

-- -----------------------------------------------------------------------------
-- 8. 브랜드 필터 + 좋아요순 정렬
-- -----------------------------------------------------------------------------
-- 데이터 조회
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = :brandId
ORDER BY like_count DESC, created_at DESC
LIMIT :limit OFFSET :offset;

-- -----------------------------------------------------------------------------
-- 9. 카테고리 필터 + 좋아요순 정렬
-- -----------------------------------------------------------------------------
-- 데이터 조회
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = :categoryId
ORDER BY like_count DESC, created_at DESC
LIMIT :limit OFFSET :offset;

-- -----------------------------------------------------------------------------
-- 10. 브랜드 + 카테고리 필터 + 가격순 정렬
-- -----------------------------------------------------------------------------
-- 데이터 조회
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = :categoryId
  AND brand_id = :brandId
ORDER BY base_price ASC
LIMIT :limit OFFSET :offset;

-- -----------------------------------------------------------------------------
-- 11. 브랜드 + 키워드 필터 (기본 정렬: 최신순)
-- -----------------------------------------------------------------------------
-- 데이터 조회
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = :brandId
  AND MATCH(name) AGAINST(:keyword IN BOOLEAN MODE)
ORDER BY created_at DESC
LIMIT :limit OFFSET :offset;

-- -----------------------------------------------------------------------------
-- 12. 전체 필터 (카테고리 + 브랜드 + 키워드) + 좋아요순 정렬
-- -----------------------------------------------------------------------------
-- 데이터 조회
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = :categoryId
  AND brand_id = :brandId
  AND MATCH(name) AGAINST(:keyword IN BOOLEAN MODE)
ORDER BY like_count DESC, created_at DESC
LIMIT :limit OFFSET :offset;


-- =============================================================================
-- EXPLAIN 실행 계획 분석 (인덱스 적용 전)
-- =============================================================================
-- 사전 조건: SeedProductTasklet으로 100만 건 시드 데이터 적재 완료 (Zipf 분포, alpha=1.2)
-- 기존 인덱스: PK(id), UNIQUE(product_code), FULLTEXT(name - ngram)
-- 없는 인덱스: category_id, brand_id, base_price, like_count, created_at, deleted_at
--
-- 브랜드 분포 (Zipf): brand_id=1(나이키) ~285,222건(28.5%) ~ brand_id=80(뱅앤올룹슨) ~1,403건(0.14%)
--
-- 바인드 파라미터 기준값:
--   categoryId = 3  (하위 카테고리, ~56개 중 1개 → 약 17,875건)
--   brandId    = 1  (80개 브랜드 중 Zipf 최상위 → 약 285,222건, 28.5%)
--   keyword    = 프리미엄  (BOOLEAN MODE, ngram → 약 12,594건)
--   limit      = 20
--   offset     = 0
--
-- 참고: EXPLAIN은 쿼리를 실행하지 않고 옵티마이저의 실행 계획만 표시한다.
--       OFFSET 값에 관계없이 동일한 실행 계획을 반환하므로, 대표 OFFSET 0 으로 측정한다.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. 사전 준비
-- -----------------------------------------------------------------------------
-- 현재 인덱스 확인
SHOW INDEX FROM products;

-- 테이블 통계 확인
SELECT COUNT(*) AS total_rows FROM products;
SELECT COUNT(*) AS active_rows FROM products WHERE deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- 1. 필터 없음 (기본 정렬: 최신순) - EXPLAIN
-- -----------------------------------------------------------------------------
-- 데이터 조회
EXPLAIN
SELECT * FROM products WHERE deleted_at IS NULL
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;

-- -----------------------------------------------------------------------------
-- 2. 카테고리 필터 단독 (기본 정렬: 최신순) - EXPLAIN
-- -----------------------------------------------------------------------------
-- 데이터 조회
EXPLAIN
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = 3
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;

-- -----------------------------------------------------------------------------
-- 3. 브랜드 필터 단독 (기본 정렬: 최신순) - EXPLAIN
-- -----------------------------------------------------------------------------
-- 데이터 조회
EXPLAIN
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = 1
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;

-- -----------------------------------------------------------------------------
-- 4. 키워드 검색 단독 (기본 정렬: 최신순) - EXPLAIN
-- -----------------------------------------------------------------------------
-- 데이터 조회
EXPLAIN
SELECT * FROM products WHERE deleted_at IS NULL
  AND MATCH(name) AGAINST('프리미엄' IN BOOLEAN MODE)
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;

-- -----------------------------------------------------------------------------
-- 5. 가격순 정렬 단독 (필터 없음) - EXPLAIN
-- -----------------------------------------------------------------------------
-- 데이터 조회
EXPLAIN
SELECT * FROM products WHERE deleted_at IS NULL
ORDER BY base_price ASC
LIMIT 20 OFFSET 0;

-- -----------------------------------------------------------------------------
-- 6. 좋아요순 정렬 단독 (필터 없음) - EXPLAIN
-- -----------------------------------------------------------------------------
-- 데이터 조회
EXPLAIN
SELECT * FROM products WHERE deleted_at IS NULL
ORDER BY like_count DESC, created_at DESC
LIMIT 20 OFFSET 0;

-- -----------------------------------------------------------------------------
-- 7. 브랜드 필터 + 가격순 정렬 - EXPLAIN
-- -----------------------------------------------------------------------------
-- 데이터 조회
EXPLAIN
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = 1
ORDER BY base_price ASC
LIMIT 20 OFFSET 0;

-- -----------------------------------------------------------------------------
-- 8. 브랜드 필터 + 좋아요순 정렬 - EXPLAIN
-- -----------------------------------------------------------------------------
-- 데이터 조회
EXPLAIN
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = 1
ORDER BY like_count DESC, created_at DESC
LIMIT 20 OFFSET 0;

-- -----------------------------------------------------------------------------
-- 9. 카테고리 필터 + 좋아요순 정렬 - EXPLAIN
-- -----------------------------------------------------------------------------
-- 데이터 조회
EXPLAIN
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = 3
ORDER BY like_count DESC, created_at DESC
LIMIT 20 OFFSET 0;

-- -----------------------------------------------------------------------------
-- 10. 브랜드 + 카테고리 필터 + 가격순 정렬 - EXPLAIN
-- -----------------------------------------------------------------------------
-- 데이터 조회
EXPLAIN
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = 3
  AND brand_id = 1
ORDER BY base_price ASC
LIMIT 20 OFFSET 0;

-- -----------------------------------------------------------------------------
-- 11. 브랜드 + 키워드 필터 (기본 정렬: 최신순) - EXPLAIN
-- -----------------------------------------------------------------------------
-- 데이터 조회
EXPLAIN
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = 1
  AND MATCH(name) AGAINST('프리미엄' IN BOOLEAN MODE)
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;

-- -----------------------------------------------------------------------------
-- 12. 전체 필터 (카테고리 + 브랜드 + 키워드) + 좋아요순 정렬 - EXPLAIN
-- -----------------------------------------------------------------------------
-- 데이터 조회
EXPLAIN
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = 3
  AND brand_id = 1
  AND MATCH(name) AGAINST('프리미엄' IN BOOLEAN MODE)
ORDER BY like_count DESC, created_at DESC
LIMIT 20 OFFSET 0;
