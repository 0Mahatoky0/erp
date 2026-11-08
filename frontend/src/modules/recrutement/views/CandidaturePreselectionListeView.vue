<script setup>
import { computed, onMounted, ref } from 'vue'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDateHeure } from '@/shared/utils/format'
import { recrutementApi as api, ErreurApi } from '../api/recrutementApi'
import { STATUTS_PRESELECTION, libelleStatut } from '../utils/statutsCandidature'

const notifications = useNotificationStore()

const offres = ref([])
const candidatures = ref([])
const recherche = ref('')
const idOffre = ref('')
const loading = ref(false)

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
    if (!STATUTS_PRESELECTION.includes(candidature.statut)) return false
    const candidat = candidature.candidat || {}
    const texte = [candidat.nom, candidat.prenom, candidat.email, candidature.offre?.titre]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return (
      (!terme || texte.includes(terme)) &&
      (!idOffre.value || String(candidature.offre?.idOffre) === idOffre.value)
    )
  })
})

const compteurs = computed(() =>
  STATUTS_PRESELECTION.map((statut) => ({
    statut,
    libelle: libelleStatut(statut),
    nombre: filtrees.value.filter((candidature) => candidature.statut === statut).length,
  })),
)

async function charger() {
  loading.value = true
  try {
    const [listeOffres, listeCandidatures] = await Promise.all([
      api.get('/recrutement/offres'),
      api.get('/recrutement/candidatures'),
    ])
    offres.value = listeOffres
    candidatures.value = listeCandidatures
  } catch (cause) {
    notifications.erreur(
      cause instanceof ErreurApi ? cause.message : 'Impossible de charger les candidatures.',
    )
  } finally {
    loading.value = false
  }
}

onMounted(charger)
</script>

<template>
  <section>
    <header class="mb-6 flex flex-col justify-between gap-4 md:flex-row md:items-start">
      <div>
        <h1 class="m-0 text-2xl font-bold">Présélection</h1>
        <p class="mt-1 mb-0 text-muted-color">
          Consultez les candidatures reçues et présélectionnées. Les décisions se prennent dans la fiche.
        </p>
      </div>
      <div class="flex flex-wrap gap-2">
        <Button
          label="Actualiser"
          icon="pi pi-refresh"
          severity="secondary"
          variant="outlined"
          :loading="loading"
          @click="charger"
        />
      </div>
    </header>

    <div class="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
      <div
        v-for="compteur in compteurs"
        :key="compteur.statut"
        class="rounded-lg border border-surface bg-surface-0 p-4 dark:bg-surface-900"
      >
        <p class="m-0 text-sm text-muted-color">{{ compteur.libelle }}</p>
        <strong class="text-3xl">{{ compteur.nombre }}</strong>
      </div>
    </div>

    <Card>
      <template #content>
        <div class="mb-4 flex flex-wrap items-end gap-3">
          <div class="flex flex-col gap-2">
            <label for="preselection-recherche" class="text-sm font-semibold text-muted-color">Recherche</label>
            <InputText
              id="preselection-recherche"
              v-model="recherche"
              type="search"
              placeholder="Nom, prénom, email ou offre"
            />
          </div>
          <div class="flex flex-col gap-2">
            <label for="preselection-offre" class="text-sm font-semibold text-muted-color">Offre</label>
            <Select
              input-id="preselection-offre"
              v-model="idOffre"
              :options="offresOptions"
              option-label="libelle"
              option-value="idOffre"
              placeholder="Toutes les offres"
              filter
            />
          </div>
        </div>

        <DataTable
          :value="filtrees"
          data-key="idCandidature"
          row-hover
          paginator
          :rows="10"
          :rows-per-page-options="[10, 25, 50]"
          :loading="loading"
        >
          <Column header="Candidat" sortable sort-field="candidat.nom">
            <template #body="{ data }">
              <strong>{{ data.candidat?.prenom }} {{ data.candidat?.nom }}</strong>
              <div class="text-sm text-muted-color">{{ data.candidat?.email }}</div>
            </template>
          </Column>
          <Column header="Offre">
            <template #body="{ data }">{{ data.offre?.reference }} · {{ data.offre?.titre }}</template>
          </Column>
          <Column field="dateCandidature" header="Reçue le" sortable>
            <template #body="{ data }">{{ formatDateHeure(data.dateCandidature) }}</template>
          </Column>
          <Column field="statut" header="Statut" sortable>
            <template #body="{ data }"><StatusBadge :valeur="data.statut" /></template>
          </Column>
          <Column header="Action">
            <template #body="{ data }">
              <Button
                label="Détail"
                icon="pi pi-eye"
                size="small"
                variant="outlined"
                as="router-link"
                :to="{
                  name: 'recrutement-candidature-fiche',
                  params: { idCandidature: data.idCandidature },
                }"
              />
            </template>
          </Column>
          <template #empty>
            <p class="m-0 py-6 text-center text-muted-color">
              Aucune candidature à présélectionner pour ces filtres.
            </p>
          </template>
        </DataTable>
      </template>
    </Card>
  </section>
</template>
