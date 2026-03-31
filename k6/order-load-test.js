import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭
const orderSuccess = new Counter('order_success');
const orderFail = new Counter('order_fail');
const orderErrorRate = new Rate('order_error_rate');
const orderDuration = new Trend('order_duration', true);

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = 'Test1234!';
const USER_COUNT = 100;

// Ramp-up 테스트 시나리오
export const options = {
  scenarios: {
    ramp_up: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },   // 10 VU 워밍업
        { duration: '30s', target: 10 },   // 10 VU 유지
        { duration: '30s', target: 30 },   // 30 VU 증가
        { duration: '30s', target: 30 },   // 30 VU 유지
        { duration: '30s', target: 50 },   // 50 VU 증가
        { duration: '30s', target: 50 },   // 50 VU 유지
        { duration: '30s', target: 100 },  // 100 VU 증가
        { duration: '30s', target: 100 },  // 100 VU 유지
        { duration: '30s', target: 0 },    // 감소
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<5000'],  // P95 < 5초 (관찰용)
    order_error_rate: ['rate<0.5'],     // 에러율 50% 미만 (관찰용)
  },
};

// Setup: 유저 배송지 매핑 조회
export function setup() {
  console.log('=== Setup: 유저별 배송지 ID 조회 ===');

  const users = [];
  for (let i = 1; i <= USER_COUNT; i++) {
    const loginId = `k6user${i}`;

    // 배송지 목록 조회
    const addrRes = http.get(`${BASE_URL}/api/v1/addresses`, {
      headers: {
        'X-Loopers-LoginId': loginId,
        'X-Loopers-LoginPw': PASSWORD,
      },
    });

    if (addrRes.status === 200) {
      const body = JSON.parse(addrRes.body);
      if (body.data && body.data.length > 0) {
        users.push({
          loginId: loginId,
          addressId: body.data[0].id,
        });
      }
    }
  }

  // SALE 상태 상품 여러 개 조회 (VU별로 분산시키기 위해)
  const products = [];
  const TARGET_PRODUCT_COUNT = 100;
  let page = 0;
  while (products.length < TARGET_PRODUCT_COUNT && page < 20) {
    const productRes = http.get(`${BASE_URL}/api/v1/products?page=${page}&size=20`, {
      headers: { 'Accept': 'application/json' },
    });

    if (productRes.status !== 200) break;

    const body = JSON.parse(productRes.body);
    const content = body.data && body.data.content ? body.data.content : [];
    if (content.length === 0) break;

    for (const product of content) {
      if (products.length >= TARGET_PRODUCT_COUNT) break;

      // 상품 상세 조회로 상태와 옵션 확인
      const detailRes = http.get(`${BASE_URL}/api/v1/products/${product.id}`, {
        headers: { 'Accept': 'application/json' },
      });
      if (detailRes.status === 200) {
        const detail = JSON.parse(detailRes.body);
        // SALE 상태이고 옵션이 있는 상품만 추가
        if (detail.data && detail.data.status === 'SALE'
            && detail.data.options && detail.data.options.length > 0) {
          products.push({
            productId: detail.data.id,
            optionId: detail.data.options[0].id,
          });
        }
      }
    }
    page++;
  }

  console.log(`유저 ${users.length}명, 상품 ${products.length}개 준비 완료`);
  return { users, products };
}

// 각 VU의 주문 생성 반복
export default function (data) {
  if (!data.users || data.users.length === 0 || !data.products || data.products.length === 0) {
    console.log('테스트 데이터가 없습니다. seedProductJob + SQL 먼저 실행하세요.');
    sleep(1);
    return;
  }

  // VU별로 다른 유저 순환 사용
  const userIndex = (__VU - 1) % data.users.length;
  const user = data.users[userIndex];

  // 반복(iteration)마다 다른 상품 사용 → 락 분산
  const productIndex = (__VU + __ITER) % data.products.length;
  const product = data.products[productIndex];

  const payload = JSON.stringify({
    addressId: user.addressId,
    shippingMemo: '부하 테스트 주문',
    items: [
      {
        productId: product.productId,
        productOptionId: product.optionId,
        quantity: 1,
      },
    ],
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Loopers-LoginId': user.loginId,
      'X-Loopers-LoginPw': PASSWORD,
    },
    tags: { name: 'create_order' },
  };

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}/api/v1/orders`, payload, params);
  const duration = Date.now() - startTime;

  orderDuration.add(duration);

  const success = check(res, {
    'status is 201': (r) => r.status === 201,
  });

  if (success) {
    orderSuccess.add(1);
    orderErrorRate.add(0);
  } else {
    orderFail.add(1);
    orderErrorRate.add(1);
    if (__ITER < 3) {
      console.log(`주문 실패: VU=${__VU}, status=${res.status}, body=${res.body?.substring(0, 200)}`);
    }
  }

  // 유저 행동 시뮬레이션 (0.5~1.5초 대기)
  sleep(Math.random() + 0.5);
}

export function teardown(data) {
  console.log('=== 부하 테스트 완료 ===');
  console.log('결과를 분석하여 배치 크기를 산정하세요.');
  console.log('배치 크기 = 안정 TPS × 스케줄러 주기(초) × 안전 계수(0.6~0.7)');
}
