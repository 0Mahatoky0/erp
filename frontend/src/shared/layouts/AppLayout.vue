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
  <div class="app-layout">
    <AppSidebar />

    <div class="app-content">
      <AppTopbar :titre="titre" @deconnexion="deconnexion" />

      <main class="app-main">
        <div class="app-container">
          <RouterView v-slot="{ Component, route: r }">
            <Transition name="page" mode="out-in">
              <component :is="Component" :key="r.path" />
            </Transition>
          </RouterView>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  background: var(--p-surface-50, #f8fafc);
}

.app-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.app-main {
  flex: 1;
  padding: 1.5rem;
}

.app-container {
  max-width: 90rem;
  margin: 0 auto;
}

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

@media (max-width: 768px) {
  .app-main {
    padding: 1rem;
  }
}
</style>