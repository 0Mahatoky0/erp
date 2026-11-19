<script setup>
import { computed, onMounted, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import PrimeSelect from 'primevue/select'
import PrimeInputNumber from 'primevue/inputnumber'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import { ruptureApi as api } from '@/modules/rupture/api/ruptureApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const notifications = useNotificationStore()
const aujourdHui = new Date().toISOString().slice(0, 10)

const motifsParDefaut = [
  'DEMISSION',
  'LICENCIEMENT_DISCIPLINAIRE',
  'LICENCIEMENT_ECONOMIQUE',
  'LICENCIEMENT_INAPTITUDE',
  'LICENCIEMENT_INSUFFISANCE',
  'FIN_CDD',
  'RUPTURE_PERIODE_ESSAI',
  'RUPTURE_AMIABLE',
  'RETRAITE',
  'DECES',
  'ABANDON_POSTE',
  'FIN_STAGE',
  'AUTRE',
]
const initiateursParDefaut = ['SALARIE', 'EMPLOYEUR', 'COMMUN_ACCORD', 'ECHEANCE', 'AUTRE']
const modesPreavisParDefaut = ['EFFECTUE', 'DISPENSE_EMPLOYEUR', 'DISPENSE_SALARIE', 'SANS_PREAVIS']
const statutsParDefaut = [
  'INITIEE',
  'EN_VALIDATION',
  'NOTIFIEE',
  'EN_PREAVIS',
  'DROITS_ARRETES',
  'SOLDE_EN_PREPARATION',
  'SOLDE_VALIDE',
  'SOLDE_PAYE',
  'DOCUMENTS_REMIS',
  'CLOTUREE',
  'ARCHIVEE',
  'ANNULEE',
]
const modesPaiementParDefaut = ['VIREMENT', 'CHEQUE', 'ESPECES']
const statutsTacheParDefaut = ['A_FAIRE', 'EN_COURS', 'TERMINEE', 'BLOQUEE']
const satisfactions = ['SATISFAIT', 'NEUTRE', 'INSATISFAIT']

const libellesStatut = {
  EN_VALIDATION: 'En validation',
  EN_PREAVIS: 'En préavis',
  DROITS_ARRETES: 'Droits arrêtés',
  SOLDE_EN_PREPARATION: 'Solde en préparation',
  SOLDE_VALIDE: 'Solde validé',
  SOLDE_PAYE: 'Solde payé',
  DOCUMENTS_REMIS: 'Documents remis',
}

const libellesMotif = {
  DEMISSION: 'Démission',
  LICENCIEMENT_DISCIPLINAIRE: 'Licenciement disciplinaire',
  LICENCIEMENT_ECONOMIQUE: 'Licenciement économique',
  LICENCIEMENT_INAPTITUDE: 'Licenciement pour inaptitude',
  LICENCIEMENT_INSUFFISANCE: 'Licenciement pour insuffisance',
  FIN_CDD: 'Fin de CDD',
  RUPTURE_PERIODE_ESSAI: 'Rupture en période d’essai',
  RUPTURE_AMIABLE: 'Rupture amiable',
  RETRAITE: 'Retraite',
  DECES: 'Décès',
  ABANDON_POSTE: 'Abandon de poste',
  FIN_STAGE: 'Fin de stage',
  AUTRE: 'Autre',
}

const dossiers = ref([])
const indicateurs = ref(null)
const types = ref({})
const recherche = ref('')
const statutFiltre = ref('')
const motifFiltre = ref('')
const chargement = ref(false)
const chargementDetail = ref(false)
const actionEnCours = ref(false)
const erreur = ref('')
const information = ref('')
const detail = ref(null)
const remises = ref({})
const tachesForm = ref({})

const creationForm = ref({
  idContrat: null,
  motif: 'DEMISSION',
  motifAutre: '',
  initiateur: 'SALARIE',
  dateNotification: aujourdHui,
  dateSortieSouhaitee: '',
  preavisJours: null,
  modePreavis: 'EFFECTUE',
  description: '',
  idProcedureDisciplinaire: null,
})

const dateSortieForm = ref({ dateSortie: '', avenantSigne: false, referencePiece: '' })
const paiementForm = ref({ referencePaiement: '', modePaiement: 'VIREMENT' })
const entretienForm = ref({
  dateEntretien: aujourdHui,
  interlocuteur: '',
  satisfaction: 'SATISFAIT',
  commentaire: '',
})

const lire = (source, chemin) => chemin.split('.').reduce((acc, cle) => acc?.[cle], source)

function champ(source, ...cles) {
  for (const cle of cles) {
    const valeur = lire(source, cle)
    if (valeur !== undefined && valeur !== null && valeur !== '') return valeur
  }
  return null
}

function listeDepuis(source, ...chemins) {
  if (Array.isArray(source)) return source
  for (const chemin of chemins) {
    const valeur = lire(source, chemin)
    if (Array.isArray(valeur)) return valeur
  }
  return []
}

function normaliser(valeur) {
  return String(valeur ?? '')
    .toUpperCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[\s-]+/g, '_')
}

const nombre = (valeur) =>
  valeur === '' || valeur === null || valeur === undefined ? null : Number(valeur)
const vide = (valeur) => (valeur === '' || valeur === undefined ? null : valeur)

const formatNombre = (valeur) => {
  if (valeur === null || valeur === undefined || valeur === '') return '—'
  const nombreFormate = Number(valeur)
  return Number.isFinite(nombreFormate)
    ? new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 }).format(nombreFormate)
    : String(valeur)
}

const formatMontant = (valeur) => {
  const affichage = formatNombre(valeur)
  return affichage === '—' ? affichage : `${affichage} Ar`
}

const formatAnciennete = (valeur) => {
  if (valeur === null || valeur === undefined || valeur === '') return '—'
  const mois = Number(valeur)
  if (!Number.isFinite(mois)) return String(valeur)
  if (mois < 12) return `${formatNombre(mois)} mois`
  const annees = mois / 12
  return `${formatNombre(annees)} an${annees > 1 ? 's' : ''}`
}

function libelleMotif(valeur) {
  if (!valeur) return '—'
  return libellesMotif[valeur] || String(valeur).replace(/_/g, ' ')
}

function libelleStatut(valeur) {
  if (!valeur) return '—'
  const cle = normaliser(valeur)
  return libellesStatut[cle] || String(valeur)
    .replace(/_/g, ' ')
    .replace(/^./, (lettre) => lettre.toUpperCase())
}

function severiteStatut(valeur) {
  const statut = normaliser(valeur)
  if (['CLOTUREE', 'ARCHIVEE', 'SOLDE_VALIDE', 'SOLDE_PAYE', 'DOCUMENTS_REMIS'].includes(statut)) return 'success'
  if (['ANNULEE', 'REFUSEE', 'BLOQUEE'].includes(statut)) return 'danger'
  if (['INITIEE', 'EN_VALIDATION', 'NOTIFIEE', 'EN_PREAVIS', 'DROITS_ARRETES',
    'SOLDE_EN_PREPARATION'].includes(statut)) return 'warn'
  return 'info'
}

function nomEmploye(dossier) {
  const employe = champ(dossier, 'employe', 'salarie')
  if (typeof employe === 'string') return employe
  const nom = [champ(employe, 'prenom'), champ(employe, 'nom')]
    .filter(Boolean)
    .join(' ')
  return (
    nom ||
    champ(dossier, 'nomEmploye', 'employeNom', 'employeLabel') ||
    (champ(dossier, 'idEmploye') ? `#${champ(dossier, 'idEmploye')}` : '—')
  )
}

function contratDossier(dossier) {
  const contrat = champ(dossier, 'contrat', 'referenceContrat', 'contratReference')
  if (contrat && typeof contrat === 'object') {
    return (
      champ(contrat, 'reference', 'numero', 'libelle') ||
      (champ(contrat, 'id') ? `#${champ(contrat, 'id')}` : '—')
    )
  }
  return contrat || (champ(dossier, 'idContrat') ? `#${champ(dossier, 'idContrat')}` : '—')
}

const netDossier = (dossier) =>
  champ(dossier, 'solde.netAvantCotisations', 'soldeToutCompte.netAvantCotisations',
    'netIndicatif', 'netAvantCotisations', 'soldeNet')

function optionsDepuis(cles, parDefaut) {
  if (Array.isArray(types.value)) {
    return types.value.map((item) =>
      typeof item === 'string' ? item : champ(item, 'code', 'valeur', 'value', 'libelle', 'nom'),
    ).filter(Boolean)
  }
  for (const cle of cles) {
    const brut = lire(types.value, cle)
    if (Array.isArray(brut) && brut.length) {
      return brut.map((item) =>
        typeof item === 'string' ? item : champ(item, 'code', 'valeur', 'value', 'libelle', 'nom'),
      ).filter(Boolean)
    }
  }
  return parDefaut
}

const motifs = computed(() => optionsDepuis(['motifs', 'motifsRupture'], motifsParDefaut))
const initiateurs = computed(() => optionsDepuis(['initiateurs'], initiateursParDefaut))
const modesPreavis = computed(() => optionsDepuis(['modesPreavis'], modesPreavisParDefaut))
const statuts = computed(() => optionsDepuis(['statuts', 'statutsDossier'], statutsParDefaut))
const modesPaiement = computed(() => optionsDepuis(['modesPaiement'], modesPaiementParDefaut))
const statutsTache = computed(() => optionsDepuis(['statutsTache'], statutsTacheParDefaut))

const dossiersFiltres = computed(() => {
  const rechercheNormalisee = normaliser(recherche.value).replace(/_/g, ' ')
  return dossiers.value.filter((dossier) => {
    const statutOk =
      !statutFiltre.value ||
      normaliser(champ(dossier, 'statut', 'etat')) === normaliser(statutFiltre.value)
    const motifOk = !motifFiltre.value || champ(dossier, 'motif') === motifFiltre.value
    const texte = [
      champ(dossier, 'reference'),
      nomEmploye(dossier),
      contratDossier(dossier),
      libelleMotif(champ(dossier, 'motif')),
    ].join(' ')
    const rechercheOk =
      !rechercheNormalisee ||
      normaliser(texte).replace(/_/g, ' ').includes(rechercheNormalisee)
    return statutOk && motifOk && rechercheOk
  })
})

const cartesIndicateurs = computed(() => {
  const source = champ(indicateurs.value, 'indicateurs') || indicateurs.value || {}
  return [
    {
      libelle: 'Taux de rotation',
      valeur: `${formatNombre(champ(source, 'tauxRotation', 'taux_rotation'))} %`,
      icon: 'pi pi-chart-line',
    },
    {
      libelle: 'Ancienneté moyenne',
      valeur: formatAnciennete(champ(source, 'ancienneteMoyenneMois', 'ancienneteMoyenne',
        'anciennete_moyenne')),
      icon: 'pi pi-calendar',
    },
    {
      libelle: 'Départs',
      valeur: formatNombre(champ(source, 'departs', 'nombreDeparts')),
      icon: 'pi pi-user-minus',
    },
    {
      libelle: 'Effectif moyen',
      valeur: formatNombre(champ(source, 'effectifMoyen', 'effectif_moyen')),
      icon: 'pi pi-users',
    },
  ]
})

const statutDetail = computed(() => normaliser(champ(detail.value, 'statut', 'etat')))
const estLectureSeule = computed(() => ['ARCHIVEE', 'ARCHIVE'].includes(statutDetail.value))

const documents = computed(() =>
  listeDepuis(detail.value, 'documents', 'documentsSortie', 'piecesSortie'),
)
const taches = computed(() => listeDepuis(detail.value, 'taches', 'tachesDepart', 'checklist'))
const lignesSolde = computed(() =>
  listeDepuis(detail.value, 'solde.lignes', 'soldeToutCompte.lignes', 'lignesSolde'),
)
const lignesDroits = computed(() =>
  listeDepuis(detail.value, 'droits.lignes', 'droitsRestants.lignes', 'lignesDroits'),
)
const droits = computed(() => champ(detail.value, 'droits', 'droitsRestants') || {})
const solde = computed(() => champ(detail.value, 'solde', 'soldeToutCompte') || {})

const actions = computed(() => {
  const statut = statutDetail.value
  if (estLectureSeule.value || statut === 'ANNULEE') return []
  const disponibles = []
  if (statut === 'INITIEE') {
    disponibles.push({ cle: 'valider', label: 'Valider', icon: 'pi pi-check' })
  }
  if (['EN_VALIDATION', 'NOTIFIEE'].includes(statut)) {
    disponibles.push({ cle: 'notifier', label: 'Notifier', icon: 'pi pi-send' })
  }
  if (['NOTIFIEE', 'EN_PREAVIS'].includes(statut)) {
    disponibles.push({ cle: 'arreter', label: 'Arrêter les droits', icon: 'pi pi-ban' })
  }
  if (statut === 'DROITS_ARRETES') {
    disponibles.push({ cle: 'preparer', label: 'Préparer le solde', icon: 'pi pi-calculator' })
  }
  if (statut === 'SOLDE_EN_PREPARATION') {
    disponibles.push({ cle: 'validerSolde', label: 'Valider le solde', icon: 'pi pi-check-circle' })
  }
  if (statut === 'SOLDE_VALIDE') {
    disponibles.push({ cle: 'payer', label: 'Payer le solde', icon: 'pi pi-money-bill' })
  }
  if (statut === 'SOLDE_PAYE') {
    disponibles.push({ cle: 'generer', label: 'Générer les documents', icon: 'pi pi-file-export' })
  }
  if (statut === 'DOCUMENTS_REMIS') {
    disponibles.push({ cle: 'cloturer', label: 'Clôturer', icon: 'pi pi-check-square' })
  }
  if (statut === 'CLOTUREE') {
    disponibles.push({ cle: 'archiver', label: 'Archiver', icon: 'pi pi-archive' })
  }
  return disponibles
})

function initialiserFormulaires() {
  dateSortieForm.value = {
    dateSortie: champ(detail.value, 'dateSortie', 'date_sortie') || '',
    avenantSigne: false,
    referencePiece: '',
  }
  paiementForm.value = { referencePaiement: '', modePaiement: 'VIREMENT' }
  entretienForm.value = {
    dateEntretien: aujourdHui,
    interlocuteur: '',
    satisfaction: 'SATISFAIT',
    commentaire: '',
  }
  remises.value = {}
  for (const document of documents.value) {
    const id = champ(document, 'id')
    remises.value[id] = {
      modeRemise: champ(document, 'modeRemise') || 'EMAIL',
      preuveRemise: champ(document, 'preuveRemise') || '',
    }
  }
  tachesForm.value = {}
  for (const tache of taches.value) {
    const id = champ(tache, 'id')
    tachesForm.value[id] = champ(tache, 'statut') || 'A_FAIRE'
  }
}

async function charger() {
  chargement.value = true
  erreur.value = ''
  try {
    const [dossiersResultat, indicateursResultat, typesResultat] = await Promise.all([
      api.listDossiers(),
      api.indicateurs(),
      api.types(),
    ])
    dossiers.value = listeDepuis(dossiersResultat, 'content', 'dossiers', 'data', 'results')
    indicateurs.value = indicateursResultat ?? null
    types.value = typesResultat ?? {}
    information.value = `${dossiers.value.length} dossier(s) chargé(s).`
  } catch (cause) {
    erreur.value = cause.message
    information.value = ''
    notifications.erreur(cause.message)
  } finally {
    chargement.value = false
  }
}

async function executer(operation, message, apres) {
  if (actionEnCours.value) return
  actionEnCours.value = true
  erreur.value = ''
  information.value = ''
  try {
    await operation()
    if (apres) await apres()
    information.value = message
    notifications.succes(message)
  } catch (cause) {
    erreur.value = cause.message
    notifications.erreur(cause.message)
  } finally {
    actionEnCours.value = false
  }
}

async function apresAction() {
  await charger()
  const id = champ(detail.value, 'id')
  if (id !== null) {
    const resultat = await api.getDossier(id)
    detail.value = resultat?.dossier ?? resultat ?? detail.value
    initialiserFormulaires()
  }
}

async function ouvrir(dossier) {
  const id = typeof dossier === 'object' ? champ(dossier, 'id') : dossier
  if (!id) return
  chargementDetail.value = true
  erreur.value = ''
  try {
    const resultat = await api.getDossier(id)
    detail.value =
      resultat?.dossier ??
      resultat ??
      dossiers.value.find((item) => champ(item, 'id') === id) ??
      null
    initialiserFormulaires()
  } catch (cause) {
    erreur.value = cause.message
    notifications.erreur(cause.message)
  } finally {
    chargementDetail.value = false
  }
}

function reinitialiserCreation() {
  creationForm.value = {
    idContrat: null,
    motif: 'DEMISSION',
    motifAutre: '',
    initiateur: 'SALARIE',
    dateNotification: aujourdHui,
    dateSortieSouhaitee: '',
    preavisJours: null,
    modePreavis: 'EFFECTUE',
    description: '',
    idProcedureDisciplinaire: null,
  }
}

function creerDossier() {
  const f = creationForm.value
  executer(
    () =>
      api.creerDossier({
        idContrat: nombre(f.idContrat),
        motif: f.motif === 'AUTRE' && f.motifAutre ? f.motifAutre : f.motif,
        motifAutre: vide(f.motifAutre),
        initiateur: f.initiateur,
        dateNotification: vide(f.dateNotification),
        dateSortieSouhaitee: vide(f.dateSortieSouhaitee),
        preavisJours: nombre(f.preavisJours),
        modePreavis: f.modePreavis,
        description: vide(f.description),
        idProcedureDisciplinaire: nombre(f.idProcedureDisciplinaire),
      }),
    'Dossier de rupture créé.',
    async () => {
      await charger()
      reinitialiserCreation()
    },
  )
}

function executerAction(cle) {
  const id = champ(detail.value, 'id')
  const operations = {
    valider: () => api.validerDossier(id),
    notifier: () => api.notifierDossier(id),
    arreter: () => api.arreterDroits(id),
    preparer: () => api.preparerSolde(id),
    validerSolde: () => api.validerSolde(id),
    payer: () => api.payerSolde(id, { ...paiementForm.value }),
    generer: () => api.genererDocuments(id),
    cloturer: () => api.cloturerDossier(id),
    archiver: () => api.archiverDossier(id),
  }
  const messages = {
    valider: 'Dossier validé.',
    notifier: 'Rupture notifiée.',
    arreter: 'Droits arrêtés.',
    preparer: 'Solde préparé.',
    validerSolde: 'Solde validé.',
    payer: 'Solde payé.',
    generer: 'Documents générés.',
    cloturer: 'Dossier clôturé.',
    archiver: 'Dossier archivé.',
  }
  executer(operations[cle], messages[cle], apresAction)
}

function modifierDateSortie() {
  const f = dateSortieForm.value
  executer(
    () =>
      api.modifierDateSortie(champ(detail.value, 'id'), {
        dateSortie: f.dateSortie,
        avenantSigne: f.avenantSigne,
        referencePiece: vide(f.referencePiece),
      }),
    'Date de sortie modifiée.',
    apresAction,
  )
}

function changerStatutTache(tache) {
  const id = champ(tache, 'id')
  executer(
    () =>
      api.gererTache(champ(detail.value, 'id'), id, {
        statut: tachesForm.value[id],
        preuve: champ(tache, 'preuve') || null,
      }),
    'Tâche mise à jour.',
    apresAction,
  )
}

function remettreDocument(document) {
  const id = champ(document, 'id')
  const f = remises.value[id] || {}
  executer(
    () =>
      api.remettreDocument(id, {
        modeRemise: f.modeRemise,
        preuveRemise: vide(f.preuveRemise),
      }),
    'Document remis.',
    apresAction,
  )
}

function enregistrerEntretien() {
  executer(
    () => api.enregistrerEntretien(champ(detail.value, 'id'), { ...entretienForm.value }),
    'Entretien de sortie enregistré.',
    apresAction,
  )
}

onMounted(charger)
</script>

<template>
  <div class="flex flex-col gap-5">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold">Rupture et offboarding</h1>
        <p class="text-sm text-surface-500">
          Dossiers de rupture, droits restants, solde de tout compte et archivage.
        </p>
      </div>
      <PrimeButton
        icon="pi pi-refresh"
        label="Actualiser"
        outlined
        :loading="chargement"
        @click="charger"
      />
    </div>

    <Message v-if="erreur" severity="error" :closable="false">{{ erreur }}</Message>
    <Message v-if="information" severity="success" :closable="false">{{ information }}</Message>
    <Message v-if="chargement" severity="info" :closable="false">Chargement en cours…</Message>

    <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
      <div
        v-for="carte in cartesIndicateurs"
        :key="carte.libelle"
        class="rounded-lg border border-surface bg-surface-0 p-3 dark:bg-surface-900"
      >
        <p class="m-0 text-xs text-muted-color">
          <i :class="carte.icon" class="mr-1" />{{ carte.libelle }}
        </p>
        <strong class="text-xl">{{ carte.valeur }}</strong>
      </div>
    </div>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="text-lg font-semibold">Recherche</h2>
      <div class="mt-3 grid gap-3 md:grid-cols-4">
        <PrimeInputText v-model="recherche" type="search" placeholder="Référence, employé, contrat" />
        <PrimeSelect
          v-model="statutFiltre"
          :options="statuts"
          class="w-full"
          placeholder="Tous les statuts"
        />
        <PrimeSelect
          v-model="motifFiltre"
          :options="motifs"
          class="w-full"
          placeholder="Tous les motifs"
        />
        <PrimeButton label="Réinitialiser les filtres" icon="pi pi-filter-slash" text
          @click="recherche = ''; statutFiltre = ''; motifFiltre = ''" />
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="text-lg font-semibold">Nouveau dossier de rupture</h2>
      <div class="mt-3 grid gap-3 md:grid-cols-4">
        <PrimeInputNumber v-model="creationForm.idContrat" placeholder="Identifiant du contrat"
          class="w-full" :use-grouping="false" />
        <PrimeSelect v-model="creationForm.motif" :options="motifs" class="w-full" placeholder="Motif" />
        <PrimeSelect v-model="creationForm.initiateur" :options="initiateurs" class="w-full"
          placeholder="Initiateur" />
        <PrimeSelect v-model="creationForm.modePreavis" :options="modesPreavis" class="w-full"
          placeholder="Mode de préavis" />
        <input v-model="creationForm.dateNotification" type="date"
          class="rounded border border-surface bg-surface-0 px-3 py-2 dark:bg-surface-900" />
        <input v-model="creationForm.dateSortieSouhaitee" type="date"
          class="rounded border border-surface bg-surface-0 px-3 py-2 dark:bg-surface-900" />
        <PrimeInputNumber v-model="creationForm.preavisJours" placeholder="Préavis (jours)"
          class="w-full" :use-grouping="false" />
        <PrimeInputNumber v-model="creationForm.idProcedureDisciplinaire"
          placeholder="Procédure disciplinaire" class="w-full" :use-grouping="false" />
        <PrimeInputText v-if="creationForm.motif === 'AUTRE'" v-model="creationForm.motifAutre"
          placeholder="Précision du motif" class="md:col-span-2" />
        <PrimeTextarea v-model="creationForm.description" rows="2" placeholder="Description"
          class="md:col-span-3" />
        <PrimeButton label="Créer le dossier" icon="pi pi-plus" :loading="actionEnCours"
          @click="creerDossier" />
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="text-lg font-semibold">Dossiers de rupture</h2>
      <DataTable :value="dossiersFiltres" data-key="id" striped-rows paginator :rows="10"
        class="mt-3" responsive-layout="scroll">
        <Column field="reference" header="Référence" />
        <Column header="Employé">
          <template #body="{ data }">{{ nomEmploye(data) }}</template>
        </Column>
        <Column header="Contrat">
          <template #body="{ data }">{{ contratDossier(data) }}</template>
        </Column>
        <Column header="Motif">
          <template #body="{ data }">{{ libelleMotif(champ(data, 'motif')) }}</template>
        </Column>
        <Column header="Initiateur">
          <template #body="{ data }">{{ champ(data, 'initiateur') || '—' }}</template>
        </Column>
        <Column header="Notification">
          <template #body="{ data }">{{ champ(data, 'dateNotification') || '—' }}</template>
        </Column>
        <Column header="Sortie">
          <template #body="{ data }">{{ champ(data, 'dateSortie') || '—' }}</template>
        </Column>
        <Column header="Statut">
          <template #body="{ data }">
            <Tag :value="libelleStatut(champ(data, 'statut', 'etat'))"
              :severity="severiteStatut(champ(data, 'statut', 'etat'))" />
          </template>
        </Column>
        <Column header="Solde net">
          <template #body="{ data }">{{ formatMontant(netDossier(data)) }}</template>
        </Column>
        <Column header="Actions">
          <template #body="{ data }">
            <PrimeButton label="Ouvrir" icon="pi pi-folder-open" text size="small"
              @click="ouvrir(data)" />
          </template>
        </Column>
      </DataTable>
      <p v-if="!dossiersFiltres.length && !chargement" class="mt-3 text-sm text-surface-500">
        Aucun dossier de rupture.
      </p>
    </section>

    <section v-if="detail" class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 class="text-lg font-semibold">
            Dossier {{ champ(detail, 'reference') || `#${champ(detail, 'id')}` }}
          </h2>
          <p class="m-0 text-sm text-surface-500">
            {{ nomEmploye(detail) }} · {{ libelleMotif(champ(detail, 'motif')) }}
          </p>
        </div>
        <div class="flex items-center gap-2">
          <Tag :value="libelleStatut(champ(detail, 'statut', 'etat'))"
            :severity="severiteStatut(champ(detail, 'statut', 'etat'))" />
          <PrimeButton icon="pi pi-times" text rounded title="Fermer" @click="detail = null" />
        </div>
      </div>

      <Message v-if="estLectureSeule" severity="info" :closable="false" class="mt-3">
        Dossier archivé : lecture seule.
      </Message>
      <Message v-if="chargementDetail" severity="info" :closable="false" class="mt-3">
        Chargement du dossier…
      </Message>

      <template v-if="!chargementDetail">
        <div class="mt-3 grid gap-3 md:grid-cols-4">
          <p class="m-0 text-sm"><strong>Contrat :</strong> {{ contratDossier(detail) }}</p>
          <p class="m-0 text-sm"><strong>Notification :</strong> {{ champ(detail, 'dateNotification') || '—' }}</p>
          <p class="m-0 text-sm"><strong>Début préavis :</strong> {{ champ(detail, 'dateDebutPreavis') || '—' }}</p>
          <p class="m-0 text-sm"><strong>Fin préavis :</strong> {{ champ(detail, 'dateFinPreavis') || '—' }}</p>
          <p class="m-0 text-sm"><strong>Date de sortie :</strong> {{ champ(detail, 'dateSortie') || '—' }}</p>
          <p class="m-0 text-sm"><strong>Préavis :</strong> {{ champ(detail, 'preavisJours') ?? '—' }} jours · {{ champ(detail, 'modePreavis') || '—' }}</p>
          <p class="m-0 text-sm"><strong>Initiateur :</strong> {{ champ(detail, 'initiateur') || '—' }}</p>
          <p class="m-0 text-sm"><strong>Solde net :</strong> {{ formatMontant(netDossier(detail)) }}</p>
        </div>

        <div v-if="!estLectureSeule" class="mt-4 flex flex-wrap gap-2">
          <PrimeButton v-for="action in actions" :key="action.cle" :label="action.label"
            :icon="action.icon" :loading="actionEnCours" outlined @click="executerAction(action.cle)" />
          <span v-if="!actions.length" class="text-sm text-surface-500">
            Aucune action disponible pour ce statut.
          </span>
        </div>

        <div v-if="!estLectureSeule" class="mt-4 grid gap-4 lg:grid-cols-2">
          <div class="rounded-lg border border-surface p-3">
            <h3 class="m-0 text-base font-semibold">Modifier la date de sortie</h3>
            <div class="mt-3 grid gap-2">
              <input v-model="dateSortieForm.dateSortie" type="date"
                class="rounded border border-surface bg-surface-0 px-3 py-2 dark:bg-surface-900" />
              <PrimeInputText v-model="dateSortieForm.referencePiece" placeholder="Référence de la pièce" />
              <label class="flex items-center gap-2 text-sm">
                <input v-model="dateSortieForm.avenantSigne" type="checkbox" /> Avenant signé
              </label>
              <PrimeButton label="Enregistrer la date" icon="pi pi-calendar" outlined
                :loading="actionEnCours" @click="modifierDateSortie" />
            </div>
          </div>

          <div class="rounded-lg border border-surface p-3">
            <h3 class="m-0 text-base font-semibold">Paiement du solde</h3>
            <div class="mt-3 grid gap-2">
              <PrimeInputText v-model="paiementForm.referencePaiement" placeholder="Référence de paiement" />
              <PrimeSelect v-model="paiementForm.modePaiement" :options="modesPaiement" class="w-full"
                placeholder="Mode de paiement" />
              <PrimeButton label="Payer le solde" icon="pi pi-money-bill" outlined
                :loading="actionEnCours" @click="executerAction('payer')" />
            </div>
          </div>
        </div>

        <div class="mt-4 rounded-lg border border-surface p-3">
          <h3 class="m-0 text-base font-semibold">Droits restants</h3>
          <div class="mt-3 grid gap-2 md:grid-cols-3">
            <p class="m-0 text-sm"><strong>Congés non pris :</strong> {{ champ(droits, 'soldeCongesNonPris', 'solde_conges_non_pris') ?? '—' }}</p>
            <p class="m-0 text-sm"><strong>Primes au prorata :</strong> {{ formatMontant(champ(droits, 'primesProrata', 'primes_prorata')) }}</p>
            <p class="m-0 text-sm"><strong>Avances restantes :</strong> {{ formatMontant(champ(droits, 'avancesRestantDu', 'avances_restant_du')) }}</p>
            <p class="m-0 text-sm"><strong>Dédit formation :</strong> {{ formatMontant(champ(droits, 'deditFormationARembourser', 'dedit_formation_a_rembourser')) }}</p>
            <p class="m-0 text-sm"><strong>Matériel non restitué :</strong> {{ formatMontant(champ(droits, 'materielNonRestitueValeur', 'materiel_non_restitue_valeur')) }}</p>
            <p class="m-0 text-sm"><strong>Jours de formation dus :</strong> {{ champ(droits, 'joursFormationDus', 'jours_formation_dus') ?? '—' }}</p>
          </div>
          <DataTable v-if="lignesDroits.length" :value="lignesDroits" striped-rows size="small"
            class="mt-3" responsive-layout="scroll">
            <Column header="Libellé">
              <template #body="{ data }">{{ champ(data, 'libelle', 'label') || '—' }}</template>
            </Column>
            <Column field="quantite" header="Quantité" />
            <Column header="Montant">
              <template #body="{ data }">{{ formatMontant(champ(data, 'montant')) }}</template>
            </Column>
          </DataTable>
        </div>

        <div class="mt-4 rounded-lg border border-surface p-3">
          <h3 class="m-0 text-base font-semibold">Solde de tout compte</h3>
          <div class="mt-3 grid gap-2 md:grid-cols-3">
            <p class="m-0 text-sm"><strong>Total brut :</strong> {{ formatMontant(champ(solde, 'totalBrut', 'total_brut')) }}</p>
            <p class="m-0 text-sm"><strong>Retenues :</strong> {{ formatMontant(champ(solde, 'totalRetenues', 'total_retenues')) }}</p>
            <p class="m-0 text-sm"><strong>Net indicatif :</strong> {{ formatMontant(champ(solde, 'netAvantCotisations', 'net_indicatif')) }}</p>
            <p class="m-0 text-sm"><strong>Statut :</strong> {{ champ(solde, 'statut') || '—' }}</p>
            <p class="m-0 text-sm"><strong>Bulletin :</strong> {{ champ(solde, 'idBulletinPaie', 'bulletinId', 'bulletin_id') || '—' }}</p>
          </div>
          <DataTable v-if="lignesSolde.length" :value="lignesSolde" striped-rows size="small"
            class="mt-3" responsive-layout="scroll">
            <Column header="Libellé">
              <template #body="{ data }">{{ champ(data, 'libelle', 'label') || '—' }}</template>
            </Column>
            <Column field="base" header="Base" />
            <Column field="quantite" header="Quantité" />
            <Column header="Montant">
              <template #body="{ data }">{{ formatMontant(champ(data, 'montant')) }}</template>
            </Column>
            <Column field="source" header="Source" />
          </DataTable>
          <p v-if="!lignesSolde.length" class="mt-3 text-sm text-surface-500">
            Aucune ligne de solde.
          </p>
        </div>

        <div class="mt-4 rounded-lg border border-surface p-3">
          <h3 class="m-0 text-base font-semibold">Checklist de départ</h3>
          <DataTable v-if="taches.length" :value="taches" data-key="id" striped-rows size="small"
            class="mt-3" responsive-layout="scroll">
            <Column field="code" header="Code" />
            <Column field="categorie" header="Catégorie" />
            <Column field="responsable" header="Responsable" />
            <Column header="Bloquante">
              <template #body="{ data }">{{ data.bloquante ? 'Oui' : 'Non' }}</template>
            </Column>
            <Column header="Statut">
              <template #body="{ data }">
                <PrimeSelect v-if="!estLectureSeule" v-model="tachesForm[champ(data, 'id')]"
                  :options="statutsTache" class="w-full" />
                <span v-else>{{ champ(data, 'statut') || '—' }}</span>
              </template>
            </Column>
            <Column header="Preuve">
              <template #body="{ data }">{{ champ(data, 'preuve') || '—' }}</template>
            </Column>
            <Column v-if="!estLectureSeule" header="Action">
              <template #body="{ data }">
                <PrimeButton label="Enregistrer" icon="pi pi-save" text size="small"
                  :loading="actionEnCours" @click="changerStatutTache(data)" />
              </template>
            </Column>
          </DataTable>
          <p v-if="!taches.length" class="mt-3 text-sm text-surface-500">Aucune tâche de départ.</p>
        </div>

        <div class="mt-4 rounded-lg border border-surface p-3">
          <h3 class="m-0 text-base font-semibold">Documents de sortie</h3>
          <DataTable v-if="documents.length" :value="documents" data-key="id" striped-rows size="small"
            class="mt-3" responsive-layout="scroll">
            <Column field="type" header="Type" />
            <Column field="gabaritVersion" header="Version du gabarit" />
            <Column field="dateGeneration" header="Génération" />
            <Column field="dateRemise" header="Remise" />
            <Column header="Preuve">
              <template #body="{ data }">{{ champ(data, 'preuveRemise') || '—' }}</template>
            </Column>
            <Column v-if="!estLectureSeule" header="Remettre">
              <template #body="{ data }">
                <div class="flex flex-wrap items-center gap-2">
                  <PrimeSelect v-model="remises[champ(data, 'id')].modeRemise"
                    :options="['EMAIL', 'PAPIER', 'MAIN_PROPRE']" class="w-36" />
                  <PrimeInputText v-model="remises[champ(data, 'id')].preuveRemise"
                    placeholder="Preuve de remise" class="w-44" />
                  <PrimeButton icon="pi pi-send" text rounded title="Remettre le document"
                    :loading="actionEnCours" @click="remettreDocument(data)" />
                </div>
              </template>
            </Column>
          </DataTable>
          <p v-if="!documents.length" class="mt-3 text-sm text-surface-500">
            Aucun document généré.
          </p>
        </div>

        <div v-if="!estLectureSeule" class="mt-4 rounded-lg border border-surface p-3">
          <h3 class="m-0 text-base font-semibold">Entretien de sortie</h3>
          <div class="mt-3 grid gap-3 md:grid-cols-4">
            <input v-model="entretienForm.dateEntretien" type="date"
              class="rounded border border-surface bg-surface-0 px-3 py-2 dark:bg-surface-900" />
            <PrimeInputText v-model="entretienForm.interlocuteur" placeholder="Interlocuteur" />
            <PrimeSelect v-model="entretienForm.satisfaction" :options="satisfactions" class="w-full" />
            <PrimeTextarea v-model="entretienForm.commentaire" rows="2" placeholder="Commentaire"
              class="md:col-span-3" />
            <PrimeButton label="Enregistrer l'entretien" icon="pi pi-comments" outlined
              :loading="actionEnCours" @click="enregistrerEntretien" />
          </div>
        </div>
      </template>
    </section>
  </div>
</template>
