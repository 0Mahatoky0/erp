<script setup>
import { computed } from 'vue'

/**
 * Fenetre modale : titre, contenu et actions de validation.
 * La fermeture par la touche Echap et le clic sur le fond sont geres ici.
 */
const props = defineProps({
  titre: { type: String, required: true },
  sousTitre: { type: String, default: null },
  large: { type: Boolean, default: false },
  fermable: { type: Boolean, default: true },
})

const emit = defineEmits(['fermer'])

function surFond(event) {
  if (props.fermable && event.target === event.currentTarget) {
    emit('fermer')
  }
}

function surEchec(event) {
  if (props.fermable && event.key === 'Escape') {
    emit('fermer')
  }
}

const classes = computed(() => (props.large ? 'modale modale--large' : 'modale'))
</script>

<template>
  <div class="modale-fond" role="dialog" aria-modal="true" @click="surFond" @keydown="surEchec">
    <div :class="classes">
      <div class="modale__entete">
        <div>
          <h2>{{ titre }}</h2>
          <p v-if="sousTitre" class="texte-discret" style="margin: 4px 0 0">{{ sousTitre }}</p>
        </div>
        <button v-if="fermable" class="modale__fermer" type="button" aria-label="Fermer" @click="emit('fermer')">
          ×
        </button>
      </div>

      <div class="modale__corps">
        <slot />
      </div>

      <div v-if="$slots.actions" class="modale__pied">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>
