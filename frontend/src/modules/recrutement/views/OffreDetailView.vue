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

const offre = ref(null)
const candidatures = ref([])
const loading = ref(false)
const error = ref('')

const dialogVisible = ref(false)
const saving = ref(false)
const form = ref({})

const idOffre = computed(() => route.params.idOffre)
const peutGerer = computed(() => auth.estRh || auth.estDirection)

const statutSeverity = {
  BROUILLON: 'secondary',
  PUBLIEE: 'success',
  CLOTUREE: 'contrast',
  ANNULEE: 'danger',
}

const salaire = computed(() => {
  const { salaireMin: min, salaireMax: max } = offre.value || {}
  if (min == null && max == null) return '—'
  if (min != null && max != null) return `${min} – ${max}`
  return min != null ? `À partir de ${min}` : `Jusqu’à ${max}`
})

const infos = computed(() => {
  const o = offre.value
  if (!o) return []
  return [
    { label: 'Référence', value: o.reference },
    { label: 'Lieu de travail', value: o.lieuTravail || 'Lieu à définir' },
    { label: 'Type de contrat', value: o.typeContrat || '—' },
    { label: 'Effectif recherché', value: o.effectifRecherche },
    { label: 'Salaire', value: salaire.value },
    { label: 'Mode de sélection', value: o.modeSelection || '—' },
    { label: 'Date limite', value: formatDate(o.dateLimiteCandidature) },
    { label: 'Demande liée', value: o.idDemandePoste ? `#${o.idDemandePoste}` : '—' },
  ]
})

function formatDate(value) {
  return value ? new Date(value).toLocaleString('fr-FR') : '—'
}

async function charger() {
  loading.value = true
  error.value = ''
  try {
    // Si ton API expose GET /recrutement/offres/{id}, remplace le find par cet appel
    const [offres, cands] = await Promise.all([
      api.get('/recrutement/offres'),
      api.get(`/recrutement/offres/${idOffre.value}/candidatures`),
    ])
    offre.value = offres.find((o) => String(o.idOffre) === String(idOffre.value)) || null
    candidatures.value = cands
    if (!offre.value) error.value = 'Offre introuvable.'
  } catch (cause) {
    error.value = cause instanceof ErreurApi ? cause.message : 'Impossible de charger l’offre.'
  } finally {
    loading.value = false
  }
}

async function changeOfferStatus(action) {
  error.value = ''
  try {
    await api.post(`/recrutement/offres/${idOffre.value}/${action}`)
    await charger()
  } catch (cause) {
    error.value =
      cause instanceof ErreurApi ? cause.message : 'Impossible de changer le statut de l’offre.'
  }
}

async function deleteOffer() {
  if (!window.confirm(`Supprimer l’offre ${offre.value.reference} ?`)) return
  try {
    await api.delete(`/recrutement/offres/${idOffre.value}`)
    router.push({ name: 'recrutement-offres' })
  } catch (cause) {
    error.value = cause instanceof ErreurApi ? cause.message : 'Impossible de supprimer l’offre.'
  }
}

function startEdit() {
  form.value = {
    ...offre.value,
    // le champ datetime-local attend le format YYYY-MM-DDTHH:mm
    dateLimiteCandidature: offre.value.dateLimiteCandidature?.slice(0, 16) ?? null,
  }
  error.value = ''
  dialogVisible.value = true
}

async function saveOffer() {
  saving.value = true
  error.value = ''
  try {
    const payload = { ...form.value, idDemandePoste: Number(form.value.idDemandePoste) }
    await api.put(`/recrutement/offres/${idOffre.value}`, payload)
    dialogVisible.value = false
    await charger()
  } catch (cause) {
    error.value = cause instanceof ErreurApi ? cause.message : 'Impossible d’enregistrer l’offre.'
  } finally {
    saving.value = false
  }
}

onMounted(charger)
</script>

<template>
  <section class="recrutement-page">
    <PrimeButton
      label="Retour aux offres"
      icon="pi pi-arrow-left"
      text
      severity="secondary"
      class="retour"
      @click="router.push({ name: 'recrutement-offres' })"
    />

    <Message v-if="error && !dialogVisible" severity="error" :closable="false" class="mb-3">
      {{ error }}
    </Message>
    <p v-if="loading" class="muted">Chargement...</p>

    <template v-else-if="offre">
      <header class="page-header">
        <div>
          <p class="eyebrow">Offre · {{ offre.reference }}</p>
          <div class="titre-ligne">
            <h1>{{ offre.titre }}</h1>
            <Tag :value="offre.statut" :severity="statutSeverity[offre.statut] || 'secondary'" />
          </div>
        </div>

        <div v-if="peutGerer" class="actions">
          <PrimeButton
            v-if="offre.statut === 'BROUILLON'"
            label="Modifier"
            icon="pi pi-pencil"
            severity="secondary"
            outlined
            @click="startEdit"
          />
          <PrimeButton
            v-if="offre.statut === 'BROUILLON'"
            label="Publier"
            icon="pi pi-send"
            severity="success"
            @click="changeOfferStatus('publier')"
          />
          <PrimeButton
            v-if="offre.statut === 'PUBLIEE'"
            label="Clôturer"
            icon="pi pi-lock"
            severity="warn"
            @click="changeOfferStatus('cloturer')"
          />
          <PrimeButton
            v-if="!['CLOTUREE', 'ANNULEE'].includes(offre.statut)"
            label="Annuler"
            icon="pi pi-ban"
            severity="danger"
            outlined
            @click="changeOfferStatus('annuler')"
          />
          <PrimeButton
            v-if="offre.statut === 'BROUILLON'"
            label="Supprimer"
            icon="pi pi-trash"
            severity="danger"
            text
            @click="deleteOffer"
          />
        </div>
      </header>

      <!-- Informations de l'offre -->
      <div class="bloc">
        <h2>Informations</h2>
        <dl class="infos">
          <div v-for="info in infos" :key="info.label" class="info">
            <dt>{{ info.label }}</dt>
            <dd>{{ info.value }}</dd>
          </div>
        </dl>

        <div class="textes">
          <div>
            <h3>Description</h3>
            <p>{{ offre.description || '—' }}</p>
          </div>
          <div>
            <h3>Profil recherché</h3>
            <p>{{ offre.profilRecherche || '—' }}</p>
          </div>
        </div>
      </div>

      <!-- Candidatures -->
      <div class="bloc bloc-table">
        <div class="bloc-titre">
          <h2>Candidatures</h2>
          <Tag :value="String(candidatures.length)" severity="secondary" rounded />
        </div>

        <DataTable
          :value="candidatures"
          dataKey="idCandidature"
          rowHover
          stripedRows
          paginator
          :rows="10"
          :rowsPerPageOptions="[10, 25, 50]"
        >
          <template #empty>
            <div class="vide">
              <i class="pi pi-users" />
              <span>Aucune candidature pour cette offre.</span>
            </div>
          </template>

          <Column header="Candidat">
            <template #body="{ data }">
              <strong>{{ data.candidat.prenom }} {{ data.candidat.nom }}</strong>
            </template>
          </Column>
          <Column header="Email">
            <template #body="{ data }">{{ data.candidat.email }}</template>
          </Column>
          <Column field="statut" header="Statut" sortable>
            <template #body="{ data }">
              <Tag :value="data.statut" severity="info" />
            </template>
          </Column>
        </DataTable>
      </div>
    </template>

    <!-- Formulaire de modification -->
    <Dialog
      v-model:visible="dialogVisible"
      modal
      header="Modifier l’offre brouillon"
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
.retour {
  margin-bottom: 0.75rem;
}
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
.titre-ligne {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}
h1 {
  margin: 0;
  font-size: 1.6rem;
  color: var(--p-text-color);
}
.muted {
  color: var(--p-text-muted-color);
}

.bloc {
  background: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  border-radius: 0.75rem;
  padding: 1.25rem;
  margin-bottom: 1.25rem;
}
.bloc h2 {
  margin: 0 0 1rem;
  font-size: 1.1rem;
  color: var(--p-text-color);
}
.bloc h3 {
  margin: 0 0 0.35rem;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--p-text-muted-color);
}
.bloc-table {
  padding: 0;
  overflow: hidden;
}
.bloc-titre {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 1.25rem 1.25rem 0.75rem;
}
.bloc-titre h2 {
  margin: 0;
}

.infos {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(13rem, 1fr));
  gap: 1rem 1.5rem;
  margin: 0 0 1.25rem;
}
.info dt {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
  margin-bottom: 0.15rem;
}
.info dd {
  margin: 0;
  font-weight: 600;
  color: var(--p-text-color);
}
.textes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.5rem;
  padding-top: 1.25rem;
  border-top: 1px solid var(--p-content-border-color);
}
.textes p {
  margin: 0;
  white-space: pre-line;
  color: var(--p-text-color);
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
  .textes,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>