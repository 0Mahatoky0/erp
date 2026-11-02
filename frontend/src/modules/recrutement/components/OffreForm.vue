<script setup>
import { ref } from 'vue'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import ModalDialog from '@/shared/components/ModalDialog.vue'

const props = defineProps({
  offre: { type: Object, default: null }, // null = création
  idDemandePoste: { type: [String, Number], default: '' },
  saving: Boolean,
})
const emit = defineEmits(['save', 'fermer'])

const champs = [
  { key: 'idDemandePoste', label: 'Demande validée (ID)', attrs: { type: 'number', min: 1, required: true } },
  { key: 'reference', label: 'Référence', attrs: { required: true, maxlength: 50 } },
  { key: 'titre', label: 'Titre', attrs: { required: true, maxlength: 150 } },
  { key: 'typeContrat', label: 'Type de contrat', attrs: { maxlength: 50 } },
  { key: 'effectifRecherche', label: 'Effectif recherché', attrs: { type: 'number', min: 1, required: true } },
  { key: 'modeSelection', label: 'Mode de sélection', attrs: { required: true, placeholder: 'CV, entretien, test' } },
  { key: 'salaireMin', label: 'Salaire min', attrs: { type: 'number', min: 0, step: 0.01 } },
  { key: 'salaireMax', label: 'Salaire max', attrs: { type: 'number', min: 0, step: 0.01 } },
  { key: 'dateLimiteCandidature', label: 'Date limite', attrs: { type: 'datetime-local' } },
  { key: 'lieuTravail', label: 'Lieu', attrs: { maxlength: 255 } },
]

const textes = [
  { key: 'mt-1 text-sm text-muted-color', label: 'Description' },
  { key: 'profilRecherche', label: 'Profil recherché' },
]

const form = ref(
  props.offre
    ? { ...props.offre, dateLimiteCandidature: props.offre.dateLimiteCandidature?.slice(0, 16) ?? null }
    : {
        idDemandePoste: props.idDemandePoste ?? '',
        reference: '', titre: '', description: '', profilRecherche: '',
        lieuTravail: '', typeContrat: '', effectifRecherche: 1,
        salaireMin: null, salaireMax: null, modeSelection: '', dateLimiteCandidature: null,
      },
)

const submit = () => emit('save', { ...form.value, idDemandePoste: Number(form.value.idDemandePoste) })
</script>

<template>
  <ModalDialog
    :titre="offre ? 'Modifier l’offre brouillon' : 'Créer une offre'"
    large
    @fermer="emit('fermer')"
  >
    <form id="offre-form" class="flex flex-col gap-3" @submit.prevent="submit">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4 gap-y-3">
        <label v-for="c in champs" :key="c.key" class="flex flex-col gap-1 text-sm font-semibold">
          {{ c.label }}
          <PrimeInputText v-model="form[c.key]" v-bind="c.attrs" />
        </label>
      </div>

      <label v-for="t in textes" :key="t.key" class="flex flex-col gap-1 text-sm font-semibold">
        {{ t.label }}
        <PrimeTextarea v-model="form[t.key]" required rows="4" auto-resize />
      </label>
    </form>

    <template #actions>
      <PrimeButton label="Annuler" severity="secondary" text @click="emit('fermer')" />
      <!-- hors du <form> : l'attribut form le relie quand même -->
      <PrimeButton type="submit" form="offre-form" label="Enregistrer" icon="pi pi-check" :loading="saving" />
    </template>
  </ModalDialog>
</template>
