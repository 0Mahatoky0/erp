<script setup>
import { onMounted, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'

import { tempsApi } from '@/modules/temps/api/tempsApi'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { useAuthStore } from '@/shared/store/useAuthStore'

const notifications = useNotificationStore()
const auth = useAuthStore()
const idEmploye = ref('')
const periodesCloturees = ref([])
const mois = ref(new Date().toISOString().slice(0, 7))
const planning = ref([])
const pointages = ref([])
const resume = ref(null)
const chargement = ref(false)
const dateJour = ref(new Date().toISOString().slice(0, 10))
const jour = ref(null)
const semaine = ref(new Date().toISOString().slice(0, 10))
const heureSup = ref(null)
const planningForm = ref({
  joursTravailles: 'LUNDI,MARDI,MERCREDI,JEUDI,VENDREDI',
  heureDebut: '08:00',
  heureFin: '17:00',
  dateDebut: new Date().toISOString().slice(0, 10),
  dateFin: '',
})

function employeRequis() {
  if (!idEmploye.value.trim()) {
    notifications.erreur('Saisissez l’identifiant de l’employe.')
    return null
  }
  return Number(idEmploye.value)
}

async function charger() {
  const id = employeRequis()
  if (!id) return
  chargement.value = true
  try {
    const [plannings, moisEmploye] = await Promise.all([
      tempsApi.listerPlannings(id),
      tempsApi.consulterMois(id, mois.value),
    ])
    planning.value = plannings
    resume.value = moisEmploye
    pointages.value = moisEmploye.pointages
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

async function enregistrerPlanning() {
  const id = employeRequis()
  if (!id) return
  try {
    await tempsApi.creerPlanning({
      idEmploye: id,
      ...planningForm.value,
      dateFin: planningForm.value.dateFin || null,
    })
    notifications.succes('Les horaires de travail ont ete enregistres.')
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function pointer(action) {
  const id = employeRequis()
  if (!id) return
  try {
    await tempsApi[action]({ idEmploye: id })
    notifications.succes(action === 'pointerEntree' ? 'Entree enregistree.' : 'Sortie enregistree.')
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function calculerJour() {
  const id = employeRequis()
  if (!id) return
  try {
    jour.value = await tempsApi.calculerJour(id, dateJour.value)
    if (!jour.value) notifications.erreur('Aucun planning ni pointage pour cette journee.')
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function validerJour() {
  const id = employeRequis()
  if (!id) return
  try {
    jour.value = await tempsApi.validerJour(id, dateJour.value)
    notifications.succes('Journee validee.')
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function traiterAnomalie(anomalie, statut) {
  try {
    await tempsApi.traiterAnomalie(anomalie.id, { statut, resolution: 'Traitee par le manager.' })
    notifications.succes('Anomalie mise a jour.')
    await calculerJour()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function calculerSemaine() {
  const id = employeRequis()
  if (!id) return
  try {
    heureSup.value = await tempsApi.calculerSemaine(id, semaine.value)
    notifications.succes('Heures supplementaires calculees.')
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function chargerPeriodes() {
  try {
    periodesCloturees.value = await tempsApi.periodesCloturees()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function cloturerPeriode() {
  const [annee, moisCloture] = mois.value.split('-').map(Number)
  try {
    await tempsApi.cloturerPeriode(annee, moisCloture)
    notifications.succes('La periode a ete cloturee.')
    await chargerPeriodes()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

function formatHeure(value) {
  return value ? value.slice(0, 5) : '—'
}

onMounted(() => {
  idEmploye.value = ''
  if (auth.estRh || auth.estDirection) chargerPeriodes()
})
</script>

<template>
  <div class="flex flex-col gap-5">
    <div>
      <h1>Temps de travail et pointage</h1>
      <p class="mt-1 text-sm text-muted-color">Definir les horaires puis enregistrer les entrees et sorties.</p>
    </div>

    <section v-if="auth.estRh || auth.estDirection" class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2>Clôture des périodes</h2>
          <p class="text-sm text-muted-color">Une période clôturée interdit tout nouveau pointage ou correction.</p>
        </div>
        <PrimeButton label="Clôturer le mois affiché" icon="pi pi-lock" severity="secondary" @click="cloturerPeriode" />
      </div>
      <DataTable :value="periodesCloturees" class="mt-4" striped-rows size="small">
        <Column header="Période"><template #body="{ data }">{{ data.annee }}-{{ String(data.mois).padStart(2, '0') }}</template></Column>
        <Column header="Du"><template #body="{ data }">{{ data.dateDebut }}</template></Column>
        <Column header="Au"><template #body="{ data }">{{ data.dateFin }}</template></Column>
        <Column header="Clôturée le"><template #body="{ data }">{{ data.dateCloture }}</template></Column>
      </DataTable>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="flex flex-col gap-3 md:flex-row md:items-end">
        <div class="flex flex-col gap-2">
          <label for="id-employe" class="text-sm font-semibold text-muted-color">Identifiant employe</label>
          <PrimeInputText id="id-employe" v-model="idEmploye" inputmode="numeric" placeholder="Ex. 12" @keyup.enter="charger" />
        </div>
        <div class="flex flex-col gap-2">
          <label for="mois-pointage" class="text-sm font-semibold text-muted-color">Mois</label>
          <PrimeInputText id="mois-pointage" v-model="mois" type="month" @change="charger" />
        </div>
        <PrimeButton label="Afficher" icon="pi pi-search" @click="charger" />
        <PrimeButton label="Entree" icon="pi pi-sign-in" severity="secondary" variant="outlined" @click="pointer('pointerEntree')" />
        <PrimeButton label="Sortie" icon="pi pi-sign-out" severity="secondary" variant="outlined" @click="pointer('pointerSortie')" />
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="mb-4">
        <h2>Horaires de travail</h2>
        <p class="mt-1 text-sm text-muted-color">Les nouvelles dates conservent l’historique des horaires precedents.</p>
      </div>
      <div class="grid gap-3 md:grid-cols-5">
        <div class="flex flex-col gap-2 md:col-span-2">
          <label for="jours-travailles" class="text-sm font-semibold text-muted-color">Jours travailles</label>
          <PrimeInputText id="jours-travailles" v-model="planningForm.joursTravailles" />
        </div>
        <div class="flex flex-col gap-2"><label for="heure-debut" class="text-sm font-semibold text-muted-color">Debut</label><PrimeInputText id="heure-debut" v-model="planningForm.heureDebut" type="time" /></div>
        <div class="flex flex-col gap-2"><label for="heure-fin" class="text-sm font-semibold text-muted-color">Fin</label><PrimeInputText id="heure-fin" v-model="planningForm.heureFin" type="time" /></div>
        <div class="flex flex-col gap-2"><label for="date-debut" class="text-sm font-semibold text-muted-color">Applicable le</label><PrimeInputText id="date-debut" v-model="planningForm.dateDebut" type="date" /></div>
      </div>
      <PrimeButton class="mt-4" label="Enregistrer les horaires" icon="pi pi-save" @click="enregistrerPlanning" />
      <DataTable v-if="planning.length" :value="planning" class="mt-4" striped-rows>
        <Column field="joursTravailles" header="Jours" />
        <Column header="Horaire"><template #body="{ data }">{{ formatHeure(data.heureDebut) }} - {{ formatHeure(data.heureFin) }}</template></Column>
        <Column field="dateDebut" header="Debut" />
        <Column field="dateFin" header="Fin" />
      </DataTable>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="mb-4"><h2>Synthese journaliere</h2><p class="mt-1 text-sm text-muted-color">Calcul du temps travaille, retards, heures supplementaires et anomalies.</p></div>
      <div class="flex flex-wrap items-end gap-3">
        <div class="flex flex-col gap-2"><label for="date-jour" class="text-sm font-semibold text-muted-color">Journee</label><PrimeInputText id="date-jour" v-model="dateJour" type="date" /></div>
        <PrimeButton label="Calculer" icon="pi pi-calculator" severity="secondary" variant="outlined" @click="calculerJour" />
        <PrimeButton label="Valider la journee" icon="pi pi-check" :disabled="!jour" @click="validerJour" />
      </div>
      <div v-if="jour" class="mt-4 grid gap-3 md:grid-cols-4">
        <div class="rounded border border-surface p-3"><div class="text-xs text-muted-color">Travaille / theorique</div><div class="font-semibold">{{ jour.minutesTravaillees }} / {{ jour.minutesTheoriques }} min</div></div>
        <div class="rounded border border-surface p-3"><div class="text-xs text-muted-color">Retard</div><div class="font-semibold">{{ jour.minutesRetard }} min</div></div>
        <div class="rounded border border-surface p-3"><div class="text-xs text-muted-color">Heures sup.</div><div class="font-semibold">{{ jour.minutesHs }} min</div></div>
        <div class="rounded border border-surface p-3"><div class="text-xs text-muted-color">Statut</div><StatusBadge :valeur="jour.statut" /></div>
      </div>
      <DataTable v-if="jour?.anomalies?.length" :value="jour.anomalies" class="mt-4" striped-rows>
        <Column field="type" header="Anomalie" />
        <Column field="gravite" header="Gravite" />
        <Column header="Statut"><template #body="{ data }"><StatusBadge :valeur="data.statut" /></template></Column>
        <Column header="Traitement"><template #body="{ data }"><div v-if="data.statut === 'OUVERTE'" class="flex gap-2"><PrimeButton size="small" label="Justifier" @click="traiterAnomalie(data, 'JUSTIFIEE')" /><PrimeButton size="small" label="Rejeter" severity="danger" variant="outlined" @click="traiterAnomalie(data, 'REJETEE')" /></div></template></Column>
      </DataTable>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="mb-4"><h2>Heures supplementaires hebdomadaires</h2><p class="mt-1 text-sm text-muted-color">Calcul par semaine civile au-dela de 40 h, avec alerte au plafond de 20 h.</p></div>
      <div class="flex flex-wrap items-end gap-3">
        <div class="flex flex-col gap-2"><label for="semaine" class="text-sm font-semibold text-muted-color">Semaine</label><PrimeInputText id="semaine" v-model="semaine" type="date" /></div>
        <PrimeButton label="Calculer la semaine" icon="pi pi-calendar" @click="calculerSemaine" />
      </div>
      <div v-if="heureSup" class="mt-4 grid gap-3 md:grid-cols-4">
        <div class="rounded border border-surface p-3"><div class="text-xs text-muted-color">Total travaille</div><div class="font-semibold">{{ heureSup.minutesTotal }} min</div></div>
        <div class="rounded border border-surface p-3"><div class="text-xs text-muted-color">HS 30 %</div><div class="font-semibold">{{ heureSup.minutesHs30 }} min</div></div>
        <div class="rounded border border-surface p-3"><div class="text-xs text-muted-color">HS 50 %</div><div class="font-semibold">{{ heureSup.minutesHs50 }} min</div></div>
        <div class="rounded border border-surface p-3"><div class="text-xs text-muted-color">Plafond</div><div class="font-semibold">{{ heureSup.alertePlafond ? 'Plafond depasse' : 'Sous le plafond' }}</div></div>
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div><h2>Pointage du mois</h2><p class="mt-1 text-sm text-muted-color">Entrees, sorties, retards et heures supplementaires.</p></div>
        <div v-if="resume" class="flex flex-wrap gap-3 text-sm"><span>{{ resume.joursPointes }} jour(s)</span><span>{{ resume.minutesRetard }} min de retard</span><span>{{ resume.minutesSupplementaires }} min supplementaires</span></div>
      </div>
      <div v-if="chargement" class="text-sm text-muted-color">Chargement...</div>
      <DataTable v-else :value="pointages" striped-rows paginator :rows="10" responsive-layout="scroll">
        <Column field="date" header="Jour" />
        <Column header="Entree"><template #body="{ data }">{{ formatHeure(data.heureEntree) }}</template></Column>
        <Column header="Sortie"><template #body="{ data }">{{ formatHeure(data.heureSortie) }}</template></Column>
        <Column field="minutesTravaillees" header="Temps travaille (min)" />
        <Column field="minutesRetard" header="Retard (min)" />
        <Column field="minutesSupplementaires" header="Suppl. (min)" />
      </DataTable>
    </section>
  </div>
</template>
