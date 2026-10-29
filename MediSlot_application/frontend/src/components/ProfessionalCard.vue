<script setup lang="ts">
import {computed,onMounted,onUnmounted,nextTick,ref,watch} from 'vue';import {specialties,timeLabel,dateLabel,type Professional,type Slot} from '../types';
const props=defineProps<{professional:Professional;slots:Slot[];variant:'A'|'B'|null;exposeIfAvailable:()=>void}>();
defineEmits<{choose:[professional:Professional,slot:Slot];profile:[professional:Professional]}>();
const days=computed(()=>{const m=new Map<string,Slot[]>();for(const s of props.slots){const day=dateLabel(s.startsAt);if(!m.has(day))m.set(day,[]);m.get(day)!.push(s);}return [...m.entries()].slice(0,3)});
const label=computed(()=>props.variant==='B'?'Choisir ce créneau':'Réserver');
const initials=computed(()=>props.professional.name.replace('Dr ','').split(' ').map(s=>s[0]).join(''));
const bookingButtons=ref<HTMLElement[]>([]);
let observer:IntersectionObserver|null=null;
const observed=new WeakSet<Element>();
function observeButtons(){if(!observer||props.variant===null)return;for(const button of bookingButtons.value){if(observed.has(button))continue;observed.add(button);observer.observe(button)}}
onMounted(()=>{if(typeof IntersectionObserver==='undefined')return;observer=new IntersectionObserver(entries=>{for(const entry of entries){if(entry.isIntersecting&&entry.intersectionRatio>=0.5){observer?.unobserve(entry.target);props.exposeIfAvailable()}}},{threshold:[0.5]});observeButtons()});
watch(()=>[props.variant,props.slots],async()=>{await nextTick();observeButtons()},{deep:true});
onUnmounted(()=>observer?.disconnect());
</script>
<template>
 <article class="doctor-card">
  <div class="doctor-info"><div class="identity-row"><div class="avatar" aria-hidden="true">{{initials}}</div><div><button class="name-link" @click="$emit('profile',professional)">{{professional.name}}</button><p class="specialty">{{specialties[professional.specialty]}}</p></div></div>
   <p class="address">{{professional.address}}<br>{{professional.city}}</p><p class="price">Consultation au cabinet · {{professional.fee}} €</p><button class="text-button" @click="$emit('profile',professional)">Voir la fiche du praticien <span aria-hidden="true">↗</span></button></div>
  <div class="availability"><p class="availability-title">Prochaines disponibilités</p><div v-if="days.length" class="days"><div v-for="[day,slotsOfDay] in days" :key="day" class="day"><h3>{{day}}</h3><template v-for="slot in slotsOfDay.slice(0,4)" :key="slot.id"><button v-if="props.variant===null" class="slot slot-loading" disabled aria-label="Chargement du bouton de réservation" aria-busy="true"></button><button v-else ref="bookingButtons" class="slot" :aria-label="`${label} avec ${professional.name} le ${day} à ${timeLabel(slot.startsAt)}`" @click="$emit('choose',professional,slot)"><span class="slot-label">{{label}}</span><span class="slot-time">{{timeLabel(slot.startsAt)}}</span></button></template></div></div><p v-else class="empty-small">Aucun créneau disponible dans les 14 prochains jours.</p></div>
 </article>
</template>
