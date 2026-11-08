<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Select from 'primevue/select'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import DatePicker from 'primevue/datepicker'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { embaucheApi as api, ErreurApi } from '../api/embaucheApi'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationStore()
const proposition = ref(null)
const reponses = ref([])
const contrat = ref(null)
const loading = ref(false)
const saving = ref(false)
const form = ref({
  decision: null,
  salaireSouhaite: null,
  lieuSouhaite: '',
  typeContratSouhaite: null,
  dateDebutSouhaitee: null,
  dateFinSouhaitee: null,
  commentaire: '',
})

const decisions = [
  { label: 'Accepter la proposition', value: 'ACCEPTEE' },
  { label: 'Refuser la proposition', value: 'REFUSEE' },
  { label: 'Demander un ajustement', value: 'AJUSTEMENT_DEMANDE' },
]
const contrats = [{ label: 'CDI', value: 'CDI' }, { label: 'CDD', value: 'CDD' }]
const estCandidat = computed(() => auth.role === 'CANDIDAT')
const peutRepondre = computed(() => (
  estCandidat.value
  && ['ENVOYEE', 'AJUSTEMENT_DEMANDE'].includes(proposition.value?.statut)
))
const demandeAjustement = computed(() => form.value.decision === 'AJUSTEMENT_DEMANDE')
const peutProposerContrat = computed(() => (
  !estCandidat.value && proposition.value?.statut === 'ACCEPTEE' && !contrat.value
))
const date = (valeur) => valeur ? new Date(valeur).toLocaleString('fr-FR') : '—'
const iso = (valeur) => valeur ? new Date(valeur).toISOString().slice(0, 19) : null

async function charger() {
  loading.value = true
  try {
    const id = route.params.idProposition
    proposition.value = await api.get(`/embauche/propositions/${id}`)
    reponses.value = await api.get(`/embauche/propositions/${id}/reponses`)
    const contrats = await api.get('/embauche/propositions/contrats')
    contrat.value = contrats.find((element) => element.idPropositionEmbauche === Number(id)) || null
    form.value.salaireSouhaite = proposition.value.salairePropose
    form.value.lieuSouhaite = proposition.value.lieuTravail || ''
    form.value.typeContratSouhaite = proposition.value.typeContrat
    form.value.dateDebutSouhaitee = proposition.value.dateDebut ? new Date(proposition.value.dateDebut) : null
    form.value.dateFinSouhaitee = proposition.value.dateFin ? new Date(proposition.value.dateFin) : null
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger la proposition.')
  } finally {
    loading.value = false
  }
}

async function proposerContrat() {
  saving.value = true
  try {
    contrat.value = await api.post(`/embauche/propositions/${route.params.idProposition}/contrat`)
    notifications.succes('Le contrat a été proposé au candidat.')
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de proposer le contrat.')
  } finally {
    saving.value = false
  }
}

function voirContrat() {
  if (!contrat.value) return
  router.push({
    name: estCandidat.value ? 'candidat-contrat-fiche' : 'embauche-contrat-fiche',
    params: { idContrat: contrat.value.idContrat },
  })
}

async function repondre() {
  if (!peutRepondre.value || !form.value.decision) return
  saving.value = true
  try {
    await api.post(`/embauche/propositions/${route.params.idProposition}/reponse`, {
      decision: form.value.decision,
      salaireSouhaite: demandeAjustement.value ? form.value.salaireSouhaite : null,
      lieuSouhaite: demandeAjustement.value ? form.value.lieuSouhaite || null : null,
      typeContratSouhaite: demandeAjustement.value ? form.value.typeContratSouhaite : null,
      dateDebutSouhaitee: demandeAjustement.value ? iso(form.value.dateDebutSouhaitee) : null,
      dateFinSouhaitee: demandeAjustement.value ? iso(form.value.dateFinSouhaitee) : null,
      commentaire: form.value.commentaire || null,
    })
    notifications.succes(form.value.decision === 'ACCEPTEE' ? 'Proposition acceptée.' : 'Votre réponse a été enregistrée.')
    await charger()
    form.value.decision = null
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible d’enregistrer votre réponse.')
  } finally {
    saving.value = false
  }
}

onMounted(charger)
</script>

<template>
  <section>
    <Button label="Retour" icon="pi pi-arrow-left" text severity="secondary" class="mb-2" @click="router.back()" />
    <div v-if="loading" class="py-8 text-center text-muted-color">Chargement de la proposition...</div>
    <template v-else-if="proposition">
      <header class="mb-5 flex flex-col justify-between gap-3 md:flex-row md:items-start">
        <div>
          <h1 class="m-0 text-2xl font-bold">{{ proposition.offre?.titre || 'Proposition d’embauche' }}</h1>
          <p class="mt-1 mb-0 text-muted-color">
            {{ proposition.candidat?.prenom }} {{ proposition.candidat?.nom }} · {{ proposition.offre?.reference }}
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <StatusBadge :valeur="proposition.statut" />
          <Button
            v-if="peutProposerContrat"
            label="Proposer le contrat"
            icon="pi pi-file-plus"
            :loading="saving"
            @click="proposerContrat"
          />
          <Button
            v-else-if="contrat"
            label="Voir le contrat"
            icon="pi pi-file"
            severity="secondary"
            outlined
            @click="voirContrat"
          />
        </div>
      </header>

      <div class="grid gap-5 lg:grid-cols-2">
        <Card>
          <template #title>Fiche de proposition</template>
          <template #content>
            <dl class="grid grid-cols-1 gap-4 text-sm sm:grid-cols-2">
              <div><dt class="text-muted-color">Type de contrat</dt><dd class="mt-1 font-semibold">{{ proposition.typeContrat }}</dd></div>
              <div><dt class="text-muted-color">Salaire proposé</dt><dd class="mt-1 font-semibold">{{ proposition.salairePropose ?? '—' }}</dd></div>
              <div><dt class="text-muted-color">Lieu de travail</dt><dd class="mt-1 font-semibold">{{ proposition.lieuTravail || '—' }}</dd></div>
              <div><dt class="text-muted-color">Date d’envoi</dt><dd class="mt-1 font-semibold">{{ date(proposition.dateProposition) }}</dd></div>
              <div><dt class="text-muted-color">Début</dt><dd class="mt-1 font-semibold">{{ date(proposition.dateDebut) }}</dd></div>
              <div><dt class="text-muted-color">Fin</dt><dd class="mt-1 font-semibold">{{ date(proposition.dateFin) }}</dd></div>
            </dl>
          </template>
        </Card>

        <Card v-if="peutRepondre">
          <template #title>Votre réponse</template>
          <template #content>
            <form class="flex flex-col gap-3" @submit.prevent="repondre">
              <label class="flex flex-col gap-1 text-sm font-semibold">Décision
                <Select v-model="form.decision" :options="decisions" option-label="label" option-value="value" required placeholder="Choisir une réponse" />
              </label>
              <template v-if="demandeAjustement">
                <div class="grid gap-3 sm:grid-cols-2">
                  <label class="flex flex-col gap-1 text-sm font-semibold">Salaire souhaité
                    <InputNumber v-model="form.salaireSouhaite" :min="0" mode="currency" currency="MGA" locale="fr-FR" />
                  </label>
                  <label class="flex flex-col gap-1 text-sm font-semibold">Type de contrat souhaité
                    <Select v-model="form.typeContratSouhaite" :options="contrats" option-label="label" option-value="value" />
                  </label>
                </div>
                <label class="flex flex-col gap-1 text-sm font-semibold">Lieu souhaité
                  <InputText v-model="form.lieuSouhaite" />
                </label>
                <div class="grid gap-3 sm:grid-cols-2">
                  <label class="flex flex-col gap-1 text-sm font-semibold">Début souhaité
                    <DatePicker v-model="form.dateDebutSouhaitee" show-time hour-format="24" />
                  </label>
                  <label class="flex flex-col gap-1 text-sm font-semibold">Fin souhaitée
                    <DatePicker v-model="form.dateFinSouhaitee" show-time hour-format="24" />
                  </label>
                </div>
              </template>
              <label class="flex flex-col gap-1 text-sm font-semibold">Message
                <Textarea v-model="form.commentaire" rows="4" maxlength="2000" placeholder="Précisez votre réponse ou votre demande d’ajustement" />
              </label>
              <Button label="Envoyer ma réponse" icon="pi pi-send" type="submit" :loading="saving" />
            </form>
          </template>
        </Card>

        <Card v-else-if="estCandidat">
          <template #title>Réponse enregistrée</template>
          <template #content>
            <div class="flex items-start gap-3 text-sm">
              <i class="pi pi-check-circle mt-0.5 text-xl text-green-600" aria-hidden="true" />
              <p class="m-0 text-muted-color">
                Cette proposition a déjà reçu votre réponse finale.
                Vous ne pouvez plus envoyer une nouvelle réponse.
              </p>
            </div>
          </template>
        </Card>

        <Card v-else class="lg:col-span-2">
          <template #title>Réponses du candidat</template>
          <template #content>
            <div v-if="!reponses.length" class="text-muted-color">Aucune réponse reçue pour le moment.</div>
            <div v-for="reponse in reponses" :key="reponse.idReponse" class="mb-3 rounded-lg border border-surface p-4 last:mb-0">
              <div class="flex flex-wrap items-center justify-between gap-2">
                <StatusBadge :valeur="reponse.decision" />
                <span class="text-sm text-muted-color">{{ date(reponse.dateReponse) }}</span>
              </div>
              <p v-if="reponse.commentaire" class="mt-2 mb-0 text-sm">{{ reponse.commentaire }}</p>
              <p v-if="reponse.decision === 'AJUSTEMENT_DEMANDE'" class="mt-2 mb-0 text-sm text-muted-color">
                Souhaits : {{ reponse.salaireSouhaite ?? 'salaire inchangé' }} · {{ reponse.lieuSouhaite || 'lieu inchangé' }} · {{ reponse.typeContratSouhaite || 'contrat inchangé' }}
              </p>
            </div>
          </template>
        </Card>
      </div>
    </template>
  </section>
</template>
