<!-- src/modules/recrutement/views/OffreSaisieView.vue -->
<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import PrimeSelect from 'primevue/select'
import PrimeInputNumber from 'primevue/inputnumber'
import PrimeDatePicker from 'primevue/datepicker'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import { recrutementApi } from '@/modules/recrutement/api/recrutementApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatMontant } from '@/shared/utils/format'

const route = useRoute()
const router = useRouter()
const notifications = useNotificationStore()

const idOffre = computed(() => route.params.idOffre)
const estEdition = computed(() => Boolean(idOffre.value))

const chargementOffre = ref(false)
const chargementBesoin = ref(false)
const saving = ref(false)

const lignesBesoin = ref([])
const besoinSelectionne = ref(null)

const form = ref({
  idDemandePoste: route.query.idDemandePoste ? Number(route.query.idDemandePoste) : null,
  reference: '',
  titre: '',
  typeContrat: '',
  effectifRecherche: 1,
  modeSelection: '',
  salaireMin: null,
  salaireMax: null,
  dateLimiteCandidature: null,
  lieuTravail: '',
  description: '',
  profilRecherche: '',
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

const chargerLignesBesoin = async () => {
  try {
    chargementBesoin.value = true
    lignesBesoin.value = await recrutementApi.get('/demandes/lignes/acceptees')
    
    if (form.value.idDemandePoste) {
      synchroniserBesoinSelectionne(form.value.idDemandePoste)
      if (!estEdition.value && besoinSelectionne.value) {
        appliquerPreremplissage(besoinSelectionne.value)
      }
    }
  } catch (cause) {
    const msg = cause?.response?.data?.message || cause?.message || 'Impossible de charger les lignes de besoin validées.'
    notifications.erreur(msg)
  } finally {
    chargementBesoin.value = false
  }
}

const chargerOffre = async (id) => {
  try {
    chargementOffre.value = true
    const offre = await recrutementApi.get(`/recrutement/offres/${id}`)
    form.value = {
      ...offre,
      dateLimiteCandidature: offre.dateLimiteCandidature ? new Date(offre.dateLimiteCandidature) : null,
    }
    synchroniserBesoinSelectionne(offre.idDemandePoste)
  } catch (cause) {
    const msg = cause?.response?.data?.message || cause?.message || 'Impossible de charger l’offre.'
    notifications.erreur(msg)
  } finally {
    chargementOffre.value = false
  }
}

const surChangementBesoin = (event) => {
  const id = event.value
  synchroniserBesoinSelectionne(id)
  if (besoinSelectionne.value && !estEdition.value) {
    appliquerPreremplissage(besoinSelectionne.value)
  }
}

const enregistrer = async () => {
  if (saving.value) return
  try {
    saving.value = true
    
    let dateLimiteFormatee = null
    if (form.value.dateLimiteCandidature) {
      const d = new Date(form.value.dateLimiteCandidature)
      dateLimiteFormatee = d.toISOString().slice(0, 19)
    }

    const payload = {
      idDemandePoste: Number(form.value.idDemandePoste),
      reference: form.value.reference,
      titre: form.value.titre,
      typeContrat: form.value.typeContrat || null,
      effectifRecherche: Number(form.value.effectifRecherche),
      modeSelection: form.value.modeSelection,
      salaireMin: form.value.salaireMin !== null ? Number(form.value.salaireMin) : null,
      salaireMax: form.value.salaireMax !== null ? Number(form.value.salaireMax) : null,
      dateLimiteCandidature: dateLimiteFormatee,
      lieuTravail: form.value.lieuTravail || null,
      description: form.value.description,
      profilRecherche: form.value.profilRecherche,
    }

    if (estEdition.value) {
      await recrutementApi.put(`/recrutement/offres/${idOffre.value}`, payload)
      notifications.succes('Offre d’emploi modifiée avec succès.')
      await router.push({ name: 'recrutement-offre-detail', params: { idOffre: idOffre.value } })
    } else {
      const nouvelleOffre = await recrutementApi.post('/recrutement/offres', payload)
      notifications.succes('Offre d’emploi créée avec succès.')
      const targetId = nouvelleOffre?.idOffre ?? nouvelleOffre?.id
      if (targetId) {
        await router.push({ name: 'recrutement-offre-detail', params: { idOffre: targetId } })
      } else {
        await router.push({ name: 'recrutement-offres' })
      }
    }
  } catch (cause) {
    console.error('Erreur enregistrement offre :', cause)
    const msg = cause?.response?.data?.message || cause?.message || 'Erreur lors de l’enregistrement de l’offre.'
    notifications.erreur(msg)
  } finally {
    saving.value = false
  }
}

const annuler = () => {
  if (estEdition.value) {
    router.push({ name: 'recrutement-offre-detail', params: { idOffre: idOffre.value } })
  } else {
    router.push({ name: 'recrutement-offres' })
  }
}

watch(
  idOffre,
  (nouveauId) => {
    if (nouveauId) {
      chargerOffre(nouveauId)
    }
  },
  { immediate: true },
)

onMounted(() => {
  chargerLignesBesoin()
})
</script>

<template>
  <div class="max-w-7xl mx-auto flex flex-col gap-6 p-4 md:p-6">
    <!-- Navigation et En-tête -->
    <div>
      <PrimeButton
        label="Retour"
        icon="pi pi-arrow-left"
        severity="secondary"
        variant="text"
        class="mb-2 p-0"
        @click="annuler"
      />
      <h1 class="m-0 text-2xl font-bold">
        {{ estEdition ? 'Modifier l’offre d’emploi' : 'Créer une offre d’emploi' }}
      </h1>
      <p class="mt-1 mb-0 text-muted-color">
        Comparez directement les informations de la demande validée à gauche avec l'offre d'emploi à configurer à droite.
      </p>
    </div>

    <!-- État de chargement initial -->
    <div v-if="chargementOffre" class="text-center py-8 text-muted-color">
      <i class="pi pi-spin pi-spinner text-2xl mb-2"></i>
      <p class="m-0">Chargement de l’offre...</p>
    </div>

    <form v-else id="offre-saisie-form" @submit.prevent="enregistrer">
      <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        
        <!-- ================= COLONNE GAUCHE : RÉFÉRENCE DU BESOIN ================= -->
        <div class="lg:col-span-5 flex flex-col gap-4 lg:sticky lg:top-6">
          <Card class="border border-surface shadow-sm">
            <template #title>
              <div class="flex items-center justify-between text-base font-bold text-primary border-b border-surface pb-3">
                <span class="flex items-center gap-2">
                  <i class="pi pi-file-check text-primary"></i>
                  Besoin en recrutement validé
                </span>
                <Tag v-if="besoinSelectionne" value="Accepté" severity="success" />
              </div>
            </template>

            <template #content>
              <div class="flex flex-col gap-4">
                <div class="flex flex-col gap-2">
                  <label for="besoin-select" class="text-sm font-semibold">
                    Sélection de la demande <span class="text-red-500">*</span>
                  </label>
                  <PrimeSelect
                    id="besoin-select"
                    v-model="form.idDemandePoste"
                    :options="lignesBesoin"
                    option-label="intituleDemande"
                    option-value="id"
                    placeholder="Choisir une demande d'emploi validée..."
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
                </div>

                <div v-if="besoinSelectionne" class="flex flex-col gap-4 mt-2">
                  <div class="p-3 bg-surface-50 dark:bg-surface-900 border border-surface rounded-lg flex flex-col gap-3 text-sm">
                    <div class="flex flex-col gap-1">
                      <span class="text-xs text-muted-color uppercase font-semibold">Intitulé de la demande</span>
                      <p class="font-bold text-base m-0 text-surface-900 dark:text-surface-0">{{ besoinSelectionne.intituleDemande }}</p>
                    </div>

                    <div class="grid grid-cols-2 gap-3 pt-2 border-t border-surface">
                      <div>
                        <span class="text-xs text-muted-color uppercase font-semibold">Service / Dépt</span>
                        <p class="font-semibold m-0 text-sm">{{ besoinSelectionne.service }}</p>
                        <span class="text-xs text-muted-color">{{ besoinSelectionne.departement || '-' }}</span>
                      </div>
                      <div>
                        <span class="text-xs text-muted-color uppercase font-semibold">Poste repère</span>
                        <p class="font-semibold m-0 text-sm">{{ besoinSelectionne.poste || 'Non spécifié' }}</p>
                      </div>
                    </div>

                    <div class="pt-2 border-t border-surface flex justify-between items-center">
                      <span class="text-xs text-muted-color uppercase font-semibold">Effectif validé</span>
                      <span class="font-bold text-sm bg-primary-100 text-primary-700 dark:bg-primary-950 dark:text-primary-300 px-2 py-0.5 rounded">
                        {{ besoinSelectionne.effectif }} personne(s)
                      </span>
                    </div>

                    <div class="pt-2 border-t border-surface flex flex-col gap-1">
                      <span class="text-xs text-muted-color uppercase font-semibold">Fourchette salaire estimée</span>
                      <p class="font-bold text-sm m-0 text-emerald-600 dark:text-emerald-400">
                        {{ formatMontant(besoinSelectionne.salaireMinEstime) }} - {{ formatMontant(besoinSelectionne.salaireMaxEstime) }}
                      </p>
                      <span class="text-xs text-muted-color">Budget total : {{ formatMontant(besoinSelectionne.budgetEstime) }}</span>
                    </div>
                  </div>

                  <div class="flex flex-col gap-3">
                    <div class="flex flex-col gap-1">
                      <span class="text-xs font-semibold text-muted-color uppercase">Description fournie</span>
                      <div class="p-3 bg-surface-50 dark:bg-surface-900 border border-surface rounded text-xs leading-relaxed max-h-48 overflow-y-auto whitespace-pre-line">
                        {{ besoinSelectionne.descriptionPoste || 'Aucune description rédigée.' }}
                      </div>
                    </div>

                    <div class="flex flex-col gap-1">
                      <span class="text-xs font-semibold text-muted-color uppercase">Profil recherché initial</span>
                      <div class="p-3 bg-surface-50 dark:bg-surface-900 border border-surface rounded text-xs leading-relaxed max-h-48 overflow-y-auto whitespace-pre-line">
                        {{ besoinSelectionne.profilRecherche || 'Aucun profil précisé.' }}
                      </div>
                    </div>
                  </div>
                </div>

                <div v-else class="text-center py-8 text-muted-color border border-dashed border-surface rounded-lg">
                  <i class="pi pi-info-circle text-xl mb-2"></i>
                  <p class="text-xs m-0">Veuillez sélectionner une demande pour afficher les informations de référence.</p>
                </div>
              </div>
            </template>
          </Card>
        </div>

        <!-- ================= COLONNE DROITE : FORMULAIRE OFFRE D'EMPLOI ================= -->
        <div class="lg:col-span-7 flex flex-col gap-4">
          <Card class="border border-surface shadow-sm">
            <template #title>
              <div class="text-base font-bold text-primary border-b border-surface pb-3 flex items-center gap-2">
                <i class="pi pi-briefcase text-primary"></i>
                Formulaire de l'Offre d'emploi
              </div>
            </template>

            <template #content>
              <div class="flex flex-col gap-5">
                
                <div class="flex flex-col gap-4 p-3 bg-surface-50/50 dark:bg-surface-900/50 border border-surface rounded-lg">
                  <span class="text-xs font-bold text-muted-color uppercase tracking-wider">Identifiants de l'offre</span>
                  <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
                    <label for="reference" class="flex flex-col gap-1 text-sm font-semibold">
                      Référence <span class="text-red-500">*</span>
                      <PrimeInputText id="reference" v-model="form.reference" required maxlength="50" placeholder="ex: OFF-2026-001" />
                    </label>

                    <label for="titre" class="flex flex-col gap-1 text-sm font-semibold md:col-span-2">
                      Titre de l'offre <span class="text-red-500">*</span>
                      <PrimeInputText id="titre" v-model="form.titre" required maxlength="150" placeholder="Titre affiché aux candidats" />
                    </label>
                  </div>
                </div>

                <div class="flex flex-col gap-4 p-3 bg-surface-50/50 dark:bg-surface-900/50 border border-surface rounded-lg">
                  <span class="text-xs font-bold text-muted-color uppercase tracking-wider">Modalités de recrutement</span>
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
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
                      <PrimeInputText id="lieuTravail" v-model="form.lieuTravail" maxlength="255" placeholder="Ville, site..." />
                    </label>
                  </div>
                </div>

                <div class="flex flex-col gap-4 p-3 bg-surface-50/50 dark:bg-surface-900/50 border border-surface rounded-lg">
                  <span class="text-xs font-bold text-muted-color uppercase tracking-wider">Rémunération et Échéance</span>
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                    <label for="salaireMin" class="flex flex-col gap-1 text-sm font-semibold">
                      Salaire min proposé
                      <PrimeInputNumber id="salaireMin" v-model="form.salaireMin" :min="0" suffix=" Ar" />
                    </label>

                    <label for="salaireMax" class="flex flex-col gap-1 text-sm font-semibold">
                      Salaire max proposé
                      <PrimeInputNumber id="salaireMax" v-model="form.salaireMax" :min="0" suffix=" Ar" />
                    </label>

                    <label for="dateLimiteCandidature" class="flex flex-col gap-1 text-sm font-semibold md:col-span-2">
                      Date limite de candidature
                      <PrimeDatePicker id="dateLimiteCandidature" v-model="form.dateLimiteCandidature" show-time hour-format="24" date-format="dd/mm/yy" class="w-full" />
                    </label>
                  </div>
                </div>

                <div class="flex flex-col gap-4">
                  <label for="description" class="flex flex-col gap-1 text-sm font-semibold">
                    Description finale du poste <span class="text-red-500">*</span>
                    <PrimeTextarea id="description" v-model="form.description" required rows="5" auto-resize placeholder="Détail des missions..." />
                  </label>

                  <label for="profilRecherche" class="flex flex-col gap-1 text-sm font-semibold">
                    Profil recherché (Exigences) <span class="text-red-500">*</span>
                    <PrimeTextarea id="profilRecherche" v-model="form.profilRecherche" required rows="5" auto-resize placeholder="Compétences, diplômes, expérience..." />
                  </label>
                </div>

                <div class="flex justify-end gap-3 pt-4 border-t border-surface">
                  <PrimeButton label="Annuler" severity="secondary" variant="outlined" @click="annuler" />
                  <PrimeButton type="submit" label="Enregistrer l'offre" icon="pi pi-check" :loading="saving" />
                </div>
              </div>
            </template>
          </Card>
        </div>

      </div>
    </form>
  </div>
</template>