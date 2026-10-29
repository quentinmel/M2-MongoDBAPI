<script setup lang="ts">
defineProps<{online:boolean}>();
</script>

<template>
 <div v-if="!online" class="network-banner" role="status">Hors ligne, vos réponses sont gardées</div>
</template>
