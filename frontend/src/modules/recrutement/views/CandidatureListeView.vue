<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Card from 'primevue/card'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Button from 'primevue/button'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import EmptyState from '@/shared/components/EmptyState.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { recrutementApi as api, ErreurApi } from '../api/recrutementApi'

const router = useRouter()
const notifications = useNotificationStore()
const offres = ref([])
const candidatures = ref([])
const recherche = ref('')
const idOffre = ref('')
const statut = ref('')
const loading = ref(false)

const statuts = [
  { valeur: '', libelle: 'Tous les statuts' },
  { valeur: 'RECUE', libelle: 'Reçue' },
  { valeur: 'EN_ETUDE', libelle: 'En étude' },
  { valeur: 'PRESELECTIONNEE', libelle: 'Présélectionnée' },
  { valeur: 'EN_EVALUATION', libelle: 'En évaluation' },
  { valeur: 'LISTE_ATTENTE', libelle: 'Liste d’attente' },
  { valeur: 'RETENUE', libelle: 'Retenue' },
  { valeur: 'REFUSEE', libelle: 'Refusée' },
  { valeur: 'RETIREE', libelle: 'Retirée' },
]

const offresOptions = computed(() => [
  { idOffre: '', libelle: 'Toutes les offres' },
  ...offres.value.map((offre) => ({
    idOffre: String(offre.idOffre),
    libelle: `${offre.reference} · ${offre.titre}`,
  })),
])

const filtrees = computed(() => {
  const terme = recherche.value.trim().toLowerCase()
  return candidatures.value.filter((candidature) => {
    const candidat = candidature.candidat || {}
    const texte = [candidat.nom, candidat.prenom, candidat.email, candidature.offre?.titre]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return (
      (!terme || texte.includes(terme)) &&
      (!idOffre.value || String(candidature.offre?.idOffre) === idOffre.value) &&
      (!statut.value || candidature.statut === statut.value)
    )
  })
})

async function charger() {
  loading.value = true
  try {
    offres.value = await api.get('/recrutement/offres')
    const resultats = await Promise.all(
      offres.value.map((offre) =>
        api.get(`/recrutement/offres/${offre.idOffre}/candidatures`),
      ),
    )
    candidatures.value = resultats.flat()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger les candidatures.')
  } finally {
    loading.value = false
  }
}

function ouvrir(candidature) {
  router.push({
    name: 'recrutement-candidature-fiche',
    params: { idCandidature: candidature.idCandidature },
  })
}

onMounted(charger)
</script>

<template>
  <section>
    <header class="mb-5 flex flex-col justify-between gap-4 md:flex-row md:items-start">
      <div>
        <h1 class="m-0 text-2xl font-bold">Candidatures</h1>
        <p class="mt-1 mb-0 text-muted-color">Liste filtrable et suivi du cycle de recrutement.</p>
      </div>
      <Button label="Actualiser" icon="pi pi-refresh" severity="secondary" outlined :loading="loading" @click="charger" />
    </header>

    <Card>
      <template #content>
        <div class="mb-4 grid grid-cols-1 gap-3 md:grid-cols-3">
          <InputText v-model="recherche" type="search" placeholder="Nom, prénom, email ou offre" />
          <Select v-model="idOffre" :options="offresOptions" option-label="libelle" option-value="idOffre" placeholder="Toutes les offres" filter />
          <Select v-model="statut" :options="statuts" option-label="libelle" option-value="valeur" placeholder="Tous les statuts" />
        </div>

        <div v-if="loading" class="py-8 text-center text-muted-color">Chargement des candidatures...</div>
        <EmptyState v-else-if="!filtrees.length" titre="Aucune candidature" message="Aucune candidature ne correspond aux filtres." />
        <DataTable v-else :value="filtrees" data-key="idCandidature" striped-rows row-hover paginator :rows="10" :rows-per-page-options="[10, 25, 50]">
          <Column header="Candidat" sortable sort-field="candidat.nom">
            <template #body="{ data }">
              <strong>{{ data.candidat?.prenom }} {{ data.candidat?.nom }}</strong>
              <div class="text-sm text-muted-color">{{ data.candidat?.email }}</div>
            </template>
          </Column>
          <Column header="Offre">
            <template #body="{ data }">{{ data.offre?.reference }} · {{ data.offre?.titre }}</template>
          </Column>
          <Column field="statut" header="Statut" sortable>
            <template #body="{ data }"><StatusBadge :valeur="data.statut" /></template>
          </Column>
          <Column header="Action">
            <template #body="{ data }">
              <Button label="Ouvrir la fiche" icon="pi pi-eye" size="small" outlined @click="ouvrir(data)" />
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </section>
</template>
