<script setup>
import { computed } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import AppSidebar from '@/shared/components/AppSidebar.vue'
import AppTopbar from '@/shared/components/AppTopbar.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const titre = computed(() => route.meta.titre || 'ERP TOVO')

function deconnexion() {
  auth.deconnexion()
  router.push({ name: 'connexion' })
}
</script>

<template>
  <div class="min-h-screen flex flex-column surface-ground">
    <AppTopbar :titre="titre" @deconnexion="deconnexion" />
    <AppSidebar />
    <main class="flex-1 p-3 md:p-4">
        <RouterView v-slot="{ Component }">
          <component :is="Component" />
        </RouterView>
    </main>
  </div>
</template>
