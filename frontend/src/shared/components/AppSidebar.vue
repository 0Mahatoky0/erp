<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

// Composants PrimeVue
import Drawer from 'primevue/drawer'      // Le panneau coulissant (Sidebar)
import Button from 'primevue/button'      // Le bouton Burger
import PanelMenu from 'primevue/panelmenu' // La liste des liens RH

import { useAuthStore } from '@/shared/store/useAuthStore'

const auth = useAuthStore()
const router = useRouter()

// 1. État d'ouverture du menu (ouvert ou fermé)
const visible = ref(false)

const liens = computed(() => {
  const liste = [
    { nom: 'tableau-bord', label: 'Tableau de bord', icon: 'pi pi-chart-bar' },
    { nom: 'demandes', label: 'Demandes', icon: 'pi pi-file' },
    { nom: 'demande-nouvelle', label: 'Nouvelle demande', icon: 'pi pi-plus' },
    { nom: 'validation', label: 'File de validation', icon: 'pi pi-check-square' },
  ]

  if (auth.estRh) liste.push({ nom: 'postes', label: 'Catalogue des postes', icon: 'pi pi-briefcase' })
  if (auth.estFinance) liste.push({ nom: 'budgets', label: 'Budgets', icon: 'pi pi-wallet' })
  liste.push({ nom: 'employes', label: 'Employés', icon: 'pi pi-users' })
  if (auth.estRh) liste.push({ nom: 'utilisateurs', label: 'Utilisateurs', icon: 'pi pi-user-cog' })

  if (auth.estRh || auth.estDirection || auth.estManager) {
    liste.push(
      { nom: 'recrutement-demandes', label: 'Demandes acceptées', icon: 'pi pi-list-check' },
      { nom: 'recrutement-offres', label: 'Offres d’emploi', icon: 'pi pi-megaphone' },
      { nom: 'recrutement-candidatures', label: 'Candidatures', icon: 'pi pi-users' },
      { nom: 'recrutement-selection', label: 'Présélection', icon: 'pi pi-check' },
      { nom: 'recrutement-suivi', label: 'Suivi candidats', icon: 'pi pi-chart-line' },
    )
  }

  return liste
})

const items = computed(() =>
  liens.value.map((lien) => ({
    label: lien.label,
    icon: lien.icon,
    command: () => {
      router.push({ name: lien.nom })
      visible.value = false // Ferme le menu après avoir cliqué sur un lien
    },
  })),
)
</script>

<template>
  <div>
    <!-- 1. En-tête / Header avec le bouton Burger -->
    <header class="flex align-items-center gap-3 p-3 border-bottom-1 surface-border bg-white">
      <!-- Bouton Burger pour ouvrir le Drawer -->
      <Button
        icon="pi pi-bars"
        severity="secondary"
        text
        rounded
        @click="visible = true"
      />

      <div class="flex align-items-center gap-2">
        <i class="pi pi-building text-primary text-xl" />
        <span class="font-semibold text-lg">ERP TOVO</span>
      </div>
    </header>

    <!-- 2. Composant Drawer de PrimeVue (la Sidebar coulissante à gauche) -->
    <Drawer v-model:visible="visible" position="left" header="Navigation">
      <PanelMenu :model="items" class="w-full border-none" />
    </Drawer>
  </div>
</template>