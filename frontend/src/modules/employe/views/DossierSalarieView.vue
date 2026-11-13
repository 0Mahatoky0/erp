<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Checkbox from 'primevue/checkbox'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import ProgressBar from 'primevue/progressbar'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDate, formatMontant, libelle } from '@/shared/utils/format'
import { employeApi as api, ErreurApi } from '../api/employeApi'

const route = useRoute()
const router = useRouter()
const notifications = useNotificationStore()

const id = computed(() => route.params.id)
const dossier = ref(null)
const contrats = ref([])
const affectations = ref([])
const chargement = ref(true)

const TYPES_DOCUMENT = [
  'PIECE_IDENTITE', 'DIPLOME', 'RIB', 'CONTRAT', 'FICHE_POSTE',
  'CERTIFICAT_MEDICAL', 'ATTESTATION', 'AUTRE',
]
const STATUTS_DOCUMENT = ['EN_ATTENTE', 'VALIDE', 'REFUSE', 'EXPIRE', 'ARCHIVE']
const PRIORITES = ['BASSE', 'NORMALE', 'HAUTE', 'URGENTE']
const CHAMPS_MODIFIABLES = [
  { valeur: 'telephone', libelle: 'Telephone' },
  { valeur: 'email', libelle: 'Email' },
  { valeur: 'adresse', libelle: 'Adresse' },
  { valeur: 'nationalite', libelle: 'Nationalite' },
  { valeur: 'situationFamiliale', libelle: 'Situation familiale' },
  { valeur: 'lieuNaissance', libelle: 'Lieu de naissance' },
]

const dialogDocument = ref(false)
const documentForm = ref({ typeDocument: 'PIECE_IDENTITE', titre: '', nomFichier: '', dateEmission: null, dateValidite: null, contenuBase64: '' })
const fichierCharge = ref(false)

const dialogMission = ref(false)
const missionForm = ref({ titre: '', description: '', lieu: '', dateDebut: null, dateFinPrevue: null, priorite: 'NORMALE' })

const dialogSuivi = ref(false)
const missionCourante = ref(null)
const suiviForm = ref({ avancement: 0, manaoAsa: false, miassaTsara: false, commentaire: '' })

const dialogDemande = ref(false)
const demandeForm = ref({ champ: 'telephone', nouvelleValeur: '', motif: '' })

async function charger() {
  chargement.value = true
  try {
    const [dossierRh, listeContrats, listeAffectations] = await Promise.all([
      api.get(`/rh/dossiers/${id.value}`),
      api.get(`/contrats/employe/${id.value}`),
      api.get(`/contrats/employe/${id.value}/affectations`),
    ])
    dossier.value = dossierRh
    contrats.value = listeContrats
    affectations.value = listeAffectations
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger le dossier.')
  } finally {
    chargement.value = false
  }
}

async function rechargerDossier() {
  dossier.value = await api.get(`/rh/dossiers/${id.value}`)
}

function telechargerDocument(document) {
  api.telecharger(`/employes/documents/${document.id}`, document.nomFichier || 'document')
    .catch((cause) => notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Telechargement impossible.'))
}

function telechargerContrat(contrat) {
  if (!contrat.documentPdfId) {
    return
  }
  api.telecharger(`/contrats/${contrat.id}/document`, `${contrat.numero || 'contrat'}.pdf`)
    .catch((cause) => notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Telechargement impossible.'))
}

function ouvrirDocument() {
  documentForm.value = { typeDocument: 'PIECE_IDENTITE', titre: '', nomFichier: '', dateEmission: null, dateValidite: null, contenuBase64: '' }
  fichierCharge.value = false
  dialogDocument.value = true
}

function lireFichier(evenement) {
  const fichier = evenement.target.files && evenement.target.files[0]
  if (!fichier) {
    return
  }
  const lecteur = new FileReader()
  lecteur.onload = () => {
    documentForm.value.nomFichier = fichier.name
    documentForm.value.contenuBase64 = String(lecteur.result).split(',')[1] || ''
    fichierCharge.value = true
  }
  lecteur.readAsDataURL(fichier)
}

async function enregistrerDocument() {
  if (!documentForm.value.titre || !fichierCharge.value) {
    notifications.erreur('Renseignez un titre et selectionnez un fichier.')
    return
  }
  try {
    await api.post(`/rh/dossiers/${id.value}/documents`, documentForm.value)
    notifications.succes('Document ajoute au dossier.')
    dialogDocument.value = false
    await rechargerDossier()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Ajout impossible.')
  }
}

async function changerStatutDocument(document, statut) {
  try {
    await api.patch(`/rh/dossiers/${id.value}/documents/${document.id}/statut`, { statut })
    notifications.succes('Statut du document mis a jour.')
    await rechargerDossier()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Mise a jour impossible.')
  }
}

function ouvrirMission() {
  missionForm.value = { titre: '', description: '', lieu: '', dateDebut: null, dateFinPrevue: null, priorite: 'NORMALE' }
  dialogMission.value = true
}

async function creerMission() {
  if (!missionForm.value.titre || !missionForm.value.dateDebut) {
    notifications.erreur('Le titre et la date de debut sont obligatoires.')
    return
  }
  try {
    await api.post(`/rh/dossiers/${id.value}/missions`, missionForm.value)
    notifications.succes('Mission creee.')
    dialogMission.value = false
    await rechargerDossier()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Creation impossible.')
  }
}

function ouvrirSuivi(mission) {
  missionCourante.value = mission
  suiviForm.value = {
    avancement: mission.avancement ?? 0,
    manaoAsa: mission.manaoAsa ?? false,
    miassaTsara: mission.miassaTsara ?? false,
    commentaire: '',
  }
  dialogSuivi.value = true
}

async function enregistrerSuivi() {
  try {
    await api.post(`/rh/dossiers/missions/${missionCourante.value.id}/suivis`, suiviForm.value)
    notifications.succes('Suivi enregistre.')
    dialogSuivi.value = false
    await rechargerDossier()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Suivi impossible.')
  }
}

async function terminerMission(mission) {
  try {
    await api.patch(`/rh/dossiers/missions/${mission.id}`, {
      titre: mission.titre,
      description: mission.description,
      lieu: mission.lieu,
      dateDebut: mission.dateDebut,
      dateFinPrevue: mission.dateFinPrevue,
      dateFinReelle: new Date().toISOString().slice(0, 10),
      priorite: mission.priorite,
      statut: 'TERMINEE',
      idResponsable: mission.idResponsable,
      avancement: 100,
      manaoAsa: mission.manaoAsa,
      miassaTsara: mission.miassaTsara,
      commentaireSuivi: mission.commentaireSuivi,
    })
    notifications.succes('Mission terminee.')
    await rechargerDossier()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Mise a jour impossible.')
  }
}

function ouvrirDemande() {
  demandeForm.value = { champ: 'telephone', nouvelleValeur: '', motif: '' }
  dialogDemande.value = true
}

async function creerDemande() {
  if (!demandeForm.value.nouvelleValeur) {
    notifications.erreur('Renseignez la nouvelle valeur.')
    return
  }
  try {
    await api.post(`/rh/dossiers/${id.value}/demandes-modification`, demandeForm.value)
    notifications.succes('Demande enregistree.')
    dialogDemande.value = false
    await rechargerDossier()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Creation impossible.')
  }
}

async function deciderDemande(demande, approuver) {
  try {
    await api.post(`/rh/dossiers/demandes-modification/${demande.id}/decision`, {
      approuver,
      motifDecision: approuver ? 'Validee par le RH' : 'Refusee par le RH',
    })
    notifications.succes('Demande traitee.')
    await rechargerDossier()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Traitement impossible.')
  }
}

async function marquerLue(alerte) {
  try {
    await api.patch(`/rh/dossiers/alertes/${alerte.id}/lue`)
    await rechargerDossier()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Mise a jour impossible.')
  }
}

onMounted(charger)
</script>

<template>
  <div class="mx-auto flex max-w-7xl flex-col gap-5">
    <Button label="Retour" icon="pi pi-arrow-left" severity="secondary" variant="text" class="w-fit p-0"
      @click="router.back()" />

    <div v-if="chargement" class="py-10 text-center text-muted-color">Chargement du dossier...</div>

    <template v-else-if="dossier">
      <header class="flex flex-col justify-between gap-3 md:flex-row md:items-start">
        <div>
          <h1 class="m-0 text-2xl font-bold">{{ dossier.employe.prenom }} {{ dossier.employe.nom }}</h1>
          <p class="mt-1 mb-0 text-muted-color">
            Matricule {{ dossier.employe.matricule || 'non attribue' }} ·
            {{ dossier.employe.poste || 'Poste non defini' }}
          </p>
        </div>
        <div class="flex items-center gap-2">
          <StatusBadge :valeur="dossier.employe.statut" />
          <Button label="Demande de correction" icon="pi pi-pencil" outlined @click="ouvrirDemande" />
        </div>
      </header>

      <Card>
        <template #title>Completude du dossier</template>
        <template #content>
          <div class="mb-3 flex items-center gap-3">
            <ProgressBar :value="dossier.completude" class="flex-1" />
            <span class="font-semibold">{{ dossier.completude }} %</span>
          </div>
          <p v-if="dossier.piecesManquantes?.length" class="m-0 text-sm text-muted-color">
            Pieces manquantes : {{ dossier.piecesManquantes.join(', ') }}
          </p>
          <p v-else class="m-0 text-sm text-green-600">Toutes les pieces obligatoires sont presentes.</p>
        </template>
      </Card>

      <div class="grid grid-cols-1 gap-5 xl:grid-cols-2">
        <Card>
          <template #title>Identite et coordonnees</template>
          <template #content>
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div><div class="text-xs font-semibold uppercase text-muted-color">Email</div><div>{{ dossier.employe.email || '—' }}</div></div>
              <div><div class="text-xs font-semibold uppercase text-muted-color">Telephone</div><div>{{ dossier.employe.telephone || '—' }}</div></div>
              <div><div class="text-xs font-semibold uppercase text-muted-color">CIN</div><div>{{ dossier.employe.cin || '—' }}</div></div>
              <div><div class="text-xs font-semibold uppercase text-muted-color">Naissance</div><div>{{ formatDate(dossier.employe.dateNaissance) }}</div></div>
              <div><div class="text-xs font-semibold uppercase text-muted-color">Service</div><div>{{ dossier.employe.service || '—' }}</div></div>
              <div><div class="text-xs font-semibold uppercase text-muted-color">Departement</div><div>{{ dossier.employe.departement || '—' }}</div></div>
              <div><div class="text-xs font-semibold uppercase text-muted-color">Date d'embauche</div><div>{{ formatDate(dossier.employe.dateEmbauche) }}</div></div>
              <div><div class="text-xs font-semibold uppercase text-muted-color">Premiere embauche</div><div>{{ formatDate(dossier.employe.datePremiereEmbauche) }}</div></div>
            </div>
          </template>
        </Card>

        <Card>
          <template #title>Alertes</template>
          <template #content>
            <p v-if="!dossier.alertes?.length" class="m-0 text-sm text-muted-color">Aucune alerte.</p>
            <div v-else class="flex flex-col gap-2">
              <div v-for="alerte in dossier.alertes" :key="alerte.id"
                class="rounded-lg border border-surface p-3">
                <div class="flex items-center justify-between gap-2">
                  <div class="flex items-center gap-2">
                    <StatusBadge :valeur="alerte.niveau" />
                    <span class="text-sm font-semibold">{{ libelle(alerte.typeAlerte) }}</span>
                  </div>
                  <Button v-if="!alerte.lue" label="Marquer lue" size="small" text @click="marquerLue(alerte)" />
                </div>
                <p class="mt-2 mb-0 text-sm">{{ alerte.message }}</p>
                <p class="mt-1 mb-0 text-xs text-muted-color">Echeance : {{ formatDate(alerte.dateEcheance) }}</p>
              </div>
            </div>
          </template>
        </Card>
      </div>

      <Card>
        <template #title>
          <div class="flex items-center justify-between gap-3">
            <span>Documents administratifs</span>
            <Button label="Ajouter un document" icon="pi pi-upload" size="small" @click="ouvrirDocument" />
          </div>
        </template>
        <template #content>
          <DataTable :value="dossier.documents" data-key="id" striped-rows size="small" responsive-layout="scroll">
            <Column header="Type"><template #body="{ data }">{{ libelle(data.typeDocument) }}</template></Column>
            <Column field="titre" header="Titre" />
            <Column field="nomFichier" header="Fichier" />
            <Column header="Emission"><template #body="{ data }">{{ formatDate(data.dateEmission) }}</template></Column>
            <Column header="Validite"><template #body="{ data }">{{ formatDate(data.dateValidite) }}</template></Column>
            <Column header="Statut">
              <template #body="{ data }">
                <Select :model-value="data.statut" :options="STATUTS_DOCUMENT" size="small"
                  @update:model-value="(valeur) => changerStatutDocument(data, valeur)" />
              </template>
            </Column>
            <Column header="Action">
              <template #body="{ data }">
                <Button label="Telecharger" icon="pi pi-download" size="small" outlined
                  @click="telechargerDocument(data)" />
              </template>
            </Column>
          </DataTable>
        </template>
      </Card>

      <Card>
        <template #title>
          <div class="flex items-center justify-between gap-3">
            <span>Missions (Manao asa ve / Miassa tsara ve)</span>
            <Button label="Nouvelle mission" icon="pi pi-plus" size="small" @click="ouvrirMission" />
          </div>
        </template>
        <template #content>
          <DataTable :value="dossier.missions" data-key="id" striped-rows size="small" responsive-layout="scroll">
            <Column field="reference" header="Reference" />
            <Column field="titre" header="Titre" />
            <Column header="Debut"><template #body="{ data }">{{ formatDate(data.dateDebut) }}</template></Column>
            <Column header="Fin prevue"><template #body="{ data }">{{ formatDate(data.dateFinPrevue) }}</template></Column>
            <Column field="priorite" header="Priorite"><template #body="{ data }">{{ libelle(data.priorite) }}</template></Column>
            <Column field="avancement" header="Avancement"><template #body="{ data }">{{ data.avancement }} %</template></Column>
            <Column header="Manao asa ve"><template #body="{ data }">{{ data.manaoAsa == null ? '—' : (data.manaoAsa ? 'Oui' : 'Non') }}</template></Column>
            <Column header="Miassa tsara ve"><template #body="{ data }">{{ data.miassaTsara == null ? '—' : (data.miassaTsara ? 'Oui' : 'Non') }}</template></Column>
            <Column header="Statut"><template #body="{ data }"><StatusBadge :valeur="data.statut" /></template></Column>
            <Column header="Actions">
              <template #body="{ data }">
                <div class="flex gap-1">
                  <Button label="Suivi" icon="pi pi-chart-line" size="small" text @click="ouvrirSuivi(data)" />
                  <Button v-if="data.statut !== 'TERMINEE' && data.statut !== 'ANNULEE'" label="Terminer"
                    icon="pi pi-check" size="small" text @click="terminerMission(data)" />
                </div>
              </template>
            </Column>
          </DataTable>
        </template>
      </Card>

      <Card>
        <template #title>Demandes de correction</template>
        <template #content>
          <p v-if="!dossier.demandesModification?.length" class="m-0 text-sm text-muted-color">
            Aucune demande de correction.
          </p>
          <DataTable v-else :value="dossier.demandesModification" data-key="id" striped-rows size="small"
            responsive-layout="scroll">
            <Column field="champ" header="Champ"><template #body="{ data }">{{ libelle(data.champ) }}</template></Column>
            <Column field="ancienneValeur" header="Avant" />
            <Column field="nouvelleValeur" header="Apres" />
            <Column field="motif" header="Motif" />
            <Column header="Statut"><template #body="{ data }"><StatusBadge :valeur="data.statut" /></template></Column>
            <Column header="Actions">
              <template #body="{ data }">
                <div v-if="data.statut === 'EN_ATTENTE'" class="flex gap-1">
                  <Button label="Approuver" size="small" text @click="deciderDemande(data, true)" />
                  <Button label="Refuser" size="small" text severity="danger" @click="deciderDemande(data, false)" />
                </div>
                <span v-else class="text-sm text-muted-color">{{ data.motifDecision || '—' }}</span>
              </template>
            </Column>
          </DataTable>
        </template>
      </Card>

      <Card>
        <template #title>Contrats et remuneration</template>
        <template #content>
          <DataTable :value="contrats" data-key="id" striped-rows size="small" responsive-layout="scroll">
            <Column field="numero" header="Numero" />
            <Column field="typeContrat" header="Type" />
            <Column header="Debut"><template #body="{ data }">{{ formatDate(data.dateDebut) }}</template></Column>
            <Column header="Fin prevue"><template #body="{ data }">{{ formatDate(data.dateFinPrevue) }}</template></Column>
            <Column header="Salaire de base">
              <template #body="{ data }">{{ formatMontant(data.salaireBase) }}</template>
            </Column>
            <Column header="Etat"><template #body="{ data }"><StatusBadge :valeur="data.etat" /></template></Column>
            <Column header="Document">
              <template #body="{ data }">
                <Button v-if="data.documentPdfId" label="PDF" icon="pi pi-download" size="small" outlined
                  @click="telechargerContrat(data)" />
                <span v-else class="text-muted-color">—</span>
              </template>
            </Column>
          </DataTable>
        </template>
      </Card>

      <Card>
        <template #title>Carriere et affectations</template>
        <template #content>
          <DataTable :value="affectations" data-key="id" striped-rows size="small" responsive-layout="scroll">
            <Column header="Poste"><template #body="{ data }">#{{ data.idPoste || '—' }}</template></Column>
            <Column header="Service"><template #body="{ data }">#{{ data.idService || '—' }}</template></Column>
            <Column header="Debut"><template #body="{ data }">{{ formatDate(data.dateDebut) }}</template></Column>
            <Column header="Fin"><template #body="{ data }">{{ formatDate(data.dateFin) }}</template></Column>
            <Column field="motif" header="Motif"><template #body="{ data }">{{ libelle(data.motif) }}</template></Column>
            <Column field="principale" header="Principale"><template #body="{ data }">{{ data.principale ? 'Oui' : 'Non' }}</template></Column>
            <Column header="Statut"><template #body="{ data }"><StatusBadge :valeur="data.statut" /></template></Column>
          </DataTable>
        </template>
      </Card>
    </template>

    <Dialog v-model:visible="dialogDocument" header="Ajouter un document" modal class="w-full max-w-xl">
      <div class="flex flex-col gap-3">
        <Select v-model="documentForm.typeDocument" :options="TYPES_DOCUMENT" placeholder="Type de document" />
        <InputText v-model="documentForm.titre" placeholder="Titre du document" />
        <input type="file" class="text-sm" @change="lireFichier" />
        <label class="flex flex-col gap-1 text-sm">
          Date d'emission
          <input v-model="documentForm.dateEmission" type="date" class="rounded border border-surface p-2" />
        </label>
        <label class="flex flex-col gap-1 text-sm">
          Date de validite
          <input v-model="documentForm.dateValidite" type="date" class="rounded border border-surface p-2" />
        </label>
      </div>
      <template #footer>
        <Button label="Annuler" text @click="dialogDocument = false" />
        <Button label="Enregistrer" icon="pi pi-check" @click="enregistrerDocument" />
      </template>
    </Dialog>

    <Dialog v-model:visible="dialogMission" header="Nouvelle mission" modal class="w-full max-w-xl">
      <div class="flex flex-col gap-3">
        <InputText v-model="missionForm.titre" placeholder="Titre de la mission" />
        <Textarea v-model="missionForm.description" rows="3" placeholder="Description" />
        <InputText v-model="missionForm.lieu" placeholder="Lieu" />
        <label class="flex flex-col gap-1 text-sm">
          Date de debut
          <input v-model="missionForm.dateDebut" type="date" class="rounded border border-surface p-2" />
        </label>
        <label class="flex flex-col gap-1 text-sm">
          Date de fin prevue
          <input v-model="missionForm.dateFinPrevue" type="date" class="rounded border border-surface p-2" />
        </label>
        <Select v-model="missionForm.priorite" :options="PRIORITES" placeholder="Priorite" />
      </div>
      <template #footer>
        <Button label="Annuler" text @click="dialogMission = false" />
        <Button label="Creer" icon="pi pi-check" @click="creerMission" />
      </template>
    </Dialog>

    <Dialog v-model:visible="dialogSuivi" header="Suivi de la mission" modal class="w-full max-w-xl">
      <div class="flex flex-col gap-3">
        <label class="flex flex-col gap-1 text-sm">
          Avancement (%)
          <InputNumber v-model="suiviForm.avancement" :min="0" :max="100" />
        </label>
        <label class="flex items-center gap-2 text-sm">
          <Checkbox v-model="suiviForm.manaoAsa" binary /> Manao asa ve (travail realise)
        </label>
        <label class="flex items-center gap-2 text-sm">
          <Checkbox v-model="suiviForm.miassaTsara" binary /> Miassa tsara ve (bien fait)
        </label>
        <Textarea v-model="suiviForm.commentaire" rows="3" placeholder="Commentaire" />
      </div>
      <template #footer>
        <Button label="Annuler" text @click="dialogSuivi = false" />
        <Button label="Enregistrer" icon="pi pi-check" @click="enregistrerSuivi" />
      </template>
    </Dialog>

    <Dialog v-model:visible="dialogDemande" header="Demande de correction" modal class="w-full max-w-xl">
      <div class="flex flex-col gap-3">
        <Select v-model="demandeForm.champ" :options="CHAMPS_MODIFIABLES" option-label="libelle"
          option-value="valeur" placeholder="Champ a corriger" />
        <InputText v-model="demandeForm.nouvelleValeur" placeholder="Nouvelle valeur" />
        <Textarea v-model="demandeForm.motif" rows="3" placeholder="Motif de la demande" />
      </div>
      <template #footer>
        <Button label="Annuler" text @click="dialogDemande = false" />
        <Button label="Envoyer" icon="pi pi-send" @click="creerDemande" />
      </template>
    </Dialog>
  </div>
</template>
