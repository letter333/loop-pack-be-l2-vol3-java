import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭 — 에러 원인별 분리
const queueEnterSuccess = new Counter('queue_enter_success');
const orderSuccess = new Counter('order_success');
const orderFail = new Counter('order_fail');
const pollTimeout = new Counter('poll_timeout');       // Polling 타임아웃
const orderApiFail = new Counter('order_api_fail');    // 주문 API 에러 (상품/재고 등)
const orderErrorRate = new Rate('order_error_rate');
const orderDuration = new Trend('order_duration', true);
const totalFlowDuration = new Trend('total_flow_duration', true);
const pollDuration = new Trend('poll_duration', true); // 토큰 대기 시간

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = 'Test1234!';
const USER_COUNT = 100;
const MAX_POLL_ATTEMPTS = 120;     // 60 → 120 (동적 Polling으로 충분한 대기)
const POLL_INTERVAL_SEC = 2;
const CONFIG_LABEL = __ENV.CONFIG_LABEL || 'default';

// 테스트 #1과 동일한 Ramp-up 시나리오
export const options = {
  scenarios: {
    ramp_up: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '30s', target: 10 },
        { duration: '30s', target: 30 },
        { duration: '30s', target: 30 },
        { duration: '30s', target: 50 },
        { duration: '30s', target: 50 },
        { duration: '30s', target: 100 },
        { duration: '30s', target: 100 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  setupTimeout: '300s',
  thresholds: {
    order_error_rate: ['rate<0.3'],
    order_duration: ['p(99)<5000'],
    total_flow_duration: ['p(99)<30000'],
  },
};

export function setup() {
  console.log(`=== Setup: 대기열 활성화 + 유저/상품 준비 [${CONFIG_LABEL}] ===`);

  // 1. Admin API로 대기열 활성화
  const activateRes = http.post(`${BASE_URL}/api/v1/admin/queue/activate`, null, {
    headers: { 'X-Loopers-Ldap': 'loopers.admin' },
  });
  console.log(`대기열 활성화: ${activateRes.status}`);

  // 2. 유저 배송지 조회
  const users = [];
  for (let i = 1; i <= USER_COUNT; i++) {
    const loginId = `k6user${i}`;
    const addrRes = http.get(`${BASE_URL}/api/v1/addresses`, {
      headers: { 'X-Loopers-LoginId': loginId, 'X-Loopers-LoginPw': PASSWORD },
    });
    if (addrRes.status === 200) {
      const body = JSON.parse(addrRes.body);
      if (body.data && body.data.length > 0) {
        users.push({ loginId, addressId: body.data[0].id });
      }
    }
  }

  // 3. SALE 상품 100개 — 상세 조회로 상태 + 재고 이중 확인
  const products = [];
  const TARGET = 100;
  let page = 0;
  while (products.length < TARGET && page < 30) {
    const res = http.get(`${BASE_URL}/api/v1/products?page=${page}&size=20`);
    if (res.status !== 200) break;
    const content = JSON.parse(res.body).data?.content || [];
    if (content.length === 0) break;
    for (const p of content) {
      if (products.length >= TARGET) break;
      const detail = http.get(`${BASE_URL}/api/v1/products/${p.id}`);
      if (detail.status === 200) {
        const d = JSON.parse(detail.body).data;
        if (d && d.status === 'SALE' && d.options?.length > 0) {
          // 재고가 충분한 옵션만 선택
          const validOption = d.options.find(opt => opt.stockQuantity > 100);
          if (validOption) {
            products.push({ productId: d.id, optionId: validOption.id });
          }
        }
      }
    }
    page++;
  }

  // 4. 상품 검증 — 실제 주문 1건 테스트
  if (users.length > 0 && products.length > 0) {
    // 대기열 비활성 상태에서 테스트 주문
    http.post(`${BASE_URL}/api/v1/admin/queue/deactivate`, null, {
      headers: { 'X-Loopers-Ldap': 'loopers.admin' },
    });

    const testOrder = http.post(`${BASE_URL}/api/v1/orders`, JSON.stringify({
      addressId: users[0].addressId,
      shippingMemo: 'setup 검증',
      items: [{ productId: products[0].productId, productOptionId: products[0].optionId, quantity: 1 }],
    }), {
      headers: {
        'X-Loopers-LoginId': users[0].loginId, 'X-Loopers-LoginPw': PASSWORD,
        'Content-Type': 'application/json',
      },
    });
    console.log(`주문 검증: ${testOrder.status} ${testOrder.status === 201 ? 'OK' : testOrder.body?.substring(0, 100)}`);

    // 다시 대기열 활성화
    http.post(`${BASE_URL}/api/v1/admin/queue/activate`, null, {
      headers: { 'X-Loopers-Ldap': 'loopers.admin' },
    });
  }

  console.log(`유저 ${users.length}명, SALE 상품 ${products.length}개 준비 완료`);
  return { users, products };
}

// 전체 플로우: 대기열 진입 → 토큰 Polling → 주문
export default function (data) {
  if (!data.users?.length || !data.products?.length) {
    sleep(1);
    return;
  }

  const flowStart = Date.now();
  // 각 VU에 고유 유저 할당 (VU 1개당 유저 1명 — 반복 시 같은 유저 재사용하되 대기열 재진입)
  const userIndex = (__VU - 1) % data.users.length;
  const user = data.users[userIndex];
  const productIndex = (__VU + __ITER) % data.products.length;
  const product = data.products[productIndex];

  const authHeaders = {
    'X-Loopers-LoginId': user.loginId,
    'X-Loopers-LoginPw': PASSWORD,
  };

  // 1. 대기열 진입
  const enterRes = http.post(`${BASE_URL}/api/v1/queue/enter`, null, {
    headers: authHeaders,
    tags: { name: 'queue_enter' },
  });

  if (enterRes.status === 201) {
    queueEnterSuccess.add(1);
  } else if (enterRes.status === 409) {
    // 이미 대기열에 있음 — 이전 iteration의 잔여 상태
    if (__ITER < 10) {
      console.log(`대기열 중복 진입: VU=${__VU}, ITER=${__ITER}, user=${user.loginId}`);
    }
  } else if (enterRes.status !== 409) {
    orderFail.add(1);
    orderErrorRate.add(1);
    if (__ITER < 3) {
      console.log(`대기열 진입 실패: VU=${__VU}, status=${enterRes.status}`);
    }
    sleep(1);
    return;
  }

  // 2. 토큰 Polling (동적 주기)
  const pollStart = Date.now();
  let token = null;
  for (let attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
    const posRes = http.get(`${BASE_URL}/api/v1/queue/position`, {
      headers: authHeaders,
      tags: { name: 'queue_position' },
    });

    if (posRes.status === 200) {
      const posData = JSON.parse(posRes.body);
      if (posData.data?.status === 'ADMITTED' && posData.data?.token) {
        token = posData.data.token;
        break;
      }
      const suggestedMs = posData.data?.suggestedPollIntervalMs || POLL_INTERVAL_SEC * 1000;
      sleep(suggestedMs / 1000);
      continue;
    }

    sleep(POLL_INTERVAL_SEC);
  }
  pollDuration.add(Date.now() - pollStart);

  if (!token) {
    pollTimeout.add(1);
    orderFail.add(1);
    orderErrorRate.add(1);
    return;
  }

  // 3. 토큰으로 주문
  const orderPayload = JSON.stringify({
    addressId: user.addressId,
    shippingMemo: '대기열 부하 테스트 주문',
    items: [{
      productId: product.productId,
      productOptionId: product.optionId,
      quantity: 1,
    }],
  });

  const orderStart = Date.now();
  const orderRes = http.post(`${BASE_URL}/api/v1/orders`, orderPayload, {
    headers: {
      ...authHeaders,
      'Content-Type': 'application/json',
      'X-Queue-Token': token,
    },
    tags: { name: 'create_order' },
  });
  orderDuration.add(Date.now() - orderStart);

  const success = check(orderRes, {
    'order status is 201': (r) => r.status === 201,
  });

  if (success) {
    orderSuccess.add(1);
    orderErrorRate.add(0);
  } else {
    orderApiFail.add(1);
    orderFail.add(1);
    orderErrorRate.add(1);
    // 에러 원인별 분류 로깅
    const errBody = orderRes.body || '';
    if (errBody.includes('판매 중인 상품')) {
      if (__ITER < 3) console.log(`비SALE 상품 에러: VU=${__VU}, product=${product.productId}`);
    } else if (errBody.includes('토큰')) {
      if (__ITER < 3) console.log(`토큰 에러: VU=${__VU}, ITER=${__ITER}, token=${token?.substring(0, 8)}, body=${errBody.substring(0, 200)}`);
    } else {
      if (__ITER < 5) console.log(`기타 에러: VU=${__VU}, status=${orderRes.status}, body=${errBody.substring(0, 200)}`);
    }
  }

  totalFlowDuration.add(Date.now() - flowStart);
  sleep(Math.random() + 0.5);
}

export function teardown(data) {
  http.post(`${BASE_URL}/api/v1/admin/queue/deactivate`, null, {
    headers: { 'X-Loopers-Ldap': 'loopers.admin' },
  });
  console.log(`=== 부하 테스트 #2 완료 [${CONFIG_LABEL}] ===`);
}
