<script setup>
import { onMounted, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeSelect from 'primevue/select'
import { congesApi as api } from '@/modules/conges/api/congesApi'
import StatusBadge from '@/shared/components/StatusBadge.vue'
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
const planning = ref([])
const periodesBloquees = ref([])
const periode = ref({
  libelle: '',
  dateDebut: new Date().toISOString().slice(0, 10),
  dateFin: new Date().toISOString().slice(0, 10),
  derogationDirection: false,
})
const demande = ref({
  idTypeConge: null,
  dateDebut: new Date().toISOString().slice(0, 10),
  dateFin: new Date().toISOString().slice(0, 10),
  motif: '',
  idRemplacant: '',
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
  const id = auth.estEmploye ? Number(employeId.value) : idRequis()
  if (!id) return
  chargement.value = true
  try {
    localStorage.setItem('erp.conges.employeId', String(id))
    const [soldesEmploye, listeDemandes, listeAbsences, listePlanning] = await Promise.all([
      api.soldes(id, new Date().getFullYear()),
      auth.estEmploye ? api.demandesEmploye(id) : api.demandes(),
      auth.estEmploye ? api.mesAbsences() : api.absences(),
      auth.estEmploye ? Promise.resolve([]) : api.planning(),
    ])
    soldes.value = soldesEmploye
    demandes.value = listeDemandes
    absences.value = listeAbsences
    planning.value = listePlanning
    periodesBloquees.value = auth.estEmploye ? [] : await api.periodesBloquees()
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

async function initialiser() {
  try {
    if (auth.estEmploye) {
      const courant = await api.employeCourant()
      employeId.value = String(courant.idEmploye)
      localStorage.setItem('erp.conges.employeId', employeId.value)
    }
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
    await api.creerDemande({
      ...demande.value,
      idEmploye: id,
      idRemplacant: demande.value.idRemplacant ? Number(demande.value.idRemplacant) : null,
    })
    notifications.succes('La demande de congé a ete envoyee au validateur.')
    demande.value.motif = ''
    demande.value.idRemplacant = ''
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

async function creerPeriodeBloquee() {
  try {
    await api.creerPeriodeBloquee(periode.value)
    notifications.succes('La periode bloquee a ete enregistree.')
    periode.value.libelle = ''
    periodesBloquees.value = await api.periodesBloquees()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function supprimerPeriodeBloquee(item) {
  try {
    await api.supprimerPeriodeBloquee(item.id)
    periodesBloquees.value = await api.periodesBloquees()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function proposerSuppleance(item) {
  try {
    const suppleance = await api.proposerSuppleance(item.id, { motif: 'Proposition automatique FIFO' })
    notifications.succes(`Remplacant propose : employe ${suppleance.idEmployeRemplacant}.`)
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

function peutTraiter(item) {
  if (auth.estManager) return item.statut === 'SOUMISE'
  if (auth.estRh) return item.statut === 'VALIDEE_MANAGER'
  if (auth.estDirection) return item.statut === 'VALIDEE_RH'
  return false
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
        <Column field="joursReserves" header="Réservés" />
        <Column field="disponible" header="Disponible" />
        <Column header="Alerte">
          <template #body="{ data }">
            <span v-if="data.alerte" class="text-sm font-semibold text-orange-500">À planifier</span>
          </template>
        </Column>
      </DataTable>
    </section>

    <div class="grid gap-5 lg:grid-cols-2">
      <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <h2 class="mb-3">Déclarer une demande de congé</h2>
        <div class="grid gap-3">
          <PrimeSelect v-model="demande.idTypeConge" :options="types" option-label="libelle" option-value="id" placeholder="Type de congé" />
          <PrimeInputText v-model="demande.motif" placeholder="Motif de la demande" />
          <div class="grid gap-3 sm:grid-cols-2">
            <PrimeInputText v-model="demande.dateDebut" type="date" />
            <PrimeInputText v-model="demande.dateFin" type="date" />
          </div>
          <PrimeInputText v-model="demande.idRemplacant" placeholder="ID du remplacant (si effectif minimum)" inputmode="numeric" />
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
        <Column v-if="!auth.estEmploye" field="idEmploye" header="Employe" />
        <Column field="typeConge" header="Type" />
        <Column header="Période"><template #body="{ data }">{{ data.dateDebut }} → {{ data.dateFin }}</template></Column>
        <Column field="nombreJours" header="Jours" />
        <Column header="Statut"><template #body="{ data }"><StatusBadge :valeur="data.statut" /></template></Column>
        <Column v-if="!auth.estEmploye" header="Actions">
          <template #body="{ data }">
            <div v-if="peutTraiter(data)" class="flex gap-2">
              <PrimeButton icon="pi pi-check" severity="success" text rounded aria-label="Valider" @click="traiter(data, true)" />
              <PrimeButton icon="pi pi-times" severity="danger" text rounded aria-label="Refuser" @click="traiter(data, false)" />
              <PrimeButton icon="pi pi-user-plus" severity="secondary" text rounded aria-label="Proposer un remplacant" @click="proposerSuppleance(data)" />
            </div>
          </template>
        </Column>
      </DataTable>
    </section>

    <section v-if="!auth.estEmploye" class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="mb-3">Périodes bloquées</h2>
      <div class="grid gap-3 md:grid-cols-5">
        <PrimeInputText v-model="periode.libelle" placeholder="Clôture annuelle, période critique..." />
        <PrimeInputText v-model="periode.dateDebut" type="date" />
        <PrimeInputText v-model="periode.dateFin" type="date" />
        <label class="flex items-center gap-2 text-sm"><input v-model="periode.derogationDirection" type="checkbox" /> Dérogation Direction</label>
        <PrimeButton label="Ajouter" icon="pi pi-plus" @click="creerPeriodeBloquee" />
      </div>
      <DataTable :value="periodesBloquees" class="mt-4" striped-rows>
        <Column field="libelle" header="Libellé" />
        <Column header="Période"><template #body="{ data }">{{ data.dateDebut }} → {{ data.dateFin }}</template></Column>
        <Column header="Dérogation"><template #body="{ data }">{{ data.derogationDirection ? 'Direction' : 'Non' }}</template></Column>
        <Column header=""><template #body="{ data }"><PrimeButton icon="pi pi-trash" severity="danger" text rounded aria-label="Supprimer" @click="supprimerPeriodeBloquee(data)" /></template></Column>
      </DataTable>
    </section>

    <section v-if="!auth.estEmploye" class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="mb-3">Planning des congés validés</h2>
      <DataTable :value="planning" :loading="chargement" striped-rows paginator :rows="8" responsive-layout="scroll">
        <Column field="idEmploye" header="Employe" />
        <Column field="typeConge" header="Type" />
        <Column header="Période"><template #body="{ data }">{{ data.dateDebut }} → {{ data.dateFin }}</template></Column>
        <Column field="jours" header="Jours" />
        <Column field="motif" header="Motif" />
      </DataTable>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="mb-3">Absences</h2>
      <DataTable :value="absences" :loading="chargement" striped-rows>
        <Column v-if="!auth.estEmploye" field="idEmploye" header="Employe" />
        <Column header="Période"><template #body="{ data }">{{ data.dateDebut }} → {{ data.dateFin }}</template></Column>
        <Column field="motif" header="Motif" />
        <Column header="Statut"><template #body="{ data }"><StatusBadge :valeur="data.statut" /></template></Column>
      </DataTable>
    </section>
  </div>
</template>
