<script setup>
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import { onMounted, ref } from 'vue'
import { employeApi as api } from '@/modules/employe/api/employeApi'
import EmptyState from '@/shared/components/EmptyState.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDate } from '@/shared/utils/format'

const notifications = useNotificationStore()

const employes = ref([])
const services = ref([])
const chargement = ref(true)
const filtreService = ref('')
const filtreStatut = ref('')
const recherche = ref('')

const STATUTS = [
  { valeur: '', libelle: 'Tous les statuts' },
  { valeur: 'ACTIF', libelle: 'Actif' },
  { valeur: 'INACTIF', libelle: 'Inactif' },
  { valeur: 'PARTI', libelle: 'Parti' },
]

async function charger() {
  chargement.value = true
  try {
    const parametres = new URLSearchParams()
    if (filtreService.value) {
      parametres.set('idService', filtreService.value)
    }
    if (filtreStatut.value) {
      parametres.set('statut', filtreStatut.value)
    }
    if (recherche.value.trim()) {
      parametres.set('recherche', recherche.value.trim())
    }
    const suffixe = parametres.toString() ? `?${parametres}` : ''
    const [listeEmployes, listeServices] = await Promise.all([
      api.get(`/referentiel/employes${suffixe}`),
      api.get('/referentiel/services'),
    ])
    employes.value = listeEmployes
    services.value = listeServices
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
        <h1>Employes</h1>
        <p class="page__description">
          Fiches des collaborateurs. Ces employes peuvent etre designes comme personnes remplacees
          dans une demande.
        </p>
      </div>
    </div>

    <div class="barre-filtres">
      <div class="champ champ--large">
        <label class="champ__libelle" for="recherche-employe">Recherche</label>
        <PrimeInputText
          id="recherche-employe"
          v-model="recherche"
          class="saisie"
          type="search"
          placeholder="Nom, prenom ou matricule"
          @keyup.enter="charger"
        />
      </div>
      <div class="champ">
        <label class="champ__libelle" for="service-employe">Service</label>
        <select id="service-employe" v-model="filtreService" class="saisie" @change="charger">
          <option value="">Tous les services</option>
          <option v-for="service in services" :key="service.id" :value="service.id">{{ service.nom }}</option>
        </select>
      </div>
      <div class="champ">
        <label class="champ__libelle" for="statut-employe">Statut</label>
        <select id="statut-employe" v-model="filtreStatut" class="saisie" @change="charger">
          <option v-for="statut in STATUTS" :key="statut.valeur" :value="statut.valeur">{{ statut.libelle }}</option>
        </select>
      </div>
      <PrimeButton class="bouton bouton--secondaire" @click="charger">Filtrer</PrimeButton>
    </div>

    <section class="carte">
      <div class="carte__entete"><h2>{{ employes.length }} employe(s)</h2></div>

      <div v-if="chargement" class="chargement">Chargement des employes...</div>

      <EmptyState
        v-else-if="employes.length === 0"
        titre="Aucun employe"
        message="Aucun employe ne correspond aux criteres selectionnes."
      />

      <div v-else class="tableau-conteneur">
        <table class="tableau">
          <thead>
            <tr>
              <th>Matricule</th>
              <th>Nom</th>
              <th>Poste</th>
              <th>Service</th>
              <th>Contact</th>
              <th>Date d'embauche</th>
              <th>Statut</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="employe in employes" :key="employe.id">
              <td class="mono">{{ employe.matricule }}</td>
              <td style="font-weight: 500">{{ employe.nom }} {{ employe.prenom }}</td>
              <td>{{ employe.poste || '—' }}</td>
              <td>{{ employe.service || '—' }}</td>
              <td>
                <div>{{ employe.email || '—' }}</div>
                <div class="texte-discret" style="font-size: 12px">{{ employe.telephone || '' }}</div>
              </td>
              <td class="mono">{{ formatDate(employe.dateEmbauche) }}</td>
              <td><StatusBadge :valeur="employe.statut" /></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
