<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import Menu from 'primevue/menu'
import Avatar from 'primevue/avatar'
import Tag from 'primevue/tag'

import { useAuthStore } from '@/shared/store/useAuthStore'

const auth = useAuthStore()
const route = useRoute()

// Sections du menu : une section sans lien (selon les droits) est masquée
const sections = computed(() => {
  const principal = [
    { nom: 'tableau-bord', label: 'Tableau de bord', icon: 'pi pi-chart-bar' },
    { nom: 'demandes', label: 'Demandes', icon: 'pi pi-file' },
    { nom: 'demande-nouvelle', label: 'Nouvelle demande', icon: 'pi pi-plus' },
    { nom: 'validation', label: 'File de validation', icon: 'pi pi-check-square' },
  ]

  const gestion = []
  if (auth.estRh) gestion.push({ nom: 'postes', label: 'Catalogue des postes', icon: 'pi pi-briefcase' })
  if (auth.estFinance) gestion.push({ nom: 'budgets', label: 'Budgets', icon: 'pi pi-wallet' })
  gestion.push({ nom: 'employes', label: 'Employés', icon: 'pi pi-users' })
  if (auth.estRh) gestion.push({ nom: 'utilisateurs', label: 'Utilisateurs', icon: 'pi pi-user-cog' })

  const recrutement = []
  if (auth.estRh || auth.estDirection || auth.estManager) {
    recrutement.push(
      { nom: 'recrutement-demandes', label: 'Demandes acceptées', icon: 'pi pi-list-check' },
      { nom: 'recrutement-offres', label: 'Offres d’emploi', icon: 'pi pi-megaphone' },
      { nom: 'recrutement-candidatures', label: 'Candidatures', icon: 'pi pi-users' },
      { nom: 'recrutement-selection', label: 'Présélection', icon: 'pi pi-check' },
      { nom: 'recrutement-suivi', label: 'Suivi candidats', icon: 'pi pi-chart-line' },
    )
  }

  return [
    { label: 'Principal', items: principal },
    { label: 'Gestion', items: gestion },
    { label: 'Recrutement', items: recrutement },
  ]
    .filter((s) => s.items.length)
    .map((s) => ({
      label: s.label,
      items: s.items.map((l) => ({ label: l.label, icon: l.icon, route: { name: l.nom }, nom: l.nom })),
    }))
})

// Actif sur la page elle-même et sur ses pages enfants (ex. fiche d'une offre)
const estActif = (item) => route.name === item.nom || route.meta.menu === item.nom

const nomUtilisateur = computed(() => auth.profil?.nom || auth.profil?.email || 'Utilisateur')
</script>

<template>
  <aside class="sticky top-0 flex h-screen w-64 shrink-0 flex-col border-r border-surface bg-surface-0 dark:bg-surface-900">
    <!-- Logo -->
    <div class="flex items-center gap-3 px-5 py-5">
      <div class="flex size-10 items-center justify-center rounded-xl bg-primary text-primary-contrast shadow-md">
        <i class="pi pi-building text-lg" />
      </div>
      <div class="flex flex-col leading-tight">
        <span class="text-base font-bold">ERP TOVO</span>
        <span class="text-xs text-muted-color">Gestion RH</span>
      </div>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 overflow-y-auto px-3 pb-3">
      <Menu
        :model="sections"
        class="!w-full !border-0 !bg-transparent !p-0"
        :pt="{ list: { class: 'flex flex-col gap-0.5' } }"
      >
        <template #submenulabel="{ item }">
          <span class="block px-3 pb-1 pt-4 text-[0.7rem] font-semibold uppercase tracking-widest text-muted-color">
            {{ item.label }}
          </span>
        </template>

        <template #item="{ item, props }">
          <router-link v-slot="{ href, navigate }" :to="item.route" custom>
            <a
              :href="href"
              v-bind="props.action"
              :aria-current="estActif(item) ? 'page' : undefined"
              class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm no-underline transition-colors"
              :class="estActif(item) ? 'bg-primary/10 font-semibold text-primary' : 'font-medium text-color hover:bg-surface-100 dark:hover:bg-surface-800'"
              @click="navigate"
            >
              <i :class="[item.icon, estActif(item) ? 'text-primary' : 'text-muted-color']" />
              <span>{{ item.label }}</span>
            </a>
          </router-link>
        </template>
      </Menu>
    </nav>

    <!-- Carte utilisateur -->
    <div class="m-3 flex items-center gap-3 rounded-xl bg-surface-100 p-3 dark:bg-surface-800">
      <Avatar :label="auth.initiales" shape="circle" class="!bg-primary !text-primary-contrast" />
      <div class="flex min-w-0 flex-col gap-1">
        <span class="truncate text-sm font-semibold">{{ nomUtilisateur }}</span>
        <Tag v-if="auth.role" :value="auth.role" severity="secondary" class="w-fit !text-[0.65rem]" />
      </div>
    </div>
  </aside>
</template>
