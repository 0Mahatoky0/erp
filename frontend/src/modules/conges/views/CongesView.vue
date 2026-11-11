<script setup>
import { onMounted, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeSelect from 'primevue/select'
import { congesApi as api } from '@/modules/conges/api/congesApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { useAuthStore } from '@/shared/store/useAuthStore'

const notifications = useNotificationStore()
const auth = useAuthStore()
const chargement = ref(true)
const employeId = ref(localStorage.getItem('erp.conges.employeId') || '')
const types = ref([])
const soldes = ref([])
const demandes = ref([])
const absences = ref([])
const demande = ref({
  idTypeConge: null,
  dateDebut: new Date().toISOString().slice(0, 10),
  dateFin: new Date().toISOString().slice(0, 10),
  motif: '',
})
const absence = ref({
  dateDebut: new Date().toISOString().slice(0, 10),
  dateFin: new Date().toISOString().slice(0, 10),
  motif: '',
  justificatif: '',
})

function idRequis() {
  const id = Number(employeId.value)
  if (!id) {
    notifications.erreur('Saisissez l’identifiant de l’employe.')
    return null
  }
  return id
}

async function charger() {
  const id = idRequis()
  if (!id) return
  chargement.value = true
  try {
    localStorage.setItem('erp.conges.employeId', String(id))
    const [soldesEmploye, listeDemandes, listeAbsences] = await Promise.all([
      api.soldes(id, new Date().getFullYear()),
      auth.estEmploye ? api.demandesEmploye(id) : api.demandes(),
      api.absences(),
    ])
    soldes.value = soldesEmploye
    demandes.value = listeDemandes
    absences.value = listeAbsences
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

async function initialiser() {
  try {
    types.value = await api.types()
    demande.value.idTypeConge = types.value[0]?.id || null
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function creerDemande() {
  const id = idRequis()
  if (!id) return
  try {
    await api.creerDemande({ ...demande.value, idEmploye: id })
    notifications.succes('La demande de congé a ete envoyee au validateur.')
    demande.value.motif = ''
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function creerAbsence() {
  const id = idRequis()
  if (!id) return
  try {
    await api.creerAbsence({ ...absence.value, idEmploye: id })
    notifications.succes('L’absence a ete enregistree.')
    absence.value.motif = ''
    absence.value.justificatif = ''
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function traiter(item, valider) {
  try {
    await (valider ? api.valider(item.id) : api.refuser(item.id, { motif: 'Refusee par le validateur.' }))
    notifications.succes(valider ? 'Demande validee.' : 'Demande refusee.')
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

onMounted(async () => {
  await initialiser()
  if (employeId.value) await charger()
})
</script>

<template>
  <div class="flex flex-col gap-5">
    <div>
      <h1>Congés et absences</h1>
      <p class="mt-1 text-sm text-muted-color">Demandes, soldes, validations et justificatifs.</p>
    </div>

    <section v-if="!auth.estEmploye" class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="flex flex-wrap items-end gap-3">
        <div class="flex flex-col gap-2">
          <label for="conge-employe" class="text-sm font-semibold text-muted-color">Identifiant employe</label>
          <PrimeInputText id="conge-employe" v-model="employeId" placeholder="Ex. 1" @keyup.enter="charger" />
        </div>
        <PrimeButton label="Charger" icon="pi pi-search" @click="charger" />
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="mb-3">Soldes de congés</h2>
      <DataTable :value="soldes" :loading="chargement" striped-rows>
        <Column field="typeConge" header="Type" />
        <Column field="joursAcquis" header="Acquis" />
        <Column field="joursPris" header="Pris" />
        <Column field="disponible" header="Disponible" />
      </DataTable>
    </section>

    <div class="grid gap-5 lg:grid-cols-2">
      <section v-if="!auth.estEmploye" class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <h2 class="mb-3">Déclarer une demande de congé</h2>
        <div class="grid gap-3">
          <PrimeSelect v-model="demande.idTypeConge" :options="types" option-label="libelle" option-value="id" placeholder="Type de congé" />
          <PrimeInputText v-model="demande.motif" placeholder="Motif de la demande" />
          <div class="grid gap-3 sm:grid-cols-2">
            <PrimeInputText v-model="demande.dateDebut" type="date" />
            <PrimeInputText v-model="demande.dateFin" type="date" />
          </div>
          <PrimeButton label="Envoyer la demande" icon="pi pi-send" @click="creerDemande" />
        </div>
      </section>

      <section v-if="!auth.estEmploye" class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <h2 class="mb-3">Déclarer une absence</h2>
        <div class="grid gap-3">
          <PrimeInputText v-model="absence.motif" placeholder="Motif de l’absence" />
          <div class="grid gap-3 sm:grid-cols-2">
            <PrimeInputText v-model="absence.dateDebut" type="date" />
            <PrimeInputText v-model="absence.dateFin" type="date" />
          </div>
          <PrimeInputText v-model="absence.justificatif" placeholder="Justificatif ou référence" />
          <PrimeButton label="Enregistrer l’absence" icon="pi pi-calendar-times" severity="secondary" @click="creerAbsence" />
        </div>
      </section>
    </div>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="mb-3">Demandes de congé</h2>
      <DataTable :value="demandes" :loading="chargement" striped-rows paginator :rows="8" responsive-layout="scroll">
        <Column field="idEmploye" header="Employe" />
        <Column field="typeConge" header="Type" />
        <Column header="Période"><template #body="{ data }">{{ data.dateDebut }} → {{ data.dateFin }}</template></Column>
        <Column field="nombreJours" header="Jours" />
        <Column field="statut" header="Statut" />
        <Column header="Actions">
          <template #body="{ data }">
            <div v-if="data.statut === 'EN_ATTENTE'" class="flex gap-2">
              <PrimeButton icon="pi pi-check" severity="success" text rounded aria-label="Valider" @click="traiter(data, true)" />
              <PrimeButton icon="pi pi-times" severity="danger" text rounded aria-label="Refuser" @click="traiter(data, false)" />
            </div>
          </template>
        </Column>
      </DataTable>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="mb-3">Absences</h2>
      <DataTable :value="absences" :loading="chargement" striped-rows>
        <Column field="idEmploye" header="Employe" />
        <Column header="Période"><template #body="{ data }">{{ data.dateDebut }} → {{ data.dateFin }}</template></Column>
        <Column field="motif" header="Motif" />
        <Column field="statut" header="Statut" />
      </DataTable>
    </section>
  </div>
</template>
