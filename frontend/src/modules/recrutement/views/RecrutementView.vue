<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import Message from 'primevue/message'
import Tag from 'primevue/tag'

import { recrutementApi as api, ErreurApi } from '@/modules/recrutement/api/recrutementApi'
import { useAuthStore } from '@/shared/store/useAuthStore'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const offres = ref([])
const loading = ref(false)
const error = ref('')
const recherche = ref('')

const dialogVisible = ref(false)
const saving = ref(false)
const form = ref(emptyOffer())

const peutGerer = computed(() => auth.estRh || auth.estDirection)

const offresFiltrees = computed(() => {
  const q = recherche.value.trim().toLowerCase()
  if (!q) return offres.value
  return offres.value.filter((o) =>
    [o.reference, o.titre, o.lieuTravail, o.typeContrat, o.statut]
      .filter(Boolean)
      .some((v) => String(v).toLowerCase().includes(q)),
  )
})

const statutSeverity = {
  BROUILLON: 'secondary',
  PUBLIEE: 'success',
  CLOTUREE: 'contrast',
  ANNULEE: 'danger',
}

function emptyOffer() {
  return {
    idDemandePoste: '',
    reference: '',
    titre: '',
    description: '',
    profilRecherche: '',
    lieuTravail: '',
    typeContrat: '',
    effectifRecherche: 1,
    salaireMin: null,
    salaireMax: null,
    modeSelection: '',
    dateLimiteCandidature: null,
  }
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString('fr-FR') : '—'
}

async function loadOffers() {
  loading.value = true
  error.value = ''
  try {
    offres.value = await api.get('/recrutement/offres')
  } catch (cause) {
    error.value = cause instanceof ErreurApi ? cause.message : 'Impossible de charger les offres.'
  } finally {
    loading.value = false
  }
}

function voirDetail(offer) {
  router.push({ name: 'recrutement-offre-detail', params: { idOffre: offer.idOffre } })
}

function startCreate() {
  form.value = emptyOffer()
  if (route.query.idDemandePoste) form.value.idDemandePoste = route.query.idDemandePoste
  error.value = ''
  dialogVisible.value = true
}

async function saveOffer() {
  saving.value = true
  error.value = ''
  try {
    const payload = { ...form.value, idDemandePoste: Number(form.value.idDemandePoste) }
    await api.post('/recrutement/offres', payload)
    dialogVisible.value = false
    await loadOffers()
  } catch (cause) {
    error.value = cause instanceof ErreurApi ? cause.message : 'Impossible d’enregistrer l’offre.'
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadOffers()
  // Arrivée depuis « Créer une offre » : ouvre directement le formulaire
  if (route.query.idDemandePoste && peutGerer.value) startCreate()
})
</script>

<template>
  <section class="recrutement-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Bloc 2 · Recrutement</p>
        <h1>Offres d’emploi</h1>
        <p class="description">Consultez les offres et ouvrez leur fiche pour les gérer.</p>
      </div>
      <div class="actions">
        <PrimeButton
          label="Actualiser"
          icon="pi pi-refresh"
          severity="secondary"
          outlined
          :loading="loading"
          @click="loadOffers"
        />
        <PrimeButton v-if="peutGerer" label="Nouvelle offre" icon="pi pi-plus" @click="startCreate" />
      </div>
    </header>

    <Message v-if="error && !dialogVisible" severity="error" :closable="false" class="mb-3">
      {{ error }}
    </Message>

    <div class="liste-wrapper">
      <div class="liste-toolbar">
        <span class="recherche">
          <PrimeInputText v-model="recherche" placeholder="Rechercher une offre..." class="w-full" />
        </span>
        <span class="muted">{{ offresFiltrees.length }} offre(s)</span>
      </div>

      <DataTable
        :value="offresFiltrees"
        :loading="loading"
        dataKey="idOffre"
        rowHover
        paginator
        :rows="10"
        :rowsPerPageOptions="[10, 25, 50]"
      >
        <template #empty>
          <div class="vide">
            <i class="pi pi-inbox" />
            <span>Aucune offre trouvée.</span>
          </div>
        </template>

        <Column field="reference" header="Référence" sortable style="width: 9rem">
          <template #body="{ data }"><strong>{{ data.reference }}</strong></template>
        </Column>

        <Column field="titre" header="Titre" sortable />

        <Column header="Lieu / Contrat">
          <template #body="{ data }">
            {{ data.lieuTravail || 'Lieu à définir' }}
            <span v-if="data.typeContrat" class="muted"> · {{ data.typeContrat }}</span>
          </template>
        </Column>

        <Column field="effectifRecherche" header="Effectif" sortable style="width: 7rem" />

        <Column header="Date limite">
          <template #body="{ data }">{{ formatDate(data.dateLimiteCandidature) }}</template>
        </Column>

        <Column field="statut" header="Statut" sortable>
          <template #body="{ data }">
            <Tag :value="data.statut" :severity="statutSeverity[data.statut] || 'secondary'" />
          </template>
        </Column>

        <Column header="" style="width: 9rem">
          <template #body="{ data }">
            <div class="row-actions">
              <PrimeButton
                label="Détail"
                icon="pi pi-eye"
                size="small"
                outlined
                @click="voirDetail(data)"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Formulaire de création -->
    <Dialog
      v-model:visible="dialogVisible"
      modal
      header="Créer une offre"
      :style="{ width: '48rem', maxWidth: '95vw' }"
    >
      <Message v-if="error" severity="error" :closable="false" class="mb-3">{{ error }}</Message>

      <form class="offer-form" @submit.prevent="saveOffer">
        <div class="form-grid">
          <label>Demande validée (ID)
            <PrimeInputText v-model="form.idDemandePoste" required type="number" min="1" />
          </label>
          <label>Référence
            <PrimeInputText v-model="form.reference" required maxlength="50" />
          </label>
          <label>Titre
            <PrimeInputText v-model="form.titre" required maxlength="150" />
          </label>
          <label>Type de contrat
            <PrimeInputText v-model="form.typeContrat" maxlength="50" />
          </label>
          <label>Effectif recherché
            <PrimeInputText v-model="form.effectifRecherche" required type="number" min="1" />
          </label>
          <label>Mode de sélection
            <PrimeInputText v-model="form.modeSelection" required placeholder="CV, entretien, test" />
          </label>
          <label>Salaire min
            <PrimeInputText v-model="form.salaireMin" type="number" min="0" step="0.01" />
          </label>
          <label>Salaire max
            <PrimeInputText v-model="form.salaireMax" type="number" min="0" step="0.01" />
          </label>
          <label>Date limite
            <PrimeInputText v-model="form.dateLimiteCandidature" type="datetime-local" />
          </label>
          <label>Lieu
            <PrimeInputText v-model="form.lieuTravail" maxlength="255" />
          </label>
        </div>
        <label>Description
          <PrimeTextarea v-model="form.description" required rows="4" auto-resize />
        </label>
        <label>Profil recherché
          <PrimeTextarea v-model="form.profilRecherche" required rows="4" auto-resize />
        </label>

        <div class="form-actions">
          <PrimeButton type="button" label="Annuler" severity="secondary" text @click="dialogVisible = false" />
          <PrimeButton type="submit" label="Enregistrer" icon="pi pi-check" :loading="saving" />
        </div>
      </form>
    </Dialog>
  </section>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1.5rem;
}
.actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.eyebrow {
  margin: 0 0 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--p-primary-color);
}
h1 {
  margin: 0 0 0.35rem;
  font-size: 1.6rem;
  color: var(--p-text-color);
}
.description {
  margin: 0;
  color: var(--p-text-muted-color);
}
.muted {
  color: var(--p-text-muted-color);
}

.liste-wrapper {
  background: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  border-radius: 0.75rem;
  overflow: hidden;
}
.liste-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--p-content-border-color);
}
.recherche {
  width: 20rem;
  max-width: 100%;
}
.row-actions {
  display: flex;
  justify-content: flex-end;
}
.vide {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem 0;
  color: var(--p-text-muted-color);
}
.vide i {
  font-size: 2rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 1rem;
}
.offer-form label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  margin-top: 0.85rem;
  font-weight: 600;
  font-size: 0.9rem;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1.5rem;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .recherche {
    width: 100%;
  }
}
</style>