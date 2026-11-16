<script setup lang="ts">
import {ref} from 'vue';
import {openConvocation, saveConvocation} from '../convocation';

const props = defineProps<{appointmentId: number}>();
const busy = ref(false);
const error = ref('');

async function run(action: typeof openConvocation | typeof saveConvocation) {
  if (busy.value) return;
  busy.value = true;
  error.value = '';
  try {
    await action(props.appointmentId);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'La convocation n’a pas pu être préparée.';
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <div class="convocation-actions">
    <button
      class="text-button"
      type="button"
      :disabled="busy"
      :aria-busy="busy"
      @click="run(openConvocation)"
    >{{ busy ? 'Préparation...' : 'Afficher la convocation' }}</button>
    <button
      class="primary convocation-download"
      type="button"
      :disabled="busy"
      :aria-busy="busy"
      @click="run(saveConvocation)"
    >{{ busy ? 'Préparation...' : 'Enregistrer le PDF' }}</button>
    <p v-if="error" class="convocation-error" role="alert">{{ error }}</p>
  </div>
</template>
