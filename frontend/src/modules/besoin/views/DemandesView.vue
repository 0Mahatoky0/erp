<script setup>
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { besoinApi as api } from '@/modules/besoin/api/besoinApi'
import EmptyState from '@/shared/components/EmptyState.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDate, formatMontant } from '@/shared/utils/format'

const auth = useAuthStore()
const notifications = useNotificationStore()

const demandes = ref([])
const chargement = ref(true)
const statutFiltre = ref('')
const recherche = ref('')

const STATUTS = [
  { valeur: '', libelle: 'Tous les statuts' },
  { valeur: 'BROUILLON', libelle: 'Brouillon' },
  { valeur: 'EN_VALIDATION', libelle: 'En validation' },
  { valeur: 'A_CORRIGER', libelle: 'A corriger' },
  { valeur: 'FINALISEE', libelle: 'Finalisee' },
  { valeur: 'FINALISEE_PARTIELLE', libelle: 'Finalisee partiellement' },
  { valeur: 'REFUSEE', libelle: 'Refusee' },
  { valeur: 'ANNULEE', libelle: 'Annulee' },
]

async function charger() {
  chargement.value = true
  try {
    const parametres = new URLSearchParams()
    if (statutFiltre.value) {
      parametres.set('statut', statutFiltre.value)
    }
    if (recherche.value.trim()) {
      parametres.set('recherche', recherche.value.trim())
    }
    const suffixe = parametres.toString() ? `?${parametres}` : ''
    demandes.value = await api.get(`/demandes${suffixe}`)
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

const totalBudget = computed(() =>
  demandes.value.reduce((somme, demande) => somme + Number(demande.budgetTotal || 0), 0),
)
</script>

<template>
  <div>
    <div class="page__entete">
      <div>
        <h1>Demandes de recrutement</h1>
        <p class="page__description">
          <template v-if="auth.estManager">Vous visualisez uniquement vos propres demandes.</template>
          <template v-else>Vous visualisez l'ensemble des demandes de l'entreprise.</template>
        </p>
      </div>
      <RouterLink class="bouton bouton--principal" :to="{ name: 'demande-nouvelle' }">Nouvelle demande</RouterLink>
    </div>

    <div class="barre-filtres">
      <div class="champ champ--large">
        <label class="champ__libelle" for="recherche">Recherche</label>
        <PrimeInputText
          id="recherche"
          v-model="recherche"
          class="saisie"
          type="search"
          placeholder="Motif ou demandeur"
          @keyup.enter="charger"
        />
      </div>
      <div class="champ">
        <label class="champ__libelle" for="statut">Statut</label>
        <select id="statut" v-model="statutFiltre" class="saisie" @change="charger">
          <option v-for="statut in STATUTS" :key="statut.valeur" :value="statut.valeur">
            {{ statut.libelle }}
          </option>
        </select>
      </div>
      <PrimeButton class="bouton bouton--secondaire" @click="charger">Filtrer</PrimeButton>
    </div>

    <section class="carte">
      <div class="carte__entete">
        <h2>{{ demandes.length }} demande(s)</h2>
        <span class="texte-secondaire">Budget total : {{ formatMontant(totalBudget) }}</span>
      </div>

      <div v-if="chargement" class="chargement">Chargement des demandes...</div>

      <EmptyState
        v-else-if="demandes.length === 0"
        titre="Aucune demande"
        message="Aucune demande ne correspond aux criteres selectionnes."
      />

      <div v-else class="tableau-conteneur">
        <table class="tableau">
          <thead>
            <tr>
              <th>Reference</th>
              <th>Motif</th>
              <th>Demandeur</th>
              <th>Date souhaitee</th>
              <th>Lignes</th>
              <th>Budget</th>
              <th>Statut</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="demande in demandes" :key="demande.id">
              <td class="mono">DEM-{{ String(demande.id).padStart(4, '0') }}</td>
              <td>{{ demande.motif }}</td>
              <td>{{ demande.demandeur }}</td>
              <td class="mono">{{ formatDate(demande.dateSouhaitee) }}</td>
              <td>
                {{ demande.nombreLignes }}
                <span class="texte-discret" style="font-size: 12px">
                  ({{ demande.lignesAcceptees }} acceptee(s), {{ demande.lignesEnValidation }} en cours)
                </span>
              </td>
              <td class="mono">{{ formatMontant(demande.budgetTotal) }}</td>
              <td><StatusBadge :valeur="demande.statut" /></td>
              <td class="tableau__actions">
                <RouterLink
                  class="bouton bouton--secondaire bouton--petit"
                  :to="{ name: 'demande-detail', params: { id: demande.id } }"
                >
                  Ouvrir
                </RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
