<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Textarea from 'primevue/textarea'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDate, formatDateHeure, formatMontant } from '@/shared/utils/format'
import { embaucheApi as api, ErreurApi } from '../api/embaucheApi'

const route = useRoute()
const notifications = useNotificationStore()

const token = computed(() => route.params.token)
const proposition = ref(null)
const resultat = ref(null)
const chargement = ref(true)
const envoi = ref(false)
const action = ref(null)
const commentaire = ref('')

async function charger() {
  chargement.value = true
  try {
    proposition.value = await api.get(`/embauche/propositions/token/${token.value}`)
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Ce lien est invalide ou expiré.')
  } finally {
    chargement.value = false
  }
}

async function accepter() {
  envoi.value = true
  try {
    resultat.value = await api.post(`/embauche/propositions/token/${token.value}/accepter`, {
      commentaire: commentaire.value || null,
    })
    notifications.succes('Votre acceptation a bien été enregistrée.')
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible d’accepter la proposition.')
  } finally {
    envoi.value = false
  }
}

async function refuser() {
  envoi.value = true
  try {
    await api.post(`/embauche/propositions/token/${token.value}/refuser`, {
      motif: commentaire.value || 'Refus du candidat.',
    })
    notifications.succes('Votre refus a bien été enregistré.')
    action.value = null
    await charger()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible d’enregistrer le refus.')
  } finally {
    envoi.value = false
  }
}

async function contreProposer() {
  envoi.value = true
  try {
    await api.post(`/embauche/propositions/token/${token.value}/contre-proposer`, {
      message: commentaire.value,
    })
    notifications.succes('Votre contre-proposition a bien été transmise.')
    action.value = null
    await charger()
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible d’envoyer la contre-proposition.')
  } finally {
    envoi.value = false
  }
}

onMounted(charger)
</script>

<template>
  <main class="min-h-screen bg-surface-50 px-4 py-10 dark:bg-surface-950">
    <div class="mx-auto flex max-w-3xl flex-col gap-5">
      <div class="text-center">
        <div class="mx-auto mb-3 flex size-12 items-center justify-center rounded-xl bg-primary text-primary-contrast">
          <i class="pi pi-building text-xl" />
        </div>
        <h1 class="m-0 text-2xl font-bold">Proposition d’embauche</h1>
        <p class="mt-1 mb-0 text-muted-color">ERP TOVO · Lien personnel et confidentiel</p>
      </div>

      <div v-if="chargement" class="py-12 text-center text-muted-color">
        <i class="pi pi-spin pi-spinner mr-2" /> Chargement de votre proposition...
      </div>

      <Card v-else-if="resultat">
        <template #title>Félicitations, votre acceptation est confirmée</template>
        <template #content>
          <div class="flex flex-col gap-3">
            <div class="flex items-center gap-2">
              <StatusBadge :valeur="resultat.statut" />
              <span class="text-muted-color">{{ resultat.reference }}</span>
            </div>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div><div class="text-xs font-semibold uppercase text-muted-color">Matricule</div><div>{{ resultat.matricule || 'Attribué à la signature du contrat' }}</div></div>
              <div><div class="text-xs font-semibold uppercase text-muted-color">Numéro de contrat</div><div>{{ resultat.numeroContrat || '—' }}</div></div>
              <div><div class="text-xs font-semibold uppercase text-muted-color">Type de contrat</div><div>{{ resultat.typeContrat || '—' }}</div></div>
              <div><div class="text-xs font-semibold uppercase text-muted-color">Date de début</div><div>{{ formatDate(resultat.dateDebut) }}</div></div>
              <div><div class="text-xs font-semibold uppercase text-muted-color">Date de fin prévue</div><div>{{ formatDate(resultat.dateFinPrevue) }}</div></div>
              <div><div class="text-xs font-semibold uppercase text-muted-color">Salaire de base</div><div>{{ formatMontant(resultat.salaireBase) }}</div></div>
            </div>
            <p class="mb-0 text-sm text-muted-color">
              Votre dossier salarié a été créé. Les ressources humaines vous contacteront pour la signature et les pièces complémentaires.
            </p>
          </div>
        </template>
      </Card>

      <Card v-else-if="proposition">
        <template #title>
          {{ proposition.candidatPrenom }} {{ proposition.candidatNom }}
        </template>
        <template #subtitle>
          <div class="flex flex-wrap items-center gap-2">
            <StatusBadge :valeur="proposition.statut" />
            <span>{{ proposition.reference }} · {{ proposition.poste || 'Poste à confirmer' }}</span>
          </div>
        </template>
        <template #content>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div><div class="text-xs font-semibold uppercase text-muted-color">Type de contrat</div><div>{{ proposition.typeContrat || '—' }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Date de début</div><div>{{ formatDate(proposition.dateDebutPrevue) }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Salaire brut mensuel</div><div>{{ formatMontant(proposition.salaireBrutMensuel) }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Lieu de travail</div><div>{{ proposition.lieuTravail || '—' }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Horaires</div><div>{{ proposition.horaires || '—' }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Valable jusqu’au</div><div>{{ formatDateHeure(proposition.dateExpiration) }}</div></div>
            <div class="sm:col-span-2"><div class="text-xs font-semibold uppercase text-muted-color">Primes et avantages</div><div class="whitespace-pre-line">{{ proposition.primesAvantages || '—' }}</div></div>
          </div>

          <div v-if="proposition.reponsePossible" class="mt-6 border-t border-surface pt-5">
            <template v-if="!action">
              <p class="mb-3 font-semibold">Souhaitez-vous accepter cette proposition ?</p>
              <div class="flex flex-wrap gap-2">
                <Button label="J’accepte la proposition" icon="pi pi-check" severity="success" @click="accepter" :loading="envoi" />
                <Button label="Faire une contre-proposition" icon="pi pi-comments" severity="secondary" outlined @click="action = 'contre'" />
                <Button label="Refuser" icon="pi pi-times" severity="danger" outlined @click="action = 'refus'" />
              </div>
            </template>

            <div v-else class="flex flex-col gap-3">
              <p class="m-0 font-semibold">
                {{ action === 'refus' ? 'Motif du refus' : 'Votre contre-proposition' }}
              </p>
              <Textarea v-model="commentaire" rows="4" auto-resize
                :placeholder="action === 'refus' ? 'Expliquez la raison de votre refus' : 'Décrivez votre demande (salaire, date de début, avantages…)'" />
              <div class="flex flex-wrap gap-2">
                <Button :label="action === 'refus' ? 'Confirmer le refus' : 'Envoyer la contre-proposition'"
                  :severity="action === 'refus' ? 'danger' : 'primary'" :loading="envoi"
                  :disabled="!commentaire.trim()" @click="action === 'refus' ? refuser() : contreProposer()" />
                <Button label="Annuler" severity="secondary" outlined @click="action = null; commentaire = ''" />
              </div>
            </div>
          </div>

          <div v-else class="mt-5 rounded-lg border border-surface bg-surface-100 p-4 text-sm text-muted-color dark:bg-surface-800">
            Cette proposition n’est plus en attente de réponse (statut : {{ proposition.statut }}).
          </div>
        </template>
      </Card>

      <div v-else class="rounded-lg border border-red-200 bg-red-50 p-6 text-center text-red-800">
        <i class="pi pi-exclamation-triangle mb-2 text-2xl" />
        <p class="m-0">Ce lien est invalide, expiré ou a déjà été utilisé.</p>
      </div>
    </div>
  </main>
</template>
