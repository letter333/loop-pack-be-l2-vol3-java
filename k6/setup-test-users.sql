-- K6 부하 테스트용 회원 + 배송지 데이터 생성
-- 사전 조건: seedProductJob 배치로 상품 데이터 생성 완료
-- 실행: mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < k6/setup-test-users.sql

-- 비밀번호: Test1234! (BCrypt 해시)
SET @bcrypt_password = '$2a$10$euwhMFwNggPRy2BYBFmIhu9wY/ABQh7ySydiFn43/dIEc.xhCH/fW';

-- 기존 테스트 유저 정리
DELETE FROM member_addresses WHERE member_id IN (SELECT id FROM members WHERE login_id LIKE 'k6user%');
DELETE FROM members WHERE login_id LIKE 'k6user%';

-- 회원 100명 생성
INSERT INTO members (login_id, password, name, birthday, email, created_at, updated_at)
SELECT
    CONCAT('k6user', seq.n),
    @bcrypt_password,
    CONCAT('K6유저', seq.n),
    '1990-01-01',
    CONCAT('k6user', seq.n, '@test.com'),
    NOW(),
    NOW()
FROM (
    SELECT a.N + b.N * 10 + 1 AS n
    FROM (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
          UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a
    CROSS JOIN (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b
) seq;

-- 각 회원에 배송지 1개씩 등록
INSERT INTO member_addresses (member_id, recipient_name, phone, zip_code, address, address_detail, is_default, created_at, updated_at)
SELECT
    m.id,
    CONCAT('수신자', m.login_id),
    '010-1234-5678',
    '06234',
    '서울시 강남구 테헤란로 123',
    CONCAT(m.id, '층 ', m.id, '01호'),
    true,
    NOW(),
    NOW()
FROM members m
WHERE m.login_id LIKE 'k6user%';
