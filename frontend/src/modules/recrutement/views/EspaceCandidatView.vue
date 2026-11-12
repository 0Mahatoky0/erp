<script setup>
import PrimeButton from 'primevue/button'
import Tag from 'primevue/tag'
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { recrutementApi as api, ErreurApi } from '@/modules/recrutement/api/recrutementApi'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import ModalDialog from '@/shared/components/ModalDialog.vue'
import { formatDate, formatDateHeure, formatMontant } from '@/shared/utils/format'

const auth = useAuthStore()
const router = useRouter()
const offres = ref([])
const candidatures = ref([])
const contrats = ref([])
const chargement = ref(false)
const erreur = ref('')
const contratASigner = ref(null)
const nomSignataire = ref('')
const signatureEnCours = ref(false)

async function chargerOffres() {
  chargement.value = true
  erreur.value = ''
  try {
    offres.value = await api.get('/recrutement/offres?statut=PUBLIEE')
  } catch (cause) {
    erreur.value = cause instanceof ErreurApi ? cause.message : 'Les offres sont momentanement indisponibles.'
  } finally {
    chargement.value = false
  }
}

async function chargerMesCandidatures() {
  try {
    candidatures.value = await api.get('/recrutement/candidatures/mes-candidatures')
  } catch (cause) {
    const message = cause instanceof ErreurApi ? cause.message : 'Impossible de charger vos candidatures.'
    console.error(message)
    candidatures.value = []
  }
}

async function chargerMesContrats() {
  try {
    contrats.value = await api.get('/contrats/mes-contrats')
  } catch {
    contrats.value = []
  }
}

function ouvrirSignature(contrat) {
  contratASigner.value = contrat
  nomSignataire.value = auth.profil?.nom || ''
}

async function signerContrat() {
  if (!contratASigner.value || !nomSignataire.value.trim()) return
  signatureEnCours.value = true
  try {
    await api.post(`/contrats/mes-contrats/${contratASigner.value.id}/signer`,
      { nomSignataire: nomSignataire.value })
    contratASigner.value = null
    nomSignataire.value = ''
    await chargerMesContrats()
  } catch (cause) {
    erreur.value = cause instanceof ErreurApi ? cause.message : 'Signature impossible.'
  } finally {
    signatureEnCours.value = false
  }
}

function seDeconnecter() {
  auth.deconnexion()
  router.push({ name: 'accueil-candidat' })
}

onMounted(async () => {
  await chargerOffres()
  await chargerMesCandidatures()
  await chargerMesContrats()
})
</script>

<template>
  <main class="min-h-screen bg-surface-50 p-4 text-color md:p-8 dark:bg-surface-950">
    <header class="mx-auto mb-8 flex w-full max-w-6xl flex-col gap-5 rounded-xl border border-surface bg-surface-0 p-5 shadow-sm dark:bg-surface-900 md:flex-row md:items-center md:justify-between">
      <div>
        <p class="text-xs font-semibold uppercase tracking-wider text-primary">ERP TOVO · ESPACE CANDIDAT</p>
        <h1 class="mt-1 text-2xl font-bold">Bonjour, {{ auth.profil?.nom }}</h1>
        <p class="text-sm text-muted-color">{{ auth.profil?.email }}</p>
      </div>
      <div class="flex flex-wrap items-center gap-3">
        <Tag value="Candidat connecté" severity="success" />
        <PrimeButton label="Se déconnecter" icon="pi pi-sign-out" severity="secondary" outlined @click="seDeconnecter" />
      </div>
    </header>

    <section class="mx-auto grid w-full max-w-6xl gap-5 lg:grid-cols-[minmax(0,2fr)_minmax(280px,1fr)]">
      <div class="rounded-xl border border-surface bg-surface-0 p-6 shadow-sm dark:bg-surface-900">
        <div class="mb-5 flex flex-wrap items-end justify-between gap-3">
          <div>
            <p class="text-xs font-semibold uppercase tracking-wider text-primary">Opportunités</p>
            <h2 class="mt-1 text-2xl font-bold">Offres d’emploi ouvertes</h2>
          </div>
          <PrimeButton label="Actualiser" icon="pi pi-refresh" outlined severity="secondary" @click="chargerOffres" />
        </div>
        <p v-if="erreur" class="mb-4 text-sm text-red-600" role="alert">{{ erreur }}</p>
        <p v-if="chargement" class="text-muted-color">Chargement des offres...</p>
        <div v-else class="grid gap-4 md:grid-cols-2">
          <article v-for="offre in offres" :key="offre.idOffre" class="flex flex-col rounded-lg border border-surface p-4">
            <span class="text-xs font-semibold uppercase tracking-wide text-muted-color">{{ offre.reference }}</span>
            <h3 class="my-2 text-lg font-bold">{{ offre.titre }}</h3>
            <p class="text-sm text-muted-color">{{ offre.lieuTravail || 'Lieu à définir' }} · {{ offre.typeContrat || 'Contrat à définir' }}</p>
            <p class="mt-3 flex-1 text-sm text-muted-color">{{ offre.description }}</p>
            <RouterLink class="mt-4 inline-flex justify-center rounded-md bg-primary px-4 py-2 font-semibold text-primary-contrast" :to="{ name: 'accueil-candidat', query: { offre: offre.idOffre } }">
              Postuler
            </RouterLink>
          </article>
          <p v-if="!offres.length" class="py-8 text-center text-muted-color md:col-span-2">Aucune offre ouverte pour le moment.</p>
        </div>
      </div>

      <aside class="rounded-xl border border-surface bg-surface-0 p-6 shadow-sm dark:bg-surface-900">
        <p class="text-xs font-semibold uppercase tracking-wider text-primary">Suivi</p>
        <h2 class="mt-1 text-2xl font-bold">Mes candidatures</h2>

        <div v-if="!candidatures.length" class="mt-5 rounded-lg border border-dashed border-surface-300 p-5 text-center dark:border-surface-700">
          <i class="pi pi-bell text-2xl text-muted-color" aria-hidden="true"></i>
          <p class="mt-3 font-semibold">Aucune candidature pour le moment</p>
          <p class="mt-1 text-sm text-muted-color">Votre historique apparaîtra ici dès que vous aurez postulé.</p>
        </div>

        <div v-else class="mt-5 space-y-3">
          <article v-for="candidature in candidatures" :key="candidature.idCandidature" class="rounded-lg border border-surface bg-surface-50 p-3 dark:bg-surface-800">
            <div class="flex items-start justify-between gap-3">
              <div>
                <div class="text-xs font-semibold uppercase tracking-wide text-muted-color">
                  {{ candidature.offre?.reference || 'Candidature' }}
                </div>
                <div class="mt-1 font-semibold">{{ candidature.offre?.titre || 'Poste' }}</div>
              </div>
              <StatusBadge :valeur="candidature.statut" />
            </div>
            <div class="mt-2 text-xs text-muted-color">Déposée le {{ formatDateHeure(candidature.dateCandidature) }}</div>
          </article>
        </div>
      </aside>
    </section>

    <section class="mx-auto mt-6 w-full max-w-6xl rounded-xl border border-surface bg-surface-0 p-6 shadow-sm dark:bg-surface-900">
      <div class="mb-4">
        <p class="text-xs font-semibold uppercase tracking-wider text-primary">Contrat</p>
        <h2 class="mt-1 text-2xl font-bold">Mes contrats</h2>
        <p class="mt-1 text-sm text-muted-color">
          Consultez votre contrat et signez-le électroniquement depuis votre compte candidat.
        </p>
      </div>

      <p v-if="!contrats.length" class="rounded-lg border border-dashed border-surface-300 p-5 text-center text-muted-color dark:border-surface-700">
        Aucun contrat disponible pour le moment.
      </p>

      <div v-else class="grid gap-4 md:grid-cols-2">
        <article v-for="contrat in contrats" :key="contrat.id" class="rounded-lg border border-surface p-4">
          <div class="flex flex-wrap items-center justify-between gap-2">
            <span class="font-semibold">{{ contrat.numero }}</span>
            <StatusBadge :valeur="contrat.etat" />
          </div>
          <dl class="mt-3 grid grid-cols-2 gap-2 text-sm">
            <div><dt class="text-xs uppercase text-muted-color">Type</dt><dd class="m-0">{{ contrat.typeContrat }}</dd></div>
            <div><dt class="text-xs uppercase text-muted-color">Début</dt><dd class="m-0">{{ formatDate(contrat.dateDebut) }}</dd></div>
            <div><dt class="text-xs uppercase text-muted-color">Fin prévue</dt><dd class="m-0">{{ formatDate(contrat.dateFinPrevue) }}</dd></div>
            <div><dt class="text-xs uppercase text-muted-color">Salaire</dt><dd class="m-0">{{ formatMontant(contrat.salaireBase) }}</dd></div>
          </dl>
          <p v-if="contrat.dateSignatureSalarie" class="mt-3 text-xs text-muted-color">
            Signé par vous le {{ formatDateHeure(contrat.dateSignatureSalarie) }}
          </p>
          <PrimeButton v-if="contrat.etat === 'CREE' && !contrat.dateSignatureSalarie"
            class="mt-3 w-full" label="Signer mon contrat" icon="pi pi-pen-to-square"
            @click="ouvrirSignature(contrat)" />
          <p v-else-if="contrat.etat === 'CREE'" class="mt-3 text-xs text-muted-color">
            En attente de la signature de l’employeur.
          </p>
        </article>
      </div>
    </section>

    <ModalDialog v-if="contratASigner" titre="Signer mon contrat"
      sous-titre="Votre signature est horodatée et accompagnée d’une empreinte SHA-256."
      @fermer="contratASigner = null">
      <label class="mb-2 block text-sm font-semibold" for="nom-signataire">Nom du signataire</label>
      <input id="nom-signataire" v-model="nomSignataire" class="w-full rounded-md border border-surface-300 p-2 dark:border-surface-700 dark:bg-surface-800" />
      <template #actions>
        <PrimeButton label="Annuler" severity="secondary" outlined @click="contratASigner = null" />
        <PrimeButton label="Signer" icon="pi pi-pen-to-square" :loading="signatureEnCours"
          :disabled="!nomSignataire.trim()" @click="signerContrat" />
      </template>
    </ModalDialog>
  </main>
</template>
