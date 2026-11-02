<script setup>
import { computed } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import AppSidebar from '@/shared/components/AppSidebar.vue'
import AppTopbar from '@/shared/components/AppTopbar.vue'
import NotificationContainer from '@/shared/components/NotificationContainer.vue'
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
  <div class="flex min-h-screen bg-surface-50 dark:bg-surface-950">
    <AppSidebar />

    <div class="flex min-w-0 flex-1 flex-col">
      <AppTopbar :titre="titre" @deconnexion="deconnexion" />

      <main class="flex-1 p-4 md:p-6">
        <div class="mx-auto max-w-[90rem]">
          <RouterView v-slot="{ Component, route: r }">
            <Transition name="page" mode="out-in">
              <component :is="Component" :key="r.path" />
            </Transition>
          </RouterView>
        </div>
      </main>
    </div>

    <NotificationContainer />
  </div>
</template>

<style scoped>
.page-enter-active,
.page-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-leave-to {
  opacity: 0;
}
</style>