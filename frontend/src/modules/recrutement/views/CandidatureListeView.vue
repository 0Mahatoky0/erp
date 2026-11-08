<script setup>
import { computed, onMounted, ref } from 'vue'
import Card from 'primevue/card'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Button from 'primevue/button'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { recrutementApi as api, ErreurApi } from '../api/recrutementApi'
import { STATUTS_CANDIDATURE } from '../utils/statutsCandidature'

const notifications = useNotificationStore()
const offres = ref([])
const candidatures = ref([])
const recherche = ref('')
const idOffre = ref('')
const statut = ref('')
const loading = ref(false)

const statuts = [
  { valeur: '', libelle: 'Tous les statuts' },
  ...STATUTS_CANDIDATURE.map((item) => ({ valeur: item.valeur, libelle: item.libelle })),
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
    const [listeOffres, listeCandidatures] = await Promise.all([
      api.get('/recrutement/offres'),
      api.get('/recrutement/candidatures'),
    ])
    offres.value = listeOffres
    candidatures.value = listeCandidatures
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger les candidatures.')
  } finally {
    loading.value = false
  }
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
      <Button
        label="Actualiser"
        icon="pi pi-refresh"
        severity="secondary"
        variant="outlined"
        :loading="loading"
        @click="charger"
      />
    </header>

    <Card>
      <template #content>
        <div class="mb-4 flex flex-wrap items-end gap-3">
          <div class="flex flex-col gap-2">
            <label for="candidatures-recherche" class="text-sm font-semibold text-muted-color">Recherche</label>
            <InputText
              id="candidatures-recherche"
              v-model="recherche"
              type="search"
              placeholder="Nom, prénom, email ou offre"
            />
          </div>
          <div class="flex flex-col gap-2">
            <label for="candidatures-offre" class="text-sm font-semibold text-muted-color">Offre</label>
            <Select
              input-id="candidatures-offre"
              v-model="idOffre"
              :options="offresOptions"
              option-label="libelle"
              option-value="idOffre"
              placeholder="Toutes les offres"
              filter
            />
          </div>
          <div class="flex flex-col gap-2">
            <label for="candidatures-statut" class="text-sm font-semibold text-muted-color">Statut</label>
            <Select
              input-id="candidatures-statut"
              v-model="statut"
              :options="statuts"
              option-label="libelle"
              option-value="valeur"
              placeholder="Tous les statuts"
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
              Aucune candidature ne correspond aux filtres.
            </p>
          </template>
        </DataTable>
      </template>
    </Card>
  </section>
</template>
