<script setup>
import PrimeButton from 'primevue/button'
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="connexion">
    <section class="connexion__panneau" style="grid-column: 1 / -1">
      <div class="connexion__boite" style="text-align: center">
        <h1 class="connexion__titre-formulaire">Page introuvable</h1>
        <p class="connexion__sous-titre">L'adresse demandee n'existe pas ou n'est plus disponible.</p>
        <PrimeButton class="bouton bouton--principal" @click="router.push({ name: 'tableau-bord' })">
          Retour au tableau de bord
        </PrimeButton>
      </div>
    </section>
  </div>
</template>
