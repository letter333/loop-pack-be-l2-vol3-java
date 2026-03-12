# 상품 목록 조회 쿼리 실행 계획 분석 (인덱스 적용 후)

## 1. 테스트 환경

| 항목 | 값 |
|------|------|
| DBMS | MySQL 8.0 (Docker) |
| 테이블 | `products` |
| 총 행 수 | 1,000,000 건 |
| 활성 행 수 (deleted_at IS NULL) | 1,000,000 건 |
| 시드 데이터 | `SeedProductTasklet` 으로 적재 (Zipf 분포, alpha=1.2) |

### 적용 인덱스 (5개 추가)

| 인덱스 | 컬럼 | 설계 근거 |
|--------|------|-----------|
| `idx_products_brand_created` | `(brand_id, created_at DESC)` | 브랜드 필터 + 최신순 정렬 커버 |
| `idx_products_category_created` | `(category_id, created_at DESC)` | 카테고리 필터 + 최신순 정렬 커버 |
| `idx_products_like_count` | `(like_count DESC, created_at DESC)` | 좋아요순 복합 정렬 커버 |
| `idx_products_base_price` | `(base_price ASC, created_at DESC)` | 가격순 정렬 커버 |
| `idx_products_created` | `(created_at DESC)` | 기본 조회(최신순) 커버 |

### 기존 인덱스 (유지)

| 인덱스 | 타입 | 컬럼 |
|--------|------|------|
| `PRIMARY` | BTREE | `id` |
| `uk_products_product_code` | BTREE (UNIQUE) | `product_code` |
| `ft_products_name` | FULLTEXT (ngram) | `name` |

### 브랜드 분포 (Zipf, alpha=1.2) — 인덱스 효과에 미치는 영향

| 브랜드 (brand_id) | 상품 수 | 선택도 | 인덱스 전략 |
|-------------------|---------|--------|------------|
| 나이키 (1) | 285,222 | 28.5% | 정렬 인덱스 스캔 + 필터가 효율적 |
| 아디다스 (2) | 123,719 | 12.4% | 정렬 인덱스 스캔 + 필터 |
| 뉴발란스 (3) | 75,867 | 7.6% | 브랜드 인덱스 또는 정렬 인덱스 (옵티마이저 판단) |
| 시디즈 (20) | 7,635 | 0.8% | 브랜드 인덱스 Lookup이 효율적 |
| 뱅앤올룹슨 (80) | 1,403 | 0.14% | 브랜드 인덱스 Lookup이 극적으로 효율적 |

---

## 2. 시나리오별 Before vs After 비교

> **참고**: EXPLAIN은 쿼리를 실행하지 않고 옵티마이저의 실행 계획만 표시한다.
> `rows`는 옵티마이저의 추정값이며, 실제 스캔 행 수와 다를 수 있다.
> 비교의 초점은 `type`(접근 방식), `key`(사용 인덱스), `Extra`(filesort 유무)의 변화이다.

### 2.1 필터 없음 + 최신순 (기본 쿼리)

```sql
SELECT * FROM products WHERE deleted_at IS NULL
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

**Before:**

| type | key | rows | filtered | Extra |
|------|-----|------|----------|-------|
| ALL | NULL | 990,629 | 10.00% | Using where; Using filesort |

**After:**

| type | key | rows | filtered | Extra |
|------|-----|------|----------|-------|
| **index** | **idx_products_created** | **20** | 10.00% | Using where |

> **분석**: `type`이 `ALL` → `index`로 변경. `idx_products_created` 인덱스를 사용하여 정렬된 순서로 20건만 스캔한다.
> `Using filesort`가 제거되었고, rows가 990,629 → 20으로 극적으로 감소했다.

---

### 2.2 카테고리 필터 단독 (최신순)

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = 3
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

**Before:**

| type | key | rows | filtered | Extra |
|------|-----|------|----------|-------|
| ALL | NULL | 990,629 | 1.00% | Using where; Using filesort |

**After:**

| type | possible_keys | key | rows | filtered | Extra |
|------|---------------|-----|------|----------|-------|
| **ref** | idx_products_category_created | **idx_products_category_created** | **33,432** | 10.00% | Using where |

> **분석**: `type`이 `ALL` → `ref`로 변경. 복합 인덱스 `(category_id, created_at DESC)`가
> 등호 필터(category_id=3)와 정렬(created_at DESC)을 모두 커버한다.
> `Using filesort`가 제거되었고, 인덱스 Lookup으로 33,432건(추정)만 접근한다.

---

### 2.3 브랜드 필터 단독 (최신순)

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = 1
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

**Before:**

| type | key | rows | filtered | Extra |
|------|-----|------|----------|-------|
| ALL | NULL | 990,629 | 1.00% | Using where; Using filesort |

**After:**

| type | possible_keys | key | rows | filtered | Extra |
|------|---------------|-----|------|----------|-------|
| **ref** | idx_products_brand_created | **idx_products_brand_created** | **495,314** | 10.00% | Using where |

> **분석**: `type`이 `ALL` → `ref`로 변경. 복합 인덱스 `(brand_id, created_at DESC)`가
> 필터+정렬을 모두 커버하여 `Using filesort`가 제거되었다.
> rows 추정이 495,314로 크지만, 인덱스가 정렬 순서를 보장하므로 LIMIT 20에서 즉시 반환 가능하다.

---

### 2.4 키워드 검색 단독 (최신순)

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND MATCH(name) AGAINST('프리미엄' IN BOOLEAN MODE)
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

| 구분 | type | key | rows | filtered | Extra |
|------|------|-----|------|----------|-------|
| Before | fulltext | ft_products_name | 1 | 10.00% | Using where; Ft_hints: no_ranking; Using filesort |
| After | fulltext | ft_products_name | 1 | 10.00% | Using where; Ft_hints: no_ranking; Using filesort |

> 새 인덱스 영향 없음. FULLTEXT 인덱스가 우선 적용되며, 실행 계획이 동일하다.

---

### 2.5 가격순 정렬 단독 (필터 없음)

```sql
SELECT * FROM products WHERE deleted_at IS NULL
ORDER BY base_price ASC
LIMIT 20 OFFSET 0;
```

**Before:**

| type | key | rows | filtered | Extra |
|------|-----|------|----------|-------|
| ALL | NULL | 990,629 | 10.00% | Using where; Using filesort |

**After:**

| type | key | rows | filtered | Extra |
|------|-----|------|----------|-------|
| **index** | **idx_products_base_price** | **20** | 10.00% | Using where |

> **분석**: `type`이 `ALL` → `index`로 변경. `idx_products_base_price` 인덱스를 사용하여
> 가격 순서로 20건만 스캔한다. `Using filesort` 제거.

---

### 2.6 좋아요순 정렬 단독 (필터 없음)

```sql
SELECT * FROM products WHERE deleted_at IS NULL
ORDER BY like_count DESC, created_at DESC
LIMIT 20 OFFSET 0;
```

**Before:**

| type | key | rows | filtered | Extra |
|------|-----|------|----------|-------|
| ALL | NULL | 990,629 | 10.00% | Using where; Using filesort |

**After:**

| type | key | rows | filtered | Extra |
|------|-----|------|----------|-------|
| **index** | **idx_products_like_count** | **20** | 10.00% | Using where |

> **분석**: `type`이 `ALL` → `index`로 변경. 복합 인덱스 `(like_count DESC, created_at DESC)`가
> 정렬 순서를 그대로 커버하여 20건만 스캔한다. `Using filesort` 제거.

---

### 2.7 브랜드 필터 + 가격순 정렬

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = 1
ORDER BY base_price ASC
LIMIT 20 OFFSET 0;
```

**Before:**

| type | key | rows | filtered | Extra |
|------|-----|------|----------|-------|
| ALL | NULL | 990,629 | 1.00% | Using where; Using filesort |

**After:**

| type | possible_keys | key | rows | filtered | Extra |
|------|---------------|-----|------|----------|-------|
| **index** | idx_products_brand_created | **idx_products_base_price** | **40** | 5.00% | Using where |

> **분석**: `type`이 `ALL` → `index`로 변경. 옵티마이저가 **정렬 인덱스(`idx_products_base_price`) 스캔 + brand_id 필터** 전략을 선택했다.
> `possible_keys`에 `idx_products_brand_created`도 후보에 있지만, 가격 인덱스 스캔이 더 효율적이라고 판단.
> brand_id=1이 28.5%를 차지하므로, 가격 인덱스를 순서대로 스캔하면 평균 3.5행마다 1건이 매칭된다.
> rows=40으로 추정(LIMIT 20을 위해 약 40건 스캔), `Using filesort` 제거.

---

### 2.8 브랜드 필터 + 좋아요순 정렬

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = 1
ORDER BY like_count DESC, created_at DESC
LIMIT 20 OFFSET 0;
```

**Before:**

| type | key | rows | filtered | Extra |
|------|-----|------|----------|-------|
| ALL | NULL | 990,629 | 1.00% | Using where; Using filesort |

**After:**

| type | possible_keys | key | rows | filtered | Extra |
|------|---------------|-----|------|----------|-------|
| **index** | idx_products_brand_created | **idx_products_like_count** | **40** | 5.00% | Using where |

> **분석**: 2.7과 동일한 패턴. 옵티마이저가 정렬 인덱스(`idx_products_like_count`) 스캔 + brand_id 필터 전략을 선택.
> `idx_products_brand_created`로 Lookup하면 495,314건을 읽고 filesort해야 하므로,
> 정렬 인덱스 스캔(rows=40)이 훨씬 효율적이다. `Using filesort` 제거.

---

### 2.9 카테고리 필터 + 좋아요순 정렬

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = 3
ORDER BY like_count DESC, created_at DESC
LIMIT 20 OFFSET 0;
```

**Before:**

| type | key | rows | filtered | Extra |
|------|-----|------|----------|-------|
| ALL | NULL | 990,629 | 1.00% | Using where; Using filesort |

**After:**

| type | possible_keys | key | rows | filtered | Extra |
|------|---------------|-----|------|----------|-------|
| **index** | idx_products_category_created | **idx_products_like_count** | **592** | 0.34% | Using where |

> **분석**: `type`이 `ALL` → `index`로 변경. 좋아요순 인덱스 스캔 + category_id 필터 전략.
> category_id=3의 선택도가 1.79%이므로, 평균 약 56행마다 1건이 매칭된다.
> rows=592(추정)으로, 좋아요 인덱스를 순서대로 스캔하면서 category_id=3인 행 20건을 찾는다.
> `Using filesort` 제거.

---

### 2.10 브랜드 + 카테고리 필터 + 가격순 정렬

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = 3 AND brand_id = 1
ORDER BY base_price ASC
LIMIT 20 OFFSET 0;
```

**Before:**

| type | key | rows | filtered | Extra |
|------|-----|------|----------|-------|
| ALL | NULL | 990,629 | 0.10% | Using where; Using filesort |

**After:**

| type | possible_keys | key | rows | filtered | Extra |
|------|---------------|-----|------|----------|-------|
| **index** | idx_products_brand_created, idx_products_category_created | **idx_products_base_price** | **592** | 0.17% | Using where |

> **분석**: `type`이 `ALL` → `index`로 변경. 가격 인덱스 스캔 + 두 필터(brand_id=1 AND category_id=3) 전략.
> brand_id=1(28.5%) × category_id=3(1.79%) ≈ 0.51%이므로,
> 가격 인덱스를 순서대로 스캔하면 평균 ~200행마다 1건이 매칭된다.
> `possible_keys`에 브랜드/카테고리 인덱스도 후보지만, 가격 인덱스 스캔이 filesort 없이 정렬을 보장하므로 선택됨.
> `Using filesort` 제거.

---

### 2.11 브랜드 + 키워드 필터 (최신순)

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = 1
  AND MATCH(name) AGAINST('프리미엄' IN BOOLEAN MODE)
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

| 구분 | type | possible_keys | key | rows | filtered | Extra |
|------|------|---------------|-----|------|----------|-------|
| Before | fulltext | ft_products_name | ft_products_name | 1 | 5.00% | Using where; Ft_hints: no_ranking; Using filesort |
| After | fulltext | idx_products_brand_created, ft_products_name | ft_products_name | 1 | 5.00% | Using where; Ft_hints: no_ranking; Using filesort |

> FULLTEXT 인덱스가 우선 적용되는 쿼리. `possible_keys`에 `idx_products_brand_created`가 추가되었지만,
> 옵티마이저는 여전히 FULLTEXT 인덱스를 선택한다. 실행 계획 동일.

---

### 2.12 전체 필터 (카테고리 + 브랜드 + 키워드) + 좋아요순 정렬

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = 3 AND brand_id = 1
  AND MATCH(name) AGAINST('프리미엄' IN BOOLEAN MODE)
ORDER BY like_count DESC, created_at DESC
LIMIT 20 OFFSET 0;
```

| 구분 | type | possible_keys | key | rows | filtered | Extra |
|------|------|---------------|-----|------|----------|-------|
| Before | fulltext | ft_products_name | ft_products_name | 1 | 5.00% | Using where; Ft_hints: no_ranking; Using filesort |
| After | fulltext | idx_products_brand_created, idx_products_category_created, ft_products_name | ft_products_name | 1 | 5.00% | Using where; Ft_hints: no_ranking; Using filesort |

> FULLTEXT 인덱스가 우선 적용. `possible_keys`에 브랜드/카테고리 인덱스가 추가되었지만 실행 계획은 동일.
> FULLTEXT 매칭(~12,594건) → brand_id + category_id 필터(~477건) → filesort 순서로 처리.

---

## 3. 실행 계획 변화 요약

### 3.1 type 변화 (접근 방식)

| 시나리오 | Before type | After type | After key | filesort 제거 |
|----------|-------------|------------|-----------|---------------|
| 필터 없음 + 최신순 | ALL | **index** | idx_products_created | ✅ |
| 카테고리 + 최신순 | ALL | **ref** | idx_products_category_created | ✅ |
| 브랜드 + 최신순 | ALL | **ref** | idx_products_brand_created | ✅ |
| 키워드 + 최신순 | fulltext | fulltext | ft_products_name | ❌ (변화 없음) |
| 가격순 | ALL | **index** | idx_products_base_price | ✅ |
| 좋아요순 | ALL | **index** | idx_products_like_count | ✅ |
| 브랜드 + 가격순 | ALL | **index** | idx_products_base_price | ✅ |
| 브랜드 + 좋아요순 | ALL | **index** | idx_products_like_count | ✅ |
| 카테고리 + 좋아요순 | ALL | **index** | idx_products_like_count | ✅ |
| 브랜드 + 카테고리 + 가격순 | ALL | **index** | idx_products_base_price | ✅ |
| 브랜드 + 키워드 | fulltext | fulltext | ft_products_name | ❌ (변화 없음) |
| 전체 필터 + 좋아요순 | fulltext | fulltext | ft_products_name | ❌ (변화 없음) |

### 3.2 rows 추정 변화

| 시나리오 | Before rows | After rows | 감소율 |
|----------|-------------|------------|--------|
| 필터 없음 + 최신순 | 990,629 | **20** | **99.998%** |
| 카테고리 + 최신순 | 990,629 | **33,432** | **96.5%** |
| 브랜드 + 최신순 | 990,629 | **495,314** | 48.8% |
| 가격순 | 990,629 | **20** | **99.998%** |
| 좋아요순 | 990,629 | **20** | **99.998%** |
| 브랜드 + 가격순 | 990,629 | **40** | **99.996%** |
| 브랜드 + 좋아요순 | 990,629 | **40** | **99.996%** |
| 카테고리 + 좋아요순 | 990,629 | **592** | **99.94%** |
| 브랜드 + 카테고리 + 가격순 | 990,629 | **592** | **99.94%** |

---

## 4. 개선 효과 분석

### 4.1 Full Table Scan 제거

| 구분 | Before | After |
|------|--------|-------|
| `type=ALL` 시나리오 (데이터 조회) | 9개 | **0개** (FULLTEXT 제외 전체 개선) |

### 4.2 filesort 제거

| 구분 | Before | After |
|------|--------|-------|
| `Using filesort` 시나리오 | 12개 (전체) | **3개** (FULLTEXT 포함 쿼리만 유지) |

> FULLTEXT 인덱스를 사용하는 3개 시나리오(키워드 단독, 브랜드+키워드, 전체 필터)에서만 filesort가 유지된다.
> 이는 FULLTEXT 검색 결과를 다른 컬럼 기준으로 재정렬해야 하기 때문이다.

### 4.3 Zipf 분포에서의 인덱스 활용 패턴

옵티마이저가 **정렬 인덱스 스캔 + 필터** 전략을 선택하는 시나리오가 두드러진다.

| 쿼리 패턴 | 선택된 인덱스 | 전략 | 이유 |
|-----------|-------------|------|------|
| 브랜드+가격순 | idx_products_base_price | 가격 인덱스 스캔 + brand_id 필터 | brand_id=1(28.5%) 매칭 확률 높아 적은 스캔으로 충분 |
| 브랜드+좋아요순 | idx_products_like_count | 좋아요 인덱스 스캔 + brand_id 필터 | 위와 동일한 이유 |
| 카테고리+좋아요순 | idx_products_like_count | 좋아요 인덱스 스캔 + category_id 필터 | category_id=3(1.79%) → 약 56행마다 1건 매칭 |
| 브랜드+카테고리+가격순 | idx_products_base_price | 가격 인덱스 스캔 + 두 필터 | 조합 선택도 0.51% → ~200행마다 1건 매칭 |

> **핵심 인사이트**: Zipf 분포에서 상위 브랜드(brand_id=1)는 매칭 확률이 높아(28.5%)
> 정렬 인덱스를 순서대로 스캔하면서 필터링하는 것이 브랜드 인덱스 Lookup(495K건 + filesort)보다 효율적이다.

---

## 5. 추가 최적화 제안

### 5.1 커서 기반 페이지네이션 (Keyset Pagination)

EXPLAIN은 OFFSET에 무관한 실행 계획을 보여주지만, 실제 실행 시 Deep Pagination(OFFSET 150K+)에서는
옵티마이저가 cost 기반으로 Index Scan → Full Table Scan으로 전환할 수 있다.
OFFSET 대신 커서 기반 페이지네이션 도입으로 항상 인덱스의 시작점부터 탐색하여 일정한 성능을 보장할 수 있다.

```sql
-- 최신순 커서 페이지네이션 예시
SELECT * FROM products
WHERE deleted_at IS NULL
  AND created_at < :lastCreatedAt
ORDER BY created_at DESC
LIMIT 20;

-- 좋아요순 커서 페이지네이션 예시
SELECT * FROM products
WHERE deleted_at IS NULL
  AND (like_count, created_at) < (:lastLikeCount, :lastCreatedAt)
ORDER BY like_count DESC, created_at DESC
LIMIT 20;
```

### 5.2 deleted_at 조건 최적화

현재 모든 활성 행의 `deleted_at`이 NULL이므로 인덱스 필터링 효과가 없다.
향후 소프트 삭제 비율이 높아지면 `deleted_at IS NULL` 조건을 복합 인덱스 선두에 배치하는 것을 검토.

---

## 6. 결론

5개 복합 인덱스 추가로 **FULLTEXT 제외 모든 데이터 조회 시나리오에서 `type=ALL` → `index`/`ref`로 전환**되었다.

| 지표 | Before | After |
|------|--------|-------|
| Full Table Scan (데이터 조회) | 9/12 시나리오 | **0/12** |
| Using filesort | 12/12 시나리오 | **3/12** (FULLTEXT만) |
| rows 추정 (최악) | 990,629 | **592** (필터+정렬 조합) |

### Zipf 분포가 실행 계획에 미치는 핵심 영향

| 관점 | 균등 분포 예상 | Zipf 분포 실제 |
|------|---------------|---------------|
| 브랜드+정렬 조합 전략 | 브랜드 인덱스 Lookup + filesort | **정렬 인덱스 스캔 + 필터** |
| 옵티마이저 인덱스 선택 | 필터 인덱스 우선 | **정렬 인덱스 우선** (고비율 브랜드) |

> **핵심**: 옵티마이저가 브랜드별로 다른 실행 계획을 선택할 수 있도록 **ANALYZE TABLE**로 통계를 최신 상태로 유지하는 것이 중요하다.
> Deep Pagination(OFFSET 150K+)은 인덱스만으로는 해결할 수 없으며, **커서 기반 페이지네이션** 도입이 근본적 해결책이다.
