<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Select from 'primevue/select'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDate, formatDateHeure, formatMontant } from '@/shared/utils/format'
import { embaucheApi as api, ErreurApi } from '../api/embaucheApi'

const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationStore()

const propositions = ref([])
const chargement = ref(false)
const statut = ref('')

const STATUTS = [
  { valeur: '', libelle: 'Tous les statuts' },
  { valeur: 'BROUILLON', libelle: 'Brouillon' },
  { valeur: 'VALIDEE_INTERNE', libelle: 'Validée en interne' },
  { valeur: 'ENVOYEE', libelle: 'Envoyée au candidat' },
  { valeur: 'ACCEPTEE', libelle: 'Acceptée' },
  { valeur: 'REFUSEE', libelle: 'Refusée' },
  { valeur: 'CONTRE_PROPOSEE', libelle: 'Contre-proposition' },
  { valeur: 'EXPIREE', libelle: 'Expirée' },
  { valeur: 'RETIREE', libelle: 'Retirée' },
]

const peutCreer = computed(() => auth.estRh || auth.estDirection)

async function charger() {
  chargement.value = true
  try {
    const suffixe = statut.value ? `?statut=${statut.value}` : ''
    propositions.value = await api.get(`/embauche/propositions${suffixe}`)
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger les propositions.')
  } finally {
    chargement.value = false
  }
}

function ouvrir(proposition) {
  router.push({ name: 'embauche-proposition-detail', params: { id: proposition.id } })
}

function creer() {
  router.push({ name: 'embauche-proposition-nouvelle' })
}

onMounted(charger)
</script>

<template>
  <section>
    <header class="mb-5 flex flex-col justify-between gap-4 md:flex-row md:items-start">
      <div>
        <h1 class="m-0 text-2xl font-bold">Propositions d’embauche</h1>
        <p class="mt-1 mb-0 text-muted-color">
          Créez, validez et envoyez les propositions issues des candidatures retenues.
        </p>
      </div>
      <div class="flex flex-wrap gap-2">
        <Button label="Actualiser" icon="pi pi-refresh" severity="secondary" outlined :loading="chargement" @click="charger" />
        <Button v-if="peutCreer" label="Nouvelle proposition" icon="pi pi-plus" @click="creer" />
      </div>
    </header>

    <Card>
      <template #content>
        <div class="mb-4 flex flex-wrap items-end gap-3">
          <div class="flex min-w-56 flex-col gap-2">
            <label class="text-sm font-semibold text-muted-color" for="statut-proposition">Statut</label>
            <Select id="statut-proposition" v-model="statut" :options="STATUTS" option-label="libelle"
              option-value="valeur" class="w-full" @change="charger" />
          </div>
        </div>

        <div v-if="chargement" class="py-8 text-center text-muted-color">Chargement des propositions...</div>
        <EmptyState v-else-if="!propositions.length" titre="Aucune proposition"
          message="Aucune proposition ne correspond au filtre sélectionné.">
          <Button v-if="peutCreer" label="Créer une proposition" icon="pi pi-plus" @click="creer" />
        </EmptyState>

        <DataTable v-else :value="propositions" data-key="id" striped-rows row-hover paginator :rows="10"
          :rows-per-page-options="[10, 25, 50]" responsive-layout="scroll">
          <Column field="reference" header="Référence" sortable>
            <template #body="{ data }">
              <div class="font-semibold">{{ data.reference }}</div>
              <div class="text-xs text-muted-color">Version {{ data.version }}</div>
            </template>
          </Column>
          <Column header="Candidat" sortable sort-field="candidat">
            <template #body="{ data }">
              <div class="font-medium">{{ data.candidat || '—' }}</div>
              <div class="text-xs text-muted-color">{{ data.email || '' }}</div>
            </template>
          </Column>
          <Column header="Poste"><template #body="{ data }">{{ data.poste || '—' }}</template></Column>
          <Column header="Contrat"><template #body="{ data }">{{ data.typeContrat || '—' }}</template></Column>
          <Column header="Entrée prévue">
            <template #body="{ data }">{{ formatDate(data.dateDebutPrevue) }}</template>
          </Column>
          <Column header="Salaire">
            <template #body="{ data }">{{ formatMontant(data.salaireBrutMensuel) }}</template>
          </Column>
          <Column field="statut" header="Statut" sortable>
            <template #body="{ data }"><StatusBadge :valeur="data.statut" /></template>
          </Column>
          <Column header="Expiration">
            <template #body="{ data }">{{ formatDateHeure(data.dateExpiration) }}</template>
          </Column>
          <Column header="Action">
            <template #body="{ data }">
              <Button label="Ouvrir" icon="pi pi-eye" size="small" outlined @click="ouvrir(data)" />
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </section>
</template>
