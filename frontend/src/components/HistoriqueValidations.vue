<script setup>
import { computed } from 'vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { formatDateHeure, libelle } from '@/utils/format'

/**
 * Historique des decisions d'une ligne de demande : qui, quand, quelle decision.
 */
const props = defineProps({
  validations: { type: Array, default: () => [] },
})

const lignes = computed(() =>
  props.validations.map((validation) => ({
    ...validation,
    decisionLisible: libelle(validation.decision),
    etapeLisible: libelle(validation.etape),
  })),
)
</script>

<template>
  <div v-if="lignes.length === 0" class="etat-vide">
    <div class="etat-vide__titre">Aucune decision</div>
    <div>Les decisions prises sur cette ligne apparaitront ici.</div>
  </div>

  <div v-else class="tableau-conteneur">
    <table class="tableau tableau--compacte">
      <thead>
        <tr>
          <th>Date</th>
          <th>Etape</th>
          <th>Decision</th>
          <th>Valideur</th>
          <th>Commentaire</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(ligne, index) in lignes" :key="index">
          <td class="mono">{{ formatDateHeure(ligne.dateAction) }}</td>
          <td>{{ ligne.etapeLible || ligne.etapeLisible }}</td>
          <td>
            <StatusBadge :valeur="ligne.decision" />
          </td>
          <td>{{ ligne.utilisateur || '—' }}</td>
          <td>{{ ligne.commentaire || '—' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
