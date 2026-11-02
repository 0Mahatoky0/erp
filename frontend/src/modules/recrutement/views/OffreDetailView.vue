<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import PrimeButton from 'primevue/button'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'

import EmptyState from '@/shared/components/EmptyState.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { formatDateHeure, formatMontant } from '@/shared/utils/format'
import OffreForm from '../components/OffreForm.vue'
import { recrutementApi as api, ErreurApi } from '../api/recrutementApi'

const auth = useAuthStore()
const notifications = useNotificationStore()
const route = useRoute()
const router = useRouter()

const offre = ref(null)
const candidatures = ref([])
const loading = ref(false)
const saving = ref(false)
const formOuvert = ref(false)

const idOffre = computed(() => route.params.idOffre)
const peutGerer = computed(() => auth.estRh || auth.estDirection)
const retour = () => router.push({ name: 'recrutement-offres' })

const salaire = computed(() => {
  const { salaireMin: min, salaireMax: max } = offre.value || {}
  if (min == null && max == null) return '—'
  if (min != null && max != null) return `${formatMontant(min)} – ${formatMontant(max)}`
  return min != null ? `À partir de ${formatMontant(min)}` : `Jusqu’à ${formatMontant(max)}`
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
    { label: 'Date limite', value: o.dateLimiteCandidature ? formatDateHeure(o.dateLimiteCandidature) : '—' },
    { label: 'Demande liée', value: o.idDemandePoste ? `#${o.idDemandePoste}` : '—' },
  ]
})

async function charger() {
  if (!idOffre.value) return
  loading.value = true
  try {
    // Si ton API expose GET /recrutement/offres/{id}, remplace le find par cet appel
    const [offres, cands] = await Promise.all([
      api.get('/recrutement/offres'),
      api.get(`/recrutement/offres/${idOffre.value}/candidatures`),
    ])
    offre.value = offres.find((o) => String(o.idOffre ?? o.id) === String(idOffre.value)) || null
    candidatures.value = cands
  } catch (cause) {
    offre.value = null
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger l’offre.')
  } finally {
    loading.value = false
  }
}

async function changerStatut(action, message) {
  try {
    await api.post(`/recrutement/offres/${idOffre.value}/${action}`)
    notifications.succes(message)
    await charger()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de changer le statut de l’offre.')
  }
}

async function supprimer() {
  if (!window.confirm(`Supprimer l’offre ${offre.value.reference} ?`)) return
  try {
    await api.delete(`/recrutement/offres/${idOffre.value}`)
    notifications.succes('Offre supprimée.')
    retour()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de supprimer l’offre.')
  }
}

async function enregistrer(payload) {
  saving.value = true
  try {
    await api.put(`/recrutement/offres/${idOffre.value}`, payload)
    formOuvert.value = false
    notifications.succes('Offre modifiée.')
    await charger()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible d’enregistrer l’offre.')
  } finally {
    saving.value = false
  }
}

// Recharge aussi si on passe d'une offre à une autre sans quitter la page
watch(idOffre, charger, { immediate: true })
</script>

<template>
  <section>
    <PrimeButton label="Retour aux offres" icon="pi pi-arrow-left" text severity="secondary" class="mb-2" @click="retour" />

    <p v-if="loading" class="text-muted-color">Chargement...</p>

    <EmptyState v-else-if="!offre" titre="Offre introuvable" message="Cette offre n'existe pas ou n'est plus disponible.">
      <PrimeButton label="Retour aux offres" icon="pi pi-arrow-left" @click="retour" />
    </EmptyState>

    <template v-else>
      <header class="flex flex-col md:flex-row md:items-start justify-between gap-4 mb-6">
        <div class="flex flex-wrap items-center gap-3">
          <h1 class="m-0 text-2xl font-bold">{{ offre.titre }}</h1>
          <StatusBadge :valeur="offre.statut" />
        </div>

        <div v-if="peutGerer" class="flex flex-wrap gap-2">
          <PrimeButton v-if="offre.statut === 'BROUILLON'" label="Modifier" icon="pi pi-pencil" severity="secondary" outlined @click="formOuvert = true" />
          <PrimeButton v-if="offre.statut === 'BROUILLON'" label="Publier" icon="pi pi-send" severity="success" @click="changerStatut('publier', 'Offre publiée.')" />
          <PrimeButton v-if="offre.statut === 'PUBLIEE'" label="Clôturer" icon="pi pi-lock" severity="warn" @click="changerStatut('cloturer', 'Offre clôturée.')" />
          <PrimeButton v-if="!['CLOTUREE', 'ANNULEE'].includes(offre.statut)" label="Annuler" icon="pi pi-ban" severity="danger" outlined @click="changerStatut('annuler', 'Offre annulée.')" />
          <PrimeButton v-if="offre.statut === 'BROUILLON'" label="Supprimer" icon="pi pi-trash" severity="danger" text @click="supprimer" />
        </div>
      </header>

      <Card class="mb-5">
        <template #title>Informations</template>
        <template #content>
          <dl class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 m-0">
            <div v-for="info in infos" :key="info.label">
              <dt class="text-xs text-muted-color mb-1">{{ info.label }}</dt>
              <dd class="m-0 font-semibold">{{ info.value }}</dd>
            </div>
          </dl>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mt-5 pt-5 border-t border-surface">
            <div>
              <h3 class="m-0 mb-1 text-xs font-semibold uppercase tracking-wider text-muted-color">Description</h3>
              <p class="m-0 whitespace-pre-line">{{ offre.description || '—' }}</p>
            </div>
            <div>
              <h3 class="m-0 mb-1 text-xs font-semibold uppercase tracking-wider text-muted-color">Profil recherché</h3>
              <p class="m-0 whitespace-pre-line">{{ offre.profilRecherche || '—' }}</p>
            </div>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>
          <span class="flex items-center gap-2">
            Candidatures
            <Tag :value="String(candidatures.length)" severity="secondary" rounded />
          </span>
        </template>
        <template #content>
          <DataTable :value="candidatures" dataKey="idCandidature" rowHover stripedRows paginator :rows="10" :rowsPerPageOptions="[10, 25, 50]">
            <template #empty>
              <p class="text-center text-muted-color py-6 m-0">Aucune candidature pour cette offre.</p>
            </template>
            <Column header="Candidat">
              <template #body="{ data }"><strong>{{ data.candidat?.prenom }} {{ data.candidat?.nom }}</strong></template>
            </Column>
            <Column header="Email">
              <template #body="{ data }">{{ data.candidat?.email }}</template>
            </Column>
            <Column field="statut" header="Statut" sortable>
              <template #body="{ data }"><StatusBadge :valeur="data.statut" /></template>
            </Column>
          </DataTable>
        </template>
      </Card>
    </template>

    <OffreForm
      v-if="formOuvert && offre"
      :offre="offre"
      :saving="saving"
      @save="enregistrer"
      @fermer="formOuvert = false"
    />
  </section>
</template>
