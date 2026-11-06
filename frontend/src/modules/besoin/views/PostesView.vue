<script setup>
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import PrimeSelect from 'primevue/select'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import { computed, onMounted, ref } from 'vue'
import { besoinApi as api } from '@/modules/besoin/api/besoinApi'
import EmptyState from '@/shared/components/EmptyState.vue'
import ModalDialog from '@/shared/components/ModalDialog.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatMontant } from '@/shared/utils/format'

const auth = useAuthStore()
const notifications = useNotificationStore()

const postes = ref([])
const services = ref([])
const chargement = ref(true)
const filtreService = ref('')
const filtreActif = ref('')
const recherche = ref('')

const edition = ref(null)
const enregistrement = ref(false)
const formulaire = ref({
  idService: null,
  intitule: '',
  description: '',
  salaireMin: null,
  salaireMax: null,
  actif: true,
})

const filtres = computed(() => {
  const parametres = new URLSearchParams()
  if (filtreService.value) {
    parametres.set('idService', filtreService.value)
  }
  if (filtreActif.value !== '') {
    parametres.set('actif', filtreActif.value)
  }
  if (recherche.value.trim()) {
    parametres.set('recherche', recherche.value.trim())
  }
  const suffixe = parametres.toString() ? `?${parametres}` : ''
  return suffixe
})

async function charger() {
  chargement.value = true
  try {
    const [listePostes, listeServices] = await Promise.all([
      api.get(`/postes${filtres.value}`),
      api.get('/referentiel/services'),
    ])
    postes.value = listePostes
    services.value = listeServices
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

function nouvelleEdition() {
  edition.value = { id: null }
  formulaire.value = {
    idService: filtreService.value || null,
    intitule: '',
    description: '',
    salaireMin: null,
    salaireMax: null,
    actif: true,
  }
}

function modifier(poste) {
  edition.value = { id: poste.id }
  formulaire.value = {
    idService: poste.idService,
    intitule: poste.intitule,
    description: poste.description || '',
    salaireMin: poste.salaireMin,
    salaireMax: poste.salaireMax,
    actif: poste.actif,
  }
}

async function enregistrer() {
  if (!edition.value) {
    return
  }
  enregistrement.value = true
  try {
    if (edition.value.id) {
      await api.put(`/postes/${edition.value.id}`, {
        intitule: formulaire.value.intitule.trim(),
        description: formulaire.value.description.trim() || null,
        salaireMin: formulaire.value.salaireMin,
        salaireMax: formulaire.value.salaireMax,
        actif: formulaire.value.actif,
      })
      notifications.succes('Le poste a ete mis a jour.')
    } else {
      await api.post('/postes', {
        idService: formulaire.value.idService,
        intitule: formulaire.value.intitule.trim(),
        description: formulaire.value.description.trim() || null,
        salaireMin: formulaire.value.salaireMin,
        salaireMax: formulaire.value.salaireMax,
      })
      notifications.succes('Le poste a ete cree.')
    }
    edition.value = null
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    enregistrement.value = false
  }
}

async function desactiver(poste) {
  try {
    await api.post(`/postes/${poste.id}/desactiver`)
    notifications.succes('Le poste a ete desactive.')
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  }
}
</script>

<template>
  <div>
    <div class="flex flex-col gap-4 mb-5 md:flex-row md:items-start md:justify-between">
      <div>
        <h1>Catalogue des postes</h1>
        <p class="mt-1 text-sm text-muted-color">
          Postes officiels de l'entreprise. Un poste utilise par l'historique est desactive, jamais supprime.
        </p>
      </div>
      <PrimeButton v-if="auth.estRh" class="inline-flex items-center justify-center gap-2 bg-primary text-primary-contrast" @click="nouvelleEdition">
        Nouveau poste
      </PrimeButton>
    </div>

    <div class="flex flex-wrap items-end gap-3 mb-4">
      <div class="flex flex-col gap-2 mb-4 flex-1 min-w-60">
        <label class="text-sm font-semibold text-muted-color" for="recherche-poste">Recherche</label>
        <PrimeInputText
          id="recherche-poste"
          v-model="recherche"
          class="w-full"
          type="search"
          placeholder="Intitule ou description"
          @keyup.enter="charger"
        />
      </div>
      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="service-poste">Service</label>
        <PrimeSelect id="service-poste" v-model="filtreService" class="w-full" :options="[{ id: '', nom: 'Tous les services' }, ...services]" option-label="nom" option-value="id" @change="charger" />
      </div>
      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="actif-poste">Etat</label>
        <PrimeSelect id="actif-poste" v-model="filtreActif" class="w-full" :options="[{ label: 'Tous', value: '' }, { label: 'Actifs', value: 'true' }, { label: 'Inactifs', value: 'false' }]" option-label="label" option-value="value" @change="charger" />
      </div>
      <PrimeButton class="inline-flex items-center justify-center gap-2 border border-surface" @click="charger">Filtrer</PrimeButton>
    </div>

    <section class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900">
      <div class="flex items-center justify-between gap-3 border-b border-surface p-4"><h2>{{ postes.length }} poste(s)</h2></div>

      <div v-if="chargement" class="p-4 text-sm text-muted-color">Chargement du catalogue...</div>

      <EmptyState
        v-else-if="postes.length === 0"
        titre="Aucun poste"
        message="Aucun poste ne correspond aux criteres selectionnes."
      />

      <div v-else class="overflow-x-auto">
        <DataTable :value="postes" striped-rows paginator :rows="10" responsive-layout="scroll">
          <Column header="Intitule">
            <template #body="{ data }"><div class="font-medium">{{ data.intitule }}</div><div v-if="data.description" class="text-xs text-muted-color">{{ data.description }}</div></template>
          </Column>
          <Column field="service" header="Service" />
          <Column field="departement" header="Departement" />
          <Column header="Salaire min"><template #body="{ data }">{{ formatMontant(data.salaireMin) }}</template></Column>
          <Column header="Salaire max"><template #body="{ data }">{{ formatMontant(data.salaireMax) }}</template></Column>
          <Column field="nbPostesLies" header="Demandes liees" />
          <Column header="Etat"><template #body="{ data }"><Tag :value="data.actif ? 'Actif' : 'Desactive'" :severity="data.actif ? 'success' : 'secondary'" rounded /></template></Column>
          <Column v-if="auth.estRh" header="">
            <template #body="{ data }"><div class="flex justify-end gap-2"><PrimeButton label="Modifier" severity="secondary" size="small" outlined @click="modifier(data)" /><PrimeButton v-if="data.actif" label="Desactiver" severity="danger" size="small" text @click="desactiver(data)" /></div></template>
          </Column>
        </DataTable>
      </div>
    </section>

    <ModalDialog
      v-if="edition"
      :titre="edition.id ? 'Modifier le poste' : 'Nouveau poste'"
      :fermable="!enregistrement"
      @fermer="edition = null"
    >
      <div v-if="!edition.id" class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="service-form">Service <span class="text-red-500">*</span></label>
        <PrimeSelect id="service-form" v-model="formulaire.idService" class="w-full" :options="services" option-label="nom" option-value="id" placeholder="Choisir un service" />
      </div>

      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="intitule-form">Intitule <span class="text-red-500">*</span></label>
        <PrimeInputText id="intitule-form" v-model="formulaire.intitule" class="w-full" type="text" />
      </div>

      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="description-form">Description</label>
        <PrimeTextarea id="description-form" v-model="formulaire.description" class="w-full" rows="3" />
      </div>

      <div class="grid grid-cols-1 gap-x-4 gap-y-1 md:grid-cols-2">
        <div class="flex flex-col gap-2 mb-4">
          <label class="text-sm font-semibold text-muted-color" for="salaire-min-form">Salaire minimum (Ar)</label>
          <PrimeInputText id="salaire-min-form" v-model.number="formulaire.salaireMin" class="w-full" type="number" min="0" step="1000" />
        </div>
        <div class="flex flex-col gap-2 mb-4">
          <label class="text-sm font-semibold text-muted-color" for="salaire-max-form">Salaire maximum (Ar)</label>
          <PrimeInputText id="salaire-max-form" v-model.number="formulaire.salaireMax" class="w-full" type="number" min="0" step="1000" />
        </div>
      </div>

      <div v-if="edition.id" class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="actif-form">Etat du poste</label>
        <PrimeSelect id="actif-form" v-model="formulaire.actif" class="w-full" :options="[{ label: 'Actif', value: true }, { label: 'Desactive', value: false }]" option-label="label" option-value="value" />
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
