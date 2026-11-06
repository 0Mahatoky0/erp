<script setup>
import { computed } from 'vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { formatDateHeure, libelle } from '@/shared/utils/format'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

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
  <div v-if="lignes.length === 0" class="rounded-lg border border-dashed border-surface p-6 text-center text-muted-color">
    <div class="mb-1 font-semibold text-color">Aucune decision</div>
    <div>Les decisions prises sur cette ligne apparaitront ici.</div>
  </div>

  <DataTable v-else :value="lignes" striped-rows responsive-layout="scroll" size="small">
    <Column header="Date"><template #body="{ data }">{{ formatDateHeure(data.dateAction) }}</template></Column>
    <Column header="Etape"><template #body="{ data }">{{ data.etapeLible || data.etapeLisible }}</template></Column>
    <Column header="Decision"><template #body="{ data }"><StatusBadge :valeur="data.decision" /></template></Column>
    <Column header="Valideur"><template #body="{ data }">{{ data.utilisateur || '—' }}</template></Column>
    <Column field="commentaire" header="Commentaire" />
  </DataTable>
</template>
