<script setup>
import { computed, onMounted, ref } from 'vue'
import { besoinApi as api } from '@/modules/besoin/api/besoinApi'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import PrimeButton from 'primevue/button'
import { formatMontant } from '@/shared/utils/format'

const auth = useAuthStore()
const notifications = useNotificationStore()

const synthese = ref(null)
const demandes = ref([])
const chargement = ref(true)

const exercice = computed(() => synthese.value?.exercice ?? new Date().getFullYear())

/** Jauge de consommation du budget : engage + depense sur le montant alloue. */
const tauxConsommation = computed(() => {
  if (!synthese.value || !synthese.value.budgetAlloue) {
    return 0
  }
  const consomme =
    Number(synthese.value.budgetEngage || 0) + Number(synthese.value.budgetDepense || 0)
  return Math.min(100, Math.round((consomme / Number(synthese.value.budgetAlloue)) * 100))
})

const classeJauge = computed(() => {
  if (tauxConsommation.value >= 90) return 'bg-red-500'
  if (tauxConsommation.value >= 70) return 'bg-yellow-500'
  return ''
})

async function charger() {
  chargement.value = true
  try {
    const [syntheseApi, demandesApi] = await Promise.all([
      api.get('/dashboard'),
      api.get('/demandes'),
    ])
    synthese.value = syntheseApi
    demandes.value = demandesApi
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)
</script>

<template>
  <div>
    <div class="flex flex-col gap-4 mb-5 md:flex-row md:items-start md:justify-between">
      <div>
        <h1>Bonjour {{ auth.profil?.nom }}</h1>
        <p class="mt-1 text-sm text-muted-color">
          <template v-if="auth.estManager">
            Suivez vos demandes de recrutement et les validations en cours.
          </template>
          <template v-else-if="auth.estRh">
            Vous databases le besoin, validez les demandes et tenez le catalogue des postes.
          </template>
          <template v-else-if="auth.estFinance">
            Vous controlez la disponibilite budgetaire avant de valider les demandes.
          </template>
          <template v-else> Vous arbitrez les demandes finales de l'entreprise. </template>
        </p>
      </div>
      <PrimeButton
        as="router-link"
        :to="{ name: 'demande-nouvelle' }"
        label="Nouvelle demande"
        icon="pi pi-plus"
      />
    </div>

    <div v-if="chargement" class="p-4 text-sm text-muted-color">Chargement des donnees...</div>

    <template v-else-if="synthese">
      <div class="grid gap-4 grid-cols-1 sm:grid-cols-2 xl:grid-cols-4" style="margin-bottom: 16px">
        <div
          class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900 p-4"
        >
          <div class="text-xs font-semibold uppercase tracking-wide text-muted-color">Demandes</div>
          <div class="mt-1 text-2xl font-semibold">{{ synthese.totalDemandes }}</div>
          <div class="mt-1 text-sm text-muted-color">
            {{ synthese.demandesEnValidation }} en validation
          </div>
        </div>
        <div
          class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900 p-4"
        >
          <div class="text-xs font-semibold uppercase tracking-wide text-muted-color">
            En attente de validation
          </div>
          <div class="mt-1 text-2xl font-semibold">{{ synthese.lignesAValider }}</div>
          <div class="mt-1 text-sm text-muted-color">
            <template v-if="synthese.etapeValidation"
              >Votre etape : {{ synthese.etapeValidation }}</template
            >
            <template v-else>Aucune etape associee a votre role</template>
          </div>
        </div>
        <div
          class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900 p-4"
        >
          <div class="text-xs font-semibold uppercase tracking-wide text-muted-color">
            Lignes acceptees
          </div>
          <div class="mt-1 text-2xl font-semibold">{{ synthese.lignesAcceptees }}</div>
          <div class="mt-1 text-sm text-muted-color">
            {{ synthese.lignesACorriger }} ligne(s) a corriger
          </div>
        </div>
        <div
          class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900 p-4"
        >
          <div class="text-xs font-semibold uppercase tracking-wide text-muted-color">
            Budget disponible
          </div>
          <div class="mt-1 text-2xl font-semibold" style="font-size: 20px">
            {{ formatMontant(synthese.budgetDisponible) }}
          </div>
          <div class="mt-1 text-sm text-muted-color">Exercice {{ exercice }}</div>
        </div>
      </div>

      <div class="grid gap-4 grid-cols-1 md:grid-cols-2">
        <section
          class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900"
        >
          <div class="flex items-center justify-between gap-3 border-b border-surface p-4">
            <h2>Budget {{ exercice }}</h2>
            <RouterLink
              v-if="auth.estFinance"
              class="inline-flex items-center justify-center gap-2 border border-surface !text-sm"
              :to="{ name: 'budgets' }"
            >
              Gerer les budgets
            </RouterLink>
          </div>
          <div class="p-4">
            <div
              class="grid grid-cols-1 gap-3 md:grid-cols-2"
              style="grid-template-columns: repeat(2, minmax(0, 1fr))"
            >
              <div>
                <div class="text-sm text-muted-color">Alloue</div>
                <div class="font-medium">{{ formatMontant(synthese.budgetAlloue) }}</div>
              </div>
              <div>
                <div class="text-sm text-muted-color">Engage</div>
                <div class="font-medium">{{ formatMontant(synthese.budgetEngage) }}</div>
              </div>
            </div>
            <div
              class="h-2 overflow-hidden rounded-full bg-surface-200 dark:bg-surface-700"
              style="margin-top: 16px"
            >
              <div
                class="h-full bg-primary"
                :class="classeJauge"
                :style="{ width: `${tauxConsommation}%` }"
              />
            </div>
            <div class="text-muted-color" style="margin-top: 6px; font-size: 12px">
              {{ tauxConsommation }} % du budget engage ou depense
            </div>
          </div>
        </section>

        <section
          class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900"
        >
          <div class="flex items-center justify-between gap-3 border-b border-surface p-4">
            <h2>Demandes recentes</h2>
            <RouterLink
              class="inline-flex items-center justify-center gap-2 border border-surface !text-sm"
              :to="{ name: 'demandes' }"
            >
              Tout afficher
            </RouterLink>
          </div>
          <div class="p-4">
            <div
              v-if="demandes.length === 0"
              class="rounded-lg border border-dashed border-surface p-6 text-center text-muted-color"
            >
              Aucune demande enregistree.
            </div>
            <div v-else>
              <div
                v-for="demande in demandes.slice(0, 5)"
                :key="demande.id"
                class="flex items-center justify-between gap-3 border-b border-surface py-3 last:border-b-0"
              >
                <div>
                  <div style="font-weight: 500">{{ demande.motif }}</div>
                  <div class="text-muted-color" style="font-size: 12px">
                    {{ demande.nombreLignes }} ligne(s) · {{ demande.demandeur }}
                  </div>
                </div>
                <div style="display: flex; align-items: center; gap: 10px">
                  <StatusBadge :valeur="demande.statut" />
                  <RouterLink
                    class="inline-flex items-center justify-center gap-2 text-primary !text-sm"
                    :to="{ name: 'demande-detail', params: { id: demande.id } }"
                  >
                    Ouvrir
                  </RouterLink>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>
