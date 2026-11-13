<script setup>
import PrimeSelect from 'primevue/select'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { besoinApi as api } from '@/modules/besoin/api/besoinApi'
import HistoriqueValidations from '@/shared/components/HistoriqueValidations.vue'
import ModalDialog from '@/shared/components/ModalDialog.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDate, formatDateHeure, formatMontant, libelle } from '@/shared/utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationStore()

const demande = ref(null)
const chargement = ref(true)
const action = ref(null)
const onglet = ref('lignes')

const DECISIONS = [
  { valeur: 'APPROUVE', libelle: 'Approuver' },
  { valeur: 'A_CORRIGER', libelle: 'Demander une correction' },
  { valeur: 'REFUSE', libelle: 'Refuser definitivement' },
]

const decision = ref('APPROUVE')
const commentaire = ref('')
const montantVerifie = ref(null)
const enregistrement = ref(false)

/** Evaluation saisie par le RH a son etape : budget du profil et fourchette salariale. */
const evaluationRh = ref({ budgetEstime: null, salaireMinEstime: null, salaireMaxEstime: null })

const ligneEnCorrection = ref(null)
const correction = ref({
  intituleDemande: '',
  effectif: 1,
  profilRecherche: '',
  descriptionPoste: '',
})
const enregistrementCorrection = ref(false)

const idDemande = computed(() => Number(route.params.id))

const A_DEFINIR = 'A definir par le RH'

/** Un budget ou salaire absent signifie que le RH n'a pas encore statue. */
function montantRh(montant) {
  return montant === null || montant === undefined ? A_DEFINIR : formatMontant(montant)
}

function fourchetteSalaire(ligne) {
  if (ligne.salaireMinEstime == null && ligne.salaireMaxEstime == null) {
    return A_DEFINIR
  }
  return `${formatMontant(ligne.salaireMinEstime)} — ${formatMontant(ligne.salaireMaxEstime)}`
}

/** Somme des budgets deja definis par le RH ; null si aucune ligne n'est encore chiffree. */
const budgetTotal = computed(() => {
  const budgets = (demande.value?.lignes ?? [])
    .map((ligne) => ligne.budgetEstime)
    .filter((budget) => budget !== null && budget !== undefined)
  return budgets.length === 0 ? null : budgets.reduce((somme, budget) => somme + Number(budget), 0)
})

/** Le RH approuve : il doit alors fixer le budget du profil. */
const saisieEvaluationRh = computed(
  () => action.value?.etapeCourante === 'RH' && decision.value === 'APPROUVE',
)

/** Le demandeur peut-il agir sur la demande ? */
const estDemandeur = computed(() => demande.value?.idDemandeur === auth.profil?.id)

const peutSoumettre = computed(
  () => demande.value?.statut === 'BROUILLON' && estDemandeur.value,
)

const peutCorriger = computed(() => {
  if (!demande.value || !estDemandeur.value) {
    return false
  }
  return demande.value.lignes.some((ligne) => ligne.statut === 'A_CORRIGER')
})

/** Un validateur ne peut agir que sur la ligne dont c'est l'etape (RM-039). */
function peutValider(ligne) {
  return ligne.etapeCourante === auth.role
}

async function charger() {
  chargement.value = true
  try {
    demande.value = await api.get(`/demandes/${idDemande.value}`)
  } catch (e) {
    notifications.erreur(e.message)
    router.push({ name: 'demandes' })
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

async function soumettre() {
  try {
    await api.post(`/demandes/${idDemande.value}/soumettre`)
    notifications.succes('La demande a ete soumise au circuit de validation.')
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function resoumettre(ligne) {
  try {
    await api.post(`/demandes/${idDemande.value}/lignes/${ligne.id}/resoumettre`)
    notifications.succes('La ligne a ete resoumise.')
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

async function annulerLigne(ligne) {
  try {
    await api.post(`/demandes/${idDemande.value}/lignes/${ligne.id}/annuler`)
    notifications.succes('La ligne a ete annulee.')
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  }
}

function ouvrirDecision(ligne) {
  action.value = ligne
  decision.value = 'APPROUVE'
  commentaire.value = ''
  montantVerifie.value = null
  evaluationRh.value = {
    budgetEstime: ligne.budgetEstime ?? null,
    salaireMinEstime: ligne.salaireMinEstime ?? null,
    salaireMaxEstime: ligne.salaireMaxEstime ?? null,
  }
}

function nombreOuNull(valeur) {
  return valeur === null || valeur === undefined || valeur === '' ? null : Number(valeur)
}

async function enregistrerDecision() {
  if (!action.value) {
    return
  }
  const corps = {
    decision: decision.value,
    commentaire: commentaire.value.trim() || null,
    montantVerifie: nombreOuNull(montantVerifie.value),
  }
  if (saisieEvaluationRh.value) {
    const budget = nombreOuNull(evaluationRh.value.budgetEstime)
    if (budget === null) {
      notifications.erreur('Definissez le budget du profil avant de l\'approuver.')
      return
    }
    corps.budgetEstime = budget
    corps.salaireMinEstime = nombreOuNull(evaluationRh.value.salaireMinEstime)
    corps.salaireMaxEstime = nombreOuNull(evaluationRh.value.salaireMaxEstime)
  }
  enregistrement.value = true
  try {
    await api.post(`/validation/lignes/${action.value.id}/decision`, corps)
    notifications.succes('Decision enregistree.')
    action.value = null
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    enregistrement.value = false
  }
}

const totalLignes = computed(() => demande.value?.lignes?.length ?? 0)

/** Ouvre la correction d'une ligne demandee (RM-021, RM-037). */
function ouvrirCorrection(ligne) {
  ligneEnCorrection.value = ligne
  correction.value = {
    intituleDemande: ligne.intituleDemande ?? '',
    effectif: ligne.effectif ?? 1,
    profilRecherche: ligne.profilRecherche ?? '',
    descriptionPoste: ligne.descriptionPoste ?? '',
  }
}

/** Enregistre la correction : la ligne reste A_CORRIGER jusqu'a la resoumission. */
async function enregistrerCorrection() {
  if (!ligneEnCorrection.value) {
    return
  }
  enregistrementCorrection.value = true
  try {
    await api.put(`/demandes/${idDemande.value}/lignes/${ligneEnCorrection.value.id}`, {
      intituleDemande: correction.value.intituleDemande,
      effectif: Number(correction.value.effectif),
      profilRecherche: correction.value.profilRecherche || null,
      descriptionPoste: correction.value.descriptionPoste || null,
    })
    notifications.succes('Ligne corrigee. Resoumettez-la pour repartir en validation.')
    ligneEnCorrection.value = null
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    enregistrementCorrection.value = false
  }
}
</script>

<template>
  <div v-if="chargement" class="p-4 text-sm text-muted-color">Chargement de la demande...</div>

  <div v-else-if="demande">
    <div class="mb-4 text-sm text-muted-color">
      <RouterLink :to="{ name: 'demandes' }">Demandes</RouterLink>
      <span>/</span>
      <span class="tabular-nums">DEM-{{ String(demande.id).padStart(4, '0') }}</span>
    </div>

    <div class="flex flex-col gap-4 mb-5 md:flex-row md:items-start md:justify-between">
      <div>
        <h1>{{ demande.motif }}</h1>
        <p class="mt-1 text-sm text-muted-color">
          Demandee par {{ demande.demandeur }} · creation le {{ formatDateHeure(demande.dateCreation) }}
        </p>
      </div>
      <div style="display: flex; gap: 10px; align-items: center">
        <StatusBadge :valeur="demande.statut" />
        <PrimeButton
          v-if="peutSoumettre"
          class="inline-flex items-center justify-center gap-2 bg-primary text-primary-contrast"

          @click="soumettre"
        >
          Soumettre la demande
        </PrimeButton>
      </div>
    </div>

    <div class="grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3" style="margin-bottom: 16px">
      <section class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900">
        <div class="p-4">
          <div class="grid grid-cols-1 gap-3 md:grid-cols-2" style="grid-template-columns: repeat(2, minmax(0, 1fr))">
            <div>
              <div class="text-sm text-muted-color">Date souhaitee</div>
              <div class="font-medium">{{ formatDate(demande.dateSouhaitee) }}</div>
            </div>
            <div>
              <div class="text-sm text-muted-color">Date de soumission</div>
              <div class="font-medium">{{ formatDateHeure(demande.dateSoumission) }}</div>
            </div>
            <div>
              <div class="text-sm text-muted-color">Lignes</div>
              <div class="font-medium">{{ totalLignes }}</div>
            </div>
            <div>
              <div class="text-sm text-muted-color">Budget total</div>
              <div class="font-medium">{{ montantRh(budgetTotal) }}</div>
            </div>
          </div>
        </div>
      </section>

      <section class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900" style="grid-column: span 2">
        <div class="p-4">
          <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
            <div>
              <div class="text-sm text-muted-color">Lignes acceptees</div>
              <div class="font-medium">
                {{ demande.lignes.filter((l) => l.statut === 'ACCEPTEE').length }}
              </div>
            </div>
            <div>
              <div class="text-sm text-muted-color">En cours de validation</div>
              <div class="font-medium">
                {{ demande.lignes.filter((l) => ['EN_ATTENTE_RH', 'EN_ATTENTE_FINANCE', 'EN_ATTENTE_DIRECTION'].includes(l.statut)).length }}
              </div>
            </div>
            <div>
              <div class="text-sm text-muted-color">A corriger</div>
              <div class="font-medium">
                {{ demande.lignes.filter((l) => l.statut === 'A_CORRIGER').length }}
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <div v-if="peutCorriger" class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900 border-primary-200 bg-primary-50 dark:border-primary-800 dark:bg-primary-950" style="margin-bottom: 14px">
      <div class="p-4">
        <strong>Des lignes vous ont ete renvoyees a corriger.</strong>
        Corrigez-les puis resoumettez-les : elles reprendront le circuit a l'etape concernee.
      </div>
    </div>

    <div class="flex flex-wrap gap-2 border-b border-surface">
      <PrimeButton class="rounded-t-lg px-3 py-2 text-sm" :class="{ 'bg-highlight text-primary': onglet === 'lignes' }" @click="onglet = 'lignes'">
        Lignes ({{ totalLignes }})
      </PrimeButton>
      <PrimeButton class="rounded-t-lg px-3 py-2 text-sm" :class="{ 'bg-highlight text-primary': onglet === 'historique' }" @click="onglet = 'historique'">
        Journal des statuts
      </PrimeButton>
    </div>

    <template v-if="onglet === 'lignes'">
      <section v-for="ligne in demande.lignes" :key="ligne.id" class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900" style="margin-bottom: 14px">
        <div class="flex items-center justify-between gap-3 border-b border-surface p-4">
          <div>
            <h2>{{ ligne.intituleDemande }}</h2>
            <p class="text-muted-color" style="margin: 2px 0 0; font-size: 12.5px">
              {{ ligne.service }} · {{ libelle(ligne.typeBesoin) }}
              <template v-if="ligne.typeRemplacement"> · {{ libelle(ligne.typeRemplacement) }}</template>
              · episode {{ ligne.episode }}
            </p>
          </div>
          <div style="display: flex; align-items: center; gap: 10px">
            <StatusBadge :valeur="ligne.statut" />
            <StatusBadge v-if="ligne.etapeCourante" :valeur="ligne.etapeCourante" :libelle="`Etape : ${libelle(ligne.etapeCourante)}`" />
          </div>
        </div>

        <div class="p-4">
          <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
            <div>
              <div class="text-sm text-muted-color">Effectif</div>
              <div class="font-medium">{{ ligne.effectif }}</div>
            </div>
            <div>
              <div class="text-sm text-muted-color">Budget (defini par le RH)</div>
              <div class="font-medium">{{ montantRh(ligne.budgetEstime) }}</div>
            </div>
            <div>
              <div class="text-sm text-muted-color">Fourchette salariale (definie par le RH)</div>
              <div class="font-medium">{{ fourchetteSalaire(ligne) }}</div>
            </div>
            <div>
              <div class="text-sm text-muted-color">Poste rattache</div>
              <div class="font-medium">{{ ligne.poste || 'A creer a l\'acceptation' }}</div>
            </div>
            <div>
              <div class="text-sm text-muted-color">Employe remplace</div>
              <div class="font-medium">
                {{ ligne.employeRemplace ? `${ligne.employeRemplace} (${ligne.matriculeRemplace || '—'})` : '—' }}
              </div>
            </div>
            <div>
              <div class="text-sm text-muted-color">Periode de remplacement</div>
              <div class="font-medium">
                {{ ligne.dateDebutRemplacement ? `${formatDate(ligne.dateDebutRemplacement)} → ${formatDate(ligne.dateFinRemplacement)}` : '—' }}
              </div>
            </div>
            <div>
              <div class="text-sm text-muted-color">Profil recherche</div>
              <div class="font-medium">{{ ligne.profilRecherche || '—' }}</div>
            </div>
            <div>
              <div class="text-sm text-muted-color">Description</div>
              <div class="font-medium">{{ ligne.descriptionPoste || '—' }}</div>
            </div>
            <div>
              <div class="text-sm text-muted-color">Motif du remplacement</div>
              <div class="font-medium">{{ ligne.motifRemplacement || '—' }}</div>
            </div>
          </div>
        </div>

        <div class="border-t border-surface bg-surface-50 p-4 dark:bg-surface-800" style="display: flex; justify-content: space-between; gap: 10px; flex-wrap: wrap">
          <details style="flex: 1">
            <summary class="text-secondary-color" style="cursor: pointer; font-size: 13px">
              Historique des decisions ({{ ligne.validations.length }})
            </summary>
            <div style="margin-top: 10px">
              <HistoriqueValidations :validations="ligne.validations" />
            </div>
          </details>

          <div style="display: flex; gap: 8px">
            <PrimeButton
              v-if="ligne.statut === 'A_CORRIGER' && estDemandeur"
              class="inline-flex items-center justify-center gap-2 border border-surface !text-sm"

              @click="ouvrirCorrection(ligne)"
            >
              Corriger
            </PrimeButton>
            <PrimeButton
              v-if="ligne.statut === 'A_CORRIGER' && estDemandeur"
              class="inline-flex items-center justify-center gap-2 bg-primary text-primary-contrast !text-sm"

              @click="resoumettre(ligne)"
            >
              Resoumettre
            </PrimeButton>
            <PrimeButton
              v-if="['EN_ATTENTE_RH', 'EN_ATTENTE_FINANCE', 'EN_ATTENTE_DIRECTION'].includes(ligne.statut) && peutValider(ligne)"
              class="inline-flex items-center justify-center gap-2 border border-surface !text-sm"

              @click="ouvrirDecision(ligne)"
            >
              Valider
            </PrimeButton>
            <PrimeButton
              v-if="estDemandeur && ['EN_ATTENTE_RH', 'EN_ATTENTE_FINANCE', 'EN_ATTENTE_DIRECTION', 'A_CORRIGER'].includes(ligne.statut)"
              class="inline-flex items-center justify-center gap-2 text-primary !text-sm"

              @click="annulerLigne(ligne)"
            >
              Annuler la ligne
            </PrimeButton>
          </div>
        </div>
      </section>
    </template>

    <section v-else class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900">
      <div class="p-4">
        <div v-if="demande.historique.length === 0" class="rounded-lg border border-dashed border-surface p-6 text-center text-muted-color">Aucun changement de statut enregistre.</div>
        <div v-else class="overflow-x-auto">
             <DataTable :value="demande.historique" striped-rows size="small" responsive-layout="scroll">
               <Column header="Date"><template #body="{ data }">{{ formatDateHeure(data.dateAction) }}</template></Column>
               <Column field="idDemandePoste" header="Ligne" />
               <Column header="Ancien statut"><template #body="{ data }"><StatusBadge :valeur="data.ancienStatut" /></template></Column>
               <Column header="Nouveau statut"><template #body="{ data }"><StatusBadge :valeur="data.nouveauStatut" /></template></Column>
               <Column header="Utilisateur"><template #body="{ data }">{{ data.utilisateur || '—' }}</template></Column>
               <Column header="Commentaire"><template #body="{ data }">{{ data.commentaire || '—' }}</template></Column>
             </DataTable>
        </div>
      </div>
    </section>

    <ModalDialog
      v-if="action"
      titre="Decider sur la ligne"
      :sous-titre="action.intituleDemande"
      :fermable="!enregistrement"
      @fermer="action = null"
    >
      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="decision">Decision</label>
        <PrimeSelect id="decision" v-model="decision" class="w-full" :options="DECISIONS" option-label="libelle" option-value="valeur" />
      </div>

      <template v-if="saisieEvaluationRh">
        <div class="rounded-lg border p-3 text-sm border-blue-200 bg-blue-50 text-blue-700 mb-4">
          Examinez le dossier (profil, description, effectif) puis definissez le budget et la
          fourchette salariale de ce profil.
        </div>

        <div class="flex flex-col gap-2 mb-4">
          <label class="text-sm font-semibold text-muted-color" for="rh-budget"
            >Budget du profil (Ar) <span class="text-red-500">*</span></label
          >
          <PrimeInputText
            id="rh-budget"
            v-model.number="evaluationRh.budgetEstime"
            class="w-full"
            type="number"
            min="0"
            step="1000"
          />
          <span class="text-xs text-muted-color">Verifie ensuite par la Finance (disponibilite budgetaire).</span>
        </div>

        <div class="grid grid-cols-1 gap-x-4 md:grid-cols-2">
          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" for="rh-salaire-min">Salaire minimum (Ar)</label>
            <PrimeInputText
              id="rh-salaire-min"
              v-model.number="evaluationRh.salaireMinEstime"
              class="w-full"
              type="number"
              min="0"
              step="1000"
            />
          </div>
          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" for="rh-salaire-max">Salaire maximum (Ar)</label>
            <PrimeInputText
              id="rh-salaire-max"
              v-model.number="evaluationRh.salaireMaxEstime"
              class="w-full"
              type="number"
              min="0"
              step="1000"
            />
          </div>
        </div>
      </template>

      <div v-if="action.etapeCourante === 'FINANCE'" class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="montantVerifie">Montant verifie (Ar)</label>
        <PrimeInputText
          id="montantVerifie"
          v-model.number="montantVerifie"
          class="w-full"
          type="number"
          min="0"
          step="1000"
          :placeholder="String(action.budgetEstime ?? '')"
        />
        <span class="text-xs text-muted-color">Doit correspondre au budget defini par le RH ({{ montantRh(action.budgetEstime) }}).</span>
      </div>

      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="commentaire">Commentaire</label>
        <PrimeTextarea id="commentaire" v-model="commentaire" class="w-full" rows="3" />
        <span class="text-xs text-muted-color">Le commentaire est conserve dans l'historique de la ligne.</span>
      </div>

      <template #actions>
        <PrimeButton class="inline-flex items-center justify-center gap-2 border border-surface" @click="action = null">Annuler</PrimeButton>
        <PrimeButton class="inline-flex items-center justify-center gap-2 bg-primary text-primary-contrast" :disabled="enregistrement" @click="enregistrerDecision">
          Enregistrer la decision
        </PrimeButton>
      </template>
    </ModalDialog>

    <ModalDialog
      v-if="ligneEnCorrection"
      titre="Corriger la ligne"
      :sous-titre="ligneEnCorrection.intituleDemande"
      :fermable="!enregistrementCorrection"
      @fermer="ligneEnCorrection = null"
    >
      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="corrige-intitule">Intitule du poste</label>
        <PrimeInputText id="corrige-intitule" v-model="correction.intituleDemande" class="w-full" type="text" />
      </div>

      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="corrige-effectif">Effectif</label>
        <PrimeInputText
          id="corrige-effectif"
          v-model.number="correction.effectif"
          class="w-full"
          type="number"
          min="1"
          step="1"
        />
      </div>

      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="corrige-profil">Profil recherche</label>
        <PrimeInputText id="corrige-profil" v-model="correction.profilRecherche" class="w-full" type="text" />
      </div>

      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="corrige-description">Description du poste</label>
        <PrimeTextarea id="corrige-description" v-model="correction.descriptionPoste" class="w-full" rows="3" />
      </div>

      <template #actions>
        <PrimeButton
          class="inline-flex items-center justify-center gap-2 border border-surface"

          :disabled="enregistrementCorrection"
          @click="ligneEnCorrection = null"
        >
          Annuler
        </PrimeButton>
        <PrimeButton
          class="inline-flex items-center justify-center gap-2 bg-primary text-primary-contrast"

          :disabled="enregistrementCorrection"
          @click="enregistrerCorrection"
        >
          Enregistrer la correction
        </PrimeButton>
      </template>
    </ModalDialog>
  </div>
</template>
