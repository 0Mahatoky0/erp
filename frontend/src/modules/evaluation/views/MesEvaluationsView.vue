<script setup>
import { onMounted, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeTextarea from 'primevue/textarea'
import Tag from 'primevue/tag'
import { evaluationApi as api } from '@/modules/evaluation/api/evaluationApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const notifications = useNotificationStore()
const evaluations = ref([])
const evaluation = ref(null)
const objectifs = ref([])
const competences = ref([])
const chargement = ref(false)
const autoForm = ref({ pointsForts: '', axesProgres: '', commentaire: '' })
const signatureForm = ref({ commentaire: '', signer: true })
const contestationForm = ref({ motif: '' })

async function charger() {
  chargement.value = true
  try {
    evaluations.value = await api.mesEvaluations()
    if (evaluations.value.length) await selectionner(evaluations.value[0])
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

async function selectionner(ligne) {
  evaluation.value = ligne
  const [objectifsResultat, competencesResultat] = await Promise.all([
    api.objectifs(ligne.idCampagne, ligne.idEmploye),
    api.employeCompetences(ligne.idEmploye),
  ])
  objectifs.value = objectifsResultat
  competences.value = competencesResultat
}

async function action(promise, message) {
  try {
    await promise
    notifications.succes(message)
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

function severity(statut) {
  if (['CLOTUREE', 'ATTEINT', 'VALIDE'].includes(statut)) return 'success'
  if (['CONTESTEE', 'NON_ATTEINT'].includes(statut)) return 'danger'
  if (['SIGNEE', 'PARTIEL'].includes(statut)) return 'info'
  return 'warn'
}

onMounted(charger)
</script>

<template>
  <div class="flex flex-col gap-5">
    <div>
      <h1>Mes évaluations</h1>
      <p class="mt-1 text-sm text-muted-color">Objectifs, auto-évaluation, signature et suivi des compétences.</p>
    </div>

    <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
      <div class="flex items-center justify-between">
        <h2>Historique</h2>
        <PrimeButton icon="pi pi-refresh" text rounded aria-label="Actualiser" @click="charger" />
      </div>
      <DataTable :value="evaluations" :loading="chargement" striped-rows class="mt-3" selection-mode="single"
        :selection="evaluation" @update:selection="selectionner">
        <Column field="idCampagne" header="Campagne" />
        <Column field="statut" header="Statut">
          <template #body="{ data }"><Tag :value="data.statut" :severity="severity(data.statut)" /></template>
        </Column>
        <Column field="noteGlobale" header="Note" />
        <Column field="appreciation" header="Appréciation" />
        <Column field="version" header="Version" />
      </DataTable>
    </section>

    <template v-if="evaluation">
      <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <h2>Auto-évaluation</h2>
        <div class="mt-3 grid gap-3">
          <PrimeTextarea v-model="autoForm.pointsForts" rows="3" placeholder="Points forts" />
          <PrimeTextarea v-model="autoForm.axesProgres" rows="3" placeholder="Axes de progrès" />
          <PrimeTextarea v-model="autoForm.commentaire" rows="3" placeholder="Commentaire" />
          <PrimeButton label="Enregistrer" icon="pi pi-save" class="w-fit"
            :disabled="!['AUTO_EVALUATION', 'EVALUATION_MANAGER', 'ENTRETIEN'].includes(evaluation.statut)"
            @click="action(api.autoEvaluer(evaluation.id, autoForm), 'Auto-évaluation enregistrée.')" />
        </div>
      </section>

      <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <h2>Signature</h2>
        <div class="mt-3 grid gap-3 md:grid-cols-2">
          <PrimeTextarea v-model="signatureForm.commentaire" rows="3" placeholder="Commentaire éventuel" />
          <div class="flex flex-col gap-3">
            <PrimeTextarea v-model="contestationForm.motif" rows="2" placeholder="Motif de contestation" />
            <div class="flex gap-2">
              <PrimeButton label="Signer" icon="pi pi-check"
                :disabled="!['N2_VALIDE', 'VALIDEE_N2'].includes(evaluation.statut)"
                @click="action(api.signer(evaluation.id, signatureForm), 'Évaluation signée.')" />
              <PrimeButton label="Contester" icon="pi pi-exclamation-triangle" outlined
                :disabled="evaluation.statut !== 'SIGNEE'"
                @click="action(api.contester(evaluation.id, contestationForm), 'Contestation enregistrée.')" />
            </div>
          </div>
        </div>
      </section>

      <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <h2>Objectifs</h2>
        <DataTable :value="objectifs" striped-rows class="mt-3">
          <Column field="libelle" header="Objectif" />
          <Column field="valeurCible" header="Cible" />
          <Column field="valeurRealisee" header="Réalisé" />
          <Column field="tauxAtteinte" header="Taux" />
          <Column field="statut" header="Statut">
            <template #body="{ data }"><Tag :value="data.statut" :severity="severity(data.statut)" /></template>
          </Column>
        </DataTable>
      </section>

      <section class="rounded-lg border border-surface bg-surface-0 p-4 shadow-sm dark:bg-surface-900">
        <h2>Mes compétences</h2>
        <DataTable :value="competences" striped-rows class="mt-3">
          <Column field="idCompetence" header="Compétence" />
          <Column field="niveauAcquis" header="Niveau" />
          <Column field="dateEvaluation" header="Évaluée le" />
          <Column field="source" header="Source" />
        </DataTable>
      </section>
    </template>

    <p v-else class="rounded-lg border border-surface bg-surface-0 p-6 text-center text-sm text-muted-color">
      Aucune évaluation disponible pour le moment.
    </p>
  </div>
</template>
