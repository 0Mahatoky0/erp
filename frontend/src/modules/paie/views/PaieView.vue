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
const parametres = ref({ tauxCotisationsSalariales: '', tauxCotisationsPatronales: '', tauxIrsa: '' })
const bulletins = ref([])
const periodes = ref([])
const moisPaie = ref(aujourdHui.toISOString().slice(0, 7))
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

async function chargerParametres() {
  try {
    const resultat = await api.parametres()
    if (resultat) parametres.value = resultat
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function chargerPeriodes() {
  try {
    periodes.value = await api.periodes()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function enregistrerParametres() {
  try {
    parametres.value = await api.enregistrerParametres({
      tauxCotisationsSalariales: Number(parametres.value.tauxCotisationsSalariales),
      tauxCotisationsPatronales: Number(parametres.value.tauxCotisationsPatronales),
      tauxIrsa: Number(parametres.value.tauxIrsa),
    })
    notifications.succes('Les paramètres de paie ont été enregistrés.')
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function calculerPeriode() {
  try {
    const resultat = await api.calculerPeriode(moisPaie.value)
    bulletins.value = resultat.bulletins
    await chargerPeriodes()
    notifications.succes('La période de paie a été calculée.')
  } catch (e) {
    notifications.erreur(e.message)
  }
}

onMounted(async () => {
  await Promise.all([charger(), chargerParametres(), chargerPeriodes()])
})
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
      <h2 class="mb-3">Paramètres de calcul</h2>
      <p class="mb-3 text-xs text-muted-color">Les taux doivent être configurés selon la réglementation applicable avant tout calcul.</p>
      <div class="grid gap-3 md:grid-cols-4">
        <PrimeInputText v-model="parametres.tauxCotisationsSalariales" type="number" placeholder="Cotisations salariales (%)" />
        <PrimeInputText v-model="parametres.tauxCotisationsPatronales" type="number" placeholder="Cotisations patronales (%)" />
        <PrimeInputText v-model="parametres.tauxIrsa" type="number" placeholder="IRSA (%)" />
        <PrimeButton label="Enregistrer" icon="pi pi-save" @click="enregistrerParametres" />
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="flex flex-wrap items-end gap-3">
        <div class="flex flex-col gap-2">
          <label for="mois-paie" class="text-sm font-semibold text-muted-color">Période</label>
          <PrimeInputText id="mois-paie" v-model="moisPaie" type="month" />
        </div>
        <PrimeButton label="Calculer les bulletins" icon="pi pi-calculator" @click="calculerPeriode" />
      </div>
      <DataTable v-if="bulletins.length" :value="bulletins" class="mt-4" striped-rows>
        <Column field="idEmploye" header="Employé" />
        <Column field="salaireBrut" header="Brut" />
        <Column field="cotisationsSalariales" header="Cotisations salariales" />
        <Column field="impot" header="IRSA" />
        <Column header="Retenue">
          <template #body="{ data }">
            <PrimeInputText v-model="data.retenues" type="number" class="w-28" />
            <PrimeButton
              icon="pi pi-save"
              text
              rounded
              aria-label="Enregistrer la retenue"
              @click="api.enregistrerRetenue(data.id, Number(data.retenues))"
            />
          </template>
        </Column>
        <Column field="salaireNet" header="Net à payer" />
        <Column field="statut" header="Statut" />
      </DataTable>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="mb-3">Historique des périodes</h2>
      <DataTable :value="periodes" striped-rows>
        <Column field="mois" header="Mois" />
        <Column field="statut" header="Statut" />
        <Column field="dateCreation" header="Créée le" />
        <Column field="dateCloture" header="Clôturée le" />
      </DataTable>
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
