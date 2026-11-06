<script setup>
import PrimeSelect from 'primevue/select'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { besoinApi as api } from '@/modules/besoin/api/besoinApi'
import EmptyState from '@/shared/components/EmptyState.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDate, formatMontant } from '@/shared/utils/format'

const auth = useAuthStore()
const notifications = useNotificationStore()

const demandes = ref([])
const chargement = ref(true)
const statutFiltre = ref('')
const recherche = ref('')

const STATUTS = [
  { valeur: '', libelle: 'Tous les statuts' },
  { valeur: 'BROUILLON', libelle: 'Brouillon' },
  { valeur: 'EN_VALIDATION', libelle: 'En validation' },
  { valeur: 'A_CORRIGER', libelle: 'A corriger' },
  { valeur: 'FINALISEE', libelle: 'Finalisee' },
  { valeur: 'FINALISEE_PARTIELLE', libelle: 'Finalisee partiellement' },
  { valeur: 'REFUSEE', libelle: 'Refusee' },
  { valeur: 'ANNULEE', libelle: 'Annulee' },
]

async function charger() {
  chargement.value = true
  try {
    const parametres = new URLSearchParams()
    if (statutFiltre.value) {
      parametres.set('statut', statutFiltre.value)
    }
    if (recherche.value.trim()) {
      parametres.set('recherche', recherche.value.trim())
    }
    const suffixe = parametres.toString() ? `?${parametres}` : ''
    demandes.value = await api.get(`/demandes${suffixe}`)
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

const totalBudget = computed(() =>
  demandes.value.reduce((somme, demande) => somme + Number(demande.budgetTotal || 0), 0),
)
</script>

<template>
  <div>
    <div class="flex flex-col gap-4 mb-5 md:flex-row md:items-start md:justify-between">
      <div>
        <h1>Demandes de recrutement</h1>
        <p class="mt-1 text-sm text-muted-color">
          <template v-if="auth.estManager"
            >Vous visualisez uniquement vos propres demandes.</template
          >
          <template v-else>Vous visualisez l'ensemble des demandes de l'entreprise.</template>
        </p>
      </div>
      <PrimeButton
        as="router-link"
        :to="{ name: 'demande-nouvelle' }"
        label="Nouvelle demande"
        icon="pi pi-plus"
      />
    </div>

    <div class="flex flex-wrap items-end gap-3 mb-4">
      <div class="flex flex-col gap-2 mb-4 flex-1 min-w-60">
        <label class="text-sm font-semibold text-muted-color" for="recherche">Recherche</label>
        <PrimeInputText
          id="recherche"
          v-model="recherche"
          class="w-full"
          type="search"
          placeholder="Motif ou demandeur"
          @keyup.enter="charger"
        />
      </div>
      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="statut">Statut</label>
        <PrimeSelect
          id="statut"
          v-model="statutFiltre"
          class="w-full"
          :options="STATUTS"
          option-label="libelle"
          option-value="valeur"
          @change="charger"
        />
      </div>
      <PrimeButton
        class="inline-flex items-center justify-center gap-2 border border-surface"
        @click="charger"
        >Filtrer</PrimeButton
      >
    </div>

    <section class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900">
      <div class="flex items-center justify-between gap-3 border-b border-surface p-4">
        <h2>{{ demandes.length }} demande(s)</h2>
        <span class="text-secondary-color">Budget total : {{ formatMontant(totalBudget) }}</span>
      </div>

      <div v-if="chargement" class="p-4 text-sm text-muted-color">Chargement des demandes...</div>

      <EmptyState
        v-else-if="demandes.length === 0"
        titre="Aucune demande"
        message="Aucune demande ne correspond aux criteres selectionnes."
      />

      <DataTable
        v-else
        :value="demandes"
        striped-rows
        paginator
        :rows="10"
        responsive-layout="scroll"
      >
        <Column header="Reference">
          <template #body="{ data }">DEM-{{ String(data.id).padStart(4, '0') }}</template>
        </Column>
        <Column field="motif" header="Motif" />
        <Column field="demandeur" header="Demandeur" />
        <Column header="Date souhaitee">
          <template #body="{ data }">{{ formatDate(data.dateSouhaitee) }}</template>
        </Column>
        <Column header="Lignes">
          <template #body="{ data }">
            {{ data.nombreLignes }}
            <span class="text-xs text-muted-color"
              >({{ data.lignesAcceptees }} acceptee(s), {{ data.lignesEnValidation }} en
              cours)</span
            >
          </template>
        </Column>
        <Column header="Budget">
          <template #body="{ data }">{{ formatMontant(data.budgetTotal) }}</template>
        </Column>
        <Column header="Statut">
          <template #body="{ data }"><StatusBadge :valeur="data.statut" /></template>
        </Column>
        <Column header="">
          <template #body="{ data }">
            <RouterLink :to="{ name: 'demande-detail', params: { id: data.id } }">
              <PrimeButton label="Ouvrir" severity="secondary" size="small" outlined />
            </RouterLink>
          </template>
        </Column>
      </DataTable>
    </section>
  </div>
</template>
