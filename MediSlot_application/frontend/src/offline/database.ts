import {openDB,type IDBPDatabase} from 'idb';

export interface BookingDraft {
 key:string;
 userId:number;
 professionalId:number;
 professionalName:string;
 slotId:number;
 startsAt:string;
 appointmentId:number|null;
 status:'draft'|'pending'|'rejected';
 idemKey?:string;
 updatedAt:string;
}

export interface BookingOutboxItem {
 idemKey:string;
 userId:number;
 slotId:number;
 appointmentId:number|null;
 createdAt:string;
}

interface MediSlotDatabase {
 drafts: {
  key:string;
  value:BookingDraft;
  indexes: Record<string,never>;
 };
 outbox: {
  key:string;
  value:BookingOutboxItem;
  indexes: Record<string,never>;
 };
}

let database:Promise<IDBPDatabase<MediSlotDatabase>>|undefined;
function openDatabase(){
 database??=openDB<MediSlotDatabase>('medislot-offline',1,{
  upgrade(db){
   db.createObjectStore('drafts',{keyPath:'key'});
   db.createObjectStore('outbox',{keyPath:'idemKey'});
  }
 });
 return database;
}

export const draftKey=(userId:number)=>`booking:${userId}`;

export async function saveBookingDraft(draft:BookingDraft):Promise<void>{
 const db=await openDatabase();
 await db.put('drafts',draft);
}

export async function getBookingDraft(userId:number):Promise<BookingDraft|undefined>{
 return (await openDatabase()).get('drafts',draftKey(userId));
}

export async function clearBookingDraft(userId:number):Promise<void>{
 await (await openDatabase()).delete('drafts',draftKey(userId));
}

export async function getBookingOutbox():Promise<BookingOutboxItem[]>{
 return (await openDatabase()).getAll('outbox');
}

export async function enqueueBooking(
 draft:BookingDraft,
 item:BookingOutboxItem
):Promise<void>{
 const db=await openDatabase();
 const tx=db.transaction(['drafts','outbox'],'readwrite');
 const existing=await tx.objectStore('outbox').getAll();
 if(existing.some(pending=>pending.userId===item.userId)){
  await tx.done;
  throw new Error('Une réservation est déjà en attente d’envoi.');
 }
 await Promise.all([
  tx.objectStore('outbox').put(item),
  tx.objectStore('drafts').put({...draft,status:'pending',idemKey:item.idemKey,updatedAt:new Date().toISOString()})
 ]);
 await tx.done;
}

export async function clearQueuedBooking(item:BookingOutboxItem):Promise<void>{
 const db=await openDatabase();
 const tx=db.transaction(['drafts','outbox'],'readwrite');
 await Promise.all([
  tx.objectStore('outbox').delete(item.idemKey),
  tx.objectStore('drafts').delete(draftKey(item.userId))
 ]);
 await tx.done;
}

export async function rejectQueuedBooking(item:BookingOutboxItem):Promise<BookingDraft|undefined>{
 const db=await openDatabase();
 const tx=db.transaction(['drafts','outbox'],'readwrite');
 const key=draftKey(item.userId);
 const draft=await tx.objectStore('drafts').get(key);
 await tx.objectStore('outbox').delete(item.idemKey);
 if(draft){
  draft.status='rejected';
  delete draft.idemKey;
  draft.updatedAt=new Date().toISOString();
  await tx.objectStore('drafts').put(draft);
 }
 await tx.done;
 return draft;
}
