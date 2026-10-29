import {test,expect} from '@playwright/test';
test('une personne réserve puis annule son rendez-vous',async({page})=>{
 await page.goto('/');await expect(page.getByText('6 praticiens à découvrir')).toBeVisible({timeout:15000});
 await page.getByRole('button',{name:'Se connecter',exact:true}).click();
 await page.getByLabel('Adresse e-mail',{exact:true}).fill('patient@medislot.test');await page.getByLabel('Mot de passe',{exact:true}).fill('MediSlot-demo-2026!');
 await page.getByRole('dialog').getByRole('button',{name:'Se connecter',exact:true}).click();await expect(page.getByRole('button',{name:'Camille · Déconnexion'})).toBeVisible();
 await page.locator('button.slot:not(.slot-loading)').first().click();
 await page.waitForTimeout(650);
 const savedDraft=await page.evaluate(()=>new Promise<Record<string,unknown>|undefined>((resolve,reject)=>{
  const request=indexedDB.open('medislot-offline');
  request.onerror=()=>reject(request.error);
  request.onsuccess=()=>{const get=request.result.transaction('drafts').objectStore('drafts').getAll();get.onsuccess=()=>resolve(get.result[0]);get.onerror=()=>reject(get.error)};
 }));
 expect(savedDraft).toMatchObject({key:/^booking:\d+$/,status:'draft',slotId:expect.any(Number)});
 expect(savedDraft).not.toHaveProperty('token');
 expect(savedDraft).not.toHaveProperty('password');
 await page.getByRole('button',{name:'Confirmer le rendez-vous',exact:true}).click();
 await expect(page.getByText('Rendez-vous confirmé',{exact:true})).toBeVisible();
 await page.getByRole('button',{name:'Annuler',exact:true}).first().click();await page.getByRole('button',{name:'Confirmer l’annulation'}).click();
 await expect(page.getByText('Votre rendez-vous a été annulé.',{exact:true})).toBeVisible();
});

test('une réservation hors ligne est mise en cache puis confirmée au retour du réseau',async({page,context})=>{
 await page.goto('/');
 await page.getByRole('button',{name:'Se connecter',exact:true}).click();
 await page.getByLabel('Adresse e-mail',{exact:true}).fill('patient@medislot.test');
 await page.getByLabel('Mot de passe',{exact:true}).fill('MediSlot-demo-2026!');
 await page.getByRole('dialog').getByRole('button',{name:'Se connecter',exact:true}).click();
 await expect(page.getByRole('button',{name:'Camille · Déconnexion'})).toBeVisible();

 let offlineBookingRequests=0,requestSlotId:number|undefined;
 page.on('request',request=>{if(request.url().endsWith('/api/appointments')&&request.method()==='POST')offlineBookingRequests++});
 await context.setOffline(true);
 await page.locator('button.slot:not(.slot-loading)').first().click();
 await page.getByRole('button',{name:'Confirmer le rendez-vous',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Vous êtes hors ligne'})).toBeVisible();
 await expect(page.getByRole('status').getByText('Hors ligne, vos réponses sont gardées')).toBeVisible();
 await expect(page.getByText("En attente d'envoi : le créneau n'est pas encore réservé.")).toBeVisible();
 expect(offlineBookingRequests).toBe(0);
 const cachedDraft=await page.evaluate(()=>new Promise<{key:string;status:string}|undefined>((resolve,reject)=>{
  const request=indexedDB.open('medislot-offline');
  request.onerror=()=>reject(request.error);
  request.onsuccess=()=>{const db=request.result;const get=db.transaction('drafts').objectStore('drafts').getAll();get.onsuccess=()=>{const value=get.result[0];resolve(value&&{key:value.key,status:value.status})};get.onerror=()=>reject(get.error)};
 }));
 expect(cachedDraft).toMatchObject({key:/^booking:\d+$/,status:'pending'});
 const cachedOutbox=await page.evaluate(()=>new Promise<{idemKey:string;slotId:number}|undefined>((resolve,reject)=>{
  const request=indexedDB.open('medislot-offline');
  request.onerror=()=>reject(request.error);
  request.onsuccess=()=>{const get=request.result.transaction('outbox').objectStore('outbox').getAll();get.onsuccess=()=>resolve(get.result[0]);get.onerror=()=>reject(get.error)};
 }));
 expect(cachedOutbox).toMatchObject({idemKey:/^[0-9a-f-]{36}$/,slotId:expect.any(Number)});
 const cachedDraftKey=cachedDraft?.key;

 let idempotencyKey:string|undefined,bookingStatus:number|undefined,appointmentId:number|undefined;
 page.on('request',request=>{if(request.url().endsWith('/api/appointments')&&request.method()==='POST'){idempotencyKey=request.headers()['idempotency-key'];requestSlotId=request.postDataJSON().slotId}});
 page.on('response',async response=>{if(response.url().endsWith('/api/appointments')&&response.request().method()==='POST'){bookingStatus=response.status();appointmentId=(await response.json()).id}});
 await context.setOffline(false);
 await expect(page.getByRole('heading',{name:'Rendez-vous confirmé'})).toBeVisible({timeout:15000});
 await expect(page.getByText(/Le serveur a confirmé la réservation/)).toBeVisible();
 expect(idempotencyKey).toMatch(/^[0-9a-f-]{36}$/);
 expect(idempotencyKey).toBe(cachedOutbox?.idemKey);
 expect(bookingStatus).toBe(201);
 await expect.poll(()=>appointmentId).toBeGreaterThan(0);
 if(!idempotencyKey||requestSlotId===undefined)throw new Error('The offline request did not retain its idempotency key and slot.');
 const replay=await page.evaluate(async({key,slotId})=>{
  const csrf=await fetch('/api/csrf').then(response=>response.json());
  const response=await fetch('/api/appointments',{method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token,'Idempotency-Key':key},body:JSON.stringify({slotId})});
  return {status:response.status,appointment:await response.json()};
 },{key:idempotencyKey,slotId:requestSlotId});
 expect(replay.status).toBe(201);
 expect(replay.appointment.id).toBe(appointmentId);
 await expect.poll(()=>page.evaluate(key=>new Promise<boolean>(resolve=>{const open=indexedDB.open('medislot-offline');open.onsuccess=()=>{const req=open.result.transaction('drafts').objectStore('drafts').get(key);req.onsuccess=()=>resolve(req.result===undefined);req.onerror=()=>resolve(false)};open.onerror=()=>resolve(false)}),cachedDraftKey)).toBe(true);
 await page.getByRole('button',{name:'Fermer'}).last().click();
 await page.getByRole('button',{name:'Annuler',exact:true}).first().click();
 await page.getByRole('button',{name:'Confirmer l’annulation'}).click();
 await expect(page.getByText('Votre rendez-vous a été annulé.',{exact:true})).toBeVisible();
});

test('un conflit conserve le brouillon rejected et propose les autres créneaux',async({page,context,browser})=>{
 await page.goto('/');
 await page.getByRole('button',{name:'Se connecter',exact:true}).click();
 await page.getByLabel('Adresse e-mail',{exact:true}).fill('patient@medislot.test');
 await page.getByLabel('Mot de passe',{exact:true}).fill('MediSlot-demo-2026!');
 await page.getByRole('dialog').getByRole('button',{name:'Se connecter',exact:true}).click();
 await expect(page.getByRole('button',{name:'Camille · Déconnexion'})).toBeVisible();

 await context.setOffline(true);
 await page.locator('button.slot:not(.slot-loading)').first().click();
 await page.getByRole('button',{name:'Confirmer le rendez-vous',exact:true}).click();
 await expect(page.getByText("En attente d'envoi : le créneau n'est pas encore réservé.")).toBeVisible();
 let conflictStatus:number|undefined,conflictCode:string|undefined;
 page.on('response',async response=>{if(response.url().endsWith('/api/appointments')&&response.request().method()==='POST'&&response.status()===409){conflictStatus=response.status();conflictCode=(await response.json()).code}});
 const queued=await page.evaluate(()=>new Promise<{slotId:number}|undefined>(resolve=>{const open=indexedDB.open('medislot-offline');open.onsuccess=()=>{const req=open.result.transaction('outbox').objectStore('outbox').getAll();req.onsuccess=()=>resolve(req.result[0])}}));
 if(!queued)throw new Error('The offline reservation was not stored in the outbox.');
 const rivalContext=await browser.newContext();
 const rivalPage=await rivalContext.newPage();
 let rivalAppointmentId:number|undefined;
 try{
  await rivalPage.goto('/');
  await rivalPage.getByRole('button',{name:'Se connecter',exact:true}).click();
  await rivalPage.getByLabel('Adresse e-mail',{exact:true}).fill('autre@medislot.test');
  await rivalPage.getByLabel('Mot de passe',{exact:true}).fill('MediSlot-demo-2026!');
  await rivalPage.getByRole('dialog').getByRole('button',{name:'Se connecter',exact:true}).click();
  await expect(rivalPage.getByRole('button',{name:/Déconnexion/})).toBeVisible();
  const rivalBooking=await rivalPage.evaluate(async slotId=>{
   const csrf=await fetch('/api/csrf').then(response=>response.json());
   const response=await fetch('/api/appointments',{method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify({slotId})});
   return {status:response.status,appointment:await response.json()};
  },queued.slotId);
  expect(rivalBooking.status).toBe(201);
  rivalAppointmentId=rivalBooking.appointment.id;
  await context.setOffline(false);
  await expect(page.getByRole('heading',{name:'Le créneau n’est plus disponible'})).toBeVisible({timeout:15000});
  await expect.poll(()=>conflictStatus).toBe(409);
  await expect.poll(()=>conflictCode).toBe('SLOT_TAKEN');
  await expect(page.getByText('Votre brouillon est conservé')).toBeVisible();
  const rejected=await page.evaluate(()=>new Promise<string|undefined>(resolve=>{const open=indexedDB.open('medislot-offline');open.onsuccess=()=>{const req=open.result.transaction('drafts').objectStore('drafts').getAll();req.onsuccess=()=>resolve(req.result[0]?.status)}}));
  expect(rejected).toBe('rejected');
  const otherSlots=page.locator('.alternative-slots .slot');
  await expect(otherSlots.first()).toBeVisible();
  await otherSlots.first().click();
  await expect(page.getByRole('heading',{name:'Confirmer votre rendez-vous'})).toBeVisible();
 }finally{
  if(rivalAppointmentId!==undefined)await rivalPage.evaluate(async appointmentId=>{
   const csrf=await fetch('/api/csrf').then(response=>response.json());
   await fetch(`/api/appointments/${appointmentId}/cancel`,{method:'POST',headers:{[csrf.headerName]:csrf.token}});
  },rivalAppointmentId);
  await rivalContext.close();
 }
});
