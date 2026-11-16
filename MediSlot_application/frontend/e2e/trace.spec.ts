import {expect, test} from '@playwright/test';

test('propage le contexte W3C du navigateur au back et exporte les traces OTLP', async ({page}) => {
  const traceparents: string[] = [];
  const traceIds: string[] = [];
  let traceExports = 0;

  page.on('request', request => {
    if (request.url().includes('/api/')) {
      const traceparent = request.headers()['traceparent'];
      if (traceparent) traceparents.push(traceparent);
    }
    if (request.url().includes('/otlp/v1/traces') && request.method() === 'POST') {
      traceExports++;
    }
  });
  page.on('response', response => {
    const traceId = response.headers()['x-trace-id'];
    if (traceId) traceIds.push(traceId);
  });

  await expect.poll(
    async () => (await page.request.get('/api/professionals')).status(),
    {timeout: 30000},
  ).toBe(200);
  await page.goto('/');
  await expect(page.getByRole('button', {name: 'Se connecter', exact: true})).toBeVisible({timeout: 15000});
  await page.getByRole('button', {name: 'Se connecter', exact: true}).click();
  await page.getByLabel('Adresse e-mail', {exact: true}).fill('patient@medislot.test');
  await page.getByLabel('Mot de passe', {exact: true}).fill('MediSlot-demo-2026!');
  await page.getByRole('dialog').getByRole('button', {name: 'Se connecter', exact: true}).click();
  await expect(page.getByRole('button', {name: 'Camille · Déconnexion'})).toBeVisible();

  await page.locator('button.slot:not(.slot-loading)').first().click();
  await page.getByRole('button', {name: 'Confirmer le rendez-vous', exact: true}).click();
  await expect(page.getByText('Rendez-vous confirmé', {exact: true})).toBeVisible();
  await expect.poll(() => traceparents.length).toBeGreaterThan(0);
  expect(traceparents[0]).toMatch(/^00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$/);
  await expect.poll(() => traceIds.length).toBeGreaterThan(0);
  expect(traceIds).toContain(traceparents[0].split('-')[1]);

  await expect.poll(() => traceExports, {timeout: 15000}).toBeGreaterThan(0);
  await page.getByRole('button', {name: 'Annuler', exact: true}).first().click();
  await page.getByRole('button', {name: 'Confirmer l’annulation'}).click();
  await expect(page.getByText('Votre rendez-vous a été annulé.', {exact: true})).toBeVisible();

  const errorTraceId = traceparents[0].split('-')[1];
  await page.route('**/api/appointments/*/history', route => route.fulfill({
    status: 500,
    contentType: 'application/problem+json',
    headers: {'X-Trace-Id': errorTraceId},
    body: JSON.stringify({detail: 'Échec simulé de l’historique', status: 500}),
  }));
  await page.getByRole('button', {name: 'Historique', exact: true}).first().click();
  await expect(page.getByRole('alert')).toContainText(`Référence de trace : ${errorTraceId}`);
});
