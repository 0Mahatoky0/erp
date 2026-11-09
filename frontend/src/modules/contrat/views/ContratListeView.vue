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
import { formatDate, formatMontant } from '@/shared/utils/format'
import { contratApi as api, ErreurApi } from '../api/contratApi'

const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationStore()

const contrats = ref([])
const alertes = ref([])
const employes = ref([])
const chargement = ref(false)
const etat = ref('')

const ETATS = [
  { valeur: '', libelle: 'Tous les états' },
  { valeur: 'CREE', libelle: 'Créé' },
  { valeur: 'SIGNE', libelle: 'Signé' },
  { valeur: 'VALIDE', libelle: 'Validé' },
  { valeur: 'ANNULE', libelle: 'Annulé' },
]

const peutAgir = computed(() => auth.estRh || auth.estDirection)

const employesParId = computed(() => {
  const map = new Map()
  for (const employe of employes.value) map.set(employe.id, employe)
  return map
})

const alertesGraves = computed(() =>
  alertes.value.filter((alerte) => alerte.niveau === 'DANGER').slice(0, 5),
)

function nomEmploye(id) {
  const employe = employesParId.value.get(id)
  return employe ? `${employe.prenom || ''} ${employe.nom || ''}`.trim() : `Employé #${id}`
}

async function charger() {
  chargement.value = true
  try {
    const suffixe = etat.value ? `?etat=${etat.value}` : ''
    const [listeContrats, listeAlertes, listeEmployes] = await Promise.all([
      api.get(`/contrats${suffixe}`),
      api.get('/contrats/alertes'),
      api.get('/referentiel/employes'),
    ])
    contrats.value = listeContrats
    alertes.value = listeAlertes
    employes.value = listeEmployes
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger les contrats.')
  } finally {
    chargement.value = false
  }
}

function ouvrir(contrat) {
  router.push({ name: 'contrat-detail', params: { id: contrat.id } })
}

function ouvrirOnboarding() {
  router.push({ name: 'contrat-onboarding-liste' })
}

onMounted(charger)
</script>

<template>
  <section>
    <header class="mb-5 flex flex-col justify-between gap-4 md:flex-row md:items-start">
      <div>
        <h1 class="m-0 text-2xl font-bold">Contrats</h1>
        <p class="mt-1 mb-0 text-muted-color">
          Suivez le cycle de vie des contrats : signature, validation, avenants, renouvellements et échéances.
        </p>
      </div>
      <div class="flex flex-wrap gap-2">
        <Button label="Actualiser" icon="pi pi-refresh" severity="secondary" outlined :loading="chargement" @click="charger" />
        <Button label="Onboarding" icon="pi pi-list-check" severity="secondary" outlined @click="ouvrirOnboarding" />
      </div>
    </header>

    <Card v-if="alertesGraves.length" class="mb-5 border border-red-300">
      <template #title>Alertes à traiter</template>
      <template #content>
        <ul class="m-0 flex list-none flex-col gap-2 p-0">
          <li v-for="(alerte, index) in alertesGraves" :key="`${alerte.categorie}-${alerte.idContrat}-${index}`"
            class="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-900">
            <strong>{{ alerte.categorie }} :</strong> {{ alerte.message }}
          </li>
        </ul>
      </template>
    </Card>

    <Card>
      <template #content>
        <div class="mb-4 flex flex-wrap items-end gap-3">
          <div class="flex min-w-56 flex-col gap-2">
            <label class="text-sm font-semibold text-muted-color" for="etat-contrat">État</label>
            <Select id="etat-contrat" v-model="etat" :options="ETATS" option-label="libelle"
              option-value="valeur" class="w-full" @change="charger" />
          </div>
          <div class="text-sm text-muted-color">
            {{ contrats.length }} contrat(s) · {{ alertes.length }} alerte(s)
          </div>
        </div>

        <div v-if="chargement" class="py-8 text-center text-muted-color">Chargement des contrats...</div>
        <EmptyState v-else-if="!contrats.length" titre="Aucun contrat"
          message="Aucun contrat ne correspond au filtre sélectionné." />

        <DataTable v-else :value="contrats" data-key="id" striped-rows row-hover paginator :rows="10"
          :rows-per-page-options="[10, 25, 50]" responsive-layout="scroll">
          <Column field="numero" header="Numéro" sortable>
            <template #body="{ data }">
              <div class="font-semibold">{{ data.numero }}</div>
              <div class="text-xs text-muted-color">Rang {{ data.rang }}</div>
            </template>
          </Column>
          <Column header="Salarié">
            <template #body="{ data }">{{ nomEmploye(data.idEmploye) }}</template>
          </Column>
          <Column field="typeContrat" header="Type">
            <template #body="{ data }"><StatusBadge :valeur="data.typeContrat" /></template>
          </Column>
          <Column header="Début">
            <template #body="{ data }">{{ formatDate(data.dateDebut) }}</template>
          </Column>
          <Column header="Fin prévue">
            <template #body="{ data }">{{ formatDate(data.dateFinPrevue) }}</template>
          </Column>
          <Column header="Salaire">
            <template #body="{ data }">{{ formatMontant(data.salaireBase) }}</template>
          </Column>
          <Column field="etat" header="État" sortable>
            <template #body="{ data }"><StatusBadge :valeur="data.etat" /></template>
          </Column>
          <Column header="Exécution">
            <template #body="{ data }"><StatusBadge :valeur="data.statutExecution" /></template>
          </Column>
          <Column header="Action">
            <template #body="{ data }">
              <Button label="Ouvrir" icon="pi pi-eye" size="small" outlined @click="ouvrir(data)" />
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <p v-if="!peutAgir" class="mt-3 text-sm text-muted-color">
      Consultation seule : les actions de signature et de validation sont réservées aux rôles RH et Direction.
    </p>
  </section>
</template>
