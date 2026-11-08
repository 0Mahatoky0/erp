<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { embaucheApi as api, ErreurApi } from '../api/embaucheApi'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationStore()
const contrat = ref(null)
const loading = ref(false)
const saving = ref(false)
const estCandidat = computed(() => auth.role === 'CANDIDAT')
const peutRepondre = computed(() => estCandidat.value && contrat.value?.statut === 'PROPOSE')
const peutValider = computed(() => !estCandidat.value && contrat.value?.statut === 'ACCEPTE')
const date = (valeur) => valeur ? new Date(valeur).toLocaleString('fr-FR') : '—'

async function charger() {
  loading.value = true
  try {
    contrat.value = await api.get(`/embauche/propositions/contrats/${route.params.idContrat}`)
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger le contrat.')
  } finally {
    loading.value = false
  }
}

async function repondre(decision) {
  saving.value = true
  try {
    contrat.value = await api.post(`/embauche/propositions/contrats/${route.params.idContrat}/reponse`, { decision })
    notifications.succes(decision === 'ACCEPTE' ? 'Contrat accepté.' : 'Contrat refusé.')
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible d’enregistrer la réponse.')
  } finally {
    saving.value = false
  }
}

async function valider() {
  saving.value = true
  try {
    contrat.value = await api.post(`/embauche/propositions/contrats/${route.params.idContrat}/validation`)
    notifications.succes('Contrat validé et employé créé.')
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de valider le contrat.')
  } finally {
    saving.value = false
  }
}

onMounted(charger)
</script>

<template>
  <section>
    <Button label="Retour" icon="pi pi-arrow-left" text severity="secondary" class="mb-2" @click="router.back()" />
    <div v-if="loading" class="py-8 text-center text-muted-color">Chargement du contrat...</div>
    <template v-else-if="contrat">
      <header class="mb-5 flex flex-col justify-between gap-3 md:flex-row md:items-start">
        <div>
          <h1 class="m-0 text-2xl font-bold">Fiche contrat d’embauche</h1>
          <p class="mt-1 mb-0 text-muted-color">Candidat #{{ contrat.idCandidat }} · Proposition #{{ contrat.idPropositionEmbauche }}</p>
        </div>
        <StatusBadge :valeur="contrat.statut" />
      </header>
      <Card>
        <template #title>Informations du contrat</template>
        <template #content>
          <dl class="grid grid-cols-1 gap-4 text-sm sm:grid-cols-2">
            <div><dt class="text-muted-color">Type de contrat</dt><dd class="mt-1 font-semibold">{{ contrat.typeContrat }}</dd></div>
            <div><dt class="text-muted-color">Salaire</dt><dd class="mt-1 font-semibold">{{ contrat.salaire ?? '—' }}</dd></div>
            <div><dt class="text-muted-color">Lieu de travail</dt><dd class="mt-1 font-semibold">{{ contrat.lieuTravail || '—' }}</dd></div>
            <div><dt class="text-muted-color">Début</dt><dd class="mt-1 font-semibold">{{ date(contrat.dateDebut) }}</dd></div>
            <div><dt class="text-muted-color">Fin</dt><dd class="mt-1 font-semibold">{{ date(contrat.dateFin) }}</dd></div>
            <div><dt class="text-muted-color">Proposé le</dt><dd class="mt-1 font-semibold">{{ date(contrat.dateCreation) }}</dd></div>
            <div><dt class="text-muted-color">Réponse le</dt><dd class="mt-1 font-semibold">{{ date(contrat.dateReponse) }}</dd></div>
            <div><dt class="text-muted-color">Validation finale</dt><dd class="mt-1 font-semibold">{{ date(contrat.dateValidation) }}</dd></div>
          </dl>
          <div v-if="peutRepondre" class="mt-6 flex flex-wrap gap-2">
            <Button label="Accepter le contrat" icon="pi pi-check" :loading="saving" @click="repondre('ACCEPTE')" />
            <Button label="Refuser le contrat" icon="pi pi-times" severity="danger" outlined :loading="saving" @click="repondre('REFUSE')" />
          </div>
          <div v-if="peutValider" class="mt-6">
            <Button label="Valider le contrat et créer l’employé" icon="pi pi-check-circle" :loading="saving" @click="valider" />
          </div>
        </template>
      </Card>
    </template>
  </section>
</template>
