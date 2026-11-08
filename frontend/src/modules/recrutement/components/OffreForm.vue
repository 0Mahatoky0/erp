<!-- src/modules/recrutement/components/OffreForm.vue -->
<script setup>
import { ref, watch, onMounted } from 'vue'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import PrimeSelect from 'primevue/select'
import PrimeInputNumber from 'primevue/inputnumber'
import PrimeDatePicker from 'primevue/datepicker'
import ModalDialog from '@/shared/components/ModalDialog.vue'
import { recrutementApi } from '@/modules/recrutement/api/recrutementApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatMontant } from '@/shared/utils/format'

const props = defineProps({
  offre: { type: Object, default: null },
  idDemandePoste: { type: [String, Number], default: '' },
  saving: Boolean,
})

const emit = defineEmits(['save', 'fermer'])
const notifications = useNotificationStore()

const lignesBesoin = ref([])
const chargementBesoin = ref(false)
const besoinSelectionne = ref(null)

const form = ref({
  idDemandePoste: props.offre?.idDemandePoste ?? props.idDemandePoste ?? null,
  reference: props.offre?.reference ?? '',
  titre: props.offre?.titre ?? '',
  typeContrat: props.offre?.typeContrat ?? '',
  effectifRecherche: props.offre?.effectifRecherche ?? 1,
  modeSelection: props.offre?.modeSelection ?? '',
  salaireMin: props.offre?.salaireMin ?? null,
  salaireMax: props.offre?.salaireMax ?? null,
  dateLimiteCandidature: props.offre?.dateLimiteCandidature ? new Date(props.offre.dateLimiteCandidature) : null,
  lieuTravail: props.offre?.lieuTravail ?? '',
  description: props.offre?.description ?? '',
  profilRecherche: props.offre?.profilRecherche ?? '',
})

const modesSelectionOptions = [
  { label: 'Sélection manuelle', value: 'MANUEL' },
  { label: 'Grille de notation', value: 'GRILLE' },
  { label: 'Filtres éliminatoires', value: 'FILTRES' },
]

const typesContratOptions = [
  { label: 'CDI', value: 'CDI' },
  { label: 'CDD', value: 'CDD' },
  { label: 'Essai', value: 'ESSAI' },
  { label: 'Stage', value: 'STAGE' },
]

const chargerLignesBesoin = async () => {
  try {
    chargementBesoin.value = true
    lignesBesoin.value = await recrutementApi.get('/demandes/lignes/acceptees')
    if (form.value.idDemandePoste) {
      synchroniserBesoinSelectionne(form.value.idDemandePoste)
    }
  } catch {
    notifications.erreur('Impossible de charger les lignes de besoin validées.')
  } finally {
    chargementBesoin.value = false
  }
}

const synchroniserBesoinSelectionne = (idLigne) => {
  if (!idLigne) {
    besoinSelectionne.value = null
    return
  }
  const trouve = lignesBesoin.value.find((l) => Number(l.id) === Number(idLigne))
  besoinSelectionne.value = trouve || null
}

const appliquerPreremplissage = (ligne) => {
  if (!ligne) return
  if (!form.value.titre) form.value.titre = ligne.intituleDemande || ''
  if (!form.value.description) form.value.description = ligne.descriptionPoste || ''
  if (!form.value.profilRecherche) form.value.profilRecherche = ligne.profilRecherche || ''
  if (!form.value.effectifRecherche || form.value.effectifRecherche === 1) {
    form.value.effectifRecherche = ligne.effectif || 1
  }
  if (form.value.salaireMin === null) form.value.salaireMin = ligne.salaireMinEstime ?? null
  if (form.value.salaireMax === null) form.value.salaireMax = ligne.salaireMaxEstime ?? null
}

const surChangementBesoin = (event) => {
  const id = event.value
  synchroniserBesoinSelectionne(id)
  if (besoinSelectionne.value && !props.offre) {
    appliquerPreremplissage(besoinSelectionne.value)
  }
}

watch(
  () => props.offre,
  (nouvelleOffre) => {
    if (nouvelleOffre) {
      form.value = {
        ...nouvelleOffre,
        dateLimiteCandidature: nouvelleOffre.dateLimiteCandidature ? new Date(nouvelleOffre.dateLimiteCandidature) : null,
      }
      synchroniserBesoinSelectionne(nouvelleOffre.idDemandePoste)
    }
  },
  { deep: true },
)

onMounted(() => {
  chargerLignesBesoin()
})

const submit = () => {
  emit('save', {
    ...form.value,
    idDemandePoste: Number(form.value.idDemandePoste),
    dateLimiteCandidature: form.value.dateLimiteCandidature
      ? form.value.dateLimiteCandidature.toISOString()
      : null,
  })
}
</script>

<template>
  <ModalDialog
    :titre="props.offre ? 'Modifier l’offre brouillon' : 'Créer une offre d’emploi'"
    large
    @fermer="emit('fermer')"
  >
    <form id="offre-form" class="flex flex-col gap-4" @submit.prevent="submit">
      <!-- Zone Sélection du besoin -->
      <div class="flex flex-col gap-2 p-3 bg-surface-50 dark:bg-surface-900 border border-surface rounded-lg">
        <label for="besoin-select" class="text-sm font-semibold">
          Besoin en recrutement validé <span class="text-red-500">*</span>
        </label>
        <PrimeSelect
          id="besoin-select"
          v-model="form.idDemandePoste"
          :options="lignesBesoin"
          option-label="intituleDemande"
          option-value="id"
          placeholder="Sélectionner une ligne de besoin acceptée..."
          :loading="chargementBesoin"
          filter
          class="w-full"
          @change="surChangementBesoin"
        >
          <template #option="slotProps">
            <div class="flex flex-col gap-1">
              <span class="font-semibold">{{ slotProps.option.intituleDemande }}</span>
              <span class="text-xs text-muted-color">
                Service : {{ slotProps.option.service || 'N/A' }} | Effectif : {{ slotProps.option.effectif }}
              </span>
            </div>
          </template>
        </PrimeSelect>

        <!-- Récapitulatif du besoin sélectionné -->
        <div
          v-if="besoinSelectionne"
          class="mt-2 p-3 bg-surface-0 dark:bg-surface-800 border border-surface rounded text-xs flex flex-col gap-2"
        >
          <div class="font-bold text-sm text-primary">Récapitulatif de la demande de besoin</div>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-2">
            <div>
              <span class="text-muted-color">Service / Dépt :</span>
              <p class="font-semibold m-0">{{ besoinSelectionne.service }} ({{ besoinSelectionne.departement || '-' }})</p>
            </div>
            <div>
              <span class="text-muted-color">Poste repère :</span>
              <p class="font-semibold m-0">{{ besoinSelectionne.poste || 'Non spécifié' }}</p>
            </div>
            <div>
              <span class="text-muted-color">Type besoin :</span>
              <p class="font-semibold m-0">{{ besoinSelectionne.typeBesoin }}</p>
            </div>
            <div>
              <span class="text-muted-color">Effectif validé :</span>
              <p class="font-semibold m-0">{{ besoinSelectionne.effectif }} personne(s)</p>
            </div>
            <div>
              <span class="text-muted-color">Fourchette salaire estimée :</span>
              <p class="font-semibold m-0">
                {{ formatMontant(besoinSelectionne.salaireMinEstime) }} - {{ formatMontant(besoinSelectionne.salaireMaxEstime) }}
              </p>
            </div>
            <div>
              <span class="text-muted-color">Budget estimé :</span>
              <p class="font-semibold m-0">{{ formatMontant(besoinSelectionne.budgetEstime) }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Grille des informations clés de l'offre -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4 gap-y-3">
        <label for="reference" class="flex flex-col gap-1 text-sm font-semibold">
          Référence <span class="text-red-500">*</span>
          <PrimeInputText id="reference" v-model="form.reference" required maxlength="50" placeholder="ex: OFF-2026-001" />
        </label>

        <label for="titre" class="flex flex-col gap-1 text-sm font-semibold">
          Titre de l'offre <span class="text-red-500">*</span>
          <PrimeInputText id="titre" v-model="form.titre" required maxlength="150" />
        </label>

        <label for="typeContrat" class="flex flex-col gap-1 text-sm font-semibold">
          Type de contrat
          <PrimeSelect
            id="typeContrat"
            v-model="form.typeContrat"
            :options="typesContratOptions"
            option-label="label"
            option-value="value"
            placeholder="Choisir le type..."
          />
        </label>

        <label for="effectifRecherche" class="flex flex-col gap-1 text-sm font-semibold">
          Effectif recherché <span class="text-red-500">*</span>
          <PrimeInputNumber id="effectifRecherche" v-model="form.effectifRecherche" :min="1" required />
        </label>

        <label for="modeSelection" class="flex flex-col gap-1 text-sm font-semibold">
          Mode de sélection <span class="text-red-500">*</span>
          <PrimeSelect
            id="modeSelection"
            v-model="form.modeSelection"
            :options="modesSelectionOptions"
            option-label="label"
            option-value="value"
            placeholder="Sélectionner..."
            required
          />
        </label>

        <label for="lieuTravail" class="flex flex-col gap-1 text-sm font-semibold">
          Lieu de travail
          <PrimeInputText id="lieuTravail" v-model="form.lieuTravail" maxlength="255" />
        </label>

        <label for="salaireMin" class="flex flex-col gap-1 text-sm font-semibold">
          Salaire min proposé
          <PrimeInputNumber id="salaireMin" v-model="form.salaireMin" :min="0" suffix=" Ar" />
        </label>

        <label for="salaireMax" class="flex flex-col gap-1 text-sm font-semibold">
          Salaire max proposé
          <PrimeInputNumber id="salaireMax" v-model="form.salaireMax" :min="0" suffix=" Ar" />
        </label>

        <label for="dateLimiteCandidature" class="flex flex-col gap-1 text-sm font-semibold col-span-1 md:col-span-2">
          Date limite de candidature
          <PrimeDatePicker id="dateLimiteCandidature" v-model="form.dateLimiteCandidature" show-time hour-format="24" date-format="dd/mm/yy" />
        </label>
      </div>

      <!-- Textes longs (Description et Profil recherché) -->
      <label for="description" class="flex flex-col gap-1 text-sm font-semibold">
        Description du poste <span class="text-red-500">*</span>
        <PrimeTextarea id="description" v-model="form.description" required rows="4" auto-resize />
      </label>

      <label for="profilRecherche" class="flex flex-col gap-1 text-sm font-semibold">
        Profil recherché <span class="text-red-500">*</span>
        <PrimeTextarea id="profilRecherche" v-model="form.profilRecherche" required rows="4" auto-resize />
      </label>
    </form>

    <template #actions>
      <PrimeButton label="Annuler" severity="secondary" variant="outlined" @click="emit('fermer')" />
      <PrimeButton type="submit" form="offre-form" label="Enregistrer" icon="pi pi-check" :loading="saving" />
    </template>
  </ModalDialog>
</template>