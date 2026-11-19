<script setup>
import { onMounted, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import PrimeSelect from 'primevue/select'
import PrimeInputNumber from 'primevue/inputnumber'
import { disciplineApi as api } from '@/modules/discipline/api/disciplineApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const notifications = useNotificationStore()
const aujourdHui = new Date().toISOString().slice(0, 10)
const debut = ref(`${aujourdHui.slice(0, 4)}-01-01`)
const fin = ref(aujourdHui)
const motif = ref('Instruction disciplinaire')
const employes = ref([])
const incidents = ref([])
const procedures = ref([])
const sanctions = ref([])
const absences = ref([])
const indicateurs = ref(null)
const journal = ref([])
const pieces = ref([])
const chargement = ref(false)

const incidentForm = ref({
  dateFaits: aujourdHui,
  dateConnaissance: aujourdHui,
  lieu: '',
  description: '',
  categorie: 'RETARDS_REPETES',
  graviteInitiale: 'LEGERE',
  idEmploye: null,
})

const qualificationForm = ref({
  idIncident: null,
  graviteRetenue: 'LEGERE',
  ouvrirProcedure: true,
  motif: '',
  idEmploye: null,
  idInstructeur: null,
  idDecideur: null,
  miseAPied: false,
  miseAPiedDebut: aujourdHui,
  miseAPiedFin: aujourdHui,
  derogationPrescription: false,
})

const procedureForm = ref({ idProcedure: null })
const etapeForm = ref({
  type: 'DEMANDE_EXPLICATION',
  modeRemise: 'EMAIL',
  preuve: '',
  delaiReponseJours: 5,
  idDocument: null,
  commentaire: '',
})
const decisionForm = ref({ decision: '', motivation: '', idPiece: null, entretienRealise: false })
const notificationForm = ref({
  type: 'AVERTISSEMENT_ECRIT',
  dateEffet: aujourdHui,
  dureeJours: null,
  motif: '',
  baseReglement: '',
  modeRemise: 'EMAIL',
  preuve: '',
})
const pieceForm = ref({ idIncident: null, idProcedure: null, type: 'PREUVE', fichier: '', hashSha256: '' })

const categories = [
  'RETARDS_REPETES', 'ABSENCE_INJUSTIFIEE', 'INSUBORDINATION', 'NON_RESPECT_SECURITE',
  'VOL', 'FRAUDE', 'HARCELEMENT', 'VIOLENCE', 'ABUS_CONFIANCE', 'ABANDON_POSTE', 'AUTRE',
]
const gravités = ['LEGERE', 'MOYENNE', 'GRAVE', 'TRES_GRAVE']
const typesEtape = [
  'DEMANDE_EXPLICATION', 'REPONSE', 'CONVOCATION', 'ENTRETIEN', 'DELIBERATION',
]
const typesSanction = [
  'AVERTISSEMENT_VERBAL', 'AVERTISSEMENT_ECRIT', 'BLAME', 'MISE_A_PIED',
  'RETROGRADATION', 'LICENCIEMENT_DISCIPLINAIRE',
]
const typesPiece = [
  'RAPPORT_INCIDENT', 'TEMOIGNAGE', 'PREUVE', 'DEMANDE_EXPLICATION', 'REPONSE',
  'COMPTE_RENDU_ENTRETIEN', 'DECISION', 'NOTIFICATION', 'RECOURS',
]

const nombre = (valeur) =>
  valeur === '' || valeur === null || valeur === undefined ? null : Number(valeur)

async function action(promise, message, apres) {
  try {
    const resultat = await promise
    notifications.succes(message)
    if (apres) await apres(resultat)
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function charger() {
  chargement.value = true
  try {
    const [employesResultat, incidentsResultat, proceduresResultat, sanctionsResultat,
      absencesResultat, indicateursResultat, journalResultat] = await Promise.all([
      api.employes(),
      api.incidents(motif.value),
      api.procedures(),
      api.sanctions(),
      api.absences(),
      api.indicateurs(debut.value, fin.value),
      api.journal(motif.value),
    ])
    employes.value = employesResultat
    incidents.value = incidentsResultat
    procedures.value = proceduresResultat
    sanctions.value = sanctionsResultat
    absences.value = absencesResultat
    indicateurs.value = indicateursResultat
    journal.value = journalResultat
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

function declarerIncident() {
  const f = incidentForm.value
  const donnees = {
    dateFaits: f.dateFaits,
    dateConnaissance: f.dateConnaissance,
    lieu: f.lieu || null,
    description: f.description,
    categorie: f.categorie,
    graviteInitiale: f.graviteInitiale,
    employesConcernes: f.idEmploye ? [Number(f.idEmploye)] : [],
    temoins: [],
  }
  action(api.declarerIncident(donnees, motif.value), 'Incident déclaré.', charger)
}

function qualifierIncident() {
  const f = qualificationForm.value
  const donnees = {
    graviteRetenue: f.graviteRetenue,
    ouvrirProcedure: f.ouvrirProcedure,
    motif: f.motif || motif.value,
    employesConcernes: f.idEmploye ? [Number(f.idEmploye)] : [],
    idInstructeur: nombre(f.idInstructeur),
    idDecideur: nombre(f.idDecideur),
    miseAPied: f.miseAPied,
    miseAPiedDebut: f.miseAPied ? f.miseAPiedDebut : null,
    miseAPiedFin: f.miseAPied ? f.miseAPiedFin : null,
    derogationPrescription: f.derogationPrescription,
  }
  action(api.qualifier(f.idIncident, donnees), 'Incident qualifié.', charger)
}

function etape(type) {
  const f = etapeForm.value
  action(
    api.etape(procedureForm.value.idProcedure, type, {
      type: f.type,
      modeRemise: f.modeRemise,
      preuve: f.preuve,
      delaiReponseJours: nombre(f.delaiReponseJours),
      idDocument: nombre(f.idDocument),
      commentaire: f.commentaire || null,
    }, motif.value),
    'Étape enregistrée.',
    charger,
  )
}

function simpleAction(type) {
  action(api.action(procedureForm.value.idProcedure, type, motif.value), 'Procédure mise à jour.', charger)
}

function rendreDecision() {
  const f = decisionForm.value
  action(api.decision(procedureForm.value.idProcedure, {
    decision: f.decision,
    motivation: f.motivation,
    piecesReferencees: f.idPiece ? [Number(f.idPiece)] : [],
    entretienRealise: f.entretienRealise,
  }, motif.value), 'Décision enregistrée.', charger)
}

function notifierSanction() {
  const f = notificationForm.value
  action(api.notification(procedureForm.value.idProcedure, {
    ...f,
    dureeJours: nombre(f.dureeJours),
  }, motif.value), 'Sanction notifiée.', charger)
}

function contester() {
  action(api.recours(procedureForm.value.idProcedure, { motif: motif.value }, motif.value),
    'Recours enregistré.', charger)
}

function chargerPieces() {
  const idIncident = pieceForm.value.idIncident
  const idProcedure = pieceForm.value.idProcedure
  const promesse = idProcedure
    ? api.piecesProcedure(idProcedure, motif.value)
    : idIncident
      ? api.piecesIncident(idIncident, motif.value)
      : Promise.resolve([])
  action(promesse, 'Pièces chargées.', (resultat) => { pieces.value = resultat })
}

function ajouterPiece() {
  const f = pieceForm.value
  action(api.ajouterPiece({
    type: f.type,
    fichier: f.fichier,
    hashSha256: f.hashSha256,
    idIncident: nombre(f.idIncident),
    idProcedure: nombre(f.idProcedure),
  }, motif.value), 'Pièce ajoutée.', chargerPieces)
}

function chargerIndicateurs() {
  action(api.indicateurs(debut.value, fin.value), 'Indicateurs chargés.',
    (resultat) => { indicateurs.value = resultat })
}

onMounted(charger)
</script>

<template>
  <div class="flex flex-col gap-5">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold">Discipline et procédures</h1>
        <p class="text-sm text-surface-500">Incidents, contradictoire, sanctions et accès nominatifs.</p>
      </div>
      <div class="flex items-center gap-2">
        <PrimeInputText v-model="motif" class="w-64" placeholder="Motif de consultation" />
        <PrimeButton icon="pi pi-refresh" label="Actualiser" outlined :loading="chargement" @click="charger" />
      </div>
    </div>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="text-lg font-semibold">Déclarer un incident</h2>
      <p class="text-xs text-surface-500">Décrivez uniquement des faits objectifs.</p>
      <div class="mt-3 grid gap-3 md:grid-cols-4">
        <input v-model="incidentForm.dateFaits" type="date" class="rounded border border-surface bg-surface-0 px-3 py-2 dark:bg-surface-900" />
        <input v-model="incidentForm.dateConnaissance" type="date" class="rounded border border-surface bg-surface-0 px-3 py-2 dark:bg-surface-900" />
        <PrimeInputText v-model="incidentForm.lieu" placeholder="Lieu" />
        <PrimeSelect v-model="incidentForm.categorie" :options="categories" class="w-full" />
        <PrimeSelect v-model="incidentForm.graviteInitiale" :options="gravités" class="w-full" />
        <PrimeSelect v-model="incidentForm.idEmploye" :options="employes" option-label="nom" option-value="id" class="w-full" placeholder="Salarié concerné" />
        <PrimeInputText v-model="incidentForm.description" placeholder="Faits constatés" class="md:col-span-3" />
        <PrimeButton label="Déclarer" icon="pi pi-plus" @click="declarerIncident" />
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="text-lg font-semibold">Qualification</h2>
      <div class="mt-3 grid gap-3 md:grid-cols-4">
        <PrimeSelect v-model="qualificationForm.idIncident" :options="incidents" option-label="reference" option-value="id" class="w-full" placeholder="Incident" />
        <PrimeSelect v-model="qualificationForm.graviteRetenue" :options="gravités" class="w-full" />
        <PrimeSelect v-model="qualificationForm.idEmploye" :options="employes" option-label="nom" option-value="id" class="w-full" placeholder="Salarié concerné" />
        <PrimeInputNumber v-model="qualificationForm.idInstructeur" placeholder="ID instructeur" class="w-full" :use-grouping="false" />
        <PrimeInputNumber v-model="qualificationForm.idDecideur" placeholder="ID décideur" class="w-full" :use-grouping="false" />
        <PrimeInputText v-model="qualificationForm.motif" placeholder="Motif" class="md:col-span-2" />
        <label class="flex items-center gap-2 text-sm"><input v-model="qualificationForm.ouvrirProcedure" type="checkbox" /> Ouvrir une procédure</label>
        <label class="flex items-center gap-2 text-sm"><input v-model="qualificationForm.miseAPied" type="checkbox" /> Mise à pied</label>
        <input v-if="qualificationForm.miseAPied" v-model="qualificationForm.miseAPiedDebut" type="date" class="rounded border border-surface bg-surface-0 px-3 py-2 dark:bg-surface-900" />
        <input v-if="qualificationForm.miseAPied" v-model="qualificationForm.miseAPiedFin" type="date" class="rounded border border-surface bg-surface-0 px-3 py-2 dark:bg-surface-900" />
        <label class="flex items-center gap-2 text-sm"><input v-model="qualificationForm.derogationPrescription" type="checkbox" /> Dérogation prescription</label>
        <PrimeButton label="Qualifier" icon="pi pi-check" @click="qualifierIncident" />
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="text-lg font-semibold">Procédure contradictoire</h2>
      <div class="mt-3 grid gap-3 md:grid-cols-4">
        <PrimeSelect v-model="procedureForm.idProcedure" :options="procedures" option-label="reference" option-value="id" class="w-full md:col-span-2" placeholder="Procédure" />
        <PrimeSelect v-model="etapeForm.type" :options="typesEtape" class="w-full" />
        <PrimeInputText v-model="etapeForm.preuve" placeholder="Preuve de remise" />
        <PrimeInputText v-model="etapeForm.modeRemise" placeholder="Mode de remise" />
        <PrimeInputNumber v-model="etapeForm.delaiReponseJours" placeholder="Délai réponse" class="w-full" />
        <PrimeInputNumber v-model="etapeForm.idDocument" placeholder="ID document" class="w-full" :use-grouping="false" />
        <PrimeButton label="Enregistrer l'étape" icon="pi pi-file-edit" @click="etape(etapeForm.type === 'REPONSE' ? 'reponse' : etapeForm.type === 'CONVOCATION' ? 'convocation' : etapeForm.type === 'ENTRETIEN' ? 'entretien' : 'explication')" />
        <PrimeButton label="Constat sans réponse" icon="pi pi-hourglass" outlined @click="simpleAction('sans-reponse')" />
        <PrimeButton label="Délibération" icon="pi pi-comments" outlined @click="simpleAction('deliberation')" />
        <PrimeButton label="Clôturer" icon="pi pi-check-circle" outlined @click="simpleAction('cloture')" />
      </div>
      <div class="mt-3 grid gap-3 md:grid-cols-4">
        <PrimeInputText v-model="decisionForm.decision" placeholder="Décision (type de sanction)" class="md:col-span-2" />
        <PrimeInputNumber v-model="decisionForm.idPiece" placeholder="ID pièce" class="w-full" :use-grouping="false" />
        <label class="flex items-center gap-2 text-sm"><input v-model="decisionForm.entretienRealise" type="checkbox" /> Entretien réalisé</label>
        <PrimeTextarea v-model="decisionForm.motivation" placeholder="Motivation" class="md:col-span-3" rows="2" />
        <PrimeButton label="Rendre la décision" icon="pi pi-gavel" @click="rendreDecision" />
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="text-lg font-semibold">Sanction</h2>
      <div class="mt-3 grid gap-3 md:grid-cols-4">
        <PrimeSelect v-model="notificationForm.type" :options="typesSanction" class="w-full" />
        <input v-model="notificationForm.dateEffet" type="date" class="rounded border border-surface bg-surface-0 px-3 py-2 dark:bg-surface-900" />
        <PrimeInputNumber v-model="notificationForm.dureeJours" placeholder="Durée (jours)" class="w-full" />
        <PrimeInputText v-model="notificationForm.baseReglement" placeholder="Base réglementaire" />
        <PrimeInputText v-model="notificationForm.motif" placeholder="Motif" />
        <PrimeInputText v-model="notificationForm.modeRemise" placeholder="Mode de remise" />
        <PrimeInputText v-model="notificationForm.preuve" placeholder="Preuve de notification" />
        <PrimeButton label="Notifier" icon="pi pi-send" @click="notifierSanction" />
        <PrimeButton label="Enregistrer un recours" icon="pi pi-undo" outlined @click="contester" />
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="text-lg font-semibold">Pièces du dossier</h2>
      <div class="mt-3 grid gap-3 md:grid-cols-5">
        <PrimeSelect v-model="pieceForm.type" :options="typesPiece" class="w-full" />
        <PrimeInputNumber v-model="pieceForm.idIncident" placeholder="ID incident" class="w-full" :use-grouping="false" />
        <PrimeInputNumber v-model="pieceForm.idProcedure" placeholder="ID procédure" class="w-full" :use-grouping="false" />
        <PrimeInputText v-model="pieceForm.fichier" placeholder="Fichier" />
        <PrimeInputText v-model="pieceForm.hashSha256" placeholder="Hash SHA-256" />
        <PrimeButton label="Ajouter la pièce" icon="pi pi-paperclip" @click="ajouterPiece" />
        <PrimeButton label="Charger les pièces" icon="pi pi-search" outlined @click="chargerPieces" />
      </div>
      <DataTable v-if="pieces.length" :value="pieces" striped-rows class="mt-3">
        <Column field="type" header="Type" />
        <Column field="fichier" header="Fichier" />
        <Column field="version" header="Version" />
        <Column field="confidentialite" header="Confidentialité" />
      </DataTable>
      <p v-if="!pieces.length" class="mt-3 text-sm text-surface-500">Aucune pièce chargée.</p>
    </section>

    <section class="grid gap-4 lg:grid-cols-2">
      <div class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <h2 class="text-lg font-semibold">Incidents</h2>
        <DataTable :value="incidents" striped-rows class="mt-3" paginator :rows="6">
          <Column field="reference" header="Référence" />
          <Column field="categorie" header="Catégorie" />
          <Column field="graviteInitiale" header="Gravité" />
          <Column field="statut" header="Statut" />
        </DataTable>
      </div>
      <div class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <h2 class="text-lg font-semibold">Procédures</h2>
        <DataTable :value="procedures" striped-rows class="mt-3" paginator :rows="6">
          <Column field="reference" header="Référence" />
          <Column field="idEmploye" header="Salarié" />
          <Column field="statut" header="Statut" />
          <Column field="dateOuverture" header="Ouverture" />
        </DataTable>
      </div>
    </section>

    <section class="grid gap-4 lg:grid-cols-3">
      <div class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <h2 class="text-lg font-semibold">Sanctions</h2>
        <DataTable :value="sanctions" striped-rows class="mt-3" paginator :rows="6">
          <Column field="type" header="Type" />
          <Column field="statut" header="Statut" />
          <Column field="dateEffet" header="Effet" />
        </DataTable>
      </div>
      <div class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <h2 class="text-lg font-semibold">Absences paie</h2>
        <DataTable :value="absences" striped-rows class="mt-3" paginator :rows="6">
          <Column field="idEmploye" header="Salarié" />
          <Column field="jours" header="Jours" />
          <Column field="remuneree" header="Rémunérée" />
          <Column field="traiteePaie" header="Traitée" />
        </DataTable>
      </div>
      <div class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <h2 class="text-lg font-semibold">Indicateurs</h2>
        <div class="mt-3 grid gap-3">
          <input v-model="debut" type="date" class="rounded border border-surface bg-surface-0 px-3 py-2 dark:bg-surface-900" />
          <input v-model="fin" type="date" class="rounded border border-surface bg-surface-0 px-3 py-2 dark:bg-surface-900" />
          <PrimeButton label="Actualiser les indicateurs" icon="pi pi-chart-bar" outlined @click="chargerIndicateurs" />
          <pre v-if="indicateurs" class="overflow-auto rounded bg-surface-100 p-3 text-xs dark:bg-surface-950">{{ indicateurs }}</pre>
        </div>
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="text-lg font-semibold">Journal d’accès</h2>
      <DataTable :value="journal" striped-rows class="mt-3" paginator :rows="8">
        <Column field="dateAction" header="Date" />
        <Column field="action" header="Action" />
        <Column field="utilisateurId" header="Utilisateur" />
        <Column field="ressource" header="Ressource" />
        <Column field="motif" header="Motif" />
      </DataTable>
    </section>
  </div>
</template>
