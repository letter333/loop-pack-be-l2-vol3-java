# 상품 목록 조회 쿼리 실행 계획 분석 (인덱스 적용 전)

## 1. 테스트 환경

| 항목 | 값 |
|------|------|
| DBMS | MySQL 8.0 (Docker) |
| 테이블 | `products` |
| 총 행 수 | 1,000,000 건 |
| 활성 행 수 (deleted_at IS NULL) | 1,000,000 건 |
| 시드 데이터 | `SeedProductTasklet` 으로 적재 (Zipf 분포, alpha=1.2) |

### 브랜드 분포 (Zipf)

| 순위 | 브랜드 (brand_id) | 상품 수 | 비율 |
|------|-------------------|---------|------|
| 1 | 나이키 (1) | 285,222 | 28.5% |
| 2 | 아디다스 (2) | 123,719 | 12.4% |
| 3 | 뉴발란스 (3) | 75,867 | 7.6% |
| 10 | 샤오미 (10) | 17,993 | 1.8% |
| 20 | 시디즈 (20) | 7,635 | 0.8% |
| 80 | 뱅앤올룹슨 (80) | 1,403 | 0.14% |

> Zipf 분포에 의해 brand_id=1(나이키)이 전체의 28.5%를 차지하고, brand_id=80(뱅앤올룹슨)은 0.14%만 차지하는 극단적 편향이 존재한다.
> 균등 분포(12,500건/브랜드) 대비 상위 브랜드의 선택도가 크게 달라졌다.

### 기존 인덱스

| 인덱스 | 타입 | 컬럼 |
|--------|------|------|
| `PRIMARY` | BTREE | `id` |
| `uk_products_product_code` | BTREE (UNIQUE) | `product_code` |
| `ft_products_name` | FULLTEXT (ngram) | `name` |

### 인덱스가 없는 컬럼

`category_id`, `brand_id`, `base_price`, `like_count`, `created_at`, `deleted_at`

### 바인드 파라미터 기준값

| 파라미터 | 값 | 매칭 건수 |
|----------|------|-----------|
| `categoryId` | 3 | ~17,875건 (56개 카테고리 중 1개) |
| `brandId` | 1 | ~285,222건 (80개 브랜드 중 1개, Zipf 최상위) |
| `keyword` | `프리미엄` (BOOLEAN MODE, ngram) | ~12,594건 |
| `limit` | 20 | - |
| `offset` | 0 | - |

---

## 2. 시나리오별 EXPLAIN 결과

> **참고**: EXPLAIN은 쿼리를 실행하지 않고 옵티마이저의 실행 계획만 표시한다.
> `rows`는 옵티마이저의 추정값이며, 실제 스캔 행 수와 다를 수 있다.
> OFFSET 값에 관계없이 동일한 실행 계획을 반환하므로, 대표 OFFSET 0 으로 분석한다.

### 2.1 필터 없음 + 최신순 (기본 쿼리)

```sql
SELECT * FROM products WHERE deleted_at IS NULL
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

| id | type | possible_keys | key | rows | filtered | Extra |
|----|------|---------------|-----|------|----------|-------|
| 1 | ALL | NULL | NULL | 990,629 | 10.00% | Using where; Using filesort |

> **분석**: `type=ALL` — Full Table Scan. `created_at`에 인덱스가 없어 990,629건 전체를 스캔하고 filesort로 정렬한다.
> `filtered=10.00%`는 `deleted_at IS NULL` 조건의 추정 통과율이다.

---

### 2.2 카테고리 필터 단독 (최신순)

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = 3
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

| id | type | possible_keys | key | rows | filtered | Extra |
|----|------|---------------|-----|------|----------|-------|
| 1 | ALL | NULL | NULL | 990,629 | 1.00% | Using where; Using filesort |

> **분석**: `type=ALL` — Full Table Scan. `category_id`에 인덱스가 없어 100만 건을 스캔하여 약 17,875건(1.79%)만 필터링한다.
> `filtered=1.00%`로 선택도가 높음에도 인덱스가 없어 Full Table Scan을 수행한다.

---

### 2.3 브랜드 필터 단독 (최신순)

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = 1
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

| id | type | possible_keys | key | rows | filtered | Extra |
|----|------|---------------|-----|------|----------|-------|
| 1 | ALL | NULL | NULL | 990,629 | 1.00% | Using where; Using filesort |

> **분석**: `type=ALL` — Full Table Scan. `brand_id`에 인덱스가 없어 100만 건 전체를 스캔한다.
> Zipf 분포로 brand_id=1의 실제 매칭 건수가 285,222건(28.5%)이지만, 옵티마이저는 `filtered=1.00%`로 추정한다.

---

### 2.4 키워드 검색 단독 (최신순)

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND MATCH(name) AGAINST('프리미엄' IN BOOLEAN MODE)
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

| id | type | possible_keys | key | rows | filtered | Extra |
|----|------|---------------|-----|------|----------|-------|
| 1 | fulltext | ft_products_name | ft_products_name | 1 | 10.00% | Using where; Ft_hints: no_ranking; Using filesort |

> **분석**: `type=fulltext` — FULLTEXT 인덱스를 사용하는 유일한 시나리오.
> `rows=1`은 FULLTEXT 검색의 추정값이며, 실제로는 약 12,594건을 매칭한다.
> `Using filesort`가 발생하는데, 이는 `created_at` 정렬을 위한 추가 정렬이다.
> Full Table Scan 시나리오 대비 훨씬 적은 행을 스캔하므로 성능이 양호하다.

---

### 2.5 가격순 정렬 단독 (필터 없음)

```sql
SELECT * FROM products WHERE deleted_at IS NULL
ORDER BY base_price ASC
LIMIT 20 OFFSET 0;
```

| id | type | possible_keys | key | rows | filtered | Extra |
|----|------|---------------|-----|------|----------|-------|
| 1 | ALL | NULL | NULL | 990,629 | 10.00% | Using where; Using filesort |

> **분석**: `type=ALL` — Full Table Scan. `base_price`에 인덱스가 없어 100만 건을 스캔하고 filesort를 수행한다.

---

### 2.6 좋아요순 정렬 단독 (필터 없음)

```sql
SELECT * FROM products WHERE deleted_at IS NULL
ORDER BY like_count DESC, created_at DESC
LIMIT 20 OFFSET 0;
```

| id | type | possible_keys | key | rows | filtered | Extra |
|----|------|---------------|-----|------|----------|-------|
| 1 | ALL | NULL | NULL | 990,629 | 10.00% | Using where; Using filesort |

> **분석**: `type=ALL` — Full Table Scan. `like_count`, `created_at` 복합 정렬에 대한 인덱스가 없어
> 100만 건 전체를 스캔하고 filesort를 수행한다.

---

### 2.7 브랜드 필터 + 가격순 정렬

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = 1
ORDER BY base_price ASC
LIMIT 20 OFFSET 0;
```

| id | type | possible_keys | key | rows | filtered | Extra |
|----|------|---------------|-----|------|----------|-------|
| 1 | ALL | NULL | NULL | 990,629 | 1.00% | Using where; Using filesort |

> **분석**: `type=ALL` — Full Table Scan. `brand_id`, `base_price` 모두 인덱스가 없다.
> 100만 건을 스캔하여 brand_id=1(285,222건)을 필터링한 뒤 filesort로 가격순 정렬한다.

---

### 2.8 브랜드 필터 + 좋아요순 정렬

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = 1
ORDER BY like_count DESC, created_at DESC
LIMIT 20 OFFSET 0;
```

| id | type | possible_keys | key | rows | filtered | Extra |
|----|------|---------------|-----|------|----------|-------|
| 1 | ALL | NULL | NULL | 990,629 | 1.00% | Using where; Using filesort |

> **분석**: `type=ALL` — Full Table Scan + filesort. 2.7과 동일한 패턴이다.

---

### 2.9 카테고리 필터 + 좋아요순 정렬

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = 3
ORDER BY like_count DESC, created_at DESC
LIMIT 20 OFFSET 0;
```

| id | type | possible_keys | key | rows | filtered | Extra |
|----|------|---------------|-----|------|----------|-------|
| 1 | ALL | NULL | NULL | 990,629 | 1.00% | Using where; Using filesort |

> **분석**: `type=ALL` — Full Table Scan. `category_id`에 인덱스가 없어 100만 건을 스캔하여
> 17,875건을 필터링한 뒤 filesort로 좋아요순 정렬한다.

---

### 2.10 브랜드 + 카테고리 필터 + 가격순 정렬

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = 3 AND brand_id = 1
ORDER BY base_price ASC
LIMIT 20 OFFSET 0;
```

| id | type | possible_keys | key | rows | filtered | Extra |
|----|------|---------------|-----|------|----------|-------|
| 1 | ALL | NULL | NULL | 990,629 | 0.10% | Using where; Using filesort |

> **분석**: `type=ALL` — Full Table Scan. 두 필터 조합으로 `filtered=0.10%`(추정 약 967건)이지만,
> 인덱스가 없어 100만 건 전체를 스캔한다.
> 실제 매칭 건수는 약 5,061건(0.51%)으로, 인덱스가 있으면 극적인 개선이 기대된다.

---

### 2.11 브랜드 + 키워드 필터 (최신순)

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND brand_id = 1
  AND MATCH(name) AGAINST('프리미엄' IN BOOLEAN MODE)
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

| id | type | possible_keys | key | rows | filtered | Extra |
|----|------|---------------|-----|------|----------|-------|
| 1 | fulltext | ft_products_name | ft_products_name | 1 | 5.00% | Using where; Ft_hints: no_ranking; Using filesort |

> **분석**: `type=fulltext` — FULLTEXT 인덱스 사용. `brand_id`에 인덱스가 없으므로
> FULLTEXT 검색(~12,594건) 후 brand_id=1 조건을 행 단위로 필터링한다.
> `filtered=5.00%`로, FULLTEXT 매칭 결과 중 약 5%가 brand_id 조건을 통과할 것으로 추정한다.

---

### 2.12 전체 필터 (카테고리 + 브랜드 + 키워드) + 좋아요순 정렬

```sql
SELECT * FROM products WHERE deleted_at IS NULL
  AND category_id = 3 AND brand_id = 1
  AND MATCH(name) AGAINST('프리미엄' IN BOOLEAN MODE)
ORDER BY like_count DESC, created_at DESC
LIMIT 20 OFFSET 0;
```

| id | type | possible_keys | key | rows | filtered | Extra |
|----|------|---------------|-----|------|----------|-------|
| 1 | fulltext | ft_products_name | ft_products_name | 1 | 5.00% | Using where; Ft_hints: no_ranking; Using filesort |

> **분석**: `type=fulltext` — FULLTEXT 인덱스 사용. 12,594건의 FULLTEXT 매칭 결과에서
> `brand_id = 1 AND category_id = 3` 필터링 후 약 477건으로 축소된 뒤 filesort 수행.
> FULLTEXT 인덱스가 검색 범위를 크게 줄여주므로, Full Table Scan 시나리오 대비 양호하다.

---

## 3. 문제점 분석

### 3.1 핵심 병목: Full Table Scan

FULLTEXT 인덱스를 사용하는 키워드 검색을 제외한 **모든 시나리오에서 `type=ALL` (Full Table Scan)**이 발생한다.

| type | 시나리오 수 | 특징 |
|------|-------------|------|
| `ALL` | 9개 (시나리오 1~3, 5~10) | Full Table Scan + filesort, rows 추정 990,629 |
| `fulltext` | 3개 (시나리오 4, 11, 12) | FULLTEXT Index 사용, rows 추정 1 |

`category_id`, `brand_id`, `base_price`, `like_count`, `created_at`, `deleted_at` 컬럼에 인덱스가 없어 옵티마이저가 선택할 수 있는 접근 경로가 없다.

### 3.2 filesort 발생

Full Table Scan 시나리오에서 모든 정렬이 `Using filesort`로 처리된다.

| 정렬 기준 | filesort 대상 |
|-----------|---------------|
| `created_at DESC` | 990,629건 (전체) 또는 필터 후 잔여 행 |
| `base_price ASC` | 990,629건 |
| `like_count DESC, created_at DESC` | 990,629건 |

인덱스 기반 정렬이 불가능하므로, 필터링 후에도 정렬 비용이 추가로 발생한다.

### 3.3 Zipf 분포에 의한 브랜드 필터 선택도 변화

균등 분포 대비 Zipf 분포에서 brand_id=1의 선택도가 크게 달라졌다.

| 분포 | brand_id=1 매칭 건수 | 선택도 | 비고 |
|------|---------------------|--------|------|
| 균등 분포 | 12,523건 | 1.25% | 인덱스 효과 극대화 |
| **Zipf 분포** | **285,222건** | **28.5%** | 인덱스 활용 제한적 |

> brand_id=1의 선택도가 28.5%로 매우 낮아졌다. 일반적으로 옵티마이저는 선택도가 20~30% 이상이면
> 인덱스 사용보다 Full Table Scan이 효율적이라고 판단한다.
> 반면, brand_id=80(0.14%, 1,403건)과 같은 하위 브랜드는 인덱스 효과가 여전히 클 것으로 기대된다.

### 3.4 높은 선택도에서의 비효율

| 시나리오 | 매칭 건수 | 선택도 | type | rows (추정) |
|----------|-----------|--------|------|-------------|
| 브랜드 + 카테고리 | 5,061건 | 0.51% | ALL | 990,629 |
| 카테고리 단독 | 17,875건 | 1.79% | ALL | 990,629 |
| 브랜드 단독 | 285,222건 | 28.5% | ALL | 990,629 |

선택도가 매우 높은(매칭 비율이 낮은) 쿼리도 인덱스 없이 동일하게 `type=ALL`로 100만 건을 스캔한다.
특히 브랜드+카테고리 조합(0.51%)은 인덱스가 있으면 극적인 개선이 기대된다.

---

## 4. 인덱스 필요성 도출

### 4.1 필수 인덱스 후보

| 우선순위 | 인덱스 대상 | 근거 |
|----------|-------------|------|
| 1 | `deleted_at` (or 복합 인덱스 선두) | 모든 쿼리의 공통 조건. 소프트 삭제 필터가 Full Table Scan을 유발 |
| 2 | `brand_id` | 단독/조합 필터 모두에서 사용. Zipf 하위 브랜드(선택도 0.14~1.8%)에서 효과적 |
| 3 | `category_id` | 단독/조합 필터 모두에서 사용. 선택도 1.79% |
| 4 | `created_at` | 기본 정렬 컬럼. filesort 제거 가능 |
| 5 | `base_price` | 가격순 정렬에 사용. filesort 제거 가능 |
| 6 | `like_count` + `created_at` | 좋아요순 복합 정렬. filesort 제거 |

### 4.2 복합 인덱스 설계 시 Zipf 분포 고려사항

단일 컬럼 인덱스보다는 **쿼리 패턴에 맞는 복합 인덱스**가 효과적이다:

1. **필터 → 정렬 → OFFSET** 순서를 고려한 인덱스 설계
2. `deleted_at IS NULL` 조건을 활용한 부분 인덱스(Partial Index) 또는 복합 인덱스 선두 배치

Zipf 분포 특유의 고려사항:
- **상위 브랜드(brand_id=1, 28.5%)**: `brand_id` 인덱스로 필터링하면 285K건을 읽어야 하므로, 정렬 인덱스 활용이 더 효율적일 수 있다
- **하위 브랜드(brand_id=80, 0.14%)**: `brand_id` 인덱스 효과가 극적 (1,403건만 읽으면 됨)
- 옵티마이저가 브랜드별로 다른 실행 계획을 선택할 수 있도록 **통계 정보 최신 상태 유지** 필요

### 4.3 기대 효과

| 개선 대상 | 현재 (EXPLAIN) | 기대 |
|-----------|----------------|------|
| `type=ALL` 제거 | 9개 시나리오 Full Table Scan | `index`, `ref`, `range` 접근 |
| `Using filesort` 제거 | 모든 정렬 쿼리에서 filesort | 인덱스 순서 활용 |
