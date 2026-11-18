<script setup>
import { onMounted, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import PrimeSelect from 'primevue/select'
import PrimeDialog from 'primevue/dialog'
import Tag from 'primevue/tag'
import { paieApi as api } from '@/modules/paie/api/paieApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const notifications = useNotificationStore()
const aujourdHui = new Date()
const debut = ref(new Date(aujourdHui.getFullYear(), aujourdHui.getMonth(), 1).toISOString().slice(0, 10))
const fin = ref(new Date(aujourdHui.getFullYear(), aujourdHui.getMonth() + 1, 0).toISOString().slice(0, 10))
const variables = ref([])
const parametres = ref({})
const bulletins = ref([])
const periodes = ref([])
const moisPaie = ref(aujourdHui.toISOString().slice(0, 7))
const idPeriode = ref(null)
const periode = ref(null)
const fiches = ref([])
const detail = ref(null)
const dialogueAjustement = ref(false)
const ajustement = ref({ idBulletin: null, primeExceptionnelle: 0, avance: 0, personnesCharge: 0, motif: '' })
const statutCible = ref('PUBLIEE')
const chargement = ref(false)
const statuts = ['PUBLIEE', 'PAYEE']

async function charger() {
  chargement.value = true
  try {
    variables.value = await api.variables(debut.value, fin.value)
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

async function chargerParametres() {
  try {
    const resultat = await api.parametres()
    if (resultat) parametres.value = resultat
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function chargerPeriodes() {
  try {
    periodes.value = await api.periodes()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function enregistrerParametres() {
  const champs = [
    'tauxCotisationsSalariales', 'tauxCotisationsPatronales', 'tauxIrsa', 'smig',
    'plafondCnapsSmig', 'tauxCnapsSalarial', 'tauxCnapsPatronal', 'tauxOstieSalarial',
    'tauxOstiePatronal', 'tauxFmfpPatronal', 'majorationHs30', 'majorationHs50',
    'majorationNuit', 'majorationDimanche', 'majorationFerie', 'minimumIrsa',
    'reductionCharge', 'quotientRetenue',
  ]
  const payload = {}
  champs.forEach((champ) => {
    payload[champ] = parametres.value[champ] === '' || parametres.value[champ] == null
      ? null : Number(parametres.value[champ])
  })
  try {
    parametres.value = await api.enregistrerParametres(payload)
    notifications.succes('Les paramètres de paie ont été enregistrés.')
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function calculerPeriode() {
  try {
    const resultat = await api.calculerPeriode(moisPaie.value)
    bulletins.value = resultat.bulletins
    idPeriode.value = resultat.periode.id
    await chargerPeriodes()
    await lirePeriode()
    notifications.succes('La période de paie a été calculée.')
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function lirePeriode() {
  if (!idPeriode.value) return
  try {
    const resultat = await api.periode(idPeriode.value)
    periode.value = resultat.periode
    bulletins.value = resultat.bulletins
    await chargerFiches()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function chargerFiches() {
  if (!idPeriode.value) {
    fiches.value = []
    return
  }
  try {
    fiches.value = await api.fichesPeriode(idPeriode.value)
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function actionPeriode(nom, message) {
  if (!idPeriode.value) return
  try {
    await api[nom](idPeriode.value)
    await Promise.all([chargerPeriodes(), lirePeriode()])
    notifications.succes(message)
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function changerStatut() {
  if (!idPeriode.value) return
  try {
    await api.changerStatut(idPeriode.value, statutCible.value)
    await Promise.all([chargerPeriodes(), lirePeriode()])
    notifications.succes('Statut de période mis à jour.')
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function ouvrirDetail(idBulletin) {
  try {
    detail.value = await api.detailBulletin(idBulletin)
  } catch (e) {
    notifications.erreur(e.message)
  }
}

function ouvrirAjustement(bulletin) {
  ajustement.value = {
    idBulletin: bulletin.id,
    primeExceptionnelle: Number(bulletin.primeExceptionnelle || 0),
    avance: Number(bulletin.avance || 0),
    personnesCharge: bulletin.personnesCharge || 0,
    motif: bulletin.motifAjustement || '',
  }
  dialogueAjustement.value = true
}

async function enregistrerAjustement() {
  try {
    const { idBulletin, ...payload } = ajustement.value
    await api.ajuster(idBulletin, payload)
    dialogueAjustement.value = false
    await lirePeriode()
    notifications.succes('Bulletin ajusté et recalculé.')
  } catch (e) {
    notifications.erreur(e.message)
  }
}

onMounted(async () => {
  await Promise.all([charger(), chargerParametres(), chargerPeriodes()])
})
</script>

<template>
  <div class="flex flex-col gap-5">
    <div>
      <h1>Fiche de paie</h1>
      <p class="mt-1 text-sm text-muted-color">Calcul déterministe, validation, clôture et fiches figées de la paie mensuelle.</p>
    </div>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="mb-3">Variables de paie</h2>
      <div class="flex flex-wrap items-end gap-3">
        <div class="flex flex-col gap-2">
          <label for="paie-debut" class="text-sm font-semibold text-muted-color">Début</label>
          <PrimeInputText id="paie-debut" v-model="debut" type="date" />
        </div>
        <div class="flex flex-col gap-2">
          <label for="paie-fin" class="text-sm font-semibold text-muted-color">Fin</label>
          <PrimeInputText id="paie-fin" v-model="fin" type="date" />
        </div>
        <PrimeButton label="Actualiser" icon="pi pi-refresh" @click="charger" />
      </div>
      <DataTable :value="variables" :loading="chargement" class="mt-4" striped-rows paginator :rows="10" responsive-layout="scroll">
        <Column field="idEmploye" header="Employé" />
        <Column field="typeConge" header="Type" />
        <Column field="joursConges" header="Jours" />
        <Column field="salaireBase" header="Salaire de base" />
        <Column field="montantConges" header="Montant congé" />
      </DataTable>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="mb-3">Paramètres réglementaires</h2>
      <div class="grid gap-3 md:grid-cols-4">
        <PrimeInputText v-model.number="parametres.smig" type="number" placeholder="SMIG" />
        <PrimeInputText v-model.number="parametres.tauxCnapsSalarial" type="number" placeholder="CNaPS salarial (%)" />
        <PrimeInputText v-model.number="parametres.tauxCnapsPatronal" type="number" placeholder="CNaPS patronal (%)" />
        <PrimeInputText v-model.number="parametres.tauxOstieSalarial" type="number" placeholder="OSTIE salarial (%)" />
        <PrimeInputText v-model.number="parametres.tauxOstiePatronal" type="number" placeholder="OSTIE patronal (%)" />
        <PrimeInputText v-model.number="parametres.tauxFmfpPatronal" type="number" placeholder="FMFP patronal (%)" />
        <PrimeInputText v-model.number="parametres.majorationHs30" type="number" placeholder="HS 30 (%)" />
        <PrimeInputText v-model.number="parametres.majorationHs50" type="number" placeholder="HS 50 (%)" />
        <PrimeInputText v-model.number="parametres.minimumIrsa" type="number" placeholder="IRSA minimum" />
        <PrimeInputText v-model.number="parametres.reductionCharge" type="number" placeholder="Réduction / charge" />
        <PrimeButton label="Enregistrer" icon="pi pi-save" @click="enregistrerParametres" />
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="flex flex-wrap items-end gap-3">
        <div class="flex flex-col gap-2">
          <label for="mois-paie" class="text-sm font-semibold text-muted-color">Période</label>
          <PrimeInputText id="mois-paie" v-model="moisPaie" type="month" />
        </div>
        <PrimeButton label="Calculer les bulletins" icon="pi pi-calculator" @click="calculerPeriode" />
      </div>
      <DataTable v-if="bulletins.length" :value="bulletins" class="mt-4" striped-rows responsive-layout="scroll">
        <Column field="idEmploye" header="Employé" />
        <Column field="jourPayes" header="Jours" />
        <Column field="salaireBrut" header="Brut" />
        <Column field="cotisationsSalariales" header="Cotisations" />
        <Column field="impot" header="IRSA" />
        <Column field="salaireNet" header="Net à payer" />
        <Column field="statut" header="Statut" />
        <Column header="Actions">
          <template #body="{ data }">
            <div class="flex gap-2">
              <PrimeButton icon="pi pi-eye" text rounded aria-label="Voir le détail" @click="ouvrirDetail(data.id)" />
              <PrimeButton icon="pi pi-pencil" text rounded aria-label="Ajuster" @click="ouvrirAjustement(data)" />
            </div>
          </template>
        </Column>
      </DataTable>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="mb-3">Périodes et workflow</h2>
      <div class="flex flex-wrap items-end gap-3">
        <PrimeSelect
          v-model="idPeriode"
          :options="periodes"
          option-label="mois"
          option-value="id"
          placeholder="Choisir une période"
          class="w-56"
          @change="lirePeriode"
        />
        <Tag v-if="periode" :value="periode.statut" />
        <PrimeButton label="Lire" icon="pi pi-search" @click="lirePeriode" />
        <PrimeButton label="Consolider" icon="pi pi-link" @click="actionPeriode('consolider', 'Variables consolidées.')" />
        <PrimeButton label="Valider" icon="pi pi-check" @click="actionPeriode('valider', 'Période validée.')" />
        <PrimeButton label="Clôturer" icon="pi pi-lock" @click="actionPeriode('cloturer', 'Période clôturée, fiches figées.')" />
        <PrimeSelect v-model="statutCible" :options="statuts" class="w-40" />
        <PrimeButton label="Changer statut" icon="pi pi-sync" @click="changerStatut" />
      </div>
      <DataTable :value="periodes" striped-rows class="mt-4">
        <Column field="mois" header="Mois" />
        <Column field="statut" header="Statut" />
        <Column field="versionRegles" header="Version des règles" />
        <Column field="dateClotureVariables" header="Consolidation" />
        <Column field="datePaiement" header="Paiement" />
        <Column field="dateCloture" header="Clôture" />
      </DataTable>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="mb-3">Fiches figées</h2>
      <DataTable :value="fiches" striped-rows>
        <Column field="numero" header="Numéro" />
        <Column field="idEmploye" header="Employé" />
        <Column field="mois" header="Mois" />
        <Column field="netAPayer" header="Net à payer" />
        <Column field="dateGeneration" header="Générée le" />
      </DataTable>
    </section>

    <PrimeDialog v-model:visible="detail" modal header="Détail du bulletin" class="w-[min(60rem,95vw)]">
      <template v-if="detail">
        <div class="mb-4 grid gap-2 text-sm md:grid-cols-3">
          <span>Brut : {{ detail.bulletin.salaireBrut }}</span>
          <span>Cotisations : {{ detail.bulletin.cotisationsSalariales }}</span>
          <span>IRSA : {{ detail.bulletin.impot }}</span>
          <span>Net : {{ detail.bulletin.salaireNet }}</span>
          <span>Jours payés : {{ detail.bulletin.jourPayes }}</span>
          <span>Statut : {{ detail.bulletin.statut }}</span>
        </div>
        <DataTable :value="detail.lignes" striped-rows>
          <Column field="ordre" header="#" />
          <Column field="libelle" header="Rubrique" />
          <Column field="base" header="Base" />
          <Column field="quantite" header="Quantité" />
          <Column field="montantSalarial" header="Salarial" />
          <Column field="montantPatronal" header="Patronal" />
          <Column field="sources" header="Sources" />
        </DataTable>
      </template>
    </PrimeDialog>

    <PrimeDialog v-model:visible="dialogueAjustement" modal header="Ajuster le bulletin" class="w-[min(32rem,95vw)]">
      <div class="flex flex-col gap-3">
        <PrimeInputText v-model.number="ajustement.primeExceptionnelle" type="number" placeholder="Prime exceptionnelle" />
        <PrimeInputText v-model.number="ajustement.avance" type="number" placeholder="Avance / retenue" />
        <PrimeInputText v-model.number="ajustement.personnesCharge" type="number" placeholder="Personnes à charge" />
        <PrimeTextarea v-model="ajustement.motif" rows="3" placeholder="Motif de l'ajustement" />
        <PrimeButton label="Enregistrer" icon="pi pi-save" @click="enregistrerAjustement" />
      </div>
    </PrimeDialog>
  </div>
</template>
