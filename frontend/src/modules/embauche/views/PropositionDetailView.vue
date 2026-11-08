<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import ModalDialog from '@/shared/components/ModalDialog.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDate, formatDateHeure, formatMontant, libelle } from '@/shared/utils/format'
import { embaucheApi as api, ErreurApi } from '../api/embaucheApi'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationStore()

const proposition = ref(null)
const contrat = ref(null)
const onboarding = ref(null)
const chargement = ref(false)
const actionEnCours = ref(false)

const dialogue = ref(null)
const joursProlongation = ref(7)
const motifRetrait = ref('')
const reponseInterne = ref({ typeReponse: 'ACCEPTATION', commentaire: '', preuve: '' })

const TYPES_REPONSE = [
  { label: 'Acceptation', value: 'ACCEPTATION' },
  { label: 'Refus', value: 'REFUS' },
  { label: 'Contre-proposition', value: 'CONTRE_PROPOSITION' },
]

const id = computed(() => route.params.id)
const peutEcrire = computed(() => auth.estRh || auth.estDirection)
const peutValider = computed(() => {
  if (!auth.estRh && !auth.estDirection && !auth.estFinance) return false
  if (proposition.value?.derogationFinanceDirection) return auth.estFinance || auth.estDirection
  return true
})
const peutModifier = computed(() => peutEcrire.value && proposition.value?.statut === 'BROUILLON')
const peutValiderMaintenant = computed(() =>
  proposition.value?.statut === 'BROUILLON' && peutValider.value,
)
const peutEnvoyer = computed(() => peutEcrire.value && proposition.value?.statut === 'VALIDEE_INTERNE')
const peutRetirer = computed(() =>
  peutEcrire.value && ['BROUILLON', 'VALIDEE_INTERNE', 'ENVOYEE'].includes(proposition.value?.statut),
)
const peutProlonger = computed(() => peutEcrire.value && proposition.value?.statut === 'ENVOYEE')
const peutRepondre = computed(() => peutEcrire.value && proposition.value?.statut === 'ENVOYEE')
const lienPublic = computed(() =>
  proposition.value?.tokenAcceptation
    ? router.resolve({
        name: 'embauche-proposition-publique',
        params: { token: proposition.value.tokenAcceptation },
      }).href
    : null,
)

async function charger() {
  chargement.value = true
  try {
    proposition.value = await api.get(`/embauche/propositions/${id.value}`)
    contrat.value = null
    onboarding.value = null
    if (proposition.value.idContratCree) {
      contrat.value = await api.get(`/contrats/${proposition.value.idContratCree}`)
    }
    if (proposition.value.idEmployeCree) {
      onboarding.value = await api.get(`/contrats/employe/${proposition.value.idEmployeCree}/onboarding`)
    }
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger la proposition.')
  } finally {
    chargement.value = false
  }
}

async function executer(action, succes) {
  if (actionEnCours.value) return
  actionEnCours.value = true
  try {
    await action()
    notifications.succes(succes)
    dialogue.value = null
    await charger()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Action impossible.')
  } finally {
    actionEnCours.value = false
  }
}

const modifier = () => router.push({
  name: 'embauche-proposition-modification',
  params: { id: id.value },
})

const valider = () => executer(
  () => api.post(`/embauche/propositions/${id.value}/valider`),
  'Proposition validée en interne.',
)

const envoyer = () => executer(
  () => api.post(`/embauche/propositions/${id.value}/envoyer`),
  'Proposition envoyée au candidat.',
)

const confirmerRetrait = () => executer(
  () => api.post(`/embauche/propositions/${id.value}/retirer`, { motif: motifRetrait.value || null }),
  'Proposition retirée.',
)

const confirmerProlongation = () => executer(
  () => api.post(`/embauche/propositions/${id.value}/prolonger`, { jours: Number(joursProlongation.value) }),
  'Date d’expiration prolongée.',
)

const confirmerReponse = () => executer(
  () => api.post(`/embauche/propositions/${id.value}/repondre`, {
    typeReponse: reponseInterne.value.typeReponse,
    commentaire: reponseInterne.value.commentaire || null,
    preuve: reponseInterne.value.preuve || null,
  }),
  'Réponse du candidat enregistrée.',
)

function ouvrirLienPublic() {
  if (lienPublic.value) window.open(lienPublic.value, '_blank')
}

async function copierLien() {
  if (!lienPublic.value) return
  const url = `${window.location.origin}${lienPublic.value}`
  try {
    await navigator.clipboard.writeText(url)
    notifications.succes('Lien copié dans le presse-papiers.')
  } catch {
    notifications.erreur('Impossible de copier le lien.')
  }
}

const telechargerContrat = () => executer(
  () => api.telecharger(`/contrats/${proposition.value.idContratCree}/document`,
    `${contrat.value?.numero || 'contrat'}.pdf`),
  'Téléchargement du contrat lancé.',
)

const ouvrirDossier = () => router.push({
  name: 'embauche-dossier-employe',
  params: { id: proposition.value.idEmployeCree },
})

onMounted(charger)
</script>

<template>
  <div v-if="chargement" class="py-10 text-center text-muted-color">Chargement de la proposition...</div>

  <div v-else-if="proposition" class="mx-auto flex max-w-7xl flex-col gap-5">
    <header class="flex flex-col justify-between gap-4 md:flex-row md:items-start">
      <div>
        <Button label="Retour à la liste" icon="pi pi-arrow-left" severity="secondary" variant="text"
          class="mb-2 p-0" @click="router.push({ name: 'embauche-proposition-liste' })" />
        <div class="flex flex-wrap items-center gap-3">
          <h1 class="m-0 text-2xl font-bold">{{ proposition.reference }}</h1>
          <StatusBadge :valeur="proposition.statut" />
          <span class="text-sm text-muted-color">Version {{ proposition.version }}</span>
        </div>
        <p class="mt-1 mb-0 text-muted-color">
          {{ proposition.candidatPrenom }} {{ proposition.candidatNom }} · {{ proposition.poste || 'Poste non défini' }}
        </p>
      </div>
      <div class="flex flex-wrap gap-2">
        <Button v-if="peutModifier" label="Modifier" icon="pi pi-pencil" severity="secondary" outlined @click="modifier" />
        <Button v-if="peutValiderMaintenant" label="Valider" icon="pi pi-check" severity="success" :loading="actionEnCours" @click="valider" />
        <Button v-if="peutEnvoyer" label="Envoyer au candidat" icon="pi pi-send" :loading="actionEnCours" @click="envoyer" />
      </div>
    </header>

    <div v-if="lienPublic" class="rounded-lg border border-primary/30 bg-primary/5 p-4">
      <div class="mb-2 font-semibold">Lien sécurisé du candidat</div>
      <div class="flex flex-wrap items-center gap-2">
        <InputText :model-value="lienPublic" readonly class="min-w-72 flex-1" />
        <Button label="Copier" icon="pi pi-copy" severity="secondary" outlined @click="copierLien" />
        <Button label="Ouvrir" icon="pi pi-external-link" outlined @click="ouvrirLienPublic" />
      </div>
      <p class="mt-2 mb-0 text-sm text-muted-color">
        Expire le {{ formatDateHeure(proposition.dateExpiration) }}. Le lien permet au candidat d’accepter, refuser ou faire une contre-proposition.
      </p>
    </div>

    <div class="grid grid-cols-1 gap-5 lg:grid-cols-3">
      <Card class="lg:col-span-2">
        <template #title>Proposition</template>
        <template #content>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div><div class="text-xs font-semibold uppercase text-muted-color">Type de contrat</div><div>{{ proposition.typeContrat || '—' }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Durée</div><div>{{ proposition.dureeMois ? `${proposition.dureeMois} mois` : '—' }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Entrée prévue</div><div>{{ formatDate(proposition.dateDebutPrevue) }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Période d’essai</div><div>{{ proposition.periodeEssaiMois ?? '—' }} mois</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Salaire brut</div><div>{{ formatMontant(proposition.salaireBrutMensuel) }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Devise</div><div>{{ proposition.devise || '—' }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Poste</div><div>{{ proposition.poste || '—' }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Service</div><div>{{ proposition.service || '—' }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Lieu de travail</div><div>{{ proposition.lieuTravail || '—' }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Horaires</div><div>{{ proposition.horaires || '—' }}</div></div>
            <div class="sm:col-span-2"><div class="text-xs font-semibold uppercase text-muted-color">Primes et avantages</div><div class="whitespace-pre-line">{{ proposition.primesAvantages || '—' }}</div></div>
          </div>
          <div v-if="proposition.derogationFinanceDirection" class="mt-3 rounded border border-amber-300 bg-amber-50 p-2 text-sm text-amber-800">
            Dérogation salariale : validation Finance / Direction requise.
          </div>
        </template>
      </Card>

      <Card>
        <template #title>Candidat</template>
        <template #content>
          <div class="flex flex-col gap-2 text-sm">
            <div><span class="text-muted-color">Nom :</span> {{ proposition.candidatPrenom }} {{ proposition.candidatNom }}</div>
            <div><span class="text-muted-color">Email :</span> {{ proposition.candidatEmail || '—' }}</div>
            <div><span class="text-muted-color">Téléphone :</span> {{ proposition.candidatTelephone || '—' }}</div>
            <div><span class="text-muted-color">CIN :</span> {{ proposition.candidatCin || '—' }}</div>
            <div><span class="text-muted-color">Naissance :</span> {{ formatDate(proposition.candidatDateNaissance) }}</div>
            <div><span class="text-muted-color">Adresse :</span> {{ proposition.candidatAdresse || '—' }}</div>
            <div class="mt-2 border-t border-surface pt-2"><span class="text-muted-color">Offre :</span> {{ proposition.referenceOffre }} · {{ proposition.titreOffre }}</div>
          </div>
        </template>
      </Card>
    </div>

    <Card v-if="contrat">
      <template #title>Contrat et intégration</template>
      <template #content>
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div><div class="text-xs font-semibold uppercase text-muted-color">Contrat</div><div>{{ contrat.numero }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Début</div><div>{{ formatDate(contrat.dateDebut) }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Fin prévue</div><div>{{ formatDate(contrat.dateFinPrevue) }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Salaire de base</div><div>{{ formatMontant(contrat.salaireBase) }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">État</div><div>{{ libelle(contrat.etat) }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Employé</div><div>#{{ proposition.idEmployeCree }}</div></div>
        </div>

        <div v-if="onboarding" class="mt-4">
          <div class="mb-2 flex items-center justify-between">
            <strong>Checklist d’intégration ({{ onboarding.progression || 0 }} %)</strong>
            <StatusBadge :valeur="onboarding.statut" />
          </div>
          <DataTable :value="onboarding.taches || []" data-key="id" size="small" striped-rows>
            <Column field="libelle" header="Tâche" />
            <Column field="categorie" header="Catégorie">
              <template #body="{ data }">{{ libelle(data.categorie) }}</template>
            </Column>
            <Column field="responsable" header="Responsable" />
            <Column header="Échéance"><template #body="{ data }">{{ formatDate(data.echeance) }}</template></Column>
            <Column header="Statut"><template #body="{ data }"><StatusBadge :valeur="data.statut" /></template></Column>
          </DataTable>
        </div>

        <div class="mt-4 flex flex-wrap gap-2">
          <Button label="Télécharger le contrat" icon="pi pi-download" outlined :loading="actionEnCours" @click="telechargerContrat" />
          <Button label="Voir le dossier salarié" icon="pi pi-user" outlined @click="ouvrirDossier" />
        </div>
      </template>
    </Card>

    <div v-if="peutRetirer || peutProlonger || peutRepondre" class="flex flex-wrap gap-2">
      <Button v-if="peutProlonger" label="Prolonger" icon="pi pi-calendar-plus" severity="secondary" outlined @click="dialogue = 'prolonger'" />
      <Button v-if="peutRepondre" label="Saisir la réponse du candidat" icon="pi pi-reply" severity="secondary" outlined @click="dialogue = 'repondre'" />
      <Button v-if="peutRetirer" label="Retirer la proposition" icon="pi pi-times" severity="danger" outlined @click="dialogue = 'retirer'" />
    </div>

    <div class="grid grid-cols-1 gap-5 lg:grid-cols-2">
      <Card>
        <template #title>Historique</template>
        <template #content>
          <DataTable :value="proposition.historique || []" data-key="id" size="small" striped-rows>
            <Column header="Date"><template #body="{ data }">{{ formatDateHeure(data.dateAction) }}</template></Column>
            <Column field="auteurType" header="Auteur"><template #body="{ data }">{{ libelle(data.auteurType) }}</template></Column>
            <Column header="Transition">
              <template #body="{ data }">
                {{ data.ancienStatut ? libelle(data.ancienStatut) : '—' }} → {{ libelle(data.nouveauStatut) }}
              </template>
            </Column>
            <Column field="motif" header="Motif" />
          </DataTable>
        </template>
      </Card>

      <Card>
        <template #title>Réponses et versions</template>
        <template #content>
          <div class="mb-3">
            <div class="mb-1 text-sm font-semibold">Réponses du candidat</div>
            <DataTable :value="proposition.reponses || []" data-key="id" size="small" striped-rows>
              <Column header="Date"><template #body="{ data }">{{ formatDateHeure(data.dateReponse) }}</template></Column>
              <Column field="typeReponse" header="Type"><template #body="{ data }">{{ libelle(data.typeReponse) }}</template></Column>
              <Column field="commentaire" header="Commentaire" />
            </DataTable>
          </div>
          <div>
            <div class="mb-1 text-sm font-semibold">Versions</div>
            <DataTable :value="proposition.versions || []" data-key="id" size="small" striped-rows>
              <Column field="numeroVersion" header="Version" />
              <Column header="Date"><template #body="{ data }">{{ formatDateHeure(data.dateCreation) }}</template></Column>
              <Column field="contenu" header="Contenu" />
            </DataTable>
          </div>
        </template>
      </Card>
    </div>
  </div>

  <ModalDialog v-if="dialogue === 'prolonger'" titre="Prolonger la proposition"
    sous-titre="La nouvelle échéance repart de la date d’expiration actuelle ou de maintenant." @fermer="dialogue = null">
    <div class="flex flex-col gap-2">
      <label class="text-sm font-semibold" for="jours">Nombre de jours</label>
      <InputNumber id="jours" v-model="joursProlongation" :min="1" show-buttons />
    </div>
    <template #actions>
      <Button label="Annuler" severity="secondary" outlined @click="dialogue = null" />
      <Button label="Prolonger" :loading="actionEnCours" @click="confirmerProlongation" />
    </template>
  </ModalDialog>

  <ModalDialog v-if="dialogue === 'retirer'" titre="Retirer la proposition"
    sous-titre="Le candidat ne pourra plus répondre à cette proposition." @fermer="dialogue = null">
    <div class="flex flex-col gap-2">
      <label class="text-sm font-semibold" for="motif-retrait">Motif</label>
      <Textarea id="motif-retrait" v-model="motifRetrait" rows="3" auto-resize />
    </div>
    <template #actions>
      <Button label="Annuler" severity="secondary" outlined @click="dialogue = null" />
      <Button label="Retirer" severity="danger" :loading="actionEnCours" @click="confirmerRetrait" />
    </template>
  </ModalDialog>

  <ModalDialog v-if="dialogue === 'repondre'" titre="Saisir la réponse du candidat"
    sous-titre="Utilisé lorsque la réponse arrive par email ou par courrier." large @fermer="dialogue = null">
    <div class="flex flex-col gap-4">
      <div class="flex flex-col gap-2">
        <label class="text-sm font-semibold" for="type-reponse">Réponse</label>
        <Select id="type-reponse" v-model="reponseInterne.typeReponse" :options="TYPES_REPONSE"
          option-label="label" option-value="value" />
      </div>
      <div class="flex flex-col gap-2">
        <label class="text-sm font-semibold" for="commentaire-reponse">Commentaire / motif</label>
        <Textarea id="commentaire-reponse" v-model="reponseInterne.commentaire" rows="3" auto-resize />
      </div>
      <div class="flex flex-col gap-2">
        <label class="text-sm font-semibold" for="preuve-reponse">Preuve (email, courrier, référence)</label>
        <InputText id="preuve-reponse" v-model="reponseInterne.preuve" />
      </div>
    </div>
    <template #actions>
      <Button label="Annuler" severity="secondary" outlined @click="dialogue = null" />
      <Button label="Enregistrer la réponse" :loading="actionEnCours" @click="confirmerReponse" />
    </template>
  </ModalDialog>
</template>
