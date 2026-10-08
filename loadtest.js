import http from 'k6/http';
import { check, sleep } from 'k6';

// Récupère le nombre de VUs depuis la ligne de commande (ex: -e VUS=100), par défaut 10
const targetVUs = __ENV.VUS ? parseInt(__ENV.VUS, 10) : 10;

export const options = {
  stages: [
    { duration: '5s', target: targetVUs },   // Montée rapide vers la cible
    { duration: '20s', target: targetVUs },  // Palier de test
    { duration: '5s', target: 0 },   // Descente
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],    // Moins de 1 % d'erreurs
    http_req_duration: ['p(95)<500'],  // 95 % des requêtes sous 500 ms
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:3000';
const params = { headers: { 'Content-Type': 'application/json' } };

export default function () {
  const body = JSON.stringify({ nom: `Produit ${__VU}-${__ITER}`, prix: 10, stock: 5 });
  const created = http.post(`${BASE}/api/produits`, body, params);
  const ok = check(created, { 'POST 201': (r) => r.status === 201 });
  if (!ok) return;

  const id = created.json('_id');

  http.get(`${BASE}/api/produits?limit=10`);
  http.get(`${BASE}/api/produits/${id}`);
  http.put(`${BASE}/api/produits/${id}`, JSON.stringify({ prix: 20 }), params);
  http.del(`${BASE}/api/produits/${id}`);

  sleep(1);
}