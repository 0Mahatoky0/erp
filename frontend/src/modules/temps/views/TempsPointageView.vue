<script setup>
import { onMounted, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'

import { tempsApi } from '@/modules/temps/api/tempsApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const notifications = useNotificationStore()
const idEmploye = ref('')
const mois = ref(new Date().toISOString().slice(0, 7))
const planning = ref([])
const pointages = ref([])
const resume = ref(null)
const chargement = ref(false)
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

function formatHeure(value) {
  return value ? value.slice(0, 5) : '—'
}

onMounted(() => {
  idEmploye.value = ''
})
</script>

<template>
  <div class="flex flex-col gap-5">
    <div>
      <h1>Temps de travail et pointage</h1>
      <p class="mt-1 text-sm text-muted-color">Definir les horaires puis enregistrer les entrees et sorties.</p>
    </div>

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
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div><h2>Pointage du mois</h2><p class="mt-1 text-sm text-muted-color">Entrees, sorties, retards et heures supplementaires.</p></div>
        <div v-if="resume" class="flex flex-wrap gap-3 text-sm"><span>{{ resume.joursPointes }} jour(s)</span><span>{{ resume.minutesRetard }} min de retard</span><span>{{ resume.minutesSupplementaires }} min supplementaires</span></div>
      </div>
      <div v-if="resume?.conges?.length" class="mb-4 rounded-lg bg-primary/10 p-3 text-sm">
        <strong>Congés validés sur la période :</strong>
        <span v-for="conge in resume.conges" :key="`${conge.dateDebut}-${conge.dateFin}`" class="ml-2">
          {{ conge.typeConge }} ({{ conge.dateDebut }} → {{ conge.dateFin }})
        </span>
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
