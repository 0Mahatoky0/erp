<script setup>
import Card from 'primevue/card'
import Message from 'primevue/message'

defineProps({
  titre: { type: String, default: 'Aucun resultat' },
  message: { type: String, default: 'Aucun element a afficher pour le moment.' },
})
</script>

<template>
  <Card class="my-3">
    <template #content>
      <Message severity="info" icon="pi pi-info-circle" class="m-0">
        <div>
          <strong class="block mb-1">{{ titre }}</strong>
          <span>{{ message }}</span>
        </div>
      </Message>
      <div class="mt-3">
        <slot />
      </div>
    </template>
  </Card>
</template>
