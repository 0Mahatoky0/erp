<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '@/api/client'
import EmptyState from '@/components/EmptyState.vue'
import ModalDialog from '@/components/ModalDialog.vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { formatMontant } from '@/utils/format'

const auth = useAuthStore()
const notifications = useNotificationStore()

const budgets = ref([])
const services = ref([])
const chargement = ref(true)
const exercice = ref(new Date().getFullYear())
const edition = ref(null)
const enregistrement = ref(false)
const formulaire = ref({ idService: null, exercice: null, montantAlloue: null })

async function charger() {
  chargement.value = true
  try {
    const [listeBudgets, listeServices] = await Promise.all([
      api.get(`/budgets?exercice=${exercice.value}`),
      api.get('/referentiel/services'),
    ])
    budgets.value = listeBudgets
    services.value = listeServices
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

const totalAlloue = computed(() =>
  budgets.value.reduce((somme, budget) => somme + Number(budget.montantAlloue || 0), 0),
)
const totalDisponible = computed(() =>
  budgets.value.reduce((somme, budget) => somme + Number(budget.montantDisponible || 0), 0),
)
const totalEngage = computed(() =>
  budgets.value.reduce((somme, budget) => somme + Number(budget.montantEngage || 0), 0),
)

/** Pourcentage engage par service, pour visualiser la tension budgetaire. */
function taux(budget) {
  if (!budget.montantAlloue) {
    return 0
  }
  return Math.min(100, Math.round((Number(budget.montantEngage) / Number(budget.montantAlloue)) * 100))
}

function classeTaux(budget) {
  const valeur = taux(budget)
  if (valeur >= 90) return 'jauge__valeur--danger'
  if (valeur >= 70) return 'jauge__valeur--alerte'
  return ''
}

function nouvelleEdition() {
  edition.value = { id: null }
  formulaire.value = {
    idService: null,
    exercice: exercice.value,
    montantAlloue: null,
  }
}

function modifier(budget) {
  edition.value = { id: budget.id }
  formulaire.value = {
    idService: budget.idService,
    exercice: budget.exercice,
    montantAlloue: budget.montantAlloue,
  }
}

async function enregistrer() {
  if (!edition.value) {
    return
  }
  enregistrement.value = true
  try {
    await api.put('/budgets', {
      idService: formulaire.value.idService,
      exercice: Number(formulaire.value.exercice),
      montantAlloue: Number(formulaire.value.montantAlloue),
    })
    notifications.succes('Le budget a ete enregistre.')
    edition.value = null
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    enregistrement.value = false
  }
}
</script>

<template>
  <div>
    <div class="page__entete">
      <div>
        <h1>Budgets</h1>
        <p class="page__description">
          Allocation annuelle par service. Le disponible se recalcule automatiquement : disponible =
          alloue − engage − depense.
        </p>
      </div>
      <button v-if="auth.estFinance" class="bouton bouton--principal" type="button" @click="nouvelleEdition">
          Definir un budget
      </button>
    </div>

    <div class="barre-filtres">
      <div class="champ">
        <label class="champ__libelle" for="exercice-budget">Exercice</label>
        <input id="exercice-budget" v-model.number="exercice" class="saisie" type="number" min="2020" max="2100" @change="charger" />
      </div>
      <button class="bouton bouton--secondaire" type="button" @click="charger">Afficher</button>
    </div>

    <div class="grille grille--3" style="margin-bottom: 16px">
      <div class="carte stat">
        <div class="stat__libelle">Budget alloue</div>
        <div class="stat__valeur" style="font-size: 20px">{{ formatMontant(totalAlloue) }}</div>
      </div>
      <div class="carte stat">
        <div class="stat__libelle">Budget engage</div>
        <div class="stat__valeur" style="font-size: 20px">{{ formatMontant(totalEngage) }}</div>
      </div>
      <div class="carte stat">
        <div class="stat__libelle">Budget disponible</div>
        <div class="stat__valeur" style="font-size: 20px">{{ formatMontant(totalDisponible) }}</div>
      </div>
    </div>

    <section class="carte">
      <div class="carte__entete"><h2>Budgets par service — exercice {{ exercice }}</h2></div>

      <div v-if="chargement" class="chargement">Chargement des budgets...</div>

      <EmptyState
        v-else-if="budgets.length === 0"
        titre="Aucun budget"
        message="Aucun budget n'est defini pour cet exercice."
      />

      <div v-else class="tableau-conteneur">
        <table class="tableau">
          <thead>
            <tr>
              <th>Service</th>
              <th>Departement</th>
              <th>Alloue</th>
              <th>Depense</th>
              <th>Engage</th>
              <th>Disponible</th>
              <th>Consommation</th>
              <th v-if="auth.estFinance"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="budget in budgets" :key="budget.id">
              <td style="font-weight: 500">{{ budget.service }}</td>
              <td>{{ budget.departement }}</td>
              <td class="mono">{{ formatMontant(budget.montantAlloue) }}</td>
              <td class="mono">{{ formatMontant(budget.montantDepense) }}</td>
              <td class="mono">{{ formatMontant(budget.montantEngage) }}</td>
              <td class="mono" style="font-weight: 600">{{ formatMontant(budget.montantDisponible) }}</td>
              <td style="min-width: 140px">
                <div class="jauge">
                  <div class="jauge__valeur" :class="classeTaux(budget)" :style="{ width: `${taux(budget)}%` }" />
                </div>
                <div class="texte-discret" style="font-size: 11.5px; margin-top: 4px">{{ taux(budget) }} % engage</div>
              </td>
              <td v-if="auth.estFinance" class="tableau__actions">
                <button class="bouton bouton--secondaire bouton--petit" type="button" @click="modifier(budget)">
                  Modifier
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <ModalDialog
      v-if="edition"
      titre="Definir le budget d'un service"
      :fermable="!enregistrement"
      @fermer="edition = null"
    >
      <div class="champ">
        <label class="champ__libelle" for="service-budget">Service <span class="champ__obligatoire">*</span></label>
        <select id="service-budget" v-model="formulaire.idService" class="saisie" :disabled="!!edition.id">
          <option :value="null" disabled>Choisir un service</option>
          <option v-for="service in services" :key="service.id" :value="service.id">{{ service.nom }}</option>
        </select>
      </div>

      <div class="grille-champs">
        <div class="champ">
          <label class="champ__libelle" for="exercice-form">Exercice <span class="champ__obligatoire">*</span></label>
          <input id="exercice-form" v-model.number="formulaire.exercice" class="saisie" type="number" min="2020" max="2100" />
        </div>
        <div class="champ">
          <label class="champ__libelle" for="montant-form">Montant alloue (Ar) <span class="champ__obligatoire">*</span></label>
          <input id="montant-form" v-model.number="formulaire.montantAlloue" class="saisie" type="number" min="0" step="100000" />
        </div>
      </div>

      <template #actions>
        <button class="bouton bouton--secondaire" type="button" @click="edition = null">Annuler</button>
        <button class="bouton bouton--principal" type="button" :disabled="enregistrement" @click="enregistrer">
          Enregistrer
        </button>
      </template>
    </ModalDialog>
  </div>
</template>
