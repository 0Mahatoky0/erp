<script setup>
import { onMounted, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import PrimeSelect from 'primevue/select'
import PrimeInputNumber from 'primevue/inputnumber'
import Tag from 'primevue/tag'
import { evaluationApi as api } from '@/modules/evaluation/api/evaluationApi'
import { employeApi } from '@/modules/employe/api/employeApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const notifications = useNotificationStore()
const aujourdHui = new Date().toISOString().slice(0, 10)
const annee = new Date().getFullYear()
const formations = ref([])
const sessions = ref([])
const plans = ref([])
const employes = ref([])
const inscriptions = ref([])
const indicateurs = ref(null)
const sessionCourante = ref(null)
const inscriptionCourante = ref(null)
const chargement = ref(false)
const statutsSession = ['PREVUE', 'EN_COURS', 'REALISEE', 'ANNULEE']

const formationForm = ref({
  code: '', titre: '', type: 'TECHNIQUE', organisme: '', dureeH: 7, cout: 0,
  certifiante: false, competencesVisees: '', seuilDedit: 0,
})

const sessionForm = ref({
  idFormation: null, dateDebut: aujourdHui, dateFin: aujourdHui,
  lieu: '', capacite: 10, formateur: '', cout: 0,
})

const planForm = ref({ annee, idDepartement: null, budget: 0 })
const inscriptionForm = ref({ idSession: null, idEmploye: null, idBesoin: null })
const presenceForm = ref({ presenceH: 7, resultat: 'REUSSI', certificatDocumentId: null })
const evaluationForm = ref({ type: 'A_CHAUD', satisfaction: 4, acquis: 4, transfert: 4, commentaire: '' })
const conventionForm = ref({ cout: 0, dureeEngagementMois: 12, clauseDedit: '' })

async function charger() {
  chargement.value = true
  try {
    const [formationsResultat, sessionsResultat, plansResultat, employesResultat, indicateursResultat] =
      await Promise.all([
        api.formations(),
        api.sessions(),
        api.plans(annee),
        employeApi.get('/referentiel/employes'),
        api.indicateurs(),
      ])
    formations.value = formationsResultat
    sessions.value = sessionsResultat
    plans.value = plansResultat
    employes.value = employesResultat
    indicateurs.value = indicateursResultat
    if (!sessionForm.value.idFormation && formations.value.length) {
      sessionForm.value.idFormation = formations.value[0].id
    }
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

async function chargerInscriptions() {
  if (!sessionCourante.value) {
    inscriptions.value = []
    return
  }
  try {
    inscriptions.value = await api.inscriptionsSession(sessionCourante.value.id)
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function selectionnerSession(session) {
  sessionCourante.value = session
  inscriptionForm.value.idSession = session.id
  await chargerInscriptions()
}

function selectionnerInscription(inscription) {
  inscriptionCourante.value = inscription
  conventionForm.value.cout = Number(sessionCourante.value?.cout || 0)
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

function creerFormation() {
  action(api.creerFormation({
    ...formationForm.value,
    dureeH: Number(formationForm.value.dureeH),
    cout: Number(formationForm.value.cout),
    seuilDedit: Number(formationForm.value.seuilDedit),
  }), 'Formation créée.', charger)
}

function creerSession() {
  action(api.creerSession({
    ...sessionForm.value,
    capacite: Number(sessionForm.value.capacite),
    cout: Number(sessionForm.value.cout),
  }), 'Session créée.', charger)
}

function enregistrerPlan() {
  action(api.enregistrerPlan({
    ...planForm.value,
    budget: Number(planForm.value.budget),
  }), 'Plan de formation enregistré.', charger)
}

function severity(statut) {
  if (['CONFIRMEE', 'PRESENT', 'REALISEE', 'VALIDE'].includes(statut)) return 'success'
  if (['ANNULEE', 'LISTE_ATTENTE', 'REFUSEE'].includes(statut)) return 'danger'
  if (['VALIDEE_MANAGER', 'EN_COURS', 'PREVUE'].includes(statut)) return 'info'
  return 'warn'
}

onMounted(charger)
</script>

<template>
  <div class="flex flex-col gap-5">
    <div>
      <h1>Planification des formations</h1>
      <p class="mt-1 text-sm text-muted-color">Catalogue, sessions, budget, inscriptions et indicateurs.</p>
    </div>

    <section v-if="indicateurs" class="grid gap-3 md:grid-cols-3 xl:grid-cols-6">
      <div class="rounded-lg border border-surface bg-surface-0 p-3 shadow-sm">
        <span class="text-xs text-muted-color">Participation</span>
        <strong class="block text-lg">{{ indicateurs.tauxParticipation }}%</strong>
      </div>
      <div class="rounded-lg border border-surface bg-surface-0 p-3 shadow-sm">
        <span class="text-xs text-muted-color">Heures / salarié</span>
        <strong class="block text-lg">{{ indicateurs.heuresFormationParSalarie }}</strong>
      </div>
      <div class="rounded-lg border border-surface bg-surface-0 p-3 shadow-sm">
        <span class="text-xs text-muted-color">Budget consommé</span>
        <strong class="block text-lg">{{ indicateurs.budgetConsomme }}</strong>
      </div>
      <div class="rounded-lg border border-surface bg-surface-0 p-3 shadow-sm">
        <span class="text-xs text-muted-color">Réalisation plan</span>
        <strong class="block text-lg">{{ indicateurs.tauxRealisationPlan }}%</strong>
      </div>
      <div class="rounded-lg border border-surface bg-surface-0 p-3 shadow-sm">
        <span class="text-xs text-muted-color">Comblement</span>
        <strong class="block text-lg">{{ indicateurs.tauxComblement }}%</strong>
      </div>
      <div class="rounded-lg border border-surface bg-surface-0 p-3 shadow-sm">
        <span class="text-xs text-muted-color">Sessions</span>
        <strong class="block text-lg">{{ sessions.length }}</strong>
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2>Catalogue</h2>
      <div class="mt-3 grid gap-3 md:grid-cols-4">
        <PrimeInputText v-model="formationForm.code" placeholder="Code" />
        <PrimeInputText v-model="formationForm.titre" placeholder="Titre" />
        <PrimeInputText v-model="formationForm.type" placeholder="Type" />
        <PrimeInputText v-model="formationForm.organisme" placeholder="Organisme" />
        <PrimeInputNumber v-model="formationForm.dureeH" placeholder="Durée (h)" />
        <PrimeInputNumber v-model="formationForm.cout" placeholder="Coût" />
        <PrimeInputNumber v-model="formationForm.seuilDedit" placeholder="Seuil dédit" />
        <PrimeInputText v-model="formationForm.competencesVisees" placeholder='[{"competence":1,"niveau":3}]' />
      </div>
      <PrimeButton label="Ajouter au catalogue" icon="pi pi-plus" class="mt-3" @click="creerFormation" />
      <DataTable :value="formations" :loading="chargement" striped-rows class="mt-4">
        <Column field="code" header="Code" />
        <Column field="titre" header="Titre" />
        <Column field="type" header="Type" />
        <Column field="dureeH" header="Heures" />
        <Column field="cout" header="Coût" />
        <Column field="certifiante" header="Certifiante" />
      </DataTable>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2>Sessions</h2>
      <div class="mt-3 grid gap-3 md:grid-cols-4">
        <PrimeSelect v-model="sessionForm.idFormation" :options="formations" option-label="titre" option-value="id"
          class="w-full" placeholder="Formation" />
        <PrimeInputText v-model="sessionForm.dateDebut" placeholder="Début AAAA-MM-JJ" />
        <PrimeInputText v-model="sessionForm.dateFin" placeholder="Fin AAAA-MM-JJ" />
        <PrimeInputText v-model="sessionForm.lieu" placeholder="Lieu" />
        <PrimeInputNumber v-model="sessionForm.capacite" :min="1" placeholder="Capacité" />
        <PrimeInputText v-model="sessionForm.formateur" placeholder="Formateur" />
        <PrimeInputNumber v-model="sessionForm.cout" placeholder="Coût" />
        <PrimeButton label="Créer la session" icon="pi pi-plus" @click="creerSession" />
      </div>
      <DataTable :value="sessions" striped-rows class="mt-4" selection-mode="single" :selection="sessionCourante"
        @update:selection="selectionnerSession">
        <Column field="id" header="#" />
        <Column field="dateDebut" header="Début" />
        <Column field="dateFin" header="Fin" />
        <Column field="lieu" header="Lieu" />
        <Column field="capacite" header="Capacité" />
        <Column field="confirms" header="Confirmés" />
        <Column field="statut" header="Statut">
          <template #body="{ data }"><Tag :value="data.statut" :severity="severity(data.statut)" /></template>
        </Column>
        <Column header="Statut">
          <template #body="{ data }">
            <PrimeSelect :model-value="data.statut" :options="statutsSession" class="w-36"
              @update:model-value="action(api.changerStatutSession(data.id, $event), 'Statut mis à jour.', charger)" />
          </template>
        </Column>
      </DataTable>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2>Budget par département</h2>
      <div class="mt-3 grid gap-3 md:grid-cols-4">
        <PrimeInputNumber v-model="planForm.annee" placeholder="Année" />
        <PrimeSelect v-model="planForm.idDepartement" :options="employes" option-label="departementNom"
          option-value="departementId" class="w-full" placeholder="Département" />
        <PrimeInputNumber v-model="planForm.budget" placeholder="Budget" />
        <PrimeButton label="Enregistrer" icon="pi pi-save" @click="enregistrerPlan" />
      </div>
      <DataTable :value="plans" striped-rows class="mt-4">
        <Column field="annee" header="Année" />
        <Column field="idDepartement" header="Département" />
        <Column field="budget" header="Budget" />
      </DataTable>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2>Inscriptions</h2>
      <div class="mt-3 grid gap-3 md:grid-cols-4">
        <PrimeSelect v-model="inscriptionForm.idSession" :options="sessions" option-label="id" option-value="id"
          class="w-full" placeholder="Session" />
        <PrimeSelect v-model="inscriptionForm.idEmploye" :options="employes" option-label="nom" option-value="id"
          class="w-full" placeholder="Employé" />
        <PrimeInputNumber v-model="inscriptionForm.idBesoin" placeholder="Besoin (facultatif)" />
        <PrimeButton label="Demander une inscription" icon="pi pi-plus"
          @click="action(api.demanderInscription(inscriptionForm), 'Demande enregistrée.', chargerInscriptions)" />
      </div>
      <DataTable :value="inscriptions" striped-rows class="mt-4" selection-mode="single" :selection="inscriptionCourante"
        @update:selection="selectionnerInscription">
        <Column field="id" header="#" />
        <Column field="idEmploye" header="Employé" />
        <Column field="dateDemande" header="Demande" />
        <Column field="statut" header="Statut">
          <template #body="{ data }"><Tag :value="data.statut" :severity="severity(data.statut)" /></template>
        </Column>
        <Column field="presenceH" header="Présence" />
        <Column header="Actions">
          <template #body="{ data }">
            <div class="flex gap-1">
              <PrimeButton icon="pi pi-check" text rounded aria-label="Valider manager"
                @click="action(api.deciderInscriptionManager(data.id, { approuver: true, motif: '' }), 'Demande validée.', chargerInscriptions)" />
              <PrimeButton icon="pi pi-calendar-plus" text rounded aria-label="Confirmer"
                @click="action(api.confirmerInscription(data.id), 'Inscription confirmée.', chargerInscriptions)" />
              <PrimeButton icon="pi pi-ban" text rounded aria-label="Annuler"
                @click="action(api.annulerInscription(data.id), 'Inscription annulée.', chargerInscriptions)" />
            </div>
          </template>
        </Column>
      </DataTable>
    </section>

    <section v-if="inscriptionCourante" class="grid gap-5 lg:grid-cols-3">
      <div class="flex flex-col gap-3 rounded-lg border border-surface bg-surface-0 p-4 shadow-sm">
        <h3>Présence</h3>
        <PrimeInputNumber v-model="presenceForm.presenceH" placeholder="Heures" />
        <PrimeInputText v-model="presenceForm.resultat" placeholder="Résultat" />
        <PrimeButton label="Enregistrer" @click="action(api.enregistrerPresence(inscriptionCourante.id, presenceForm), 'Présence enregistrée.', chargerInscriptions)" />
      </div>
      <div class="flex flex-col gap-3 rounded-lg border border-surface bg-surface-0 p-4 shadow-sm">
        <h3>Évaluation formation</h3>
        <PrimeInputText v-model="evaluationForm.type" placeholder="Type" />
        <PrimeInputNumber v-model="evaluationForm.satisfaction" :min="1" :max="5" placeholder="Satisfaction" />
        <PrimeInputNumber v-model="evaluationForm.acquis" :min="0" :max="5" placeholder="Acquis" />
        <PrimeInputNumber v-model="evaluationForm.transfert" :min="0" :max="5" placeholder="Transfert" />
        <PrimeTextarea v-model="evaluationForm.commentaire" rows="2" placeholder="Commentaire" />
        <PrimeButton label="Évaluer" @click="action(api.evaluerFormation(inscriptionCourante.id, evaluationForm), 'Évaluation enregistrée.', chargerInscriptions)" />
      </div>
      <div class="flex flex-col gap-3 rounded-lg border border-surface bg-surface-0 p-4 shadow-sm">
        <h3>Convention de formation</h3>
        <PrimeInputNumber v-model="conventionForm.cout" placeholder="Coût" />
        <PrimeInputNumber v-model="conventionForm.dureeEngagementMois" placeholder="Engagement (mois)" />
        <PrimeTextarea v-model="conventionForm.clauseDedit" rows="2" placeholder="Clause de dédit" />
        <PrimeButton label="Enregistrer la convention" @click="action(api.enregistrerConvention(inscriptionCourante.id, conventionForm), 'Convention enregistrée.')" />
      </div>
    </section>
  </div>
</template>
