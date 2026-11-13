<script setup>
import { reactive, ref } from 'vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { recrutementApi as api, ErreurApi } from '../api/recrutementApi'

const props = defineProps({
  idCandidature: { type: Number, required: true },
})

const emit = defineEmits(['saved'])
const notifications = useNotificationStore()
const saving = ref(false)

const decision = reactive({
  decision: 'RETENUE',
  motif: '',
})

async function enregistrerDecision() {
  saving.value = true
  try {
    await api.post('/recrutement/decisions', {
      idCandidature: props.idCandidature,
      ...decision,
    })
    notifications.succes('Décision enregistrée.')
    decision.motif = ''
    emit('saved')
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible d’enregistrer la décision.')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <Card>
    <template #title>Décision finale</template>
    <template #content>
      <form class="grid gap-3" @submit.prevent="enregistrerDecision">
        <Select v-model="decision.decision" :options="['RETENUE', 'LISTE_ATTENTE', 'REFUSEE']" />
        <Textarea v-model="decision.motif" rows="4" placeholder="Motif obligatoire" required />
        <Button type="submit" label="Enregistrer la décision" icon="pi pi-check" severity="success" :loading="saving" />
      </form>
    </template>
  </Card>
</template>