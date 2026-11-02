<script setup>
import { computed, onMounted, ref } from 'vue'
import { besoinApi as api } from '@/modules/besoin/api/besoinApi'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
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
  const consomme = Number(synthese.value.budgetEngage || 0) + Number(synthese.value.budgetDepense || 0)
  return Math.min(100, Math.round((consomme / Number(synthese.value.budgetAlloue)) * 100))
})

const classeJauge = computed(() => {
  if (tauxConsommation.value >= 90) return 'jauge__valeur--danger'
  if (tauxConsommation.value >= 70) return 'jauge__valeur--alerte'
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
    <div class="page__entete">
      <div>
        <h1>Bonjour {{ auth.profil?.nom }}</h1>
        <p class="page__description">
          <template v-if="auth.estManager">
            Suivez vos demandes de recrutement et les validations en cours.
          </template>
          <template v-else-if="auth.estRh">
            Vous databases le besoin, validez les demandes et tenez le catalogue des postes.
          </template>
          <template v-else-if="auth.estFinance">
            Vous controlez la disponibilite budgetaire avant de valider les demandes.
          </template>
          <template v-else>
            Vous arbitrez les demandes finales de l'entreprise.
          </template>
        </p>
      </div>
      <RouterLink class="bouton bouton--principal" :to="{ name: 'demande-nouvelle' }">Nouvelle demande</RouterLink>
    </div>

    <div v-if="chargement" class="chargement">Chargement des donnees...</div>

    <template v-else-if="synthese">
      <div class="grille grille--4" style="margin-bottom: 16px">
        <div class="carte stat">
          <div class="stat__libelle">Demandes</div>
          <div class="stat__valeur">{{ synthese.totalDemandes }}</div>
          <div class="stat__detail">{{ synthese.demandesEnValidation }} en validation</div>
        </div>
        <div class="carte stat">
          <div class="stat__libelle">En attente de validation</div>
          <div class="stat__valeur">{{ synthese.lignesAValider }}</div>
          <div class="stat__detail">
            <template v-if="synthese.etapeValidation">Votre etape : {{ synthese.etapeValidation }}</template>
            <template v-else>Aucune etape associee a votre role</template>
          </div>
        </div>
        <div class="carte stat">
          <div class="stat__libelle">Lignes acceptees</div>
          <div class="stat__valeur">{{ synthese.lignesAcceptees }}</div>
          <div class="stat__detail">{{ synthese.lignesACorriger }} ligne(s) a corriger</div>
        </div>
        <div class="carte stat">
          <div class="stat__libelle">Budget disponible</div>
          <div class="stat__valeur" style="font-size: 20px">{{ formatMontant(synthese.budgetDisponible) }}</div>
          <div class="stat__detail">Exercice {{ exercice }}</div>
        </div>
      </div>

      <div class="grille grille--2">
        <section class="carte">
          <div class="carte__entete">
            <h2>Budget {{ exercice }}</h2>
            <RouterLink v-if="auth.estFinance" class="bouton bouton--secondaire bouton--petit" :to="{ name: 'budgets' }">
              Gerer les budgets
            </RouterLink>
          </div>
          <div class="carte__corps">
            <div class="liste-definitions" style="grid-template-columns: repeat(2, minmax(0, 1fr))">
              <div>
                <div class="liste-definitions__libelle">Alloue</div>
                <div class="liste-definitions__valeur">{{ formatMontant(synthese.budgetAlloue) }}</div>
              </div>
              <div>
                <div class="liste-definitions__libelle">Engage</div>
                <div class="liste-definitions__valeur">{{ formatMontant(synthese.budgetEngage) }}</div>
              </div>
            </div>
            <div class="jauge" style="margin-top: 16px">
              <div class="jauge__valeur" :class="classeJauge" :style="{ width: `${tauxConsommation}%` }" />
            </div>
            <div class="texte-discret" style="margin-top: 6px; font-size: 12px">
              {{ tauxConsommation }} % du budget engage ou depense
            </div>
          </div>
        </section>

        <section class="carte">
          <div class="carte__entete">
            <h2>Demandes recentes</h2>
            <RouterLink class="bouton bouton--secondaire bouton--petit" :to="{ name: 'demandes' }">
              Tout afficher
            </RouterLink>
          </div>
          <div class="carte__corps">
            <div v-if="demandes.length === 0" class="etat-vide">Aucune demande enregistree.</div>
            <div v-else>
              <div
                v-for="demande in demandes.slice(0, 5)"
                :key="demande.id"
                class="ligne-liste"
              >
                <div>
                  <div style="font-weight: 500">{{ demande.motif }}</div>
                  <div class="texte-discret" style="font-size: 12px">
                    {{ demande.nombreLignes }} ligne(s) · {{ demande.demandeur }}
                  </div>
                </div>
                <div style="display: flex; align-items: center; gap: 10px">
                  <StatusBadge :valeur="demande.statut" />
                  <RouterLink class="bouton bouton--fantome bouton--petit" :to="{ name: 'demande-detail', params: { id: demande.id } }">
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
