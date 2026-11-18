<script setup>
import { onMounted, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeDialog from 'primevue/dialog'
import { paieApi as api } from '@/modules/paie/api/paieApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const notifications = useNotificationStore()
const fiches = ref([])
const selectionnee = ref(null)
const chargement = ref(false)

async function charger() {
  chargement.value = true
  try {
    fiches.value = await api.mesFiches()
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

function contenu(fiche) {
  if (!fiche.contenu) return ''
  try {
    return JSON.stringify(JSON.parse(fiche.contenu), null, 2)
  } catch {
    return fiche.contenu
  }
}

onMounted(charger)
</script>

<template>
  <div class="flex flex-col gap-5">
    <div>
      <h1>Mes fiches de paie</h1>
      <p class="mt-1 text-sm text-muted-color">Fiches individuelles figées et consultables après clôture de la période.</p>
    </div>
    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <PrimeButton label="Actualiser" icon="pi pi-refresh" @click="charger" />
      <DataTable :value="fiches" :loading="chargement" striped-rows class="mt-4">
        <Column field="numero" header="Numéro" />
        <Column field="mois" header="Mois" />
        <Column field="netAPayer" header="Net à payer" />
        <Column field="dateGeneration" header="Générée le" />
        <Column header="Détail">
          <template #body="{ data }">
            <PrimeButton icon="pi pi-eye" text rounded aria-label="Voir la fiche" @click="selectionnee = data" />
          </template>
        </Column>
      </DataTable>
    </section>
    <PrimeDialog v-model:visible="selectionnee" modal header="Fiche de paie" class="w-[min(60rem,95vw)]">
      <pre v-if="selectionnee" class="max-h-[70vh] overflow-auto rounded bg-surface-100 p-3 text-xs dark:bg-surface-800">{{ contenu(selectionnee) }}</pre>
    </PrimeDialog>
  </div>
</template>
