<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Select from 'primevue/select'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import DatePicker from 'primevue/datepicker'
import { embaucheApi as api, ErreurApi } from '../api/embaucheApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const router = useRouter()
const notifications = useNotificationStore()
const candidatures = ref([])
const loading = ref(false)
const saving = ref(false)
const candidatureId = ref(null)
const form = ref({
  typeContrat: 'CDI',
  salairePropose: null,
  lieuTravail: '',
  dateDebut: null,
  dateFin: null,
})

const contratOptions = [
  { label: 'CDI', value: 'CDI' },
  { label: 'CDD', value: 'CDD' },
]
const candidatureOptions = computed(() => candidatures.value.map((candidature) => ({
  label: `${candidature.candidat?.prenom} ${candidature.candidat?.nom} · ${candidature.offre?.reference} · ${candidature.offre?.titre}`,
  value: candidature.idCandidature,
})))
const candidatureSelectionnee = computed(() => candidatures.value.find((candidature) => candidature.idCandidature === candidatureId.value))

function preRemplir(candidature) {
  if (!candidature) return
  form.value.salairePropose = candidature.offre?.salaireMax ?? candidature.offre?.salaireMin ?? null
  form.value.lieuTravail = candidature.offre?.lieuTravail || ''
  form.value.typeContrat = candidature.offre?.typeContrat === 'CDD' ? 'CDD' : 'CDI'
}

async function chargerCandidaturesRetenues() {
  loading.value = true
  try {
    const offres = await api.get('/recrutement/offres')
    const resultats = await Promise.all(
      offres.map((offre) => api.get(`/recrutement/offres/${offre.idOffre}/candidatures?statut=RETENUE`)),
    )
    candidatures.value = resultats.flat()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger les candidatures retenues.')
  } finally {
    loading.value = false
  }
}

function surCandidature(event) {
  candidatureId.value = event.value
  preRemplir(candidatureSelectionnee.value)
}

function formaterDate(valeur) {
  return valeur ? new Date(valeur).toISOString().slice(0, 19) : null
}

async function enregistrer() {
  if (!candidatureId.value) return
  saving.value = true
  try {
    const proposition = await api.post('/embauche/propositions', {
      idCandidature: candidatureId.value,
      typeContrat: form.value.typeContrat,
      salairePropose: form.value.salairePropose,
      lieuTravail: form.value.lieuTravail || null,
      dateDebut: formaterDate(form.value.dateDebut),
      dateFin: formaterDate(form.value.dateFin),
    })
    notifications.succes('La proposition d’embauche a été créée.')
    router.push({ name: 'embauche-proposition-fiche', params: { idProposition: proposition.idPropositionEmbauche } })
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de créer la proposition.')
  } finally {
    saving.value = false
  }
}

onMounted(chargerCandidaturesRetenues)
</script>

<template>
  <section>
    <Button label="Retour aux propositions" icon="pi pi-arrow-left" text severity="secondary" class="mb-2" @click="router.push({ name: 'embauche-proposition-liste' })" />
    <header class="mb-5">
      <h1 class="m-0 text-2xl font-bold">Créer une proposition d’embauche</h1>
      <p class="mt-1 mb-0 text-muted-color">Seules les candidatures ayant le statut « Retenue » sont proposées.</p>
    </header>
    <Card>
      <template #content>
        <form class="grid grid-cols-1 gap-4 md:grid-cols-2" @submit.prevent="enregistrer">
          <label class="flex flex-col gap-1 text-sm font-semibold md:col-span-2">
            Candidature retenue
            <Select v-model="candidatureId" :options="candidatureOptions" option-label="label" option-value="value" filter required :loading="loading" placeholder="Sélectionner une candidature" @change="surCandidature" />
          </label>
          <div v-if="candidatureSelectionnee" class="rounded-lg bg-surface-100 p-3 text-sm dark:bg-surface-800 md:col-span-2">
            <strong>{{ candidatureSelectionnee.candidat?.prenom }} {{ candidatureSelectionnee.candidat?.nom }}</strong>
            <span class="text-muted-color"> · {{ candidatureSelectionnee.offre?.reference }} · {{ candidatureSelectionnee.offre?.titre }}</span>
          </div>
          <label class="flex flex-col gap-1 text-sm font-semibold">
            Type de contrat
            <Select v-model="form.typeContrat" :options="contratOptions" option-label="label" option-value="value" required />
          </label>
          <label class="flex flex-col gap-1 text-sm font-semibold">
            Salaire proposé
            <InputNumber v-model="form.salairePropose" :min="0" mode="currency" currency="MGA" locale="fr-FR" />
          </label>
          <label class="flex flex-col gap-1 text-sm font-semibold">
            Lieu de travail
            <InputText v-model="form.lieuTravail" maxlength="255" />
          </label>
          <label class="flex flex-col gap-1 text-sm font-semibold">
            Date de début
            <DatePicker v-model="form.dateDebut" show-time hour-format="24" required />
          </label>
          <label class="flex flex-col gap-1 text-sm font-semibold">
            Date de fin
            <DatePicker v-model="form.dateFin" show-time hour-format="24" :required="form.typeContrat === 'CDD'" />
          </label>
          <div class="flex justify-end gap-2 md:col-span-2">
            <Button label="Annuler" severity="secondary" outlined type="button" @click="router.push({ name: 'embauche-proposition-liste' })" />
            <Button label="Créer la proposition" icon="pi pi-check" type="submit" :loading="saving" />
          </div>
        </form>
      </template>
    </Card>
  </section>
</template>
