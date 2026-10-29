import 'fake-indexeddb/auto';
import {describe,it,expect} from 'vitest';
import {clearQueuedBooking,draftKey,enqueueBooking,getBookingDraft,getBookingOutbox,rejectQueuedBooking,saveBookingDraft,type BookingDraft,type BookingOutboxItem} from './database';

let nextUserId=100;
const draft=(userId:number,status:BookingDraft['status']='draft'):BookingDraft=>({
 key:draftKey(userId),userId,professionalId:5,professionalName:'Dr Test',slotId:9,
 startsAt:'2026-10-31T13:00:00Z',appointmentId:null,status,updatedAt:new Date().toISOString()
});
const outbox=(userId:number):BookingOutboxItem=>({
 idemKey:crypto.randomUUID(),userId,slotId:9,appointmentId:null,createdAt:new Date().toISOString()
});

describe('Réservation hors ligne IndexedDB',()=>{
 it('enregistre atomiquement la demande dans outbox et passe son brouillon en pending',async()=>{
  const userId=++nextUserId,saved=draft(userId);
  await saveBookingDraft(saved);
  const queued=outbox(userId);
  await enqueueBooking(saved,queued);
  expect(await getBookingOutbox()).toEqual([queued]);
  expect(await getBookingDraft(userId)).toMatchObject({status:'pending',idemKey:queued.idemKey});
 });

 it('conserve le brouillon rejected après un conflit et enlève la requête',async()=>{
  const userId=++nextUserId,saved=draft(userId),queued=outbox(userId);
  await saveBookingDraft(saved);
  await enqueueBooking(saved,queued);
  expect(await rejectQueuedBooking(queued)).toMatchObject({key:draftKey(userId),status:'rejected',slotId:9});
  expect(await getBookingDraft(userId)).toMatchObject({status:'rejected',slotId:9});
  expect((await getBookingOutbox()).filter(item=>item.userId===userId)).toEqual([]);
 });

 it('supprime brouillon et entrée outbox après une confirmation',async()=>{
  const userId=++nextUserId,saved=draft(userId),queued=outbox(userId);
  await saveBookingDraft(saved);
  await enqueueBooking(saved,queued);
  await clearQueuedBooking(queued);
  expect(await getBookingDraft(userId)).toBeUndefined();
  expect((await getBookingOutbox()).filter(item=>item.userId===userId)).toEqual([]);
 });

 it('ne met pas silencieusement une seconde demande en attente pour le même compte',async()=>{
  const userId=++nextUserId,saved=draft(userId),queued=outbox(userId);
  await saveBookingDraft(saved);
  await enqueueBooking(saved,queued);
  await expect(enqueueBooking(saved,outbox(userId))).rejects.toThrow('Une réservation est déjà en attente d’envoi.');
 });
});
