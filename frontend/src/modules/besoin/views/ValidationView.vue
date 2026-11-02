<script setup>
import { computed, onMounted, ref } from 'vue'
import { besoinApi as api } from '@/modules/besoin/api/besoinApi'
import EmptyState from '@/shared/components/EmptyState.vue'
import ModalDialog from '@/shared/components/ModalDialog.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDateHeure, formatMontant } from '@/shared/utils/format'

const auth = useAuthStore()
const notifications = useNotificationStore()

const lignes = ref([])
const chargement = ref(true)
const action = ref(null)
const decision = ref('APPROUVE')
const commentaire = ref('')
const montantVerifie = ref(null)
const enregistrement = ref(false)

const DECISIONS = [
  { valeur: 'APPROUVE', libelle: 'Approuver' },
  { valeur: 'A_CORRIGER', libelle: 'Demander une correction' },
  { valeur: 'REFUSE', libelle: 'Refuser definitivement' },
]

const ETAPES = [
  { valeur: 'RH', libelle: 'RH' },
  { valeur: 'FINANCE', libelle: 'Finance' },
  { valeur: 'DIRECTION', libelle: 'Direction' },
]

const filtreEtape = ref(auth.role)

async function charger() {
  chargement.value = true
  try {
    lignes.value = await api.get('/validation/file')
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

const lignesFiltrees = computed(() => {
  if (!filtreEtape.value) {
    return lignes.value
  }
  return lignes.value.filter((ligne) => ligne.etapeCourante === filtreEtape.value)
})

function ouvrir(ligne) {
  action.value = ligne
  decision.value = 'APPROUVE'
  commentaire.value = ''
  montantVerifie.value = null
}

async function enregistrer() {
  if (!action.value) {
    return
  }
  enregistrement.value = true
  try {
    await api.post(`/validation/lignes/${action.value.id}/decision`, {
      decision: decision.value,
      commentaire: commentaire.value.trim() || null,
      montantVerifie: montantVerifie.value === null ? null : Number(montantVerifie.value),
    })
    notifications.succes('Decision enregistree.')
    action.value = null
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
        <h1>File de validation</h1>
        <p class="page__description">
          Lignes en attente a votre etape. Chaque decision est tracee et conditionne la suite du circuit.
        </p>
      </div>
      <div class="champ" style="margin: 0; min-width: 200px">
        <label class="champ__libelle" for="etape">Etape</label>
        <select id="etape" v-model="filtreEtape" class="saisie">
          <option value="">Toutes les etapes</option>
          <option v-for="etape in ETAPES" :key="etape.valeur" :value="etape.valeur">{{ etape.libelle }}</option>
        </select>
      </div>
    </div>

    <section class="carte">
      <div class="carte__entete">
        <h2>{{ lignesFiltrees.length }} ligne(s) a traiter</h2>
        <span class="texte-secondaire">Votre role : {{ auth.role }}</span>
      </div>

      <div v-if="chargement" class="chargement">Chargement de la file...</div>

      <EmptyState
        v-else-if="lignesFiltrees.length === 0"
        titre="File vide"
        message="Aucune ligne n'attend de validation a cette etape."
      />

      <div v-else class="tableau-conteneur">
        <table class="tableau">
          <thead>
            <tr>
              <th>Ligne</th>
              <th>Service</th>
              <th>Type de besoin</th>
              <th>Effectif</th>
              <th>Budget</th>
              <th>Etape</th>
              <th>Derniere mise a jour</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ligne in lignesFiltrees" :key="ligne.id">
              <td>
                <div style="font-weight: 500">{{ ligne.intituleDemande }}</div>
                <div class="texte-discret" style="font-size: 12px">Demande #{{ ligne.idDemande }} · episode {{ ligne.episode }}</div>
              </td>
              <td>{{ ligne.service }}</td>
              <td><StatusBadge :valeur="ligne.typeBesoin" /></td>
              <td>{{ ligne.effectif }}</td>
              <td class="mono">{{ formatMontant(ligne.budgetEstime) }}</td>
              <td><StatusBadge :valeur="ligne.etapeCourante" /></td>
              <td class="mono">{{ formatDateHeure(ligne.dateModification) }}</td>
              <td class="tableau__actions">
                <button
                  class="bouton bouton--principal bouton--petit"
                  type="button"
                  :disabled="ligne.etapeCourante !== auth.role"
                  @click="ouvrir(ligne)"
                >
                  Traiter
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <ModalDialog
      v-if="action"
      titre="Decision de validation"
      :sous-titre="action.intituleDemande"
      :fermable="!enregistrement"
      @fermer="action = null"
    >
      <div class="alerte alerte--info">
        Etape en cours : <strong>{{ action.etapeCourante }}</strong> · budget demande :
        <strong>{{ formatMontant(action.budgetEstime) }}</strong>
      </div>

      <div class="champ">
        <label class="champ__libelle" for="decision-file">Decision</label>
        <select id="decision-file" v-model="decision" class="saisie">
          <option v-for="item in DECISIONS" :key="item.valeur" :value="item.valeur">{{ item.libelle }}</option>
        </select>
      </div>

      <div v-if="action.etapeCourante === 'FINANCE'" class="champ">
        <label class="champ__libelle" for="montant-file">Montant verifie (Ar)</label>
        <input
          id="montant-file"
          v-model.number="montantVerifie"
          class="saisie"
          type="number"
          min="0"
          step="1000"
          :placeholder="String(action.budgetEstime)"
        />
        <span class="champ__aide">La disponibilite budgetaire est verifiee automatiquement.</span>
      </div>

      <div class="champ">
        <label class="champ__libelle" for="commentaire-file">Commentaire</label>
        <textarea id="commentaire-file" v-model="commentaire" class="saisie" rows="3" />
      </div>

      <template #actions>
        <button class="bouton bouton--secondaire" type="button" @click="action = null">Annuler</button>
        <button class="bouton bouton--principal" type="button" :disabled="enregistrement" @click="enregistrer">
          Enregistrer la decision
        </button>
      </template>
    </ModalDialog>
  </div>
</template>
