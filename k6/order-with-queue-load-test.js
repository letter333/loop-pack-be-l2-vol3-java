import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const queueEnterSuccess = new Counter('queue_enter_success');
const orderSuccess = new Counter('order_success');
const orderFail = new Counter('order_fail');
const orderErrorRate = new Rate('order_error_rate');
const orderDuration = new Trend('order_duration', true);
const totalFlowDuration = new Trend('total_flow_duration', true);

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = 'Test1234!';
const USER_COUNT = 100;
const MAX_POLL_ATTEMPTS = 60;
const POLL_INTERVAL_SEC = 2;

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
  thresholds: {
    order_error_rate: ['rate<0.3'],
  },
};

export function setup() {
  console.log('=== Setup: 대기열 활성화 + 유저/상품 준비 ===');

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

  // 3. SALE 상품 100개 조회
  const products = [];
  let page = 0;
  while (products.length < 100 && page < 20) {
    const res = http.get(`${BASE_URL}/api/v1/products?page=${page}&size=20`);
    if (res.status !== 200) break;
    const content = JSON.parse(res.body).data?.content || [];
    if (content.length === 0) break;
    for (const p of content) {
      if (products.length >= 100) break;
      const detail = http.get(`${BASE_URL}/api/v1/products/${p.id}`);
      if (detail.status === 200) {
        const d = JSON.parse(detail.body).data;
        if (d && d.status === 'SALE' && d.options?.length > 0) {
          products.push({ productId: d.id, optionId: d.options[0].id });
        }
      }
    }
    page++;
  }

  console.log(`유저 ${users.length}명, 상품 ${products.length}개 준비 완료`);
  return { users, products };
}

// 전체 플로우: 대기열 진입 → 토큰 Polling → 주문
export default function (data) {
  if (!data.users?.length || !data.products?.length) {
    sleep(1);
    return;
  }

  const flowStart = Date.now();
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
  } else if (enterRes.status !== 409) {
    // 409 = 이미 대기열에 있음 (정상)
    orderFail.add(1);
    orderErrorRate.add(1);
    if (__ITER < 3) {
      console.log(`대기열 진입 실패: VU=${__VU}, status=${enterRes.status}`);
    }
    sleep(1);
    return;
  }

  // 2. 토큰 Polling
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
      // 서버가 제안하는 Polling 주기 사용 (동적 조절)
      const suggestedMs = posData.data?.suggestedPollIntervalMs || POLL_INTERVAL_SEC * 1000;
      sleep(suggestedMs / 1000);
      continue;
    }

    sleep(POLL_INTERVAL_SEC);
  }

  if (!token) {
    orderFail.add(1);
    orderErrorRate.add(1);
    if (__ITER < 3) {
      console.log(`토큰 대기 타임아웃: VU=${__VU}`);
    }
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
    orderFail.add(1);
    orderErrorRate.add(1);
    if (__ITER < 3) {
      console.log(`주문 실패: VU=${__VU}, status=${orderRes.status}, body=${orderRes.body?.substring(0, 200)}`);
    }
  }

  totalFlowDuration.add(Date.now() - flowStart);
  sleep(Math.random() + 0.5);
}

export function teardown(data) {
  // 대기열 비활성화
  http.post(`${BASE_URL}/api/v1/admin/queue/deactivate`, null, {
    headers: { 'X-Loopers-Ldap': 'loopers.admin' },
  });
  console.log('=== 부하 테스트 #2 완료 (대기열 적용) ===');
}
