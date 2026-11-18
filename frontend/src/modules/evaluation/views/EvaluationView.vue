<script setup>
import { onMounted, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import PrimeSelect from 'primevue/select'
import PrimeInputNumber from 'primevue/inputnumber'
import PrimeDialog from 'primevue/dialog'
import Tag from 'primevue/tag'
import { evaluationApi as api } from '@/modules/evaluation/api/evaluationApi'
import { employeApi } from '@/modules/employe/api/employeApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const notifications = useNotificationStore()
const aujourdHui = new Date().toISOString().slice(0, 10)
const campagnes = ref([])
const evaluations = ref([])
const employes = ref([])
const objectifs = ref([])
const ecarts = ref([])
const besoins = ref([])
const idCampagne = ref(null)
const idEmploye = ref(null)
const evaluation = ref(null)
const chargement = ref(false)
const typesCampagne = ['ANNUELLE', 'SEMESTRIELLE', 'PORTATION']

const campagneForm = ref({
  libelle: '',
  type: 'ANNUELLE',
  periodeDebut: aujourdHui,
  periodeFin: aujourdHui,
  dateOuverture: aujourdHui,
  dateCloture: aujourdHui,
  grilleJson: '{"ponctualite":1,"qualite":1,"collaboration":1}',
  ponderationsJson: '{"objectifs":0.5,"competences":0.3,"comportement":0.2}',
  population: 'TOUS',
})

const objectifForm = ref({
  libelle: '',
  indicateur: '',
  unite: '',
  valeurCible: 100,
  valeurRealisee: 0,
  sens: 'PLUS_EST_MIEUX',
  poids: 100,
  dateEcheance: aujourdHui,
})

const autoForm = ref({ pointsForts: '', axesProgres: '', commentaire: '' })
const managerForm = ref({
  ponctualite: 3,
  qualite: 3,
  collaboration: 3,
  pointsForts: '',
  axesProgres: '',
  appreciation: 'Conforme',
})
const entretienForm = ref({ dateEntretien: `${aujourdHui}T09:00`, compteRendu: '' })
const n2Form = ref({ idValideurN2: null, commentaire: '' })
const signatureForm = ref({ commentaire: '', signer: true })
const contestationForm = ref({ motif: '' })
const dialogueAvenant = ref(false)
const objectifAvenant = ref(null)
const avenantForm = ref({ valeurRealisee: 0, poids: 100, dateEcheance: aujourdHui, motif: '' })

async function charger() {
  chargement.value = true
  try {
    const [campagnesResultat, employesResultat] = await Promise.all([
      api.campagnes(),
      employeApi.get('/referentiel/employes'),
    ])
    campagnes.value = campagnesResultat
    employes.value = employesResultat
    if (!idCampagne.value && campagnes.value.length) idCampagne.value = campagnes.value[0].id
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

async function chargerEvaluations() {
  if (!idCampagne.value) return
  try {
    evaluations.value = await api.evaluations(idCampagne.value)
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function selectionnerEvaluation(ligne) {
  evaluation.value = ligne
  idEmploye.value = ligne.idEmploye
  entretienForm.value.dateEntretien = `${aujourdHui}T09:00`
  await Promise.all([chargerObjectifs(), chargerEcarts(), chargerBesoins()])
}

async function chargerObjectifs() {
  if (!idCampagne.value || !idEmploye.value) return
  try {
    objectifs.value = await api.objectifs(idCampagne.value, idEmploye.value)
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function chargerEcarts() {
  if (!idEmploye.value) return
  try {
    ecarts.value = await api.ecarts(idEmploye.value)
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function chargerBesoins() {
  if (!idEmploye.value) return
  try {
    besoins.value = await api.besoins(idEmploye.value)
  } catch (e) {
    notifications.erreur(e.message)
  }
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

function creerCampagne() {
  action(api.creerCampagne(campagneForm.value), 'Campagne créée.', charger)
}

function creerObjectif() {
  action(
    api.creerObjectif({
      ...objectifForm.value,
      idEmploye: idEmploye.value,
      idCampagne: idCampagne.value,
      valeurCible: Number(objectifForm.value.valeurCible),
      valeurRealisee: Number(objectifForm.value.valeurRealisee),
      poids: Number(objectifForm.value.poids),
    }),
    'Objectif enregistré.',
    chargerObjectifs,
  )
}

function ouvrirAvenant(objectif) {
  objectifAvenant.value = objectif
  avenantForm.value = {
    valeurRealisee: Number(objectif.valeurRealisee || 0),
    poids: Number(objectif.poids || 100),
    dateEcheance: objectif.dateEcheance,
    motif: '',
  }
  dialogueAvenant.value = true
}

function enregistrerAvenant() {
  action(
    api.avenantObjectif(objectifAvenant.value.id, avenantForm.value),
    'Avenant enregistré.',
    async () => {
      dialogueAvenant.value = false
      await chargerObjectifs()
    },
  )
}

function severity(statut) {
  if (['CLOTUREE', 'VALIDEE', 'CONFIRMEE'].includes(statut)) return 'success'
  if (['CONTESTEE', 'ABANDONNE', 'ANNULEE'].includes(statut)) return 'danger'
  if (['SIGNEE', 'LANCEE', 'HAUTE'].includes(statut)) return 'info'
  return 'warn'
}

onMounted(async () => {
  await charger()
  await chargerEvaluations()
})
</script>

<template>
  <div class="flex flex-col gap-5">
    <div>
      <h1>Évaluation et compétences</h1>
      <p class="mt-1 text-sm text-muted-color">Campagnes, entretiens, écarts de compétences et besoins de formation.</p>
    </div>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="flex flex-wrap items-end gap-3">
        <div class="min-w-64 flex-1">
          <label class="text-sm font-semibold">Campagne</label>
          <PrimeSelect v-model="idCampagne" :options="campagnes" option-label="libelle" option-value="id"
            class="mt-1 w-full" placeholder="Choisir une campagne" @change="chargerEvaluations" />
        </div>
        <PrimeButton label="Actualiser" icon="pi pi-refresh" outlined @click="charger" />
      </div>

      <div class="mt-5 grid gap-3 md:grid-cols-3">
        <div class="flex flex-col gap-2"><label class="text-sm font-semibold">Libellé</label>
          <PrimeInputText v-model="campagneForm.libelle" placeholder="Campagne annuelle 2026" /></div>
        <div class="flex flex-col gap-2"><label class="text-sm font-semibold">Type</label>
          <PrimeSelect v-model="campagneForm.type" :options="typesCampagne" class="w-full" /></div>
        <div class="flex flex-col gap-2"><label class="text-sm font-semibold">Population</label>
          <PrimeInputText v-model="campagneForm.population" /></div>
        <div class="flex flex-col gap-2"><label class="text-sm font-semibold">Ouverture</label>
          <PrimeInputText v-model="campagneForm.dateOuverture" placeholder="AAAA-MM-JJ" /></div>
        <div class="flex flex-col gap-2"><label class="text-sm font-semibold">Clôture</label>
          <PrimeInputText v-model="campagneForm.dateCloture" placeholder="AAAA-MM-JJ" /></div>
        <div class="flex flex-col gap-2"><label class="text-sm font-semibold">Pondérations</label>
          <PrimeInputText v-model="campagneForm.ponderationsJson" /></div>
        <div class="col-span-full flex flex-col gap-2"><label class="text-sm font-semibold">Grille comportementale</label>
          <PrimeInputText v-model="campagneForm.grilleJson" /></div>
      </div>
      <PrimeButton label="Créer la campagne" icon="pi pi-plus" class="mt-4" @click="creerCampagne" />
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2>Évaluations de la campagne</h2>
      <DataTable :value="evaluations" :loading="chargement" striped-rows class="mt-3" selection-mode="single"
        :selection="evaluation" @update:selection="selectionnerEvaluation">
        <Column field="idEmploye" header="Employé" />
        <Column field="statut" header="Statut">
          <template #body="{ data }"><Tag :value="data.statut" :severity="severity(data.statut)" /></template>
        </Column>
        <Column field="noteGlobale" header="Note globale" />
        <Column field="appreciation" header="Appréciation" />
        <Column field="version" header="Version" />
      </DataTable>
    </section>

    <section v-if="evaluation" class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <h2>Évaluation #{{ evaluation.id }}</h2>
        <Tag :value="evaluation.statut" :severity="severity(evaluation.statut)" />
      </div>

      <div class="mt-4 flex flex-wrap gap-2">
        <PrimeButton label="Démarrer" icon="pi pi-play" outlined
          @click="action(api.demarrer(idCampagne, evaluation.idEmploye), 'Évaluation démarrée.', chargerEvaluations)" />
        <PrimeButton label="Clôturer" icon="pi pi-check-circle" outlined :disabled="evaluation.statut !== 'SIGNEE'"
          @click="action(api.clore(evaluation.id), 'Évaluation clôturée.', async () => { await chargerEvaluations(); await chargerEcarts(); await chargerBesoins() })" />
        <PrimeButton label="Actualiser" icon="pi pi-refresh" text @click="async () => { evaluation = await api.evaluation(evaluation.id); await chargerObjectifs() }" />
      </div>

      <div class="mt-5 grid gap-4 lg:grid-cols-2">
        <div class="flex flex-col gap-3 rounded border border-surface p-3">
          <h3>Auto-évaluation</h3>
          <PrimeTextarea v-model="autoForm.pointsForts" rows="2" placeholder="Points forts" />
          <PrimeTextarea v-model="autoForm.axesProgres" rows="2" placeholder="Axes de progrès" />
          <PrimeTextarea v-model="autoForm.commentaire" rows="2" placeholder="Commentaire" />
          <PrimeButton label="Enregistrer l'auto-évaluation" @click="action(api.autoEvaluer(evaluation.id, autoForm), 'Auto-évaluation enregistrée.', chargerEvaluations)" />
        </div>
        <div class="flex flex-col gap-3 rounded border border-surface p-3">
          <h3>Notation manager</h3>
          <div class="grid grid-cols-3 gap-2">
            <PrimeInputNumber v-model="managerForm.ponctualite" :min="1" :max="5" placeholder="Ponctualité" />
            <PrimeInputNumber v-model="managerForm.qualite" :min="1" :max="5" placeholder="Qualité" />
            <PrimeInputNumber v-model="managerForm.collaboration" :min="1" :max="5" placeholder="Collaboration" />
          </div>
          <PrimeInputText v-model="managerForm.pointsForts" placeholder="Points forts factuels" />
          <PrimeInputText v-model="managerForm.axesProgres" placeholder="Axes de progrès factuels" />
          <PrimeInputText v-model="managerForm.appreciation" placeholder="Appréciation" />
          <PrimeButton label="Enregistrer la notation" @click="action(api.evaluerManager(evaluation.id, {
            notesComportement: {
              ponctualite: Number(managerForm.ponctualite),
              qualite: Number(managerForm.qualite),
              collaboration: Number(managerForm.collaboration),
            },
            pointsForts: managerForm.pointsForts,
            axesProgres: managerForm.axesProgres,
            appreciation: managerForm.appreciation,
          }), 'Notation enregistrée.', chargerEvaluations)" />
        </div>
        <div class="flex flex-col gap-3 rounded border border-surface p-3">
          <h3>Entretien</h3>
          <PrimeInputText v-model="entretienForm.dateEntretien" placeholder="AAAA-MM-JJTHH:mm" />
          <PrimeTextarea v-model="entretienForm.compteRendu" rows="3" placeholder="Compte rendu" />
          <PrimeButton label="Enregistrer l'entretien" @click="action(api.entretien(evaluation.id, entretienForm), 'Entretien enregistré.', chargerEvaluations)" />
        </div>
        <div class="flex flex-col gap-3 rounded border border-surface p-3">
          <h3>Validation N2</h3>
          <PrimeSelect v-model="n2Form.idValideurN2" :options="employes" option-label="nom" option-value="id"
            class="w-full" placeholder="Valideur N2" />
          <PrimeTextarea v-model="n2Form.commentaire" rows="2" placeholder="Commentaire" />
          <PrimeButton label="Valider en N2" @click="action(api.validerN2(evaluation.id, n2Form), 'Validation N2 enregistrée.', chargerEvaluations)" />
        </div>
        <div class="flex flex-col gap-3 rounded border border-surface p-3">
          <h3>Signature et contestation</h3>
          <PrimeTextarea v-model="signatureForm.commentaire" rows="2" placeholder="Commentaire du salarié" />
          <div class="flex gap-2">
            <PrimeButton label="Signer" @click="action(api.signer(evaluation.id, { ...signatureForm, signer: true }), 'Évaluation signée.', chargerEvaluations)" />
            <PrimeButton label="Contester" outlined @click="action(api.contester(evaluation.id, contestationForm), 'Contestation enregistrée.', chargerEvaluations)" />
          </div>
          <PrimeInputText v-model="contestationForm.motif" placeholder="Motif de contestation" />
          <PrimeButton label="Nouvelle version" text @click="action(api.nouvelleVersion(evaluation.id, contestationForm.motif || 'Contestation'), 'Nouvelle version créée.', chargerEvaluations)" />
        </div>
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2>Objectifs</h2>
      <div class="mt-3 grid gap-3 md:grid-cols-3">
        <PrimeInputText v-model="objectifForm.libelle" placeholder="Libellé" />
        <PrimeInputText v-model="objectifForm.indicateur" placeholder="Indicateur" />
        <PrimeInputText v-model="objectifForm.unite" placeholder="Unité" />
        <PrimeInputNumber v-model="objectifForm.valeurCible" placeholder="Cible" />
        <PrimeInputNumber v-model="objectifForm.valeurRealisee" placeholder="Réalisé" />
        <PrimeInputNumber v-model="objectifForm.poids" :min="1" :max="100" placeholder="Poids" />
        <PrimeSelect v-model="objectifForm.sens" :options="['PLUS_EST_MIEUX', 'MOINS_EST_MIEUX']" class="w-full" />
        <PrimeInputText v-model="objectifForm.dateEcheance" placeholder="AAAA-MM-JJ" />
        <PrimeButton label="Ajouter un objectif" icon="pi pi-plus" :disabled="!idEmploye" @click="creerObjectif" />
      </div>
      <PrimeButton v-if="idEmploye" label="Valider les objectifs" icon="pi pi-check" outlined class="mt-3"
        @click="action(api.validerObjectifs(idEmploye, idCampagne), 'Objectifs validés.', chargerObjectifs)" />
      <DataTable :value="objectifs" striped-rows class="mt-4">
        <Column field="libelle" header="Objectif" />
        <Column field="valeurCible" header="Cible" />
        <Column field="valeurRealisee" header="Réalisé" />
        <Column field="tauxAtteinte" header="Taux" />
        <Column field="poids" header="Poids" />
        <Column field="statut" header="Statut">
          <template #body="{ data }"><Tag :value="data.statut" :severity="severity(data.statut)" /></template>
        </Column>
        <Column header="Avenant">
          <template #body="{ data }">
            <PrimeButton icon="pi pi-pencil" text rounded aria-label="Avenant" @click="ouvrirAvenant(data)" />
          </template>
        </Column>
      </DataTable>
    </section>

    <section class="grid gap-5 lg:grid-cols-2">
      <div class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <h2>Écarts de compétences</h2>
        <DataTable :value="ecarts" striped-rows class="mt-3">
          <Column field="idCompetence" header="Compétence" />
          <Column field="ecart" header="Écart" />
          <Column field="prioriteScore" header="Score" />
          <Column field="priorite" header="Priorité">
            <template #body="{ data }"><Tag :value="data.priorite" :severity="severity(data.priorite)" /></template>
          </Column>
          <Column field="statut" header="Statut" />
        </DataTable>
      </div>
      <div class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <h2>Besoins de formation</h2>
        <DataTable :value="besoins" striped-rows class="mt-3">
          <Column field="idCompetence" header="Compétence" />
          <Column field="origine" header="Origine" />
          <Column field="priorite" header="Priorité">
            <template #body="{ data }"><Tag :value="data.priorite" :severity="severity(data.priorite)" /></template>
          </Column>
          <Column field="statut" header="Statut">
            <template #body="{ data }"><Tag :value="data.statut" :severity="severity(data.statut)" /></template>
          </Column>
        </DataTable>
      </div>
    </section>

    <PrimeDialog v-model:visible="dialogueAvenant" modal header="Avenant d'objectif" class="w-[min(32rem,95vw)]">
      <div class="flex flex-col gap-3">
        <PrimeInputNumber v-model="avenantForm.valeurRealisee" placeholder="Réalisé" />
        <PrimeInputNumber v-model="avenantForm.poids" :min="1" :max="100" placeholder="Poids" />
        <PrimeInputText v-model="avenantForm.dateEcheance" placeholder="AAAA-MM-JJ" />
        <PrimeTextarea v-model="avenantForm.motif" rows="3" placeholder="Motif de l'avenant" />
        <PrimeButton label="Enregistrer l'avenant" @click="enregistrerAvenant" />
      </div>
    </PrimeDialog>
  </div>
</template>
