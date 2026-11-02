<script setup>
import { computed } from 'vue'
import Tag from 'primevue/tag'

/**
 * Badge de statut colore selon la valeur (statut de demande, de ligne,
 * etape de validation ou decision).
 */
const props = defineProps({
  valeur: { type: String, default: null },
  libelle: { type: String, default: null },
  type: { type: String, default: null },
})

const LIBELLES = {
  BROUILLON: 'Brouillon',
  EN_VALIDATION: 'En validation',
  A_CORRIGER: 'A corriger',
  ACCEPTEE: 'Acceptee',
  REFUSEE: 'Refusee',
  ANNULEE: 'Annulee',
  FINALISEE: 'Finalisee',
  FINALISEE_PARTIELLE: 'Finalisee partiellement',
  EN_ATTENTE_RH: 'En attente RH',
  EN_ATTENTE_FINANCE: 'En attente finance',
  EN_ATTENTE_DIRECTION: 'En attente direction',
  APPROUVE: 'Approuve',
  RH: 'RH',
  FINANCE: 'Finance',
  DIRECTION: 'Direction',
  TEMPORAIRE: 'Temporaire',
  DEFINITIF: 'Definitif',
  CREATION: 'Creation',
  REMPLACEMENT: 'Remplacement',
  ACTIF: 'Actif',
  INACTIF: 'Inactif',
  PARTI: 'Parti',
  MANAGER: 'Manager',
  
  PUBLIEE: 'Publiee',
  CLOTUREE: 'Cloturee',
}

const COULEURS = {
  BROUILLON: 'neutre',
  EN_VALIDATION: 'info',
  A_CORRIGER: 'alerte',
  ACCEPTEE: 'succes',
  REFUSEE: 'danger',
  ANNULEE: 'neutre',
  FINALISEE: 'succes',
  FINALISEE_PARTIELLE: 'primaire',
  EN_ATTENTE_RH: 'info',
  EN_ATTENTE_FINANCE: 'info',
  EN_ATTENTE_DIRECTION: 'info',
  APPROUVE: 'succes',
  RH: 'primaire',
  FINANCE: 'primaire',
  DIRECTION: 'primaire',
  TEMPORAIRE: 'alerte',
  DEFINITIF: 'primaire',
  CREATION: 'primaire',
  REMPLACEMENT: 'info',
  ACTIF: 'succes',
  INACTIF: 'neutre',
  PARTI: 'neutre',
  MANAGER: 'neutre',
  PUBLIEE: 'succes',
  CLOTUREE: 'neutre',
}

const couleur = computed(() => {
  const couleur = COULEURS[props.valeur] || 'neutre'
  return {
    neutre: 'secondary',
    info: 'info',
    alerte: 'warn',
    succes: 'success',
    danger: 'danger',
    primaire: 'primary',
  }[couleur]
})
const texte = computed(() => props.libelle || LIBELLES[props.valeur] || props.valeur || '—')
</script>

<template>
  <Tag :value="texte" :severity="couleur" rounded />
</template>
