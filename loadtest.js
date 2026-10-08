import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 10 }, // montée à 10 utilisateurs virtuels
    { duration: '30s', target: 10 }, // palier
    { duration: '10s', target: 0 },  // descente
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],    // moins de 1 % d'erreurs
    http_req_duration: ['p(95)<500'],  // 95 % des requêtes sous 500 ms
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:3000';
const params = { headers: { 'Content-Type': 'application/json' } };

export default function () {
  // CREATE
  const body = JSON.stringify({ nom: `Produit ${__VU}-${__ITER}`, prix: 10, stock: 5 });
  const created = http.post(`${BASE}/api/produits`, body, params);
  const ok = check(created, { 'POST 201': (r) => r.status === 201 });
  if (!ok) return;

  const id = created.json('_id');

  // READ
  const list = http.get(`${BASE}/api/produits?limit=10`);
  check(list, { 'GET liste 200': (r) => r.status === 200 });
  const one = http.get(`${BASE}/api/produits/${id}`);
  check(one, { 'GET un 200': (r) => r.status === 200 });

  // UPDATE
  const upd = http.put(`${BASE}/api/produits/${id}`, JSON.stringify({ prix: 20 }), params);
  check(upd, { 'PUT 200': (r) => r.status === 200 });

  // DELETE (nettoie la base)
  const del = http.del(`${BASE}/api/produits/${id}`);
  check(del, { 'DELETE 200': (r) => r.status === 200 });

  sleep(1);
}