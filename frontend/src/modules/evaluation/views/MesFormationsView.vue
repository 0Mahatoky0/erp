<script setup>
import { onMounted, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import PrimeInputNumber from 'primevue/inputnumber'
import Tag from 'primevue/tag'
import { evaluationApi as api } from '@/modules/evaluation/api/evaluationApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const notifications = useNotificationStore()
const aujourdHui = new Date().toISOString().slice(0, 10)
const formations = ref([])
const sessions = ref([])
const inscriptions = ref([])
const sessionCourante = ref(null)
const inscriptionCourante = ref(null)
const idBesoin = ref(null)
const chargement = ref(false)
const evaluationForm = ref({ type: 'A_CHAUD', satisfaction: 4, acquis: 4, transfert: 4, commentaire: '' })
const mesureForm = ref({
  idCompetence: null,
  niveauAcquis: 3,
  dateEvaluation: aujourdHui,
  source: 'FORMATION',
  justificatif: '',
})

async function charger() {
  chargement.value = true
  try {
    const [formationsResultat, sessionsResultat, inscriptionsResultat] = await Promise.all([
      api.formations(),
      api.sessions(),
      api.mesInscriptions(),
    ])
    formations.value = formationsResultat
    sessions.value = sessionsResultat
    inscriptions.value = inscriptionsResultat
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

function selectionnerInscription(inscription) {
  inscriptionCourante.value = inscription
}

async function action(promise, message, apres) {
  try {
    await promise
    notifications.succes(message)
    if (apres) await apres()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

function severity(statut) {
  if (['CONFIRMEE', 'PRESENT', 'REUSSI'].includes(statut)) return 'success'
  if (['ANNULEE', 'LISTE_ATTENTE'].includes(statut)) return 'danger'
  if (['VALIDEE_MANAGER', 'PREVUE'].includes(statut)) return 'info'
  return 'warn'
}

onMounted(charger)
</script>

<template>
  <div class="flex flex-col gap-5">
    <div>
      <h1>Mes formations</h1>
      <p class="mt-1 text-sm text-muted-color">Catalogue, demandes d'inscription et suivi des acquis.</p>
    </div>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2>Sessions disponibles</h2>
      <DataTable :value="sessions" :loading="chargement" striped-rows class="mt-3" selection-mode="single"
        :selection="sessionCourante" @update:selection="sessionCourante = $event">
        <Column field="id" header="#" />
        <Column field="idFormation" header="Formation" />
        <Column field="dateDebut" header="Début" />
        <Column field="dateFin" header="Fin" />
        <Column field="lieu" header="Lieu" />
        <Column field="capacite" header="Capacité" />
        <Column field="statut" header="Statut">
          <template #body="{ data }"><Tag :value="data.statut" :severity="severity(data.statut)" /></template>
        </Column>
      </DataTable>
      <div class="mt-3 flex flex-wrap items-end gap-3">
        <div class="flex flex-col gap-2">
          <label class="text-sm font-semibold">Besoin (facultatif)</label>
          <PrimeInputNumber v-model="idBesoin" placeholder="ID besoin" />
        </div>
        <PrimeButton label="Demander mon inscription" icon="pi pi-plus"
          :disabled="!sessionCourante"
          @click="action(api.demanderInscriptionPourMoi(sessionCourante.id, idBesoin), 'Demande envoyée.', charger)" />
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2>Mes inscriptions</h2>
      <DataTable :value="inscriptions" striped-rows class="mt-3" selection-mode="single" :selection="inscriptionCourante"
        @update:selection="selectionnerInscription">
        <Column field="id" header="#" />
        <Column field="idSession" header="Session" />
        <Column field="dateDemande" header="Demande" />
        <Column field="statut" header="Statut">
          <template #body="{ data }"><Tag :value="data.statut" :severity="severity(data.statut)" /></template>
        </Column>
        <Column field="presenceH" header="Présence" />
        <Column field="resultat" header="Résultat" />
        <Column header="Annuler">
          <template #body="{ data }">
            <PrimeButton icon="pi pi-ban" text rounded aria-label="Annuler"
              @click="action(api.annulerInscription(data.id), 'Inscription annulée.', charger)" />
          </template>
        </Column>
      </DataTable>
    </section>

    <section v-if="inscriptionCourante" class="grid gap-5 lg:grid-cols-2">
      <div class="flex flex-col gap-3 rounded-lg border border-surface bg-surface-0 p-4 shadow-sm">
        <h3>Évaluer la formation</h3>
        <PrimeInputText v-model="evaluationForm.type" placeholder="Type" />
        <PrimeInputNumber v-model="evaluationForm.satisfaction" :min="1" :max="5" placeholder="Satisfaction" />
        <PrimeInputNumber v-model="evaluationForm.acquis" :min="0" :max="5" placeholder="Acquis" />
        <PrimeInputNumber v-model="evaluationForm.transfert" :min="0" :max="5" placeholder="Transfert" />
        <PrimeTextarea v-model="evaluationForm.commentaire" rows="3" placeholder="Commentaire" />
        <PrimeButton label="Envoyer l'évaluation" @click="action(api.evaluerFormation(inscriptionCourante.id, evaluationForm), 'Évaluation envoyée.', charger)" />
      </div>
      <div class="flex flex-col gap-3 rounded-lg border border-surface bg-surface-0 p-4 shadow-sm">
        <h3>Mesure à 90 jours</h3>
        <PrimeInputNumber v-model="mesureForm.idCompetence" placeholder="Compétence" />
        <PrimeInputNumber v-model="mesureForm.niveauAcquis" :min="1" :max="5" placeholder="Niveau acquis" />
        <PrimeInputText v-model="mesureForm.dateEvaluation" placeholder="AAAA-MM-JJ" />
        <PrimeInputText v-model="mesureForm.source" placeholder="Source" />
        <PrimeTextarea v-model="mesureForm.justificatif" rows="2" placeholder="Justificatif" />
        <PrimeButton label="Enregistrer la mesure" @click="action(api.mesurer90Jours(inscriptionCourante.id, {
          ...mesureForm,
          idEmploye: inscriptionCourante.idEmploye,
          formationId: null,
          evaluationId: null,
          valideParId: null,
        }), 'Mesure enregistrée.')" />
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2>Catalogue</h2>
      <DataTable :value="formations" striped-rows class="mt-3">
        <Column field="code" header="Code" />
        <Column field="titre" header="Titre" />
        <Column field="type" header="Type" />
        <Column field="organisme" header="Organisme" />
        <Column field="dureeH" header="Heures" />
        <Column field="certifiante" header="Certifiante" />
      </DataTable>
    </section>
  </div>
</template>
