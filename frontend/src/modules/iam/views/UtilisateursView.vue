<script setup>
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import Tag from 'primevue/tag'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import { onMounted, ref } from 'vue'
import { iamApi as api } from '@/modules/iam/api/iamApi'
import EmptyState from '@/shared/components/EmptyState.vue'
import ModalDialog from '@/shared/components/ModalDialog.vue'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

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
    <div class="flex flex-col gap-4 mb-5 md:flex-row md:items-start md:justify-between">
      <div>
        <h1>Utilisateurs</h1>
        <p class="mt-1 text-sm text-muted-color">
          Comptes de l'application et roles. Un compte desactive ne peut plus se connecter.
        </p>
      </div>
      <PrimeButton class="inline-flex items-center justify-center gap-2 bg-primary text-primary-contrast" @click="nouvelleEdition">Nouvel utilisateur</PrimeButton>
    </div>

    <div class="flex flex-wrap items-end gap-3 mb-4">
      <div class="flex flex-col gap-2 mb-4 flex-1 min-w-60">
        <label class="text-sm font-semibold text-muted-color" for="recherche-utilisateur">Recherche</label>
        <PrimeInputText
          id="recherche-utilisateur"
          v-model="recherche"
          class="w-full"
          type="search"
          placeholder="Nom ou adresse e-mail"
          @keyup.enter="charger"
        />
      </div>
      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="role-utilisateur">Role</label>
        <PrimeSelect
          id="role-utilisateur"
          v-model="filtreRole"
          class="w-full"
          :options="[{ label: 'Tous les roles', value: '' }, ...roles.map((role) => ({ label: role, value: role }))]"
          option-label="label"
          option-value="value"
          @change="charger"
        />
      </div>
      <PrimeButton class="inline-flex items-center justify-center gap-2 border border-surface" @click="charger">Filtrer</PrimeButton>
    </div>

    <section class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900">
      <div class="flex items-center justify-between gap-3 border-b border-surface p-4"><h2>{{ utilisateurs.length }} compte(s)</h2></div>

      <div v-if="chargement" class="p-4 text-sm text-muted-color">Chargement des utilisateurs...</div>

      <EmptyState
        v-else-if="utilisateurs.length === 0"
        titre="Aucun utilisateur"
        message="Aucun compte ne correspond aux criteres selectionnes."
      />

      <DataTable v-else :value="utilisateurs" striped-rows paginator :rows="10" responsive-layout="scroll">
        <Column header="Nom"><template #body="{ data }"><span class="font-medium">{{ data.nom }}</span></template></Column>
        <Column field="email" header="Adresse e-mail" />
        <Column header="Role"><template #body="{ data }"><StatusBadge :valeur="data.role" /></template></Column>
        <Column header="Departement"><template #body="{ data }">{{ data.departementNom || '—' }}</template></Column>
        <Column header="Etat">
          <template #body="{ data }"><Tag :value="data.actif ? 'Actif' : 'Desactive'" :severity="data.actif ? 'success' : 'secondary'" rounded /></template>
        </Column>
        <Column header="">
          <template #body="{ data }"><PrimeButton :label="data.actif ? 'Desactiver' : 'Reactiver'" severity="secondary" size="small" outlined @click="changerActivation(data)" /></template>
        </Column>
      </DataTable>
    </section>

    <ModalDialog
      v-if="edition"
      titre="Nouvel utilisateur"
      :fermable="!enregistrement"
      @fermer="edition = null"
    >
      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="nom-form">Nom complet <span class="text-red-500">*</span></label>
        <PrimeInputText id="nom-form" v-model="formulaire.nom" class="w-full" type="text" />
      </div>

      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="email-form">Adresse e-mail <span class="text-red-500">*</span></label>
        <PrimeInputText id="email-form" v-model="formulaire.email" class="w-full" type="email" />
      </div>

      <div class="flex flex-col gap-2 mb-4">
        <label class="text-sm font-semibold text-muted-color" for="mdp-form">Mot de passe <span class="text-red-500">*</span></label>
        <PrimeInputText id="mdp-form" v-model="formulaire.motDePasse" class="w-full" type="password" />
        <span class="text-xs text-muted-color">Le mot de passe est conserve chiffre.</span>
      </div>

      <div class="grid grid-cols-1 gap-x-4 gap-y-1 md:grid-cols-2">
        <div class="flex flex-col gap-2 mb-4">
          <label class="text-sm font-semibold text-muted-color" for="role-form">Role <span class="text-red-500">*</span></label>
          <PrimeSelect
            id="role-form"
            v-model="formulaire.role"
            class="w-full"
            :options="roles.map((role) => ({ label: role, value: role }))"
            option-label="label"
            option-value="value"
          />
        </div>
        <div class="flex flex-col gap-2 mb-4">
          <label class="text-sm font-semibold text-muted-color" for="departement-form">Departement</label>
          <PrimeSelect
            id="departement-form"
            v-model="formulaire.idDepartement"
            class="w-full"
            :options="[{ id: null, nom: 'Tous les departements' }, ...departements]"
            option-label="nom"
            option-value="id"
          />
        </div>
      </div>

      <template #actions>
        <PrimeButton class="inline-flex items-center justify-center gap-2 border border-surface" @click="edition = null">Annuler</PrimeButton>
        <PrimeButton class="inline-flex items-center justify-center gap-2 bg-primary text-primary-contrast" :disabled="enregistrement" @click="enregistrer">
          Creer le compte
        </PrimeButton>
      </template>
    </ModalDialog>
  </div>
</template>
