<script setup>
import { onMounted, ref } from 'vue'
import { api } from '@/api/client'
import EmptyState from '@/components/EmptyState.vue'
import ModalDialog from '@/components/ModalDialog.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { useNotificationStore } from '@/stores/notification'

const notifications = useNotificationStore()

const utilisateurs = ref([])
const departements = ref([])
const roles = ref([])
const chargement = ref(true)
const recherche = ref('')
const filtreRole = ref('')

const edition = ref(null)
const enregistrement = ref(false)
const formulaire = ref({
  nom: '',
  email: '',
  motDePasse: '',
  role: 'MANAGER',
  idDepartement: null,
})

async function charger() {
  chargement.value = true
  try {
    const [listeUtilisateurs, listeDepartements, listeRoles] = await Promise.all([
      api.get('/utilisateurs'),
      api.get('/referentiel/departements'),
      api.get('/utilisateurs/roles'),
    ])
    utilisateurs.value = listeUtilisateurs
    departements.value = listeDepartements
    roles.value = listeRoles
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
    nom: '',
    email: '',
    motDePasse: '',
    role: 'MANAGER',
    idDepartement: null,
  }
}

async function enregistrer() {
  if (!edition.value) {
    return
  }
  enregistrement.value = true
  try {
    await api.post('/utilisateurs', {
      nom: formulaire.value.nom.trim(),
      email: formulaire.value.email.trim(),
      motDePasse: formulaire.value.motDePasse,
      role: formulaire.value.role,
      idDepartement: formulaire.value.idDepartement,
    })
    notifications.succes('Le compte a ete cree.')
    edition.value = null
    await charger()
  } catch (e) {
    notifications.erreur(e.message)
  } finally {
    enregistrement.value = false
  }
}

async function changerActivation(utilisateur) {
  try {
    await api.post(`/utilisateurs/${utilisateur.id}/activation?actif=${!utilisateur.actif}`)
    notifications.succes(
      utilisateur.actif ? 'Le compte a ete desactive.' : 'Le compte a ete reactive.',
    )
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
        <h1>Utilisateurs</h1>
        <p class="page__description">
          Comptes de l'application et roles. Un compte desactive ne peut plus se connecter.
        </p>
      </div>
      <button class="bouton bouton--principal" type="button" @click="nouvelleEdition">Nouvel utilisateur</button>
    </div>

    <div class="barre-filtres">
      <div class="champ champ--large">
        <label class="champ__libelle" for="recherche-utilisateur">Recherche</label>
        <input
          id="recherche-utilisateur"
          v-model="recherche"
          class="saisie"
          type="search"
          placeholder="Nom ou adresse e-mail"
          @keyup.enter="charger"
        />
      </div>
      <div class="champ">
        <label class="champ__libelle" for="role-utilisateur">Role</label>
        <select id="role-utilisateur" v-model="filtreRole" class="saisie" @change="charger">
          <option value="">Tous les roles</option>
          <option v-for="role in roles" :key="role" :value="role">{{ role }}</option>
        </select>
      </div>
      <button class="bouton bouton--secondaire" type="button" @click="charger">Filtrer</button>
    </div>

    <section class="carte">
      <div class="carte__entete"><h2>{{ utilisateurs.length }} compte(s)</h2></div>

      <div v-if="chargement" class="chargement">Chargement des utilisateurs...</div>

      <EmptyState
        v-else-if="utilisateurs.length === 0"
        titre="Aucun utilisateur"
        message="Aucun compte ne correspond aux criteres selectionnes."
      />

      <div v-else class="tableau-conteneur">
        <table class="tableau">
          <thead>
            <tr>
              <th>Nom</th>
              <th>Adresse e-mail</th>
              <th>Role</th>
              <th>Departement</th>
              <th>Etat</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="utilisateur in utilisateurs" :key="utilisateur.id">
              <td style="font-weight: 500">{{ utilisateur.nom }}</td>
              <td>{{ utilisateur.email }}</td>
              <td><StatusBadge :valeur="utilisateur.role" /></td>
              <td>{{ utilisateur.departementNom || '—' }}</td>
              <td>
                <span class="badge" :class="utilisateur.actif ? 'badge--succes' : 'badge--neutre'">
                  {{ utilisateur.actif ? 'Actif' : 'Desactive' }}
                </span>
              </td>
              <td class="tableau__actions">
                <button class="bouton bouton--secondaire bouton--petit" type="button" @click="changerActivation(utilisateur)">
                  {{ utilisateur.actif ? 'Desactiver' : 'Reactiver' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <ModalDialog
      v-if="edition"
      titre="Nouvel utilisateur"
      :fermable="!enregistrement"
      @fermer="edition = null"
    >
      <div class="champ">
        <label class="champ__libelle" for="nom-form">Nom complet <span class="champ__obligatoire">*</span></label>
        <input id="nom-form" v-model="formulaire.nom" class="saisie" type="text" />
      </div>

      <div class="champ">
        <label class="champ__libelle" for="email-form">Adresse e-mail <span class="champ__obligatoire">*</span></label>
        <input id="email-form" v-model="formulaire.email" class="saisie" type="email" />
      </div>

      <div class="champ">
        <label class="champ__libelle" for="mdp-form">Mot de passe <span class="champ__obligatoire">*</span></label>
        <input id="mdp-form" v-model="formulaire.motDePasse" class="saisie" type="password" />
        <span class="champ__aide">Le mot de passe est conserve chiffre.</span>
      </div>

      <div class="grille-champs">
        <div class="champ">
          <label class="champ__libelle" for="role-form">Role <span class="champ__obligatoire">*</span></label>
          <select id="role-form" v-model="formulaire.role" class="saisie">
            <option v-for="role in roles" :key="role" :value="role">{{ role }}</option>
          </select>
        </div>
        <div class="champ">
          <label class="champ__libelle" for="departement-form">Departement</label>
          <select id="departement-form" v-model="formulaire.idDepartement" class="saisie">
            <option :value="null">Tous les departements</option>
            <option v-for="departement in departements" :key="departement.id" :value="departement.id">
              {{ departement.nom }}
            </option>
          </select>
        </div>
      </div>

      <template #actions>
        <button class="bouton bouton--secondaire" type="button" @click="edition = null">Annuler</button>
        <button class="bouton bouton--principal" type="button" :disabled="enregistrement" @click="enregistrer">
          Creer le compte
        </button>
      </template>
    </ModalDialog>
  </div>
</template>
