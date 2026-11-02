<script setup>
import PrimeSelect from 'primevue/select'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import { computed, onMounted, ref } from 'vue'
import { besoinApi as api } from '@/modules/besoin/api/besoinApi'
import EmptyState from '@/shared/components/EmptyState.vue'
import ModalDialog from '@/shared/components/ModalDialog.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatMontant } from '@/shared/utils/format'

const auth = useAuthStore()
const notifications = useNotificationStore()

const budgets = ref([])
const services = ref([])
const chargement = ref(true)
const exercice = ref(new Date().getFullYear())
const edition = ref(null)
const enregistrement = ref(false)
const formulaire = ref({ idService: null, exercice: null, montantAlloue: null })

async function charger() {
  chargement.value = true
  try {
    const [listeBudgets, listeServices] = await Promise.all([
      api.get(`/budgets?exercice=${exercice.value}`),
      api.get('/referentiel/services'),
    ])
    budgets.value = listeBudgets
    services.value = listeServices
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

const totalAlloue = computed(() =>
  budgets.value.reduce((somme, budget) => somme + Number(budget.montantAlloue || 0), 0),
)
const totalDisponible = computed(() =>
  budgets.value.reduce((somme, budget) => somme + Number(budget.montantDisponible || 0), 0),
)
const totalEngage = computed(() =>
  budgets.value.reduce((somme, budget) => somme + Number(budget.montantEngage || 0), 0),
)

/** Pourcentage engage par service, pour visualiser la tension budgetaire. */
function taux(budget) {
  if (!budget.montantAlloue) {
    return 0
  }
  return Math.min(100, Math.round((Number(budget.montantEngage) / Number(budget.montantAlloue)) * 100))
}

function classeTaux(budget) {
  const valeur = taux(budget)
  if (valeur >= 90) return 'bg-red-500'
  if (valeur >= 70) return 'bg-yellow-500'
  return ''
}

function nouvelleEdition() {
  edition.value = { id: null }
  formulaire.value = {
    idService: null,
    exercice: exercice.value,
    montantAlloue: null,
  }
}

function modifier(budget) {
  edition.value = { id: budget.id }
  formulaire.value = {
    idService: budget.idService,
    exercice: budget.exercice,
    montantAlloue: budget.montantAlloue,
  }
}

async function enregistrer() {
  if (!edition.value) {
    return
  }
  enregistrement.value = true
  try {
    await api.put('/budgets', {
      idService: formulaire.value.idService,
      exercice: Number(formulaire.value.exercice),
      montantAlloue: Number(formulaire.value.montantAlloue),
    })
    notifications.succes('Le budget a ete enregistre.')
    edition.value = null
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
        <h1>Budgets</h1>
        <p class="mt-1 text-sm text-muted-color">
          Allocation annuelle par service. Le disponible se recalcule automatiquement : disponible =
          alloue − engage − depense.
        </p>
      </div>
      <PrimeButton v-if="auth.estFinance" class="inline-flex items-center justify-center gap-2 bg-primary text-primary-contrast" @click="nouvelleEdition">
          Definir un budget
      </PrimeButton>
    </div>

    <div class="flex flex-wrap items-end gap-3 mb-4">
      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="exercice-budget">Exercice</label>
        <PrimeInputText id="exercice-budget" v-model.number="exercice" class="w-full" type="number" min="2020" max="2100" @change="charger" />
      </div>
      <PrimeButton class="inline-flex items-center justify-center gap-2 border border-surface" @click="charger">Afficher</PrimeButton>
    </div>

    <div class="grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3" style="margin-bottom: 16px">
      <div class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900 p-4">
        <div class="text-xs font-semibold uppercase tracking-wide text-muted-color">Budget alloue</div>
        <div class="mt-1 text-2xl font-semibold" style="font-size: 20px">{{ formatMontant(totalAlloue) }}</div>
      </div>
      <div class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900 p-4">
        <div class="text-xs font-semibold uppercase tracking-wide text-muted-color">Budget engage</div>
        <div class="mt-1 text-2xl font-semibold" style="font-size: 20px">{{ formatMontant(totalEngage) }}</div>
      </div>
      <div class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900 p-4">
        <div class="text-xs font-semibold uppercase tracking-wide text-muted-color">Budget disponible</div>
        <div class="mt-1 text-2xl font-semibold" style="font-size: 20px">{{ formatMontant(totalDisponible) }}</div>
      </div>
    </div>

    <section class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900">
      <div class="flex items-center justify-between gap-3 border-b border-surface p-4"><h2>Budgets par service — exercice {{ exercice }}</h2></div>

      <div v-if="chargement" class="p-4 text-sm text-muted-color">Chargement des budgets...</div>

      <EmptyState
        v-else-if="budgets.length === 0"
        titre="Aucun budget"
        message="Aucun budget n'est defini pour cet exercice."
      />

      <DataTable v-else :value="budgets" striped-rows responsive-layout="scroll">
        <Column field="service" header="Service" />
        <Column field="departement" header="Departement" />
        <Column header="Alloue"><template #body="{ data }">{{ formatMontant(data.montantAlloue) }}</template></Column>
        <Column header="Depense"><template #body="{ data }">{{ formatMontant(data.montantDepense) }}</template></Column>
        <Column header="Engage"><template #body="{ data }">{{ formatMontant(data.montantEngage) }}</template></Column>
        <Column header="Disponible"><template #body="{ data }"><span class="font-semibold">{{ formatMontant(data.montantDisponible) }}</span></template></Column>
        <Column header="Consommation">
          <template #body="{ data }">
            <div class="min-w-36">
              <div class="h-2 overflow-hidden rounded-full bg-surface-200 dark:bg-surface-700"><div class="h-full bg-primary" :class="classeTaux(data)" :style="{ width: `${taux(data)}%` }" /></div>
              <div class="mt-1 text-xs text-muted-color">{{ taux(data) }} % engage</div>
            </div>
          </template>
        </Column>
        <Column v-if="auth.estFinance" header="">
          <template #body="{ data }"><PrimeButton label="Modifier" severity="secondary" size="small" outlined @click="modifier(data)" /></template>
        </Column>
      </DataTable>
    </section>

    <ModalDialog
      v-if="edition"
      titre="Definir le budget d'un service"
      :fermable="!enregistrement"
      @fermer="edition = null"
    >
      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="service-budget">Service <span class="text-red-500">*</span></label>
        <PrimeSelect id="service-budget" v-model="formulaire.idService" class="w-full" :options="services" option-label="nom" option-value="id" placeholder="Choisir un service" :disabled="!!edition.id" />
      </div>

      <div class="grid grid-cols-1 gap-x-4 gap-y-1 md:grid-cols-2">
        <div class="flex flex-col gap-2 mb-4">
          <label class="text-sm font-semibold text-muted-color" for="exercice-form">Exercice <span class="text-red-500">*</span></label>
          <PrimeInputText id="exercice-form" v-model.number="formulaire.exercice" class="w-full" type="number" min="2020" max="2100" />
        </div>
        <div class="flex flex-col gap-2 mb-4">
          <label class="text-sm font-semibold text-muted-color" for="montant-form">Montant alloue (Ar) <span class="text-red-500">*</span></label>
          <PrimeInputText id="montant-form" v-model.number="formulaire.montantAlloue" class="w-full" type="number" min="0" step="100000" />
        </div>
      </div>

      <template #actions>
        <PrimeButton class="inline-flex items-center justify-center gap-2 border border-surface" @click="edition = null">Annuler</PrimeButton>
        <PrimeButton class="inline-flex items-center justify-center gap-2 bg-primary text-primary-contrast" :disabled="enregistrement" @click="enregistrer">
          Enregistrer
        </PrimeButton>
      </template>
    </ModalDialog>
  </div>
</template>
