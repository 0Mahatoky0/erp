<script setup>
import PrimeButton from 'primevue/button'
import Toolbar from 'primevue/toolbar'
import Tag from 'primevue/tag'
import { useAuthStore } from '@/shared/store/useAuthStore'

defineProps({
  titre: { type: String, required: true },
})

defineEmits(['deconnexion'])

const auth = useAuthStore()
</script>

<template>
  <Toolbar class="rounded-none border-x-0 border-t-0">
    <template #start>
      <div class="flex items-center gap-3">
        <h1 class="text-xl m-0">{{ titre }}</h1>
        <Tag :value="auth.role || 'Utilisateur'" severity="info" />
      </div>
    </template>
    <template #end>
      <div class="flex items-center gap-3">
        <span class="text-muted-color hidden md:inline">
          {{ auth.profil?.departementNom || 'Tous departements' }}
        </span>
        <PrimeButton
          label="Deconnexion"
          icon="pi pi-sign-out"
          severity="secondary"
          size="small"
          outlined
          @click="$emit('deconnexion')"
        />
      </div>
    </template>
  </Toolbar>
</template>
