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
import { carriereApi as api } from '@/modules/carriere/api/carriereApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const notifications = useNotificationStore()
const aujourdHui = new Date().toISOString().slice(0, 10)
const employes = ref([])
const mouvements = ref([])
const campagnes = ref([])
const carriere = ref(null)
const frise = ref([])
const rapport = ref(null)
const dateValidite = ref(aujourdHui)
const dateConnaissance = ref(aujourdHui)
const chargement = ref(false)

const typesMouvement = ['PROMOTION', 'MUTATION', 'DETACHEMENT', 'CHANGEMENT_POSTE', 'CHANGEMENT_SALAIRE']
const typesMutation = ['INTERNE_SERVICE', 'INTER_DEPARTEMENT', 'GEOGRAPHIQUE', 'TEMPORAIRE', 'DETACHEMENT']
const initiatives = ['EMPLOYEUR', 'SALARIE']

const mouvementForm = ref({
  idEmploye: null,
  type: 'PROMOTION',
  initiateur: 'EMPLOYEUR',
  motif: '',
  justification: '',
  dateEffetSouhaitee: aujourdHui,
  derogationDirection: false,
  motifDerogation: '',
  ancienPoste: null,
  nouveauPoste: null,
  ancienneCategorie: '',
  nouvelleCategorie: '',
  ancienEchelon: '',
  nouveauEchelon: '',
  ancienSalaire: null,
  salairePromotion: null,
  typeMutation: 'DEFINITIVE',
  initiativeMutation: 'EMPLOYEUR',
  serviceOrigine: null,
  serviceDestination: null,
  managerOrigine: null,
  managerDestination: null,
  lieuOrigine: '',
  lieuDestination: '',
  dateRetourPrevue: null,
  indemniteMobilite: null,
  nouveauSalaire: null,
})

const campagneForm = ref({
  libelle: '',
  dateEffet: aujourdHui,
  enveloppeBudgetaire: null,
  population: 'ACTIFS',
  regle: '{"taux":0.05}',
})

const nombre = (valeur) =>
  valeur === '' || valeur === null || valeur === undefined ? null : Number(valeur)

const estMutation = (type) => ['MUTATION', 'DETACHEMENT', 'CHANGEMENT_POSTE'].includes(type)

async function action(promise, message, apres) {
  try {
    await promise
    notifications.succes(message)
    if (apres) await apres()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function charger() {
  chargement.value = true
  try {
    const [employesResultat, mouvementsResultat, campagnesResultat] = await Promise.all([
      api.employes(),
      api.mouvements(),
      api.campagnes(),
    ])
    employes.value = employesResultat
    mouvements.value = mouvementsResultat
    campagnes.value = campagnesResultat
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

function payloadMouvement() {
  const f = mouvementForm.value
  return {
    idEmploye: nombre(f.idEmploye),
    type: f.type,
    initiateur: f.initiateur,
    motif: f.motif,
    justification: f.justification,
    dateEffetSouhaitee: f.dateEffetSouhaitee,
    derogationDirection: f.derogationDirection,
    motifDerogation: f.motifDerogation || null,
    nouveauSalaire: f.type === 'CHANGEMENT_SALAIRE' ? nombre(f.nouveauSalaire) : null,
    promotion: f.type === 'PROMOTION'
      ? {
          nature: 'CHANGEMENT_ECHELON',
          idAncienPoste: nombre(f.ancienPoste),
          idNouveauPoste: nombre(f.nouveauPoste),
          ancienneCategorie: f.ancienneCategorie || null,
          nouvelleCategorie: f.nouvelleCategorie || null,
          ancienEchelon: f.ancienEchelon || null,
          nouveauEchelon: f.nouveauEchelon || null,
          ancienSalaire: nombre(f.ancienSalaire),
          nouveauSalaire: nombre(f.salairePromotion),
        }
      : null,
    mutation: estMutation(f.type)
      ? {
          type: f.type === 'DETACHEMENT' ? 'DETACHEMENT'
            : f.type === 'CHANGEMENT_POSTE' ? 'INTERNE_SERVICE'
              : f.typeMutation,
          initiative: f.initiativeMutation,
          idPosteOrigine: nombre(f.ancienPoste),
          idPosteDestination: nombre(f.nouveauPoste),
          idServiceOrigine: nombre(f.serviceOrigine),
          idServiceDestination: nombre(f.serviceDestination),
          idManagerOrigine: nombre(f.managerOrigine),
          idManagerDestination: nombre(f.managerDestination),
          idLieuOrigine: f.lieuOrigine || null,
          idLieuDestination: f.lieuDestination || null,
          dateRetourPrevue: f.dateRetourPrevue || null,
          clauseMobiliteVerifiee: true,
          indemniteMobilite: nombre(f.indemniteMobilite),
        }
      : null,
  }
}

function creerMouvement() {
  action(api.creerMouvement(payloadMouvement()), 'Mouvement créé.', charger)
}

function chargerCarriere() {
  if (!mouvementForm.value.idEmploye) return
  action(
    api.carriere(mouvementForm.value.idEmploye, dateValidite.value, dateConnaissance.value),
    'Carrière reconstituée.',
    (resultat) => { carriere.value = resultat },
  )
}

function chargerFrise() {
  if (!mouvementForm.value.idEmploye) return
  action(api.frise(mouvementForm.value.idEmploye), 'Frise chargée.', (resultat) => { frise.value = resultat })
}

function chargerIntegrite() {
  action(api.integrite(), 'Rapport généré.', (resultat) => { rapport.value = resultat })
}

function creerCampagne() {
  action(api.creerCampagne({
    ...campagneForm.value,
    enveloppeBudgetaire: nombre(campagneForm.value.enveloppeBudgetaire),
  }), 'Campagne créée.', charger)
}

function refuserMouvement(mouvement) {
  const motif = window.prompt('Motif du refus', 'Refus hiérarchique')
  if (motif) action(api.refuser(mouvement.id, motif), 'Mouvement refusé.', charger)
}

function refuserAvenant(mouvement) {
  const motif = window.prompt('Motif du refus du salarié', 'Avenant refusé')
  if (motif) action(api.refuserAvenant(mouvement.id, motif), 'Avenant refusé.', charger)
}

function severity(statut) {
  if (['VALIDE', 'APPLIQUE', 'CLOTUREE'].includes(statut)) return 'success'
  if (['REFUSE', 'REFUSE_PAR_SALARIE', 'ANNULE', 'A_COMPLETER'].includes(statut)) return 'danger'
  if (['SOUMIS', 'EN_VALIDATION', 'AVENANT_EN_SIGNATURE', 'PLANIFIE'].includes(statut)) return 'warn'
  return 'info'
}

onMounted(charger)
</script>

<template>
  <div class="flex flex-col gap-5">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold">Carrière et mobilité</h1>
        <p class="text-sm text-surface-500">Historique temporel, promotions, mutations et campagnes d'augmentation.</p>
      </div>
      <PrimeButton icon="pi pi-refresh" label="Actualiser" outlined :loading="chargement" @click="charger" />
    </div>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="text-lg font-semibold">Reconstitution à une date</h2>
      <div class="mt-3 grid gap-3 md:grid-cols-4">
        <PrimeSelect v-model="mouvementForm.idEmploye" :options="employes" option-label="nom"
          option-value="id" class="w-full" placeholder="Salarié" />
        <input v-model="dateValidite" type="date"
          class="w-full rounded border border-surface bg-surface-0 px-3 py-2 dark:bg-surface-900" />
        <input v-model="dateConnaissance" type="date"
          class="w-full rounded border border-surface bg-surface-0 px-3 py-2 dark:bg-surface-900" />
        <PrimeButton label="Reconstituer" icon="pi pi-history" @click="chargerCarriere" />
      </div>
      <div v-if="carriere" class="mt-3 grid gap-2 rounded bg-surface-100 p-3 text-sm dark:bg-surface-950 md:grid-cols-4">
        <span><strong>Contrat :</strong> {{ carriere.contrat || '—' }}</span>
        <span><strong>Poste :</strong> {{ carriere.idPoste || '—' }}</span>
        <span><strong>Service :</strong> {{ carriere.idService || '—' }}</span>
        <span><strong>Lieu :</strong> {{ carriere.lieuTravail || '—' }}</span>
        <span><strong>Catégorie :</strong> {{ carriere.categorie || '—' }}</span>
        <span><strong>Échelon :</strong> {{ carriere.echelon || '—' }}</span>
        <span><strong>Taux :</strong> {{ carriere.tauxActivite ?? '—' }}%</span>
        <span><strong>Régime :</strong> {{ carriere.regimeTravail || '—' }}</span>
        <span><strong>Affectation :</strong> {{ carriere.statutAffectation || '—' }}</span>
        <span v-if="carriere.salaireBrut !== null"><strong>Salaire brut :</strong> {{ carriere.salaireBrut }}</span>
      </div>
      <div class="mt-3">
        <PrimeButton label="Voir la frise" icon="pi pi-list" outlined @click="chargerFrise" />
      </div>
      <DataTable v-if="frise.length" :value="frise" striped-rows class="mt-3">
        <Column field="date" header="Date" />
        <Column field="source" header="Source" />
        <Column field="type" header="Type" />
        <Column field="libelle" header="Libellé" />
        <Column field="confidentialite" header="Confidentialité" />
      </DataTable>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="text-lg font-semibold">Nouveau mouvement</h2>
      <div class="mt-3 grid gap-3 md:grid-cols-3">
        <PrimeSelect v-model="mouvementForm.idEmploye" :options="employes" option-label="nom"
          option-value="id" class="w-full" placeholder="Salarié" />
        <PrimeSelect v-model="mouvementForm.type" :options="typesMouvement" class="w-full" placeholder="Type" />
        <PrimeSelect v-model="mouvementForm.initiateur" :options="initiatives" class="w-full" placeholder="Initiateur" />
        <PrimeInputText v-model="mouvementForm.motif" placeholder="Motif" />
        <PrimeInputText v-model="mouvementForm.dateEffetSouhaitee" placeholder="Date d'effet (AAAA-MM-JJ)" />
        <PrimeInputText v-model="mouvementForm.motifDerogation" placeholder="Motif de dérogation (optionnel)" />
        <PrimeTextarea v-model="mouvementForm.justification" rows="2" class="md:col-span-3"
          placeholder="Justification" />
      </div>

      <div v-if="mouvementForm.type === 'PROMOTION'" class="mt-4 grid gap-3 md:grid-cols-4">
        <PrimeInputNumber v-model="mouvementForm.ancienPoste" placeholder="Poste actuel" />
        <PrimeInputNumber v-model="mouvementForm.nouveauPoste" placeholder="Nouveau poste" />
        <PrimeInputText v-model="mouvementForm.ancienneCategorie" placeholder="Catégorie actuelle" />
        <PrimeInputText v-model="mouvementForm.nouvelleCategorie" placeholder="Nouvelle catégorie" />
        <PrimeInputText v-model="mouvementForm.ancienEchelon" placeholder="Échelon actuel" />
        <PrimeInputText v-model="mouvementForm.nouveauEchelon" placeholder="Nouvel échelon" />
        <PrimeInputNumber v-model="mouvementForm.ancienSalaire" placeholder="Salaire actuel" />
        <PrimeInputNumber v-model="mouvementForm.salairePromotion" placeholder="Nouveau salaire" />
      </div>

      <div v-if="estMutation(mouvementForm.type)" class="mt-4 grid gap-3 md:grid-cols-3">
        <PrimeSelect v-model="mouvementForm.typeMutation" :options="typesMutation" class="w-full" />
        <PrimeSelect v-model="mouvementForm.initiativeMutation" :options="initiatives" class="w-full" />
        <PrimeInputNumber v-model="mouvementForm.ancienPoste" placeholder="Poste d'origine" />
        <PrimeInputNumber v-model="mouvementForm.nouveauPoste" placeholder="Poste de destination" />
        <PrimeInputNumber v-model="mouvementForm.serviceOrigine" placeholder="Service d'origine" />
        <PrimeInputNumber v-model="mouvementForm.serviceDestination" placeholder="Service de destination" />
        <PrimeInputNumber v-model="mouvementForm.managerOrigine" placeholder="Manager d'origine" />
        <PrimeInputNumber v-model="mouvementForm.managerDestination" placeholder="Manager de destination" />
        <PrimeInputText v-model="mouvementForm.lieuOrigine" placeholder="Lieu d'origine" />
        <PrimeInputText v-model="mouvementForm.lieuDestination" placeholder="Lieu de destination" />
        <PrimeInputText v-model="mouvementForm.dateRetourPrevue" placeholder="Retour prévu (AAAA-MM-JJ)" />
        <PrimeInputNumber v-model="mouvementForm.indemniteMobilite" placeholder="Indemnité de mobilité" />
      </div>

      <div v-if="mouvementForm.type === 'CHANGEMENT_SALAIRE'" class="mt-4 grid gap-3 md:grid-cols-2">
        <PrimeInputNumber v-model="mouvementForm.nouveauSalaire" placeholder="Nouveau salaire brut" />
      </div>

      <PrimeButton class="mt-4" label="Créer le mouvement" icon="pi pi-plus" @click="creerMouvement" />
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="flex items-center justify-between">
        <h2 class="text-lg font-semibold">Mouvements</h2>
        <PrimeButton label="Rapport d'intégrité" icon="pi pi-shield" text @click="chargerIntegrite" />
      </div>
      <pre v-if="rapport" class="mt-3 overflow-auto rounded bg-surface-100 p-3 text-xs dark:bg-surface-950">{{ rapport }}</pre>
      <DataTable :value="mouvements" striped-rows paginator :rows="10" class="mt-3">
        <Column field="reference" header="Référence" />
        <Column field="idEmploye" header="Salarié" />
        <Column field="type" header="Type" />
        <Column field="dateEffetSouhaitee" header="Effet souhaité" />
        <Column field="statut" header="Statut">
          <template #body="{ data }"><Tag :value="data.statut" :severity="severity(data.statut)" /></template>
        </Column>
        <Column header="Étape">
          <template #body="{ data }">
            {{ data.statut === 'EN_VALIDATION' ? (data.conditions?.etapeCourante ?? 0) + 1 : '—' }}
          </template>
        </Column>
        <Column header="Actions">
          <template #body="{ data }">
            <div class="flex flex-wrap gap-1">
              <PrimeButton v-if="['BROUILLON', 'A_COMPLETER'].includes(data.statut)" icon="pi pi-send" text rounded
                title="Soumettre" @click="action(api.soumettre(data.id), 'Mouvement soumis.', charger)" />
              <PrimeButton v-if="data.statut === 'EN_VALIDATION'" icon="pi pi-check" text rounded title="Valider l'étape"
                @click="action(api.valider(data.id, {}), 'Étape validée.', charger)" />
              <PrimeButton v-if="data.statut === 'EN_VALIDATION'" icon="pi pi-ban" text rounded title="Refuser"
                @click="refuserMouvement(data)" />
              <PrimeButton v-if="data.statut === 'VALIDE'" icon="pi pi-file-edit" text rounded title="Préparer l'avenant"
                @click="action(api.preparerAvenant(data.id), 'Avenant préparé.', charger)" />
              <PrimeButton v-if="data.statut === 'AVENANT_EN_SIGNATURE'" icon="pi pi-check-circle" text rounded
                title="Signer l'avenant" @click="action(api.signerAvenant(data.id), 'Avenant signé.', charger)" />
              <PrimeButton v-if="data.statut === 'AVENANT_EN_SIGNATURE'" icon="pi pi-times-circle" text rounded
                title="Refuser l'avenant" @click="refuserAvenant(data)" />
              <PrimeButton v-if="data.statut === 'VALIDE'" icon="pi pi-calendar" text rounded title="Planifier"
                @click="action(api.planifier(data.id), 'Mouvement planifié.', charger)" />
              <PrimeButton v-if="['VALIDE', 'PLANIFIE'].includes(data.statut)" icon="pi pi-play" text rounded
                title="Appliquer" @click="action(api.appliquer(data.id), 'Mouvement appliqué.', charger)" />
              <PrimeButton v-if="['BROUILLON', 'SOUMIS', 'EN_VALIDATION', 'PLANIFIE', 'VALIDE'].includes(data.statut)"
                icon="pi pi-undo" text rounded title="Annuler"
                @click="action(api.annuler(data.id, 'Annulation RH'), 'Mouvement annulé.', charger)" />
            </div>
          </template>
        </Column>
      </DataTable>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="text-lg font-semibold">Campagnes d'augmentation</h2>
      <div class="mt-3 grid gap-3 md:grid-cols-5">
        <PrimeInputText v-model="campagneForm.libelle" placeholder="Libellé" />
        <PrimeInputText v-model="campagneForm.dateEffet" placeholder="Date d'effet" />
        <PrimeInputNumber v-model="campagneForm.enveloppeBudgetaire" placeholder="Enveloppe" />
        <PrimeInputText v-model="campagneForm.population" placeholder="Population" />
        <PrimeInputText v-model="campagneForm.regle" placeholder='{"taux":0.05}' />
      </div>
      <PrimeButton class="mt-3" label="Créer la campagne" icon="pi pi-plus" @click="creerCampagne" />
      <DataTable :value="campagnes" striped-rows class="mt-3">
        <Column field="libelle" header="Campagne" />
        <Column field="dateEffet" header="Date d'effet" />
        <Column field="enveloppeBudgetaire" header="Enveloppe" />
        <Column field="statut" header="Statut" />
        <Column header="Simulation">
          <template #body="{ data }">
            <PrimeButton label="Simuler" icon="pi pi-calculator" text
              @click="action(api.simulerCampagne(data.id), 'Simulation générée.', charger)" />
          </template>
        </Column>
      </DataTable>
    </section>
  </div>
</template>
