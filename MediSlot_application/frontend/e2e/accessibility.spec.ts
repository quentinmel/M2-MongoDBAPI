import {test,expect} from '@playwright/test';

test('navigation au clavier, landmarks et dialogue réservent le focus',async({page})=>{
 await page.goto('/');
 await page.keyboard.press('Tab');
 await expect(page.getByRole('link',{name:'Aller au contenu'})).toBeFocused();
 await page.keyboard.press('Enter');
 await expect(page.locator('main')).toBeFocused();
 await expect(page.getByRole('main')).toBeVisible();
 await expect(page.getByRole('navigation',{name:'Navigation principale'})).toBeVisible();
 await expect(page.getByRole('button',{name:'Trouver un praticien'})).toHaveAttribute('aria-current','page');
 await expect(page.getByText('6 praticiens à découvrir')).toBeVisible({timeout:15000});

 await page.getByRole('button',{name:'Se connecter',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Heureux de vous retrouver'})).toBeFocused();
 await page.getByLabel('Adresse e-mail',{exact:true}).fill('patient@medislot.test');
 await page.getByLabel('Mot de passe',{exact:true}).fill('MediSlot-demo-2026!');
 await page.getByRole('dialog').getByRole('button',{name:'Se connecter',exact:true}).click();
 await expect(page.getByRole('button',{name:'Camille · Déconnexion'})).toBeVisible();

 const slot=page.locator('button.slot:not(.slot-loading)').first();
 await slot.click();
 const dialog=page.getByRole('dialog',{name:'Confirmer votre rendez-vous'});
 await expect(dialog).toBeVisible();
 await expect(page.getByRole('heading',{name:'Confirmer votre rendez-vous'})).toBeFocused();
 await page.keyboard.press('Shift+Tab');
 await expect(dialog.getByRole('button',{name:'Confirmer le rendez-vous'})).toBeFocused();
 await page.keyboard.press('Tab');
 await expect(dialog.getByRole('button',{name:'Fermer'})).toBeFocused();
 await page.keyboard.press('Escape');
 await expect(dialog).toBeHidden();
 await expect(slot).toBeFocused();
 await page.getByRole('button',{name:'Mes rendez-vous'}).click();
 await expect(page.getByRole('button',{name:'Mes rendez-vous'})).toHaveAttribute('aria-current','page');
 await expect(page.getByRole('heading',{name:'Mes rendez-vous'})).toBeFocused();
 await page.getByRole('button',{name:'Trouver un praticien'}).click();
 await expect(page.getByRole('heading',{name:/Trouvez le bon praticien/})).toBeFocused();
});

test('les erreurs serveur sont reliées au champ concerné',async({page})=>{
 await page.goto('/');
 await page.getByRole('button',{name:'Se connecter',exact:true}).click();
 await page.getByRole('dialog').getByRole('button',{name:'Créer un compte'}).click();
 await page.getByLabel('Votre nom').fill('Patiente de test');
 await page.getByLabel('Adresse e-mail').fill('patient@example.test');
 await page.getByLabel('Mot de passe').fill('MediSlot-demo-2026!');
 await page.route('**/api/register',route=>route.request().method()==='POST'?route.fulfill({status:400,contentType:'application/problem+json',body:JSON.stringify({detail:'Vérifiez les données saisies.',errors:[{field:'email',message:'Saisissez une adresse e-mail valide.'}]})}):route.continue());
 await page.getByRole('dialog').getByRole('button',{name:'Créer mon compte'}).click();
 const email=page.getByLabel('Adresse e-mail');
 await expect(email).toHaveAttribute('aria-invalid','true');
 await expect(email).toHaveAttribute('aria-describedby','auth-email-error');
 await expect(page.getByText('Saisissez une adresse e-mail valide.')).toBeVisible();
});
