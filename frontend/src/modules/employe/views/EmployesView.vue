<script setup>
import PrimeSelect from 'primevue/select'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { employeApi as api } from '@/modules/employe/api/employeApi'
import EmptyState from '@/shared/components/EmptyState.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDate } from '@/shared/utils/format'

const router = useRouter()
const notifications = useNotificationStore()

const employes = ref([])
const services = ref([])
const chargement = ref(true)
const filtreService = ref('')
const filtreStatut = ref('')
const recherche = ref('')

const STATUTS = [
  { valeur: '', libelle: 'Tous les statuts' },
  { valeur: 'PRE_EMBAUCHE', libelle: 'Pre-embauche' },
  { valeur: 'ACTIF', libelle: 'Actif' },
  { valeur: 'INACTIF', libelle: 'Inactif' },
  { valeur: 'PARTI', libelle: 'Parti' },
  { valeur: 'NON_PRIS_POSTE', libelle: 'Non pris de poste' },
]

async function charger() {
  chargement.value = true
  try {
    const parametres = new URLSearchParams()
    if (filtreService.value) {
      parametres.set('idService', filtreService.value)
    }
    if (filtreStatut.value) {
      parametres.set('statut', filtreStatut.value)
    }
    if (recherche.value.trim()) {
      parametres.set('recherche', recherche.value.trim())
    }
    const suffixe = parametres.toString() ? `?${parametres}` : ''
    const [listeEmployes, listeServices] = await Promise.all([
      api.get(`/referentiel/employes${suffixe}`),
      api.get('/referentiel/services'),
    ])
    employes.value = listeEmployes
    services.value = listeServices
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)
</script>

<template>
  <div>
    <div class="flex flex-col gap-4 mb-5 md:flex-row md:items-start md:justify-between">
      <div>
        <h1>Employes</h1>
        <p class="mt-1 text-sm text-muted-color">
          Fiches des collaborateurs. Ces employes peuvent etre designes comme personnes remplacees
          dans une demande.
        </p>
      </div>
    </div>

   <div class="flex flex-wrap items-end gap-3 mb-4">
  <div class="flex flex-col gap-2 flex-1 min-w-60">
    <label class="text-sm font-semibold text-muted-color" for="recherche-employe">Recherche</label>
    <PrimeInputText
      id="recherche-employe"
      v-model="recherche"
      class="w-full"
      type="search"
      placeholder="Nom, prenom ou matricule"
      @keyup.enter="charger"
    />
  </div>

  <div class="flex flex-col gap-2 w-full sm:w-56">
    <label class="text-sm font-semibold text-muted-color" for="service-employe">Service</label>
    <PrimeSelect
      id="service-employe"
      v-model="filtreService"
      class="w-full"
      :options="[{ id: '', nom: 'Tous les services' }, ...services]"
      option-label="nom"
      option-value="id"
      @change="charger"
    />
  </div>

  <div class="flex flex-col gap-2 w-full sm:w-48">
    <label class="text-sm font-semibold text-muted-color" for="statut-employe">Statut</label>
    <PrimeSelect
      id="statut-employe"
      v-model="filtreStatut"
      class="w-full"
      :options="STATUTS"
      option-label="libelle"
      option-value="valeur"
      @change="charger"
    />
  </div>

  <PrimeButton label="Filtrer" icon="pi pi-filter" severity="secondary" variant="outlined" @click="charger" />
</div>

    <section class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900">
      <div class="flex items-center justify-between gap-3 border-b border-surface p-4"><h2>{{ employes.length }} employe(s)</h2></div>

      <div v-if="chargement" class="p-4 text-sm text-muted-color">Chargement des employes...</div>

      <EmptyState
        v-else-if="employes.length === 0"
        titre="Aucun employe"
        message="Aucun employe ne correspond aux criteres selectionnes."
      />

      <DataTable v-else :value="employes" striped-rows paginator :rows="10" responsive-layout="scroll">
        <Column field="matricule" header="Matricule" />
        <Column header="Nom">
          <template #body="{ data }"><span class="font-medium">{{ data.nom }} {{ data.prenom }}</span></template>
        </Column>
        <Column header="Poste"><template #body="{ data }">{{ data.poste || '—' }}</template></Column>
        <Column header="Service"><template #body="{ data }">{{ data.service || '—' }}</template></Column>
        <Column header="Contact">
          <template #body="{ data }">
            <div>{{ data.email || '—' }}</div>
            <div class="text-xs text-muted-color">{{ data.telephone || '' }}</div>
          </template>
        </Column>
        <Column header="Date d'embauche"><template #body="{ data }">{{ formatDate(data.dateEmbauche) }}</template></Column>
        <Column header="Statut"><template #body="{ data }"><StatusBadge :valeur="data.statut" /></template></Column>
        <Column header="Actions">
          <template #body="{ data }">
            <PrimeButton label="Dossier" icon="pi pi-folder-open" size="small" outlined
              @click="router.push({ name: 'employe-dossier', params: { id: data.id } })" />
          </template>
        </Column>
      </DataTable>
    </section>
  </div>
</template>
