<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import Menu from 'primevue/menu'
import Avatar from 'primevue/avatar'

import { useAuthStore } from '@/shared/store/useAuthStore'

const auth = useAuthStore()
const route = useRoute()

// Sections du menu : une section vide (selon les droits) est masquée
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
      items: s.items.map((l) => ({
        label: l.label,
        icon: l.icon,
        route: { name: l.nom },
        nom: l.nom,
      })),
    }))
})

const estActif = (item) => route.name === item.nom

// Infos utilisateur (adapte les noms de champs à ton store)
const nomUtilisateur = computed(() => auth.utilisateur?.nom || auth.user?.nom || 'Utilisateur')
const initiale = computed(() => nomUtilisateur.value.charAt(0).toUpperCase())
</script>

<template>
  <aside class="sidebar">
    <!-- Logo -->
    <div class="sidebar-logo">
      <div class="logo-badge">
        <i class="pi pi-building" />
      </div>
      <div class="flex flex-column">
        <span class="logo-titre">ERP TOVO</span>
        <span class="logo-sous-titre">Gestion RH</span>
      </div>
    </div>

    <!-- Navigation -->
    <nav class="sidebar-nav">
      <Menu :model="sections" class="menu-erp">
        <template #submenulabel="{ item }">
          <span class="menu-section">{{ item.label }}</span>
        </template>

        <template #item="{ item, props }">
          <router-link v-slot="{ href, navigate }" :to="item.route" custom>
            <a
              :href="href"
              v-bind="props.action"
              :class="['menu-lien', { 'menu-lien-actif': estActif(item) }]"
              @click="navigate"
            >
              <i :class="item.icon" />
              <span>{{ item.label }}</span>
            </a>
          </router-link>
        </template>
      </Menu>
    </nav>

    <!-- Carte utilisateur -->
    <div class="sidebar-user">
      <Avatar :label="initiale" shape="circle" class="user-avatar" />
      <div class="flex flex-column overflow-hidden">
        <span class="user-nom">{{ nomUtilisateur }}</span>
        <span class="user-role">Connecté</span>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  position: sticky;
  top: 0;
  flex-shrink: 0;
  width: 16rem;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--p-content-background);
  border-right: 1px solid var(--p-content-border-color);
}
/* ---------- Logo ---------- */
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1.25rem 1.25rem 1rem;
}

.logo-badge {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--p-primary-contrast-color);
  background: var(--p-primary-color);
  font-size: 1.2rem;
  box-shadow: 0 4px 10px color-mix(in srgb, var(--p-primary-color) 35%, transparent);
}

.logo-titre {
  font-weight: 700;
  font-size: 1.05rem;
  color: var(--p-text-color);
  line-height: 1.2;
}

.logo-sous-titre {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
}

/* ---------- Navigation ---------- */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 0 0.75rem;
}

:deep(.menu-erp) {
  width: 100%;
  border: none;
  background: transparent;
  padding: 0;
}

:deep(.menu-erp .p-menu-list) {
  gap: 0.15rem;
}

:deep(.menu-erp .p-menu-submenu-label) {
  background: transparent;
  padding: 1rem 0.75rem 0.4rem;
}

.menu-section {
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--p-text-muted-color);
}

.menu-lien {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.65rem 0.75rem;
  border-radius: 0.6rem;
  font-size: 0.92rem;
  font-weight: 500;
  color: var(--p-text-color);
  text-decoration: none;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.menu-lien i {
  font-size: 1.05rem;
  color: var(--p-text-muted-color);
  transition: color 0.15s ease;
}

.menu-lien:hover {
  background: var(--p-content-hover-background);
}

/* Lien actif */
.menu-lien-actif {
  background: var(--p-highlight-background);
  color: var(--p-highlight-color);
  font-weight: 600;
}

.menu-lien-actif i {
  color: var(--p-primary-color);
}

/* ---------- Carte utilisateur ---------- */
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 0.75rem;
  padding: 0.75rem;
  border-radius: 0.75rem;
  background: var(--p-content-hover-background);
}

.user-avatar {
  background: var(--p-primary-color);
  color: var(--p-primary-contrast-color);
  font-weight: 600;
}

.user-nom {
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--p-text-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
}
</style>