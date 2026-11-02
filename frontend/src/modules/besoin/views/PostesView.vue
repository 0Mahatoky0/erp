<script setup>
import { computed, onMounted, ref } from 'vue'
import { besoinApi as api } from '@/modules/besoin/api/besoinApi'
import EmptyState from '@/shared/components/EmptyState.vue'
import ModalDialog from '@/shared/components/ModalDialog.vue'
import { useAuthStore } from '@/shared/store/useAuthStore'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatMontant } from '@/shared/utils/format'

const auth = useAuthStore()
const notifications = useNotificationStore()

const postes = ref([])
const services = ref([])
const chargement = ref(true)
const filtreService = ref('')
const filtreActif = ref('')
const recherche = ref('')

const edition = ref(null)
const enregistrement = ref(false)
const formulaire = ref({
  idService: null,
  intitule: '',
  description: '',
  salaireMin: null,
  salaireMax: null,
  actif: true,
})

const filtres = computed(() => {
  const parametres = new URLSearchParams()
  if (filtreService.value) {
    parametres.set('idService', filtreService.value)
  }
  if (filtreActif.value !== '') {
    parametres.set('actif', filtreActif.value)
  }
  if (recherche.value.trim()) {
    parametres.set('recherche', recherche.value.trim())
  }
  const suffixe = parametres.toString() ? `?${parametres}` : ''
  return suffixe
})

async function charger() {
  chargement.value = true
  try {
    const [listePostes, listeServices] = await Promise.all([
      api.get(`/postes${filtres.value}`),
      api.get('/referentiel/services'),
    ])
    postes.value = listePostes
    services.value = listeServices
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

function nouvelleEdition() {
  edition.value = { id: null }
  formulaire.value = {
    idService: filtreService.value || null,
    intitule: '',
    description: '',
    salaireMin: null,
    salaireMax: null,
    actif: true,
  }
}

function modifier(poste) {
  edition.value = { id: poste.id }
  formulaire.value = {
    idService: poste.idService,
    intitule: poste.intitule,
    description: poste.description || '',
    salaireMin: poste.salaireMin,
    salaireMax: poste.salaireMax,
    actif: poste.actif,
  }
}

async function enregistrer() {
  if (!edition.value) {
    return
  }
  enregistrement.value = true
  try {
    if (edition.value.id) {
      await api.put(`/postes/${edition.value.id}`, {
        intitule: formulaire.value.intitule.trim(),
        description: formulaire.value.description.trim() || null,
        salaireMin: formulaire.value.salaireMin,
        salaireMax: formulaire.value.salaireMax,
        actif: formulaire.value.actif,
      })
      notifications.succes('Le poste a ete mis a jour.')
    } else {
      await api.post('/postes', {
        idService: formulaire.value.idService,
        intitule: formulaire.value.intitule.trim(),
        description: formulaire.value.description.trim() || null,
        salaireMin: formulaire.value.salaireMin,
        salaireMax: formulaire.value.salaireMax,
      })
      notifications.succes('Le poste a ete cree.')
    }
    edition.value = null
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    enregistrement.value = false
  }
}

async function desactiver(poste) {
  try {
    await api.post(`/postes/${poste.id}/desactiver`)
    notifications.succes('Le poste a ete desactive.')
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  }
}
</script>

<template>
  <div>
    <div class="page__entete">
      <div>
        <h1>Catalogue des postes</h1>
        <p class="page__description">
          Postes officiels de l'entreprise. Un poste utilise par l'historique est desactive, jamais supprime.
        </p>
      </div>
      <button v-if="auth.estRh" class="bouton bouton--principal" type="button" @click="nouvelleEdition">
        Nouveau poste
      </button>
    </div>

    <div class="barre-filtres">
      <div class="champ champ--large">
        <label class="champ__libelle" for="recherche-poste">Recherche</label>
        <input
          id="recherche-poste"
          v-model="recherche"
          class="saisie"
          type="search"
          placeholder="Intitule ou description"
          @keyup.enter="charger"
        />
      </div>
      <div class="champ">
        <label class="champ__libelle" for="service-poste">Service</label>
        <select id="service-poste" v-model="filtreService" class="saisie" @change="charger">
          <option value="">Tous les services</option>
          <option v-for="service in services" :key="service.id" :value="service.id">{{ service.nom }}</option>
        </select>
      </div>
      <div class="champ">
        <label class="champ__libelle" for="actif-poste">Etat</label>
        <select id="actif-poste" v-model="filtreActif" class="saisie" @change="charger">
          <option value="">Tous</option>
          <option value="true">Actifs</option>
          <option value="false">Inactifs</option>
        </select>
      </div>
      <button class="bouton bouton--secondaire" type="button" @click="charger">Filtrer</button>
    </div>

    <section class="carte">
      <div class="carte__entete"><h2>{{ postes.length }} poste(s)</h2></div>

      <div v-if="chargement" class="chargement">Chargement du catalogue...</div>

      <EmptyState
        v-else-if="postes.length === 0"
        titre="Aucun poste"
        message="Aucun poste ne correspond aux criteres selectionnes."
      />

      <div v-else class="tableau-conteneur">
        <table class="tableau">
          <thead>
            <tr>
              <th>Intitule</th>
              <th>Service</th>
              <th>Departement</th>
              <th>Salaire min</th>
              <th>Salaire max</th>
              <th>Demandes liees</th>
              <th>Etat</th>
              <th v-if="auth.estRh"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="poste in postes" :key="poste.id">
              <td>
                <div style="font-weight: 500">{{ poste.intitule }}</div>
                <div v-if="poste.description" class="texte-discret" style="font-size: 12px">
                  {{ poste.description }}
                </div>
              </td>
              <td>{{ poste.service }}</td>
              <td>{{ poste.departement }}</td>
              <td class="mono">{{ formatMontant(poste.salaireMin) }}</td>
              <td class="mono">{{ formatMontant(poste.salaireMax) }}</td>
              <td>{{ poste.nbPostesLies }}</td>
              <td>
                <span class="badge" :class="poste.actif ? 'badge--succes' : 'badge--neutre'">
                  {{ poste.actif ? 'Actif' : 'Desactive' }}
                </span>
              </td>
              <td v-if="auth.estRh" class="tableau__actions">
                <button class="bouton bouton--secondaire bouton--petit" type="button" @click="modifier(poste)">
                  Modifier
                </button>
                <button
                  v-if="poste.actif"
                  class="bouton bouton--fantome bouton--petit"
                  type="button"
                  @click="desactiver(poste)"
                >
                  Desactiver
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <ModalDialog
      v-if="edition"
      :titre="edition.id ? 'Modifier le poste' : 'Nouveau poste'"
      :fermable="!enregistrement"
      @fermer="edition = null"
    >
      <div v-if="!edition.id" class="champ">
        <label class="champ__libelle" for="service-form">Service <span class="champ__obligatoire">*</span></label>
        <select id="service-form" v-model="formulaire.idService" class="saisie">
          <option :value="null" disabled>Choisir un service</option>
          <option v-for="service in services" :key="service.id" :value="service.id">{{ service.nom }}</option>
        </select>
      </div>

      <div class="champ">
        <label class="champ__libelle" for="intitule-form">Intitule <span class="champ__obligatoire">*</span></label>
        <input id="intitule-form" v-model="formulaire.intitule" class="saisie" type="text" />
      </div>

      <div class="champ">
        <label class="champ__libelle" for="description-form">Description</label>
        <textarea id="description-form" v-model="formulaire.description" class="saisie" rows="3" />
      </div>

      <div class="grille-champs">
        <div class="champ">
          <label class="champ__libelle" for="salaire-min-form">Salaire minimum (Ar)</label>
          <input id="salaire-min-form" v-model.number="formulaire.salaireMin" class="saisie" type="number" min="0" step="1000" />
        </div>
        <div class="champ">
          <label class="champ__libelle" for="salaire-max-form">Salaire maximum (Ar)</label>
          <input id="salaire-max-form" v-model.number="formulaire.salaireMax" class="saisie" type="number" min="0" step="1000" />
        </div>
      </div>

      <div v-if="edition.id" class="champ">
        <label class="champ__libelle" for="actif-form">Etat du poste</label>
        <select id="actif-form" v-model="formulaire.actif" class="saisie">
          <option :value="true">Actif</option>
          <option :value="false">Desactive</option>
        </select>
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
