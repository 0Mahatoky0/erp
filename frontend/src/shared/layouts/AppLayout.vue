<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import AppSidebar from '@/shared/components/AppSidebar.vue'
import AppTopbar from '@/shared/components/AppTopbar.vue'
import NotificationContainer from '@/shared/components/NotificationContainer.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const titre = computed(() => route.meta.titre || 'ERP TOVO')
const contenuPrincipal = ref(null)

function revenirEnHaut() {
  const defiler = () => {
    contenuPrincipal.value?.scrollTo({ top: 0, left: 0, behavior: 'auto' })
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  }
  requestAnimationFrame(() => requestAnimationFrame(defiler))
}

watch(() => route.fullPath, async () => {
  await nextTick()
  revenirEnHaut()
}, { immediate: true })

onMounted(() => {
  if ('scrollRestoration' in history) history.scrollRestoration = 'manual'
  revenirEnHaut()
  window.setTimeout(revenirEnHaut, 300)
})

function deconnexion() {
  auth.deconnexion()
  router.replace({ name: 'connexion' })
}
</script>

<template>
  <div class="flex h-screen overflow-hidden bg-surface-50 dark:bg-surface-950">
    <AppSidebar />

    <div class="flex h-screen min-h-0 min-w-0 flex-1 flex-col overflow-hidden">
      <AppTopbar :titre="titre" @deconnexion="deconnexion" />

      <main ref="contenuPrincipal" class="min-h-0 flex-1 overflow-y-auto p-4 md:p-6">
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
