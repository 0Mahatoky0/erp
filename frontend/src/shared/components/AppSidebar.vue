<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAuthStore } from '@/shared/store/useAuthStore'

const auth = useAuthStore()
const route = useRoute()

/** Liens de navigation, filtres selon le role de l'utilisateur (RM-067). */
const liens = computed(() => {
  const liste = [
    { nom: 'tableau-bord', libelle: 'Tableau de bord', icone: '◧' },
    { nom: 'demandes', libelle: 'Demandes', icone: '▤' },
    { nom: 'demande-nouvelle', libelle: 'Nouvelle demande', icone: '＋' },
    { nom: 'validation', libelle: 'File de validation', icone: '✓' },
  ]

  if (auth.estRh) {
    liste.push({ nom: 'postes', libelle: 'Catalogue des postes', icone: '▣' })
  }
  if (auth.estFinance) {
    liste.push({ nom: 'budgets', libelle: 'Budgets', icone: '¤' })
  }
  liste.push({ nom: 'employes', libelle: 'Employes', icone: '☺' })
  if (auth.estRh) {
    liste.push({ nom: 'utilisateurs', libelle: 'Utilisateurs', icone: '⚙' })
  }
  if (auth.estRh || auth.estDirection || auth.estManager) {
    liste.push({ nom: 'recrutement-demandes', libelle: 'Demandes acceptees', icone: '▥' })
    liste.push({ nom: 'recrutement-offres', libelle: 'Offres d’emploi', icone: '▣' })
    liste.push({ nom: 'recrutement-candidatures', libelle: 'Candidatures', icone: '☷' })
    liste.push({ nom: 'recrutement-selection', libelle: 'Présélection', icone: '✓' })
    liste.push({ nom: 'recrutement-suivi', libelle: 'Suivi candidats', icone: '◉' })
  }
  return liste
})

function estActif(nom) {
  if (nom === 'demandes') {
    return route.name === 'demandes' || route.name === 'demande-detail'
  }
  if (nom.startsWith('recrutement-')) return route.name === nom
  return route.name === nom
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar__marque">
      <div class="sidebar__logo">T</div>
      <div>
        <div class="sidebar__titre">ERP TOVO</div>
        <div class="sidebar__sous-titre">Recrutement</div>
      </div>
    </div>

    <nav class="sidebar__nav">
      <div class="sidebar__groupe">Pilotage</div>
      <RouterLink
        v-for="lien in liens"
        :key="lien.nom"
        :to="{ name: lien.nom }"
        class="sidebar__lien"
        :class="{ actif: estActif(lien.nom) }"
      >
        <span class="icone">{{ lien.icone }}</span>
        <span>{{ lien.libelle }}</span>
      </RouterLink>
    </nav>

    <div class="sidebar__compte">
      <div class="sidebar__utilisateur">{{ auth.profil?.nom }}</div>
      <div class="sidebar__role">{{ auth.role }}</div>
    </div>
  </aside>
</template>
