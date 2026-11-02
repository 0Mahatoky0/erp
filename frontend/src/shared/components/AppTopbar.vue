<script setup>
import { useAuthStore } from '@/stores/auth'

defineProps({
  titre: { type: String, required: true },
})

defineEmits(['deconnexion'])

const auth = useAuthStore()
</script>

<template>
  <header class="topbar">
    <div class="topbar__titre">{{ titre }}</div>
    <div class="topbar__actions">
      <span class="texte-secondaire">{{ auth.profil?.departementNom || 'Tous departements' }}</span>
      <button class="bouton bouton--secondaire bouton--petit" type="button" @click="$emit('deconnexion')">
        Deconnexion
      </button>
    </div>
  </header>
</template>
