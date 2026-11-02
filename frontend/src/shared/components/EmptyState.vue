<script setup>
/** Message affiche quand une liste est vide. */
defineProps({
  titre: { type: String, default: 'Aucun resultat' },
  message: { type: String, default: 'Aucun element a afficher pour le moment.' },
})
</script>

<template>
  <div class="etat-vide">
    <div class="etat-vide__titre">{{ titre }}</div>
    <div>{{ message }}</div>
    <div style="margin-top: 12px">
      <slot />
    </div>
  </div>
</template>
