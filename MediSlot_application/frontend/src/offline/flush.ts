import {clearQueuedBooking,getBookingOutbox,rejectQueuedBooking,type BookingOutboxItem,type BookingDraft} from './database';

export interface FlushResponse {
 status:number;
 code?:string;
 appointment?:unknown;
}

export interface FlushHandlers {
 send:(item:BookingOutboxItem)=>Promise<FlushResponse>;
 sent:(item:BookingOutboxItem,response:FlushResponse)=>void|Promise<void>;
 rejected:(item:BookingOutboxItem,draft:BookingDraft|undefined)=>void|Promise<void>;
 failed:(item:BookingOutboxItem,error:unknown)=>void;
}

export function flush(handlers:FlushHandlers):{run:()=>Promise<void>;stop:()=>void}{
 let running=false;
 let stopped=false;
 let rerunRequested=false;

 const run=async()=>{
  if(stopped||running||!navigator.onLine)return;
  running=true;
  try{
   const queued=await getBookingOutbox();
   for(const item of queued){
    if(stopped||!navigator.onLine)break;
    try{
     const response=await handlers.send(item);
     if(response.status===201){
      await clearQueuedBooking(item);
      await handlers.sent(item,response);
     }else if(response.status===409&&response.code==='SLOT_TAKEN'){
      const draft=await rejectQueuedBooking(item);
      await handlers.rejected(item,draft);
     }else{
      handlers.failed(item,new Error(`Unexpected booking response: ${response.status}`));
      break;
     }
    }catch(error){
     handlers.failed(item,error);
     break;
    }
   }
  }finally{
   running=false;
   if(rerunRequested&&!stopped&&navigator.onLine){
    rerunRequested=false;
    void run();
   }
  }
 };

 const onOnline=()=>{if(running)rerunRequested=true;else void run()};
 const onVisibilityChange=()=>{if(document.visibilityState==='visible')void run()};
 window.addEventListener('online',onOnline);
 document.addEventListener('visibilitychange',onVisibilityChange);
 void run();

 const stop=()=>{
  stopped=true;
  window.removeEventListener('online',onOnline);
  document.removeEventListener('visibilitychange',onVisibilityChange);
 };
 return {run,stop};
}
