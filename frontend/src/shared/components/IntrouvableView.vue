<script setup>
import PrimeButton from 'primevue/button'
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="grid min-h-screen grid-cols-1 bg-surface-50 lg:grid-cols-2 dark:bg-surface-950">
    <section class="flex items-center justify-center p-6" style="grid-column: 1 / -1">
      <div class="w-full max-w-md rounded-xl border border-surface bg-surface-0 p-8 shadow-sm dark:bg-surface-900" style="text-align: center">
        <h1 class="mb-2 text-2xl font-semibold">Page introuvable</h1>
        <p class="mb-6 text-sm text-muted-color">L'adresse demandee n'existe pas ou n'est plus disponible.</p>
        <PrimeButton class="inline-flex items-center justify-center gap-2 bg-primary text-primary-contrast" @click="router.push({ name: 'tableau-bord' })">
          Retour au tableau de bord
        </PrimeButton>
      </div>
    </section>
  </div>
</template>
