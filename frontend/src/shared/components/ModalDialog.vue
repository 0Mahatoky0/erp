<script setup>
import Dialog from 'primevue/dialog'

const props = defineProps({
  titre: { type: String, required: true },
  sousTitre: { type: String, default: null },
  large: { type: Boolean, default: false },
  fermable: { type: Boolean, default: true },
})

const emit = defineEmits(['fermer'])
</script>

<template>
  <Dialog
    :visible="true"
    :header="props.titre"
    :modal="true"
    :closable="props.fermable"
    :dismissable-mask="props.fermable"
    :style="{ width: props.large ? 'min(900px, 95vw)' : 'min(560px, 95vw)' }"
    @update:visible="(visible) => !visible && emit('fermer')"
  >
    <p v-if="props.sousTitre" class="texte-discret">{{ props.sousTitre }}</p>
    <slot />
    <template v-if="$slots.actions" #footer>
      <slot name="actions" />
    </template>
  </Dialog>
</template>
