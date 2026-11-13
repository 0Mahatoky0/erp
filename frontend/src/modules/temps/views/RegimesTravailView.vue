<script setup>
import { onMounted, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeSelect from 'primevue/select'

import { tempsApi } from '@/modules/temps/api/tempsApi'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const auth = useAuthStore()
const notifications = useNotificationStore()
const chargement = ref(false)
const regimes = ref([])
const regimesOptions = ref([])
const horaires = ref([])
const regimeSelectionne = ref(null)
const affectations = ref([])
const idEmploye = ref('')
const regimeEnEdition = ref(null)
const regimeForm = ref(formRegimeVide())
const horaireForm = ref({
  jourSemaine: 'LUNDI',
  heureDebut: '08:00',
  heureFin: '17:00',
  pauseDebut: '12:00',
  pauseFin: '13:00',
})
const affectationForm = ref({
  idRegime: null,
  dateDebut: new Date().toISOString().slice(0, 10),
  dateFin: '',
  appliquerPlanning: true,
})

const jours = ['LUNDI', 'MARDI', 'MERCREDI', 'JEUDI', 'VENDREDI', 'SAMEDI', 'DIMANCHE']
const types = [
  { value: 'HORAIRE_FIXE', label: 'Horaire fixe' },
  { value: 'HORAIRE_VARIABLE', label: 'Horaire variable' },
  { value: 'POSTES_ROTATIFS', label: 'Postes rotatifs' },
  { value: 'FORFAIT_JOURS', label: 'Forfait jours' },
  { value: 'TEMPS_PARTIEL', label: 'Temps partiel' },
]

function formRegimeVide() {
  return {
    code: '',
    libelle: '',
    type: 'HORAIRE_FIXE',
    heuresHebdo: 40,
    joursTravailles: 'LUNDI,MARDI,MERCREDI,JEUDI,VENDREDI',
    toleranceRetardMin: 10,
    toleranceDepartMin: 10,
    pauseMinutes: 60,
    pausePayee: false,
    decompteHeuresSup: true,
  }
}

function idEmployeRequis() {
  const id = Number(idEmploye.value)
  if (!id) {
    notifications.erreur('Saisissez l’identifiant de l’employe.')
    return null
  }
  return id
}

async function chargerRegimes() {
  chargement.value = true
  try {
    regimes.value = await tempsApi.regimes()
    regimesOptions.value = regimes.value.map((regime) => ({
      label: `${regime.code} · ${regime.libelle}`,
      value: regime.id,
    }))
    if (regimeSelectionne.value) {
      await chargerHoraires(regimeSelectionne.value)
    }
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

async function chargerHoraires(regime) {
  regimeSelectionne.value = regime.id
  try {
    horaires.value = await tempsApi.horairesRegime(regime.id)
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function enregistrerRegime() {
  if (!regimeForm.value.code.trim() || !regimeForm.value.libelle.trim()) {
    notifications.erreur('Renseignez le code et le libelle du regime.')
    return
  }
  try {
    if (regimeEnEdition.value) {
      await tempsApi.modifierRegime(regimeEnEdition.value, regimeForm.value)
      notifications.succes('Regime mis a jour.')
    } else {
      await tempsApi.creerRegime(regimeForm.value)
      notifications.succes('Regime cree.')
    }
    reinitialiserRegime()
    await chargerRegimes()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

function editerRegime(regime) {
  regimeEnEdition.value = regime.id
  regimeForm.value = {
    code: regime.code,
    libelle: regime.libelle,
    type: regime.type,
    heuresHebdo: regime.heuresHebdo,
    joursTravailles: regime.joursTravailles,
    toleranceRetardMin: regime.toleranceRetardMin,
    toleranceDepartMin: regime.toleranceDepartMin,
    pauseMinutes: regime.pauseMinutes,
    pausePayee: regime.pausePayee,
    decompteHeuresSup: regime.decompteHeuresSup,
  }
}

function reinitialiserRegime() {
  regimeEnEdition.value = null
  regimeForm.value = formRegimeVide()
}

async function supprimerRegime(regime) {
  try {
    await tempsApi.supprimerRegime(regime.id)
    notifications.succes('Regime supprime.')
    if (regimeSelectionne.value === regime.id) {
      regimeSelectionne.value = null
      horaires.value = []
    }
    await chargerRegimes()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function ajouterHoraire() {
  const regime = regimeSelectionne.value
  if (!regime) {
    notifications.erreur('Selectionnez d’abord un regime.')
    return
  }
  try {
    await tempsApi.ajouterHoraire(regime, horaireForm.value)
    notifications.succes('Plage horaire ajoutee.')
    await chargerHoraires(regime)
    await chargerRegimes()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function supprimerHoraire(horaire) {
  try {
    await tempsApi.supprimerHoraire(horaire.id)
    notifications.succes('Plage horaire supprimee.')
    await chargerHoraires(regimeSelectionne.value)
    await chargerRegimes()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function chargerAffectations() {
  const id = idEmployeRequis()
  if (!id) return
  try {
    affectations.value = await tempsApi.affectationsRegime(id)
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function affecterRegime() {
  const id = idEmployeRequis()
  if (!id) return
  if (!affectationForm.value.idRegime) {
    notifications.erreur('Selectionnez un regime.')
    return
  }
  try {
    await tempsApi.affecterRegime(id, {
      ...affectationForm.value,
      dateFin: affectationForm.value.dateFin || null,
    })
    notifications.succes('Regime affecte et planning genere.')
    await chargerAffectations()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

function libelleType(value) {
  return types.find((type) => type.value === value)?.label || value
}

onMounted(chargerRegimes)
</script>

<template>
  <div class="flex flex-col gap-5">
    <div>
      <h1>Régimes et horaires de travail</h1>
      <p class="mt-1 text-sm text-muted-color">
        Définissez les régimes, leurs plages horaires et affectez-les aux salariés.
      </p>
    </div>

    <section v-if="auth.estRh || auth.estDirection" class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="mb-4 flex items-center justify-between gap-3">
        <div>
          <h2>{{ regimeEnEdition ? 'Modifier le régime' : 'Nouveau régime' }}</h2>
          <p class="mt-1 text-sm text-muted-color">Les horaires précis se configurent après la création du régime.</p>
        </div>
        <PrimeButton v-if="regimeEnEdition" label="Annuler" severity="secondary" text @click="reinitialiserRegime" />
      </div>
      <div class="grid gap-3 md:grid-cols-3">
        <PrimeInputText v-model="regimeForm.code" placeholder="Code (ex. STANDARD)" />
        <PrimeInputText v-model="regimeForm.libelle" placeholder="Libellé" />
        <PrimeSelect v-model="regimeForm.type" :options="types" option-label="label" option-value="value" placeholder="Type" />
        <PrimeInputText v-model="regimeForm.heuresHebdo" type="number" placeholder="Heures / semaine" />
        <PrimeInputText v-model="regimeForm.joursTravailles" placeholder="Jours (LUNDI,MARDI,...)" />
        <PrimeInputText v-model="regimeForm.pauseMinutes" type="number" placeholder="Pause (minutes)" />
        <PrimeInputText v-model="regimeForm.toleranceRetardMin" type="number" placeholder="Tolérance retard (min)" />
        <PrimeInputText v-model="regimeForm.toleranceDepartMin" type="number" placeholder="Tolérance départ (min)" />
        <PrimeButton :label="regimeEnEdition ? 'Enregistrer' : 'Créer le régime'" icon="pi pi-save" @click="enregistrerRegime" />
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="mb-3">Régimes disponibles</h2>
      <DataTable :value="regimes" :loading="chargement" striped-rows responsive-layout="scroll">
        <Column field="code" header="Code" />
        <Column field="libelle" header="Libellé" />
        <Column header="Type"><template #body="{ data }">{{ libelleType(data.type) }}</template></Column>
        <Column field="heuresHebdo" header="H/sem." />
        <Column field="joursTravailles" header="Jours" />
        <Column header="Horaires"><template #body="{ data }">{{ data.horaires?.length || 0 }}</template></Column>
        <Column header="Actions">
          <template #body="{ data }">
            <div class="flex flex-wrap gap-1">
              <PrimeButton label="Horaires" size="small" severity="secondary" text @click="chargerHoraires(data)" />
              <template v-if="auth.estRh || auth.estDirection">
                <PrimeButton icon="pi pi-pencil" size="small" text rounded aria-label="Modifier" @click="editerRegime(data)" />
                <PrimeButton icon="pi pi-trash" size="small" severity="danger" text rounded aria-label="Supprimer" @click="supprimerRegime(data)" />
              </template>
            </div>
          </template>
        </Column>
      </DataTable>
    </section>

    <section v-if="regimeSelectionne" class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="mb-3">Plages horaires du régime #{{ regimeSelectionne }}</h2>
      <div v-if="auth.estRh || auth.estDirection" class="mb-4 grid gap-3 md:grid-cols-6">
        <PrimeSelect v-model="horaireForm.jourSemaine" :options="jours" placeholder="Jour" />
        <PrimeInputText v-model="horaireForm.heureDebut" type="time" />
        <PrimeInputText v-model="horaireForm.heureFin" type="time" />
        <PrimeInputText v-model="horaireForm.pauseDebut" type="time" />
        <PrimeInputText v-model="horaireForm.pauseFin" type="time" />
        <PrimeButton label="Ajouter" icon="pi pi-plus" @click="ajouterHoraire" />
      </div>
      <DataTable :value="horaires" striped-rows size="small" responsive-layout="scroll">
        <Column field="jourSemaine" header="Jour" />
        <Column field="heureDebut" header="Début" />
        <Column field="heureFin" header="Fin" />
        <Column field="pauseDebut" header="Pause début" />
        <Column field="pauseFin" header="Pause fin" />
        <Column v-if="auth.estRh || auth.estDirection" header="">
          <template #body="{ data }">
            <PrimeButton icon="pi pi-trash" severity="danger" text rounded aria-label="Supprimer" @click="supprimerHoraire(data)" />
          </template>
        </Column>
      </DataTable>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <h2 class="mb-3">Affectation d’un régime</h2>
      <div class="grid gap-3 md:grid-cols-5">
        <PrimeInputText v-model="idEmploye" placeholder="ID employé" inputmode="numeric" @keyup.enter="chargerAffectations" />
        <PrimeSelect v-model="affectationForm.idRegime" :options="regimesOptions" option-label="label" option-value="value" placeholder="Régime" />
        <PrimeInputText v-model="affectationForm.dateDebut" type="date" />
        <PrimeInputText v-model="affectationForm.dateFin" type="date" />
        <div class="flex items-center gap-2">
          <label class="flex items-center gap-2 text-sm">
            <input v-model="affectationForm.appliquerPlanning" type="checkbox" /> Générer le planning
          </label>
        </div>
      </div>
      <div class="mt-3 flex gap-2">
        <PrimeButton label="Charger les affectations" icon="pi pi-search" severity="secondary" @click="chargerAffectations" />
        <PrimeButton v-if="auth.estRh || auth.estDirection" label="Affecter" icon="pi pi-check" @click="affecterRegime" />
      </div>
      <DataTable :value="affectations" class="mt-4" striped-rows size="small" responsive-layout="scroll">
        <Column field="idEmploye" header="Employé" />
        <Column field="codeRegime" header="Régime" />
        <Column field="libelleRegime" header="Libellé" />
        <Column field="dateDebut" header="Du" />
        <Column field="dateFin" header="Au" />
        <Column field="idPlanning" header="Planning" />
      </DataTable>
    </section>
  </div>
</template>
