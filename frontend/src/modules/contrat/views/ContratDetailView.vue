<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import DatePicker from 'primevue/datepicker'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import ProgressBar from 'primevue/progressbar'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import ModalDialog from '@/shared/components/ModalDialog.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDate, formatDateHeure, formatMontant, libelle } from '@/shared/utils/format'
import { contratApi as api, ErreurApi } from '../api/contratApi'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationStore()

const id = computed(() => route.params.id)
const contrat = ref(null)
const salarie = ref(null)
const affectations = ref([])
const onboarding = ref(null)
const avenants = ref([])
const notificationsContrat = ref([])
const chargement = ref(true)
const actionEnCours = ref(false)
const dialogue = ref(null)

const motifAnnulation = ref('')
const renouvellement = ref({})
const transformation = ref({})
const avenant = ref({})
const decisionEssai = ref({ decision: 'CONFIRMATION', commentaire: '' })

const DECISIONS_ESSAI = [
  { valeur: 'CONFIRMATION', libelle: 'Confirmer le salarié' },
  { valeur: 'RUPTURE', libelle: 'Rompre la période d’essai' },
]

const TYPES_AVENANT = [
  { valeur: 'SALAIRE', libelle: 'Salaire' },
  { valeur: 'POSTE', libelle: 'Poste' },
  { valeur: 'HORAIRES', libelle: 'Horaires' },
  { valeur: 'LIEU', libelle: 'Lieu' },
  { valeur: 'DUREE', libelle: 'Durée' },
  { valeur: 'AUTRE', libelle: 'Autre' },
]

const STATUTS_TACHE = [
  { valeur: 'A_FAIRE', libelle: 'À faire' },
  { valeur: 'EN_COURS', libelle: 'En cours' },
  { valeur: 'FAIT', libelle: 'Fait' },
  { valeur: 'NON_APPLICABLE', libelle: 'Non applicable' },
  { valeur: 'BLOQUE', libelle: 'Bloqué' },
]

const peutAgir = computed(() => auth.estRh || auth.estDirection)
const peutSigner = computed(() =>
  peutAgir.value && contrat.value?.etat === 'CREE' && !contrat.value?.dateSignatureEmployeur,
)
const peutValider = computed(() => peutAgir.value && contrat.value?.etat === 'SIGNE')
const peutAnnuler = computed(() =>
  peutAgir.value && (contrat.value?.etat === 'CREE'
    || (contrat.value?.etat === 'SIGNE' && new Date(contrat.value.dateDebut) > new Date())),
)
const estCdd = computed(() => ['CDD', 'CDD_RENOUVELLEMENT'].includes(contrat.value?.typeContrat))
const peutRenouveler = computed(() => peutAgir.value && contrat.value?.etat === 'VALIDE' && estCdd.value)
const peutTransformer = computed(() => peutAgir.value && contrat.value?.etat === 'VALIDE' && estCdd.value)
const peutAvenant = computed(() =>
  peutAgir.value && ['SIGNE', 'VALIDE'].includes(contrat.value?.etat),
)
const peutDeciderEssai = computed(() =>
  peutAgir.value && ['SIGNE', 'VALIDE'].includes(contrat.value?.etat)
  && contrat.value?.finPeriodeEssai && !contrat.value?.decisionFinEssai,
)
const tachesBloquantes = computed(() =>
  (onboarding.value?.taches || []).filter((t) => t.bloquante
    && !['FAIT', 'NON_APPLICABLE'].includes(t.statut)),
)

function isoDate(date) {
  if (!date) return null
  if (typeof date === 'string') return date
  const decale = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return decale.toISOString().slice(0, 10)
}

function versDate(valeur) {
  return valeur ? new Date(`${String(valeur).slice(0, 10)}T00:00:00`) : null
}

async function charger() {
  chargement.value = true
  try {
    contrat.value = await api.get(`/contrats/${id.value}`)
    const [dossier, listeAffectations, parcours, listeAvenants, listeNotifications] = await Promise.all([
      api.get(`/employes/${contrat.value.idEmploye}/dossier`),
      api.get(`/contrats/employe/${contrat.value.idEmploye}/affectations`),
      api.get(`/contrats/${id.value}/onboarding`).catch(() => null),
      api.get(`/contrats/${id.value}/avenants`),
      api.get('/contrats/notifications').catch(() => []),
    ])
    salarie.value = dossier
    affectations.value = listeAffectations
    onboarding.value = parcours || null
    avenants.value = listeAvenants
    notificationsContrat.value = listeNotifications.filter(
      (notification) => notification.idContrat === contrat.value.id,
    )
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger le contrat.')
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

const signer = () => executer(
  () => api.post(`/contrats/${id.value}/signer-employeur`),
  'Signature employeur enregistrée.',
)

function ouvrirDecisionEssai() {
  decisionEssai.value = { decision: 'CONFIRMATION', commentaire: '' }
  dialogue.value = 'essai'
}

const confirmerDecisionEssai = () => executer(
  () => api.post(`/contrats/${id.value}/decision-fin-essai`, {
    decision: decisionEssai.value.decision,
    commentaire: decisionEssai.value.commentaire || null,
  }),
  'Décision de fin d’essai enregistrée.',
)

async function marquerNotificationLue(notification) {
  try {
    await api.patch(`/contrats/notifications/${notification.id}/lire`)
    notification.statut = 'LUE'
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Action impossible.')
  }
}

const valider = () => executer(
  () => api.post(`/contrats/${id.value}/valider`),
  'Contrat validé.',
)

const confirmerAnnulation = () => executer(
  () => api.post(`/contrats/${id.value}/annuler`, { motif: motifAnnulation.value }),
  'Contrat annulé.',
)

function ouvrirRenouvellement() {
  const fin = contrat.value?.dateFinPrevue ? versDate(contrat.value.dateFinPrevue) : new Date()
  if (fin) fin.setMonth(fin.getMonth() + 6)
  renouvellement.value = {
    dateFinPrevue: fin,
    salaireBase: contrat.value?.salaireBase ? Number(contrat.value.salaireBase) : null,
    primesAvantages: contrat.value?.primesAvantages || '',
    horaires: contrat.value?.horaires || '',
    lieuTravail: contrat.value?.lieuTravail || '',
    motifCdd: contrat.value?.motifCdd || '',
  }
  dialogue.value = 'renouvellement'
}

const confirmerRenouvellement = () => executer(
  () => api.post(`/contrats/${id.value}/renouveler`, {
    dateFinPrevue: isoDate(renouvellement.value.dateFinPrevue),
    salaireBase: renouvellement.value.salaireBase,
    primesAvantages: renouvellement.value.primesAvantages || null,
    horaires: renouvellement.value.horaires || null,
    lieuTravail: renouvellement.value.lieuTravail || null,
    motifCdd: renouvellement.value.motifCdd,
  }),
  'Renouvellement créé. Il doit être signé puis validé.',
)

function ouvrirTransformation() {
  const debut = contrat.value?.dateFinPrevue ? versDate(contrat.value.dateFinPrevue) : new Date()
  if (debut && contrat.value?.dateFinPrevue) debut.setDate(debut.getDate() + 1)
  transformation.value = {
    dateDebut: debut,
    salaireBase: contrat.value?.salaireBase ? Number(contrat.value.salaireBase) : null,
    primesAvantages: contrat.value?.primesAvantages || '',
    horaires: contrat.value?.horaires || '',
    lieuTravail: contrat.value?.lieuTravail || '',
  }
  dialogue.value = 'transformation'
}

const confirmerTransformation = () => executer(
  () => api.post(`/contrats/${id.value}/transformer-cdi`, {
    dateDebut: isoDate(transformation.value.dateDebut),
    salaireBase: transformation.value.salaireBase,
    primesAvantages: transformation.value.primesAvantages || null,
    horaires: transformation.value.horaires || null,
    lieuTravail: transformation.value.lieuTravail || null,
  }),
  'CDI créé dans la continuité du CDD.',
)

function ouvrirAvenant() {
  avenant.value = {
    type: 'SALAIRE',
    dateEffet: new Date(),
    nouveauSalaire: contrat.value?.salaireBase ? Number(contrat.value.salaireBase) : null,
    nouvelleDateFin: contrat.value?.dateFinPrevue ? versDate(contrat.value.dateFinPrevue) : null,
    nouveauxHoraires: contrat.value?.horaires || '',
    nouveauLieu: contrat.value?.lieuTravail || '',
    nouvellesValeurs: '',
  }
  dialogue.value = 'avenant'
}

const confirmerAvenant = () => executer(
  () => api.post(`/contrats/${id.value}/avenants`, {
    type: avenant.value.type,
    dateEffet: isoDate(avenant.value.dateEffet),
    nouveauSalaire: avenant.value.type === 'SALAIRE' ? avenant.value.nouveauSalaire : null,
    nouvelleDateFin: avenant.value.type === 'DUREE' ? isoDate(avenant.value.nouvelleDateFin) : null,
    nouveauxHoraires: avenant.value.type === 'HORAIRES' ? avenant.value.nouveauxHoraires : null,
    nouveauLieu: avenant.value.type === 'LIEU' ? avenant.value.nouveauLieu : null,
    nouvellesValeurs: avenant.value.nouvellesValeurs || null,
  }),
  'Avenant créé. Il doit être signé puis validé.',
)

const signerAvenant = (item) => executer(
  () => api.post(`/contrats/avenants/${item.id}/signer`),
  'Avenant signé ; les changements s’appliqueront à sa date d’effet.',
)

const validerAvenant = (item) => executer(
  () => api.post(`/contrats/avenants/${item.id}/valider`),
  'Avenant validé.',
)

async function changerStatutTache(tache, statut) {
  if (actionEnCours.value) return
  actionEnCours.value = true
  try {
    await api.patch(`/contrats/taches/${tache.id}`, {
      statut,
      dateFait: statut === 'FAIT' ? isoDate(new Date()) : null,
    })
    notifications.succes('Tâche mise à jour.')
    const [parcours, contratActualise] = await Promise.all([
      api.get(`/contrats/${id.value}/onboarding`),
      api.get(`/contrats/${id.value}`),
    ])
    onboarding.value = parcours
    contrat.value = contratActualise
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Mise à jour impossible.')
  } finally {
    actionEnCours.value = false
  }
}

const telecharger = () => executer(
  () => api.telecharger(`/contrats/${id.value}/document`, `${contrat.value?.numero || 'contrat'}.pdf`),
  'Téléchargement du contrat lancé.',
)

onMounted(charger)
</script>

<template>
  <div v-if="chargement" class="py-10 text-center text-muted-color">Chargement du contrat...</div>

  <div v-else-if="contrat" class="mx-auto flex max-w-7xl flex-col gap-5">
    <header class="flex flex-col justify-between gap-4 md:flex-row md:items-start">
      <div>
        <Button label="Retour aux contrats" icon="pi pi-arrow-left" severity="secondary" variant="text"
          class="mb-2 p-0" @click="router.push({ name: 'contrat-liste' })" />
        <div class="flex flex-wrap items-center gap-3">
          <h1 class="m-0 text-2xl font-bold">{{ contrat.numero }}</h1>
          <StatusBadge :valeur="contrat.etat" />
          <StatusBadge :valeur="contrat.statutExecution" />
          <StatusBadge :valeur="contrat.typeContrat" />
        </div>
        <p class="mt-1 mb-0 text-muted-color">
          {{ salarie?.prenom }} {{ salarie?.nom }} · Matricule {{ salarie?.matricule || 'non attribué' }}
        </p>
      </div>
      <div class="flex flex-wrap gap-2">
        <Button v-if="peutSigner" label="Signer (employeur)" icon="pi pi-pen-to-square" :loading="actionEnCours" @click="signer" />
        <Button v-if="peutDeciderEssai" label="Décision fin d’essai" icon="pi pi-flag" severity="secondary" outlined @click="ouvrirDecisionEssai" />
        <Button v-if="peutValider" label="Valider" icon="pi pi-check" severity="success" :loading="actionEnCours" @click="valider" />
        <Button v-if="peutAnnuler" label="Annuler" icon="pi pi-times" severity="danger" outlined @click="dialogue = 'annulation'" />
        <Button label="PDF" icon="pi pi-download" severity="secondary" outlined :disabled="!contrat.documentPdfId" @click="telecharger" />
      </div>
    </header>

    <div v-if="contrat.etat === 'SIGNE' && tachesBloquantes.length"
      class="rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
      Validation RH bloquée : {{ tachesBloquantes.length }} tâche(s) bloquante(s) restante(s).
    </div>

    <Card>
      <template #title>Conditions du contrat</template>
      <template #content>
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div><div class="text-xs font-semibold uppercase text-muted-color">Type</div><div>{{ libelle(contrat.typeContrat) }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Rang</div><div>{{ contrat.rang }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Contrat précédent</div><div>{{ contrat.idContratPrecedent || '—' }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Début</div><div>{{ formatDate(contrat.dateDebut) }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Fin prévue</div><div>{{ formatDate(contrat.dateFinPrevue) }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Fin réelle</div><div>{{ formatDate(contrat.dateFinReelle) }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Fin d’essai</div><div>{{ formatDate(contrat.finPeriodeEssai) }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Salaire de base</div><div>{{ formatMontant(contrat.salaireBase) }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Devise</div><div>{{ contrat.devise || '—' }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Horaires</div><div>{{ contrat.horaires || '—' }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Lieu</div><div>{{ contrat.lieuTravail || '—' }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Motif CDD</div><div>{{ contrat.motifCdd || '—' }}</div></div>
          <div class="sm:col-span-3"><div class="text-xs font-semibold uppercase text-muted-color">Primes et avantages</div><div class="whitespace-pre-line">{{ contrat.primesAvantages || '—' }}</div></div>
        </div>
        <div v-if="contrat.etat === 'ANNULE'" class="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-900">
          Annulé le {{ formatDateHeure(contrat.dateAnnulation) }} — motif : {{ contrat.motifAnnulation }}
        </div>
      </template>
    </Card>

    <Card>
      <template #title>Signature et pièces</template>
      <template #content>
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div><div class="text-xs font-semibold uppercase text-muted-color">Signature salarié</div><div>{{ formatDateHeure(contrat.dateSignatureSalarie) }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Signature employeur</div><div>{{ formatDateHeure(contrat.dateSignatureEmployeur) }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Empreinte salarié</div><div class="break-all text-xs">{{ contrat.empreinteSignatureSalarie || '—' }}</div><div class="text-xs text-muted-color">IP {{ contrat.ipSignatureSalarie || '—' }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Empreinte employeur</div><div class="break-all text-xs">{{ contrat.empreinteSignatureEmployeur || '—' }}</div><div class="text-xs text-muted-color">IP {{ contrat.ipSignatureEmployeur || '—' }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">PDF figé</div><div>{{ contrat.documentPdfId ? `Document #${contrat.documentPdfId}` : 'Non généré' }}</div></div>
          <div><div class="text-xs font-semibold uppercase text-muted-color">Dernière modification</div><div>{{ formatDateHeure(contrat.dateModification) }}</div></div>
        </div>
        <div v-if="contrat.decisionFinEssai" class="mt-4 rounded-lg border border-surface-300 p-3 text-sm dark:border-surface-700">
          <strong>Décision fin d’essai :</strong> {{ libelle(contrat.decisionFinEssai) }}
          le {{ formatDate(contrat.dateDecisionFinEssai) }}
        </div>
      </template>
    </Card>

    <Card>
      <template #title>Affectations</template>
      <template #content>
        <DataTable :value="affectations" data-key="id" striped-rows size="small" responsive-layout="scroll">
          <Column header="Poste"><template #body="{ data }">#{{ data.idPoste || '—' }}</template></Column>
          <Column header="Début"><template #body="{ data }">{{ formatDate(data.dateDebut) }}</template></Column>
          <Column header="Fin"><template #body="{ data }">{{ formatDate(data.dateFin) }}</template></Column>
          <Column header="Motif"><template #body="{ data }">{{ libelle(data.motif) }}</template></Column>
          <Column header="Principale"><template #body="{ data }">{{ data.principale ? 'Oui' : 'Non' }}</template></Column>
          <Column header="Statut"><template #body="{ data }"><StatusBadge :valeur="data.statut" /></template></Column>
        </DataTable>
      </template>
    </Card>

    <Card v-if="onboarding">
      <template #title>
        <div class="flex flex-wrap items-center justify-between gap-3">
          <span>Onboarding ({{ onboarding.progression || 0 }} %)</span>
          <StatusBadge :valeur="onboarding.statut" />
        </div>
      </template>
      <template #content>
        <ProgressBar :value="onboarding.progression || 0" class="mb-4 h-2" :show-value="false" />
        <DataTable :value="onboarding.taches || []" data-key="id" striped-rows size="small" responsive-layout="scroll">
          <Column field="libelle" header="Tâche">
            <template #body="{ data }">
              <div class="font-medium">{{ data.libelle }}</div>
              <div class="text-xs text-muted-color">
                {{ libelle(data.categorie) }} · {{ data.responsable || '—' }}
                <span v-if="data.bloquante" class="font-semibold text-red-600"> · bloquante</span>
              </div>
            </template>
          </Column>
          <Column header="Échéance"><template #body="{ data }">{{ formatDate(data.echeance) }}</template></Column>
          <Column header="Fait le"><template #body="{ data }">{{ formatDate(data.dateFait) }}</template></Column>
          <Column header="Statut">
            <template #body="{ data }">
              <Select v-if="peutAgir" :model-value="data.statut" :options="STATUTS_TACHE"
                option-label="libelle" option-value="valeur" size="small" class="min-w-36"
                @change="(event) => changerStatutTache(data, event.value)" />
              <StatusBadge v-else :valeur="data.statut" />
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <Card v-else>
      <template #title>Onboarding</template>
      <template #content>
        <p class="m-0 text-muted-color">
          Le parcours d’intégration sera créé automatiquement à la signature du contrat.
        </p>
      </template>
    </Card>

    <Card>
      <template #title>
        <div class="flex flex-wrap items-center justify-between gap-3">
          <span>Avenants</span>
          <Button v-if="peutAvenant" label="Nouvel avenant" icon="pi pi-plus" size="small" outlined @click="ouvrirAvenant" />
        </div>
      </template>
      <template #content>
        <DataTable v-if="avenants.length" :value="avenants" data-key="id" striped-rows size="small" responsive-layout="scroll">
          <Column field="numero" header="Numéro" />
          <Column header="Type"><template #body="{ data }"><StatusBadge :valeur="data.type" /></template></Column>
          <Column header="Effet"><template #body="{ data }">{{ formatDate(data.dateEffet) }}</template></Column>
          <Column header="Avant"><template #body="{ data }">{{ data.anciennesValeurs || '—' }}</template></Column>
          <Column header="Après"><template #body="{ data }">{{ data.nouvellesValeurs || '—' }}</template></Column>
          <Column header="État"><template #body="{ data }"><StatusBadge :valeur="data.etat" /></template></Column>
          <Column header="Action">
            <template #body="{ data }">
              <div class="flex gap-2">
                <Button v-if="data.etat === 'CREE' && peutAgir" label="Signer" size="small" outlined
                  :loading="actionEnCours" @click="signerAvenant(data)" />
                <Button v-if="data.etat === 'SIGNE' && peutAgir" label="Valider" size="small" severity="success" outlined
                  :loading="actionEnCours" @click="validerAvenant(data)" />
              </div>
            </template>
          </Column>
        </DataTable>
        <p v-else class="m-0 text-muted-color">Aucun avenant.</p>
      </template>
    </Card>

    <Card>
      <template #title>Notifications et relances</template>
      <template #content>
        <DataTable v-if="notificationsContrat.length" :value="notificationsContrat" data-key="id"
          striped-rows size="small" responsive-layout="scroll">
          <Column header="Niveau"><template #body="{ data }"><StatusBadge :valeur="data.niveau" /></template></Column>
          <Column field="message" header="Message" />
          <Column header="Échéance"><template #body="{ data }">{{ formatDate(data.echeance) }}</template></Column>
          <Column header="Statut"><template #body="{ data }"><StatusBadge :valeur="data.statut" /></template></Column>
          <Column header="Action">
            <template #body="{ data }">
              <Button v-if="data.statut !== 'LUE'" label="Marquer lue" size="small" outlined
                @click="marquerNotificationLue(data)" />
            </template>
          </Column>
        </DataTable>
        <p v-else class="m-0 text-muted-color">Aucune notification pour ce contrat.</p>
      </template>
    </Card>

    <Card v-if="peutRenouveler || peutTransformer">
      <template #title>Décision de fin de CDD</template>
      <template #content>
        <p class="mt-0 text-muted-color">
          Le CDD peut être renouvelé une fois dans la limite de 24 mois cumulés, ou transformé en CDI.
        </p>
        <div class="flex flex-wrap gap-2">
          <Button v-if="peutRenouveler" label="Renouveler le CDD" icon="pi pi-refresh" outlined @click="ouvrirRenouvellement" />
          <Button v-if="peutTransformer" label="Transformer en CDI" icon="pi pi-arrow-up-right" severity="success" @click="ouvrirTransformation" />
        </div>
      </template>
    </Card>

    <ModalDialog v-if="dialogue === 'essai'" titre="Décision de fin de période d’essai"
      sous-titre="La décision est tracée sur le contrat. Une rupture met fin au contrat et désactive le salarié."
      @fermer="dialogue = null">
      <div class="flex flex-col gap-4">
        <div class="flex flex-col gap-2">
          <label class="text-sm font-semibold">Décision</label>
          <Select v-model="decisionEssai.decision" :options="DECISIONS_ESSAI"
            option-label="libelle" option-value="valeur" />
        </div>
        <div class="flex flex-col gap-2">
          <label class="text-sm font-semibold">Commentaire</label>
          <Textarea v-model="decisionEssai.commentaire" rows="3" />
        </div>
      </div>
      <template #actions>
        <Button label="Fermer" severity="secondary" outlined @click="dialogue = null" />
        <Button label="Enregistrer la décision" :loading="actionEnCours" @click="confirmerDecisionEssai" />
      </template>
    </ModalDialog>

    <ModalDialog v-if="dialogue === 'annulation'" titre="Annuler le contrat"
      sous-titre="Un contrat signé ne peut être annulé qu’avant sa date de début (rétractation)." @fermer="dialogue = null">
      <label class="mb-2 block text-sm font-semibold" for="motif-annulation">Motif</label>
      <Textarea id="motif-annulation" v-model="motifAnnulation" rows="3" class="w-full" />
      <template #actions>
        <Button label="Fermer" severity="secondary" outlined @click="dialogue = null" />
        <Button label="Annuler le contrat" severity="danger" :loading="actionEnCours"
          :disabled="!motifAnnulation.trim()" @click="confirmerAnnulation" />
      </template>
    </ModalDialog>

    <ModalDialog v-if="dialogue === 'renouvellement'" titre="Renouveler le CDD"
      sous-titre="La date de début est fixée automatiquement au lendemain de la fin du contrat précédent." large
      @fermer="dialogue = null">
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div class="flex flex-col gap-2"><label class="text-sm font-semibold">Nouvelle date de fin</label><DatePicker v-model="renouvellement.dateFinPrevue" date-format="dd/mm/yy" show-icon /></div>
        <div class="flex flex-col gap-2"><label class="text-sm font-semibold">Salaire de base</label><InputNumber v-model="renouvellement.salaireBase" mode="currency" currency="MGA" locale="fr-FR" /></div>
        <div class="sm:col-span-2 flex flex-col gap-2"><label class="text-sm font-semibold">Motif du CDD</label><Textarea v-model="renouvellement.motifCdd" rows="2" /></div>
        <div class="flex flex-col gap-2"><label class="text-sm font-semibold">Horaires</label><InputText v-model="renouvellement.horaires" /></div>
        <div class="flex flex-col gap-2"><label class="text-sm font-semibold">Lieu</label><InputText v-model="renouvellement.lieuTravail" /></div>
      </div>
      <template #actions>
        <Button label="Fermer" severity="secondary" outlined @click="dialogue = null" />
        <Button label="Créer le renouvellement" :loading="actionEnCours"
          :disabled="!renouvellement.dateFinPrevue || !renouvellement.motifCdd" @click="confirmerRenouvellement" />
      </template>
    </ModalDialog>

    <ModalDialog v-if="dialogue === 'transformation'" titre="Transformer en CDI"
      sous-titre="Le CDI prend la suite du CDD sans rupture de continuité." large @fermer="dialogue = null">
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div class="flex flex-col gap-2"><label class="text-sm font-semibold">Date de début</label><DatePicker v-model="transformation.dateDebut" date-format="dd/mm/yy" show-icon /></div>
        <div class="flex flex-col gap-2"><label class="text-sm font-semibold">Salaire de base</label><InputNumber v-model="transformation.salaireBase" mode="currency" currency="MGA" locale="fr-FR" /></div>
        <div class="flex flex-col gap-2"><label class="text-sm font-semibold">Horaires</label><InputText v-model="transformation.horaires" /></div>
        <div class="flex flex-col gap-2"><label class="text-sm font-semibold">Lieu</label><InputText v-model="transformation.lieuTravail" /></div>
      </div>
      <template #actions>
        <Button label="Fermer" severity="secondary" outlined @click="dialogue = null" />
        <Button label="Créer le CDI" severity="success" :loading="actionEnCours" @click="confirmerTransformation" />
      </template>
    </ModalDialog>

    <ModalDialog v-if="dialogue === 'avenant'" titre="Nouvel avenant"
      sous-titre="Les valeurs avant/après sont conservées ; l’avenant s’applique à sa date d’effet." large
      @fermer="dialogue = null">
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div class="flex flex-col gap-2">
          <label class="text-sm font-semibold">Type</label>
          <Select v-model="avenant.type" :options="TYPES_AVENANT" option-label="libelle" option-value="valeur" />
        </div>
        <div class="flex flex-col gap-2"><label class="text-sm font-semibold">Date d’effet</label><DatePicker v-model="avenant.dateEffet" date-format="dd/mm/yy" show-icon /></div>
        <div v-if="avenant.type === 'SALAIRE'" class="flex flex-col gap-2"><label class="text-sm font-semibold">Nouveau salaire</label><InputNumber v-model="avenant.nouveauSalaire" mode="currency" currency="MGA" locale="fr-FR" /></div>
        <div v-if="avenant.type === 'DUREE'" class="flex flex-col gap-2"><label class="text-sm font-semibold">Nouvelle date de fin</label><DatePicker v-model="avenant.nouvelleDateFin" date-format="dd/mm/yy" show-icon /></div>
        <div v-if="avenant.type === 'HORAIRES'" class="flex flex-col gap-2"><label class="text-sm font-semibold">Nouveaux horaires</label><InputText v-model="avenant.nouveauxHoraires" /></div>
        <div v-if="avenant.type === 'LIEU'" class="flex flex-col gap-2"><label class="text-sm font-semibold">Nouveau lieu</label><InputText v-model="avenant.nouveauLieu" /></div>
        <div v-if="['POSTE', 'AUTRE'].includes(avenant.type)" class="sm:col-span-2 flex flex-col gap-2">
          <label class="text-sm font-semibold">Nouvelles valeurs</label>
          <Textarea v-model="avenant.nouvellesValeurs" rows="3" />
        </div>
      </div>
      <template #actions>
        <Button label="Fermer" severity="secondary" outlined @click="dialogue = null" />
        <Button label="Créer l’avenant" :loading="actionEnCours" @click="confirmerAvenant" />
      </template>
    </ModalDialog>
  </div>
</template>
