<script setup>
import { watch } from 'vue'
import Toast from 'primevue/toast'
import { useToast } from 'primevue/usetoast'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const notifications = useNotificationStore()
const toast = useToast()
const dejaAffiches = new Set()

watch(
  () => notifications.messages,
  (messages) => {
    messages.forEach((message) => {
      if (dejaAffiches.has(message.id)) return
      dejaAffiches.add(message.id)
      toast.add({
        severity: message.type === 'succes' ? 'success' : message.type === 'erreur' ? 'error' : message.type,
        summary: message.titre || undefined,
        detail: message.texte,
        life: message.type === 'erreur' ? 8000 : 5000,
      })
    })
  },
  { deep: true },
)
</script>

<template>
  <Toast position="top-right" />
</template>
