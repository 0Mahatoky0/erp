<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import DatePicker from 'primevue/datepicker'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatMontant } from '@/shared/utils/format'
import { embaucheApi as api, ErreurApi } from '../api/embaucheApi'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationStore()

const idProposition = computed(() => route.params.id)
const estEdition = computed(() => Boolean(idProposition.value))

const chargement = ref(false)
const enregistrement = ref(false)
const candidats = ref([])
const lignes = ref([])
const postes = ref([])
const managers = ref([])

const TYPES = [
  { label: 'CDI', value: 'CDI' },
  { label: 'CDD', value: 'CDD' },
  { label: 'Intérim', value: 'INTERIM' },
  { label: 'Stage', value: 'STAGE' },
  { label: 'Apprentissage', value: 'APPRENTISSAGE' },
  { label: 'Freelance', value: 'FREELANCE' },
]

const form = ref({
  idCandidature: null,
  idPoste: null,
  idManager: null,
  typeContrat: 'CDI',
  dureeMois: null,
  motifCdd: '',
  dateDebutPrevue: null,
  periodeEssaiMois: 1,
  salaireBrutMensuel: null,
  devise: 'MGA',
  primesAvantages: '',
  horaires: '',
  lieuTravail: '',
  derogationFinanceDirection: false,
})

const candidatSelectionne = ref(null)
const ligneSelectionnee = ref(null)

const optionsCandidats = computed(() =>
  candidats.value.map((candidature) => ({
    id: candidature.idCandidature,
    libelle: `${candidature.candidat?.prenom || ''} ${candidature.candidat?.nom || ''} · ${candidature.offre?.reference || ''} ${candidature.offre?.titre || ''}`.trim(),
    candidature,
  })),
)

const optionsPostes = computed(() =>
  postes.value.map((poste) => ({
    id: poste.id,
    libelle: `${poste.intitule} · ${poste.service || ''}`,
  })),
)

const estCdd = computed(() => form.value.typeContrat === 'CDD')

function isoDate(valeur) {
  if (!valeur) return null
  if (typeof valeur === 'string') return valeur.slice(0, 10)
  const annee = valeur.getFullYear()
  const mois = String(valeur.getMonth() + 1).padStart(2, '0')
  const jour = String(valeur.getDate()).padStart(2, '0')
  return `${annee}-${mois}-${jour}`
}

function versDate(valeur) {
  if (!valeur) return null
  return new Date(`${String(valeur).slice(0, 10)}T00:00:00`)
}

function chargeLigne(idDemandePoste) {
  ligneSelectionnee.value = lignes.value.find((ligne) => Number(ligne.id) === Number(idDemandePoste)) || null
  return ligneSelectionnee.value
}

async function chargerCandidature(idCandidature) {
  if (!idCandidature) {
    candidatSelectionne.value = null
    return
  }
  candidatSelectionne.value = await api.get(`/recrutement/candidatures/${idCandidature}/embauche`)
  const ligne = chargeLigne(candidatSelectionne.value.idDemandePoste)
  if (ligne?.idPoste && !form.value.idPoste) form.value.idPoste = ligne.idPoste
  if (!form.value.lieuTravail && candidatSelectionne.value.lieuTravailOffre) {
    form.value.lieuTravail = candidatSelectionne.value.lieuTravailOffre
  }
  if (!form.value.salaireBrutMensuel && ligne?.salaireMinEstime) {
    form.value.salaireBrutMensuel = Number(ligne.salaireMinEstime)
  }
  if (!form.value.typeContrat && candidatSelectionne.value.typeContratOffre) {
    form.value.typeContrat = candidatSelectionne.value.typeContratOffre
  }
}

async function chargerDetail(id) {
  const detail = await api.get(`/embauche/propositions/${id}`)
  form.value = {
    idCandidature: detail.idCandidature,
    idPoste: detail.idPoste,
    idManager: detail.idManager,
    typeContrat: detail.typeContrat || 'CDI',
    dureeMois: detail.dureeMois,
    motifCdd: detail.motifCdd || '',
    dateDebutPrevue: versDate(detail.dateDebutPrevue),
    periodeEssaiMois: detail.periodeEssaiMois ?? 1,
    salaireBrutMensuel: detail.salaireBrutMensuel === null ? null : Number(detail.salaireBrutMensuel),
    devise: detail.devise || 'MGA',
    primesAvantages: detail.primesAvantages || '',
    horaires: detail.horaires || '',
    lieuTravail: detail.lieuTravail || '',
    derogationFinanceDirection: Boolean(detail.derogationFinanceDirection),
  }
  const optionExistante = optionsCandidats.value.find((option) => option.id === detail.idCandidature)
  if (!optionExistante) {
    candidats.value.unshift({
      idCandidature: detail.idCandidature,
      candidat: { prenom: detail.candidatPrenom, nom: detail.candidatNom },
      offre: { reference: detail.referenceOffre, titre: detail.titreOffre },
    })
  }
  await chargerCandidature(detail.idCandidature)
}

async function charger() {
  chargement.value = true
  try {
    const [candidaturesRetenues, lignesAcceptees, listePostes] = await Promise.all([
      api.get('/recrutement/candidatures?statut=RETENUE'),
      api.get('/demandes/lignes/acceptees'),
      api.get('/postes?actif=true'),
    ])
    candidats.value = candidaturesRetenues
    lignes.value = lignesAcceptees
    postes.value = listePostes
    if (auth.estRh) {
      try {
        managers.value = await api.get('/utilisateurs?role=MANAGER&actif=true')
      } catch {
        managers.value = []
      }
    }
    if (estEdition.value) await chargerDetail(idProposition.value)
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger le formulaire.')
  } finally {
    chargement.value = false
  }
}

async function surChoixCandidat(event) {
  const id = event.value
  form.value.idCandidature = id
  form.value.idPoste = null
  form.value.salaireBrutMensuel = null
  form.value.lieuTravail = ''
  await chargerCandidature(id)
}

function surChoixPoste(event) {
  form.value.idPoste = event.value
  const poste = postes.value.find((element) => element.id === event.value)
  if (poste?.salaireMin && !form.value.salaireBrutMensuel) {
    form.value.salaireBrutMensuel = Number(poste.salaireMin)
  }
}

async function enregistrer() {
  if (enregistrement.value) return
  enregistrement.value = true
  try {
    const payload = {
      idCandidature: Number(form.value.idCandidature),
      idPoste: form.value.idPoste ? Number(form.value.idPoste) : null,
      idManager: form.value.idManager ? Number(form.value.idManager) : null,
      typeContrat: form.value.typeContrat,
      dureeMois: estCdd.value && form.value.dureeMois ? Number(form.value.dureeMois) : null,
      motifCdd: estCdd.value ? (form.value.motifCdd || null) : null,
      dateDebutPrevue: isoDate(form.value.dateDebutPrevue),
      periodeEssaiMois: form.value.periodeEssaiMois === null ? null : Number(form.value.periodeEssaiMois),
      salaireBrutMensuel: form.value.salaireBrutMensuel === null ? null : Number(form.value.salaireBrutMensuel),
      devise: form.value.devise || 'MGA',
      primesAvantages: form.value.primesAvantages || null,
      horaires: form.value.horaires || null,
      lieuTravail: form.value.lieuTravail || null,
      derogationFinanceDirection: Boolean(form.value.derogationFinanceDirection),
    }
    const resultat = estEdition.value
      ? await api.put(`/embauche/propositions/${idProposition.value}`, payload)
      : await api.post('/embauche/propositions', payload)
    notifications.succes(estEdition.value ? 'Proposition modifiée.' : 'Proposition créée.')
    await router.push({ name: 'embauche-proposition-detail', params: { id: resultat.id } })
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible d’enregistrer la proposition.')
  } finally {
    enregistrement.value = false
  }
}

function annuler() {
  router.push(estEdition.value
    ? { name: 'embauche-proposition-detail', params: { id: idProposition.value } }
    : { name: 'embauche-proposition-liste' })
}

watch(() => form.value.idCandidature, (id) => {
  if (id && !candidatSelectionne.value) {
    chargerCandidature(id).catch(() => {})
  }
})

onMounted(charger)
</script>

<template>
  <div class="mx-auto flex max-w-6xl flex-col gap-5">
    <div>
      <Button label="Retour" icon="pi pi-arrow-left" severity="secondary" variant="text" class="mb-2 p-0" @click="annuler" />
      <h1 class="m-0 text-2xl font-bold">
        {{ estEdition ? 'Modifier la proposition' : 'Nouvelle proposition d’embauche' }}
      </h1>
      <p class="mt-1 mb-0 text-muted-color">
        Seules les candidatures retenues peuvent recevoir une proposition. Le salaire doit respecter la fourchette du poste, sauf dérogation Finance / Direction.
      </p>
    </div>

    <div v-if="chargement" class="py-10 text-center text-muted-color">Chargement du formulaire...</div>

    <Card v-else>
      <template #content>
        <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div class="flex flex-col gap-2 md:col-span-2">
            <label class="text-sm font-semibold" for="candidature">Candidature retenue *</label>
            <Select id="candidature" v-model="form.idCandidature" :options="optionsCandidats"
              option-label="libelle" option-value="id" filter placeholder="Choisir une candidature"
              :disabled="estEdition" @change="surChoixCandidat" />
          </div>

          <div v-if="candidatSelectionne" class="rounded-lg border border-surface p-4 md:col-span-2">
            <div class="font-semibold">
              {{ candidatSelectionne.prenom }} {{ candidatSelectionne.nom }}
            </div>
            <div class="text-sm text-muted-color">
              {{ candidatSelectionne.email }} · {{ candidatSelectionne.telephone || '—' }}
            </div>
            <div class="mt-1 text-sm text-muted-color">
              Offre : {{ candidatSelectionne.referenceOffre }} · {{ candidatSelectionne.titreOffre }}
            </div>
            <div v-if="ligneSelectionnee" class="mt-2 text-sm">
              Besoin : {{ ligneSelectionnee.poste || ligneSelectionnee.intituleDemande }} ·
              effectif {{ ligneSelectionnee.nombrePourvus || 0 }}/{{ ligneSelectionnee.effectif }}
              · salaire indicatif {{ formatMontant(ligneSelectionnee.salaireMinEstime) }} –
              {{ formatMontant(ligneSelectionnee.salaireMaxEstime) }}
            </div>
          </div>

          <div class="flex flex-col gap-2">
            <label class="text-sm font-semibold" for="poste">Poste du catalogue</label>
            <Select id="poste" v-model="form.idPoste" :options="optionsPostes" option-label="libelle"
              option-value="id" filter placeholder="Poste issu du besoin" @change="surChoixPoste" />
          </div>

          <div class="flex flex-col gap-2">
            <label class="text-sm font-semibold" for="type">Type de contrat *</label>
            <Select id="type" v-model="form.typeContrat" :options="TYPES" option-label="label" option-value="value" />
          </div>

          <div v-if="estCdd" class="flex flex-col gap-2">
            <label class="text-sm font-semibold" for="duree">Durée du CDD (mois) *</label>
            <InputNumber id="duree" v-model="form.dureeMois" :min="1" show-buttons />
          </div>

          <div v-if="estCdd" class="sm:col-span-2 flex flex-col gap-2">
            <label class="text-sm font-semibold" for="motif-cdd">Motif du recours au CDD *</label>
            <InputText id="motif-cdd" v-model="form.motifCdd" />
          </div>

          <div class="flex flex-col gap-2">
            <label class="text-sm font-semibold" for="date-debut">Date de début prévue *</label>
            <DatePicker id="date-debut" v-model="form.dateDebutPrevue" date-format="dd/mm/yy" show-icon />
          </div>

          <div class="flex flex-col gap-2">
            <label class="text-sm font-semibold" for="essai">Période d’essai (mois)</label>
            <InputNumber id="essai" v-model="form.periodeEssaiMois" :min="0" :max="12" show-buttons />
          </div>

          <div class="flex flex-col gap-2">
            <label class="text-sm font-semibold" for="salaire">Salaire brut mensuel *</label>
            <InputNumber id="salaire" v-model="form.salaireBrutMensuel" mode="currency" currency="MGA"
              locale="fr-FR" :min="0" />
          </div>

          <div class="flex flex-col gap-2">
            <label class="text-sm font-semibold" for="devise">Devise</label>
            <InputText id="devise" v-model="form.devise" maxlength="3" />
          </div>

          <div class="flex flex-col gap-2">
            <label class="text-sm font-semibold" for="lieu">Lieu de travail</label>
            <InputText id="lieu" v-model="form.lieuTravail" />
          </div>

          <div class="flex flex-col gap-2">
            <label class="text-sm font-semibold" for="manager">Manager</label>
            <Select id="manager" v-model="form.idManager" :options="managers" option-label="nom"
              option-value="id" filter show-clear placeholder="Non défini" />
          </div>

          <div class="flex flex-col gap-2 md:col-span-2">
            <label class="text-sm font-semibold" for="horaires">Horaires</label>
            <InputText id="horaires" v-model="form.horaires" placeholder="Ex. 08h00 – 17h00, du lundi au vendredi" />
          </div>

          <div class="flex flex-col gap-2 md:col-span-2">
            <label class="text-sm font-semibold" for="primes">Primes et avantages</label>
            <Textarea id="primes" v-model="form.primesAvantages" rows="3" auto-resize />
          </div>

          <label class="flex items-center gap-2 md:col-span-2">
            <input v-model="form.derogationFinanceDirection" type="checkbox" class="size-4" />
            <span class="text-sm">Dérogation salariale demandée (validation Finance / Direction requise)</span>
          </label>
        </div>

        <div class="mt-5 flex justify-end gap-2">
          <Button label="Annuler" severity="secondary" outlined @click="annuler" />
          <Button :label="estEdition ? 'Enregistrer' : 'Créer la proposition'" icon="pi pi-check"
            :loading="enregistrement" @click="enregistrer" />
        </div>
      </template>
    </Card>
  </div>
</template>
