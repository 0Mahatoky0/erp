<script setup>
import { computed, reactive, watch } from 'vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDateHeure } from '@/shared/utils/format'
import { recrutementApi as api, ErreurApi } from '../api/recrutementApi'
import { APPRECIATIONS, TYPES_EVALUATION } from '../utils/notes'

const props = defineProps({
  idCandidature: { type: Number, required: true },
  entretiens: { type: Array, default: () => [] },
  passages: { type: Array, default: () => [] },
  tests: { type: Array, default: () => [] },
})

const emit = defineEmits(['saved'])
const notifications = useNotificationStore()

const saving = reactive({ etat: false })
const evaluation = reactive({
  typeEvaluation: 'DOSSIER',
  idEntretien: null,
  idPassage: null,
  note: null,
  noteMax: 20,
  appreciation: null,
  commentaire: '',
})

watch(
  () => evaluation.typeEvaluation,
  (nouveauType) => {
    if (nouveauType !== 'ENTRETIEN') evaluation.idEntretien = null
    if (nouveauType !== 'TEST') evaluation.idPassage = null
  },
)

const titresTests = computed(() => Object.fromEntries(props.tests.map((t) => [t.idTest, t.titre])))

const optionsEntretiens = computed(() =>
  props.entretiens.map((e) => ({
    valeur: e.idEntretien,
    libelle: `${e.typeEntretien} · ${formatDateHeure(e.dateDebut)}`,
  })),
)

const optionsPassages = computed(() =>
  props.passages.map((p) => ({
    valeur: p.idPassage,
    libelle: `${titresTests.value[p.idTest] || `Test n°${p.idTest}`} · tentative ${p.numeroTentative}`,
  })),
)

async function enregistrerEvaluation() {
  if (!evaluation.appreciation) {
    notifications.erreur('Choisissez une appréciation.')
    return
  }
  if (evaluation.typeEvaluation === 'ENTRETIEN' && !evaluation.idEntretien) {
    notifications.erreur('Choisissez l’entretien évalué.')
    return
  }
  if (evaluation.typeEvaluation === 'TEST' && !evaluation.idPassage) {
    notifications.erreur('Choisissez le passage de test évalué.')
    return
  }

  saving.etat = true
  try {
    await api.post('/recrutement/evaluations', {
      idCandidature: props.idCandidature,
      typeEvaluation: evaluation.typeEvaluation,
      idEntretien: evaluation.typeEvaluation === 'ENTRETIEN' ? evaluation.idEntretien : null,
      idPassage: evaluation.typeEvaluation === 'TEST' ? evaluation.idPassage : null,
      note: evaluation.note === null ? null : Number(evaluation.note),
      noteMax: Number(evaluation.noteMax),
      appreciation: evaluation.appreciation,
      commentaire: evaluation.commentaire,
    })
    notifications.succes('Évaluation enregistrée.')
    evaluation.idEntretien = null
    evaluation.idPassage = null
    evaluation.note = null
    evaluation.appreciation = null
    evaluation.commentaire = ''
    emit('saved')
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible d’enregistrer l’évaluation.')
  } finally {
    saving.etat = false
  }
}
</script>

<template>
  <Card>
    <template #title>Nouvelle évaluation</template>
    <template #content>
      <form class="grid gap-3" @submit.prevent="enregistrerEvaluation">
        <Select
          v-model="evaluation.typeEvaluation"
          :options="TYPES_EVALUATION"
          option-label="libelle"
          option-value="valeur"
          placeholder="Type d’évaluation"
        />

        <template v-if="evaluation.typeEvaluation === 'ENTRETIEN'">
          <Select
            v-model="evaluation.idEntretien"
            :options="optionsEntretiens"
            option-label="libelle"
            option-value="valeur"
            placeholder="Entretien évalué"
          />
          <p v-if="!optionsEntretiens.length" class="m-0 text-sm text-muted-color">
            Aucun entretien n’est planifié pour ce candidat.
          </p>
        </template>

        <template v-if="evaluation.typeEvaluation === 'TEST'">
          <Select
            v-model="evaluation.idPassage"
            :options="optionsPassages"
            option-label="libelle"
            option-value="valeur"
            placeholder="Passage de test évalué"
          />
          <p v-if="!optionsPassages.length" class="m-0 text-sm text-muted-color">
            Aucun passage de test n’est planifié pour ce candidat.
          </p>
        </template>

        <div class="grid grid-cols-2 gap-3">
          <InputNumber
            v-model="evaluation.note"
            :min="0"
            :max="evaluation.noteMax"
            :min-fraction-digits="0"
            :max-fraction-digits="2"
            placeholder="Note"
          />
          <InputNumber v-model="evaluation.noteMax" :min="1" :max-fraction-digits="2" placeholder="Note maximale" />
        </div>

        <Select
          v-model="evaluation.appreciation"
          :options="APPRECIATIONS"
          option-label="libelle"
          option-value="valeur"
          placeholder="Appréciation"
        />

        <Textarea v-model="evaluation.commentaire" rows="4" placeholder="Commentaire obligatoire" required />

        <Button type="submit" label="Enregistrer l’évaluation" icon="pi pi-save" :loading="saving.etat" />
      </form>
    </template>
  </Card>
</template>