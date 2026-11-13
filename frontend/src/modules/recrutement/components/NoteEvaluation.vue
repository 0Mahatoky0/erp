<script setup>
import Tag from 'primevue/tag'
import { formatNote } from '../utils/notes'

/**
 * Affiche la note d'une catégorie d'évaluation (dossier, entretien, test) sur 20,
 * ou précise pourquoi elle est absente : jamais évaluée, ou évaluée sans note.
 */
defineProps({
  categorie: { type: Object, default: null },
})
</script>

<template>
  <Tag v-if="!categorie?.evaluee" value="Non évalué" severity="warn" />
  <Tag v-else-if="categorie.note === null || categorie.note === undefined" value="Sans note" severity="secondary" />
  <span v-else class="whitespace-nowrap">
    <strong>{{ formatNote(categorie.note) }}</strong>
    <span class="text-muted-color"> / {{ formatNote(categorie.noteMax ?? 20) }}</span>
    <span v-if="categorie.nombreEvaluations > 1" class="ml-1 text-xs text-muted-color">
      ({{ categorie.nombreEvaluations }} éval.)
    </span>
  </span>
</template>
