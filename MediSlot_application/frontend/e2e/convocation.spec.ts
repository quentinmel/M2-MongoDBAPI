import {expect, test} from '@playwright/test';

test('affiche et télécharge la convocation, et refuse un rendez-vous annulé', async ({page}) => {
  const apiResponses: Array<{status: number; contentType?: string; disposition?: string}> = [];
  page.on('response', response => {
    if (response.url().includes('/convocation.pdf')) {
      apiResponses.push({
        status: response.status(),
        contentType: response.headers()['content-type'],
        disposition: response.headers()['content-disposition'],
      });
    }
  });

  await page.goto('/');
  await page.getByRole('button', {name: 'Se connecter', exact: true}).click();
  await page.getByLabel('Adresse e-mail', {exact: true}).fill('patient@medislot.test');
  await page.getByLabel('Mot de passe', {exact: true}).fill('MediSlot-demo-2026!');
  await page.getByRole('dialog').getByRole('button', {name: 'Se connecter', exact: true}).click();
  await expect(page.getByRole('button', {name: 'Camille · Déconnexion'})).toBeVisible();
  await page.getByRole('button', {name: 'Mes rendez-vous'}).click();

  const confirmedAppointment = page.locator('article.appointment').filter({hasText: 'Confirmé'}).first();
  await expect(confirmedAppointment).toBeVisible();
  const popupPromise = page.waitForEvent('popup');
  await confirmedAppointment.getByRole('button', {name: 'Afficher la convocation'}).click();
  const popup = await popupPromise;
  expect(popup.isClosed()).toBe(false);
  await expect.poll(() => apiResponses.some(response => response.status === 200)).toBe(true);

  const downloadPromise = page.waitForEvent('download');
  await confirmedAppointment.getByRole('button', {name: 'Enregistrer le PDF'}).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toMatch(/^convocation-rdv-\d+\.pdf$/);
  await expect.poll(() => apiResponses.filter(response => response.status === 200).length).toBeGreaterThanOrEqual(2);
  expect(apiResponses[0].contentType).toContain('application/pdf');
  expect(apiResponses[0].disposition).toContain('inline');

  const cancelledAppointment = page.locator('article.appointment').filter({hasText: 'Annulé'}).first();
  await expect(cancelledAppointment).toBeVisible();
  await cancelledAppointment.getByRole('button', {name: 'Enregistrer le PDF'}).click();
  await expect(cancelledAppointment.getByRole('alert')).toContainText('annulé');
  await expect.poll(() => apiResponses.some(response => response.status === 409)).toBe(true);

  await popup.close();
});
