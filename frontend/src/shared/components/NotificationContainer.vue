<script setup>
import { useNotificationStore } from '@/stores/notification'

const notifications = useNotificationStore()
</script>

<template>
  <div class="notifications">
    <div
      v-for="message in notifications.messages"
      :key="message.id"
      class="notification"
      :class="`notification--${message.type}`"
    >
      <div class="notification__texte">
        <div v-if="message.titre" class="notification__titre">{{ message.titre }}</div>
        <div>{{ message.texte }}</div>
      </div>
      <button class="notification__fermer" type="button" aria-label="Fermer" @click="notifications.fermer(message.id)">
        ×
      </button>
    </div>
  </div>
</template>
