<script setup>
import PrimeSelect from 'primevue/select'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import { computed, onMounted, ref } from 'vue'
import { besoinApi as api } from '@/modules/besoin/api/besoinApi'
import EmptyState from '@/shared/components/EmptyState.vue'
import ModalDialog from '@/shared/components/ModalDialog.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDateHeure, formatMontant } from '@/shared/utils/format'

const auth = useAuthStore()
const notifications = useNotificationStore()

const lignes = ref([])
const chargement = ref(true)
const action = ref(null)
const decision = ref('APPROUVE')
const commentaire = ref('')
const montantVerifie = ref(null)
const enregistrement = ref(false)

/** Evaluation saisie par le RH seulement si le budget n'est pas deja connu (le maximum fait office de budget). */
const evaluationRh = ref({ salaireMinEstime: null, salaireMaxEstime: null })

/** Le budget d'une ligne est son salaire maximum, deja defini cote backend. */
function budgetDe(ligne) {
  return ligne?.budgetEstime ?? ligne?.salaireMaxEstime ?? null
}

const budgetConnu = computed(() => budgetDe(action.value) !== null)

/** Le formulaire de fourchette salariale n'apparait que si le budget n'est pas deja connu. */
const saisieEvaluationRh = computed(
  () =>
    action.value?.etapeCourante === 'RH' &&
    decision.value === 'APPROUVE' &&
    !budgetConnu.value,
)

function montantRh(montant) {
  return montant === null || montant === undefined ? 'Non defini' : formatMontant(montant)
}

function nombreOuNull(valeur) {
  return valeur === null || valeur === undefined || valeur === '' ? null : Number(valeur)
}

const DECISIONS = [
  { valeur: 'APPROUVE', libelle: 'Approuver' },
  { valeur: 'A_CORRIGER', libelle: 'Demander une correction' },
  { valeur: 'REFUSE', libelle: 'Refuser definitivement' },
]

const ETAPES = [
  { valeur: 'RH', libelle: 'RH' },
  { valeur: 'FINANCE', libelle: 'Finance' },
  { valeur: 'DIRECTION', libelle: 'Direction' },
]

const filtreEtape = ref(auth.role)

async function charger() {
  chargement.value = true
  try {
    lignes.value = await api.get('/validation/file')
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

const lignesFiltrees = computed(() => {
  if (!filtreEtape.value) {
    return lignes.value
  }
  return lignes.value.filter((ligne) => ligne.etapeCourante === filtreEtape.value)
})

function ouvrir(ligne) {
  action.value = ligne
  decision.value = 'APPROUVE'
  commentaire.value = ''
  montantVerifie.value = null
  // Le budget d'une ligne est son salaire maximum : on le reprend s'il est deja defini.
  evaluationRh.value = {
    salaireMinEstime: null,
    salaireMaxEstime: budgetDe(ligne),
  }
}

async function enregistrer() {
  if (!action.value) {
    return
  }
  const corps = {
    decision: decision.value,
    commentaire: commentaire.value.trim() || null,
    montantVerifie: nombreOuNull(montantVerifie.value),
  }
  if (saisieEvaluationRh.value) {
    const salaireMax = nombreOuNull(evaluationRh.value.salaireMaxEstime)
    if (salaireMax === null) {
      notifications.erreur('Definissez le salaire maximum (budget du profil) avant de l\'approuver.')
      return
    }
    corps.salaireMinEstime = nombreOuNull(evaluationRh.value.salaireMinEstime)
    corps.salaireMaxEstime = salaireMax
  }
  enregistrement.value = true
  try {
    await api.post(`/validation/lignes/${action.value.id}/decision`, corps)
    notifications.succes('Decision enregistree.')
    action.value = null
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    enregistrement.value = false
  }
}
</script>

<template>
  <div>
    <div class="flex flex-col gap-4 mb-5 md:flex-row md:items-start md:justify-between">
      <div>
        <h1>File de validation</h1>
        <p class="mt-1 text-sm text-muted-color">
          Lignes en attente a votre etape. Chaque decision est tracee et conditionne la suite du circuit.
        </p>
      </div>
      <div class="flex flex-col gap-2 mb-4" style="margin: 0; min-width: 200px">
        <label class="text-sm font-semibold text-muted-color" for="etape">Etape</label>
        <PrimeSelect id="etape" v-model="filtreEtape" class="w-full" :options="[{ libelle: 'Toutes les etapes', valeur: '' }, ...ETAPES]" option-label="libelle" option-value="valeur" />
      </div>
    </div>

    <section class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900">
      <div class="flex items-center justify-between gap-3 border-b border-surface p-4">
        <h2>{{ lignesFiltrees.length }} ligne(s) a traiter</h2>
        <span class="text-secondary-color">Votre role : {{ auth.role }}</span>
      </div>

      <div v-if="chargement" class="p-4 text-sm text-muted-color">Chargement de la file...</div>

      <EmptyState
        v-else-if="lignesFiltrees.length === 0"
        titre="File vide"
        message="Aucune ligne n'attend de validation a cette etape."
      />

      <div v-else class="overflow-x-auto">
        <DataTable :value="lignesFiltrees" striped-rows paginator :rows="10" responsive-layout="scroll">
          <Column header="Ligne"><template #body="{ data }"><div class="font-medium">{{ data.intituleDemande }}</div><div class="text-xs text-muted-color">Demande #{{ data.idDemande }} · episode {{ data.episode }}</div></template></Column>
          <Column field="service" header="Service" />
          <Column header="Type de besoin"><template #body="{ data }"><StatusBadge :valeur="data.typeBesoin" /></template></Column>
          <Column field="effectif" header="Effectif" />
          <Column header="Budget"><template #body="{ data }">{{ montantRh(budgetDe(data)) }}</template></Column>
          <Column header="Etape"><template #body="{ data }"><StatusBadge :valeur="data.etapeCourante" /></template></Column>
          <Column header="Derniere mise a jour"><template #body="{ data }">{{ formatDateHeure(data.dateModification) }}</template></Column>
          <Column header=""><template #body="{ data }"><PrimeButton label="Traiter" severity="primary" size="small" :disabled="data.etapeCourante !== auth.role" @click="ouvrir(data)" /></template></Column>
        </DataTable>
      </div>
    </section>

    <ModalDialog
      v-if="action"
      titre="Decision de validation"
      :sous-titre="action.intituleDemande"
      :fermable="!enregistrement"
      @fermer="action = null"
    >
      <div class="rounded-lg border p-3 text-sm border-blue-200 bg-blue-50 text-blue-700">
        Etape en cours : <strong>{{ action.etapeCourante }}</strong> · budget :
        <strong>{{ montantRh(budgetDe(action)) }}</strong>
        <template v-if="action.etapeCourante === 'RH'">
          ·
          <RouterLink :to="{ name: 'demande-detail', params: { id: action.idDemande } }" class="underline">
            Consulter le dossier complet
          </RouterLink>
        </template>
      </div>

      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="decision-file">Decision</label>
        <PrimeSelect id="decision-file" v-model="decision" class="w-full" :options="DECISIONS" option-label="libelle" option-value="valeur" />
      </div>

      <template v-if="saisieEvaluationRh">
        <div class="rounded-lg border p-3 text-sm border-blue-200 bg-blue-50 text-blue-700 mb-4">
          Le budget de ce profil n'est pas encore defini. Saisissez la fourchette salariale : le salaire
          maximum fait office de budget et sera verifie par la Finance (disponibilite budgetaire).
        </div>

        <div class="grid grid-cols-1 gap-x-4 md:grid-cols-2">
          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" for="rh-salaire-min-file">Salaire minimum (Ar)</label>
            <PrimeInputText
              id="rh-salaire-min-file"
              v-model.number="evaluationRh.salaireMinEstime"
              class="w-full"
              type="number"
              min="0"
              step="1000"
            />
          </div>
          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" for="rh-salaire-max-file"
              >Salaire maximum = budget (Ar) <span class="text-red-500">*</span></label
            >
            <PrimeInputText
              id="rh-salaire-max-file"
              v-model.number="evaluationRh.salaireMaxEstime"
              class="w-full"
              type="number"
              min="0"
              step="1000"
            />
          </div>
        </div>
      </template>

      <div v-if="action.etapeCourante === 'FINANCE'" class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="montant-file">Montant verifie (Ar)</label>
        <PrimeInputText
          id="montant-file"
          v-model.number="montantVerifie"
          class="w-full"
          type="number"
          min="0"
          step="1000"
          :placeholder="String(budgetDe(action) ?? '')"
        />
        <span class="text-xs text-muted-color">La disponibilite budgetaire est verifiee automatiquement.</span>
      </div>

      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="commentaire-file">Commentaire</label>
        <PrimeTextarea id="commentaire-file" v-model="commentaire" class="w-full" rows="3" />
      </div>

      <template #actions>
        <PrimeButton class="inline-flex items-center justify-center gap-2 border border-surface" @click="action = null">Annuler</PrimeButton>
        <PrimeButton class="inline-flex items-center justify-center gap-2 bg-primary text-primary-contrast" :disabled="enregistrement" @click="enregistrer">
          Enregistrer la decision
        </PrimeButton>
      </template>
    </ModalDialog>
  </div>
</template>