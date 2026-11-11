<script setup>
import { onMounted, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import { paieApi as api } from '@/modules/paie/api/paieApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const notifications = useNotificationStore()
const aujourdHui = new Date()
const debut = ref(new Date(aujourdHui.getFullYear(), aujourdHui.getMonth(), 1).toISOString().slice(0, 10))
const fin = ref(new Date(aujourdHui.getFullYear(), aujourdHui.getMonth() + 1, 0).toISOString().slice(0, 10))
const variables = ref([])
const chargement = ref(false)

async function charger() {
  chargement.value = true
  try {
    variables.value = await api.variables(debut.value, fin.value)
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)
</script>

<template>
  <div class="flex flex-col gap-5">
    <div>
      <h1>Variables de paie</h1>
      <p class="mt-1 text-sm text-muted-color">Calcul indicatif des montants liés aux congés validés.</p>
    </div>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="flex flex-wrap items-end gap-3">
        <div class="flex flex-col gap-2">
          <label for="paie-debut" class="text-sm font-semibold text-muted-color">Début</label>
          <PrimeInputText id="paie-debut" v-model="debut" type="date" />
        </div>
        <div class="flex flex-col gap-2">
          <label for="paie-fin" class="text-sm font-semibold text-muted-color">Fin</label>
          <PrimeInputText id="paie-fin" v-model="fin" type="date" />
        </div>
        <PrimeButton label="Actualiser" icon="pi pi-refresh" @click="charger" />
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <DataTable :value="variables" :loading="chargement" striped-rows paginator :rows="10" responsive-layout="scroll">
        <Column field="idEmploye" header="Employé" />
        <Column field="typeConge" header="Type" />
        <Column header="Période">
          <template #body="{ data }">{{ data.dateDebut }} → {{ data.dateFin }}</template>
        </Column>
        <Column field="joursConges" header="Jours" />
        <Column header="Salaire de base">
          <template #body="{ data }">{{ data.salaireBase ?? 'Contrat absent' }} {{ data.devise || '' }}</template>
        </Column>
        <Column header="Montant congé">
          <template #body="{ data }">{{ data.montantConges ?? 'Non calculable' }} {{ data.devise || '' }}</template>
        </Column>
      </DataTable>
      <p class="mt-3 text-xs text-muted-color">
        Le montant est calculé selon la formule salaire de base × jours de congé / 30. Ce n’est pas encore un bulletin complet.
      </p>
    </section>
  </div>
</template>
