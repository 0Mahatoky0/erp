<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api/client'
import HistoriqueValidations from '@/components/HistoriqueValidations.vue'
import ModalDialog from '@/components/ModalDialog.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { formatDate, formatDateHeure, formatMontant, libelle } from '@/utils/format'

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

const ligneEnCorrection = ref(null)
const correction = ref({
  intituleDemande: '',
  effectif: 1,
  budgetEstime: 0,
  salaireMinEstime: null,
  salaireMaxEstime: null,
  profilRecherche: '',
  descriptionPoste: '',
})
const enregistrementCorrection = ref(false)

const idDemande = computed(() => Number(route.params.id))

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
}

async function enregistrerDecision() {
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

const totalLignes = computed(() => demande.value?.lignes?.length ?? 0)

/** Ouvre la correction d'une ligne demandee (RM-021, RM-037). */
function ouvrirCorrection(ligne) {
  ligneEnCorrection.value = ligne
  correction.value = {
    intituleDemande: ligne.intituleDemande ?? '',
    effectif: ligne.effectif ?? 1,
    budgetEstime: ligne.budgetEstime ?? 0,
    salaireMinEstime: ligne.salaireMinEstime ?? null,
    salaireMaxEstime: ligne.salaireMaxEstime ?? null,
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
      budgetEstime: Number(correction.value.budgetEstime),
      salaireMinEstime:
        correction.value.salaireMinEstime === null ? null : Number(correction.value.salaireMinEstime),
      salaireMaxEstime:
        correction.value.salaireMaxEstime === null ? null : Number(correction.value.salaireMaxEstime),
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
  <div v-if="chargement" class="chargement">Chargement de la demande...</div>

  <div v-else-if="demande">
    <div class="fil-ariane">
      <RouterLink :to="{ name: 'demandes' }">Demandes</RouterLink>
      <span>/</span>
      <span class="mono">DEM-{{ String(demande.id).padStart(4, '0') }}</span>
    </div>

    <div class="page__entete">
      <div>
        <h1>{{ demande.motif }}</h1>
        <p class="page__description">
          Demandee par {{ demande.demandeur }} · creation le {{ formatDateHeure(demande.dateCreation) }}
        </p>
      </div>
      <div style="display: flex; gap: 10px; align-items: center">
        <StatusBadge :valeur="demande.statut" />
        <button
          v-if="peutSoumettre"
          class="bouton bouton--principal"
          type="button"
          @click="soumettre"
        >
          Soumettre la demande
        </button>
      </div>
    </div>

    <div class="grille grille--3" style="margin-bottom: 16px">
      <section class="carte">
        <div class="carte__corps">
          <div class="liste-definitions" style="grid-template-columns: repeat(2, minmax(0, 1fr))">
            <div>
              <div class="liste-definitions__libelle">Date souhaitee</div>
              <div class="liste-definitions__valeur">{{ formatDate(demande.dateSouhaitee) }}</div>
            </div>
            <div>
              <div class="liste-definitions__libelle">Date de soumission</div>
              <div class="liste-definitions__valeur">{{ formatDateHeure(demande.dateSoumission) }}</div>
            </div>
            <div>
              <div class="liste-definitions__libelle">Lignes</div>
              <div class="liste-definitions__valeur">{{ totalLignes }}</div>
            </div>
            <div>
              <div class="liste-definitions__libelle">Budget total</div>
              <div class="liste-definitions__valeur">
                {{ formatMontant(demande.lignes.reduce((s, l) => s + Number(l.budgetEstime || 0), 0)) }}
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="carte" style="grid-column: span 2">
        <div class="carte__corps">
          <div class="liste-definitions">
            <div>
              <div class="liste-definitions__libelle">Lignes acceptees</div>
              <div class="liste-definitions__valeur">
                {{ demande.lignes.filter((l) => l.statut === 'ACCEPTEE').length }}
              </div>
            </div>
            <div>
              <div class="liste-definitions__libelle">En cours de validation</div>
              <div class="liste-definitions__valeur">
                {{ demande.lignes.filter((l) => ['EN_ATTENTE_RH', 'EN_ATTENTE_FINANCE', 'EN_ATTENTE_DIRECTION'].includes(l.statut)).length }}
              </div>
            </div>
            <div>
              <div class="liste-definitions__libelle">A corriger</div>
              <div class="liste-definitions__valeur">
                {{ demande.lignes.filter((l) => l.statut === 'A_CORRIGER').length }}
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <div v-if="peutCorriger" class="carte carte--accent" style="margin-bottom: 14px">
      <div class="carte__corps">
        <strong>Des lignes vous ont ete renvoyees a corriger.</strong>
        Corrigez-les puis resoumettez-les : elles reprendront le circuit a l'etape concernee.
      </div>
    </div>

    <div class="onglets">
      <button class="onglet" :class="{ actif: onglet === 'lignes' }" type="button" @click="onglet = 'lignes'">
        Lignes ({{ totalLignes }})
      </button>
      <button class="onglet" :class="{ actif: onglet === 'historique' }" type="button" @click="onglet = 'historique'">
        Journal des statuts
      </button>
    </div>

    <template v-if="onglet === 'lignes'">
      <section v-for="ligne in demande.lignes" :key="ligne.id" class="carte" style="margin-bottom: 14px">
        <div class="carte__entete">
          <div>
            <h2>{{ ligne.intituleDemande }}</h2>
            <p class="texte-discret" style="margin: 2px 0 0; font-size: 12.5px">
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

        <div class="carte__corps">
          <div class="liste-definitions">
            <div>
              <div class="liste-definitions__libelle">Effectif</div>
              <div class="liste-definitions__valeur">{{ ligne.effectif }}</div>
            </div>
            <div>
              <div class="liste-definitions__libelle">Budget estime</div>
              <div class="liste-definitions__valeur">{{ formatMontant(ligne.budgetEstime) }}</div>
            </div>
            <div>
              <div class="liste-definitions__libelle">Salaire estime</div>
              <div class="liste-definitions__valeur">
                {{ formatMontant(ligne.salaireMinEstime) }} — {{ formatMontant(ligne.salaireMaxEstime) }}
              </div>
            </div>
            <div>
              <div class="liste-definitions__libelle">Poste rattache</div>
              <div class="liste-definitions__valeur">{{ ligne.poste || 'A creer a l\'acceptation' }}</div>
            </div>
            <div>
              <div class="liste-definitions__libelle">Employe remplace</div>
              <div class="liste-definitions__valeur">
                {{ ligne.employeRemplace ? `${ligne.employeRemplace} (${ligne.matriculeRemplace || '—'})` : '—' }}
              </div>
            </div>
            <div>
              <div class="liste-definitions__libelle">Periode de remplacement</div>
              <div class="liste-definitions__valeur">
                {{ ligne.dateDebutRemplacement ? `${formatDate(ligne.dateDebutRemplacement)} → ${formatDate(ligne.dateFinRemplacement)}` : '—' }}
              </div>
            </div>
            <div>
              <div class="liste-definitions__libelle">Profil recherche</div>
              <div class="liste-definitions__valeur">{{ ligne.profilRecherche || '—' }}</div>
            </div>
            <div>
              <div class="liste-definitions__libelle">Description</div>
              <div class="liste-definitions__valeur">{{ ligne.descriptionPoste || '—' }}</div>
            </div>
            <div>
              <div class="liste-definitions__libelle">Motif du remplacement</div>
              <div class="liste-definitions__valeur">{{ ligne.motifRemplacement || '—' }}</div>
            </div>
          </div>
        </div>

        <div class="carte__pied" style="display: flex; justify-content: space-between; gap: 10px; flex-wrap: wrap">
          <details style="flex: 1">
            <summary class="texte-secondaire" style="cursor: pointer; font-size: 13px">
              Historique des decisions ({{ ligne.validations.length }})
            </summary>
            <div style="margin-top: 10px">
              <HistoriqueValidations :validations="ligne.validations" />
            </div>
          </details>

          <div style="display: flex; gap: 8px">
            <button
              v-if="ligne.statut === 'A_CORRIGER' && estDemandeur"
              class="bouton bouton--secondaire bouton--petit"
              type="button"
              @click="ouvrirCorrection(ligne)"
            >
              Corriger
            </button>
            <button
              v-if="ligne.statut === 'A_CORRIGER' && estDemandeur"
              class="bouton bouton--principal bouton--petit"
              type="button"
              @click="resoumettre(ligne)"
            >
              Resoumettre
            </button>
            <button
              v-if="['EN_ATTENTE_RH', 'EN_ATTENTE_FINANCE', 'EN_ATTENTE_DIRECTION'].includes(ligne.statut) && peutValider(ligne)"
              class="bouton bouton--secondaire bouton--petit"
              type="button"
              @click="ouvrirDecision(ligne)"
            >
              Valider
            </button>
            <button
              v-if="estDemandeur && ['EN_ATTENTE_RH', 'EN_ATTENTE_FINANCE', 'EN_ATTENTE_DIRECTION', 'A_CORRIGER'].includes(ligne.statut)"
              class="bouton bouton--fantome bouton--petit"
              type="button"
              @click="annulerLigne(ligne)"
            >
              Annuler la ligne
            </button>
          </div>
        </div>
      </section>
    </template>

    <section v-else class="carte">
      <div class="carte__corps">
        <div v-if="demande.historique.length === 0" class="etat-vide">Aucun changement de statut enregistre.</div>
        <div v-else class="tableau-conteneur">
          <table class="tableau tableau--compacte">
            <thead>
              <tr>
                <th>Date</th>
                <th>Ligne</th>
                <th>Ancien statut</th>
                <th>Nouveau statut</th>
                <th>Utilisateur</th>
                <th>Commentaire</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(historique, index) in demande.historique" :key="index">
                <td class="mono">{{ formatDateHeure(historique.dateAction) }}</td>
                <td class="mono">{{ historique.idDemandePoste }}</td>
                <td><StatusBadge :valeur="historique.ancienStatut" /></td>
                <td><StatusBadge :valeur="historique.nouveauStatut" /></td>
                <td>{{ historique.utilisateur || '—' }}</td>
                <td>{{ historique.commentaire || '—' }}</td>
              </tr>
            </tbody>
          </table>
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
      <div class="champ">
        <label class="champ__libelle" for="decision">Decision</label>
        <select id="decision" v-model="decision" class="saisie">
          <option v-for="item in DECISIONS" :key="item.valeur" :value="item.valeur">{{ item.libelle }}</option>
        </select>
      </div>

      <div v-if="action.etapeCourante === 'FINANCE'" class="champ">
        <label class="champ__libelle" for="montantVerifie">Montant verifie (Ar)</label>
        <input
          id="montantVerifie"
          v-model.number="montantVerifie"
          class="saisie"
          type="number"
          min="0"
          step="1000"
          :placeholder="String(action.budgetEstime)"
        />
        <span class="champ__aide">Doit correspondre au montant soumis ({{ formatMontant(action.budgetEstime) }}).</span>
      </div>

      <div class="champ">
        <label class="champ__libelle" for="commentaire">Commentaire</label>
        <textarea id="commentaire" v-model="commentaire" class="saisie" rows="3" />
        <span class="champ__aide">Le commentaire est conserve dans l'historique de la ligne.</span>
      </div>

      <template #actions>
        <button class="bouton bouton--secondaire" type="button" @click="action = null">Annuler</button>
        <button class="bouton bouton--principal" type="button" :disabled="enregistrement" @click="enregistrerDecision">
          Enregistrer la decision
        </button>
      </template>
    </ModalDialog>

    <ModalDialog
      v-if="ligneEnCorrection"
      titre="Corriger la ligne"
      :sous-titre="ligneEnCorrection.intituleDemande"
      :fermable="!enregistrementCorrection"
      @fermer="ligneEnCorrection = null"
    >
      <div class="champ">
        <label class="champ__libelle" for="corrige-intitule">Intitule du poste</label>
        <input id="corrige-intitule" v-model="correction.intituleDemande" class="saisie" type="text" />
      </div>

      <div class="champ">
        <label class="champ__libelle" for="corrige-effectif">Effectif</label>
        <input
          id="corrige-effectif"
          v-model.number="correction.effectif"
          class="saisie"
          type="number"
          min="1"
          step="1"
        />
      </div>

      <div class="champ">
        <label class="champ__libelle" for="corrige-budget">Budget estime (Ar)</label>
        <input
          id="corrige-budget"
          v-model.number="correction.budgetEstime"
          class="saisie"
          type="number"
          min="0"
          step="1000"
        />
      </div>

      <div class="champ">
        <label class="champ__libelle" for="corrige-salaire-min">Salaire minimum estime (Ar)</label>
        <input
          id="corrige-salaire-min"
          v-model.number="correction.salaireMinEstime"
          class="saisie"
          type="number"
          min="0"
          step="1000"
        />
      </div>

      <div class="champ">
        <label class="champ__libelle" for="corrige-salaire-max">Salaire maximum estime (Ar)</label>
        <input
          id="corrige-salaire-max"
          v-model.number="correction.salaireMaxEstime"
          class="saisie"
          type="number"
          min="0"
          step="1000"
        />
      </div>

      <div class="champ">
        <label class="champ__libelle" for="corrige-profil">Profil recherche</label>
        <input id="corrige-profil" v-model="correction.profilRecherche" class="saisie" type="text" />
      </div>

      <div class="champ">
        <label class="champ__libelle" for="corrige-description">Description du poste</label>
        <textarea id="corrige-description" v-model="correction.descriptionPoste" class="saisie" rows="3" />
      </div>

      <template #actions>
        <button
          class="bouton bouton--secondaire"
          type="button"
          :disabled="enregistrementCorrection"
          @click="ligneEnCorrection = null"
        >
          Annuler
        </button>
        <button
          class="bouton bouton--principal"
          type="button"
          :disabled="enregistrementCorrection"
          @click="enregistrerCorrection"
        >
          Enregistrer la correction
        </button>
      </template>
    </ModalDialog>
  </div>
</template>
