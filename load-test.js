import http from 'k6/http';
import { check } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
    vus: 200,        // 200 → 100으로 변경
    duration: '30s',
};

export default function () {
    const fromId = __VU * 2 + 1;
    const toId = fromId + 1;

    const payload = JSON.stringify({
        fromAccountId: fromId,
        toAccountId: toId,
        amount: 1,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Idempotency-Key': uuidv4(),
        },
    };

    const res = http.post('http://16.176.32.85:8080/transfers', payload, params);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    if (res.status !== 200) {
        console.log(`failed: ${res.status} - ${res.body}`);
    }
}