<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import StatusBadge from '@/shared/components/StatusBadge.vue'
import { useNotificationStore } from '@/shared/store/useNotificationStore'
import { formatDate, formatMontant, libelle } from '@/shared/utils/format'
import { embaucheApi as api, ErreurApi } from '../api/embaucheApi'

const route = useRoute()
const router = useRouter()
const notifications = useNotificationStore()

const id = computed(() => route.params.id)
const dossier = ref(null)
const documents = ref([])
const contrats = ref([])
const affectations = ref([])
const onboarding = ref(null)
const chargement = ref(true)

async function charger() {
  chargement.value = true
  try {
    const [employe, listeDocuments, listeContrats, listeAffectations, parcours] = await Promise.all([
      api.get(`/employes/${id.value}/dossier`),
      api.get(`/employes/${id.value}/documents`),
      api.get(`/contrats/employe/${id.value}`),
      api.get(`/contrats/employe/${id.value}/affectations`),
      api.get(`/contrats/employe/${id.value}/onboarding`),
    ])
    dossier.value = employe
    documents.value = listeDocuments
    contrats.value = listeContrats
    affectations.value = listeAffectations
    onboarding.value = parcours
  } catch (cause) {
    notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Impossible de charger le dossier salarié.')
  } finally {
    chargement.value = false
  }
}

function telecharger(document) {
  api.telecharger(`/employes/documents/${document.id}`, document.nomFichier || 'document')
    .catch((cause) => notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Téléchargement impossible.'))
}

function telechargerContrat(contrat) {
  api.telecharger(`/contrats/${contrat.id}/document`, `${contrat.numero || 'contrat'}.pdf`)
    .catch((cause) => notifications.erreur(cause instanceof ErreurApi ? cause.message : 'Téléchargement impossible.'))
}

onMounted(charger)
</script>

<template>
  <div class="mx-auto flex max-w-7xl flex-col gap-5">
    <Button label="Retour" icon="pi pi-arrow-left" severity="secondary" variant="text" class="w-fit p-0"
      @click="router.back()" />

    <div v-if="chargement" class="py-10 text-center text-muted-color">Chargement du dossier...</div>

    <template v-else-if="dossier">
      <header class="flex flex-col justify-between gap-3 md:flex-row md:items-start">
        <div>
          <h1 class="m-0 text-2xl font-bold">{{ dossier.prenom }} {{ dossier.nom }}</h1>
          <p class="mt-1 mb-0 text-muted-color">
            Matricule {{ dossier.matricule || 'non attribué' }} · {{ dossier.poste || 'Poste non défini' }}
          </p>
        </div>
        <StatusBadge :valeur="dossier.statut" />
      </header>

      <Card>
        <template #title>Dossier salarié</template>
        <template #content>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div><div class="text-xs font-semibold uppercase text-muted-color">Email</div><div>{{ dossier.email || '—' }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Téléphone</div><div>{{ dossier.telephone || '—' }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">CIN</div><div>{{ dossier.cin || '—' }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Naissance</div><div>{{ formatDate(dossier.dateNaissance) }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Service</div><div>{{ dossier.service || '—' }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Département</div><div>{{ dossier.departement || '—' }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Date d’embauche</div><div>{{ formatDate(dossier.dateEmbauche) }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Première embauche</div><div>{{ formatDate(dossier.datePremiereEmbauche) }}</div></div>
            <div><div class="text-xs font-semibold uppercase text-muted-color">Créé le</div><div>{{ formatDate(dossier.dateCreation) }}</div></div>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>Contrats</template>
        <template #content>
          <DataTable :value="contrats" data-key="id" striped-rows size="small" responsive-layout="scroll">
            <Column field="numero" header="Numéro" />
            <Column field="typeContrat" header="Type" />
            <Column header="Début"><template #body="{ data }">{{ formatDate(data.dateDebut) }}</template></Column>
            <Column header="Fin prévue"><template #body="{ data }">{{ formatDate(data.dateFinPrevue) }}</template></Column>
            <Column header="Salaire"><template #body="{ data }">{{ formatMontant(data.salaireBase) }}</template></Column>
            <Column header="État"><template #body="{ data }"><StatusBadge :valeur="data.etat" /></template></Column>
            <Column header="Document">
              <template #body="{ data }">
                <Button v-if="data.documentPdfId" label="PDF" icon="pi pi-download" size="small" outlined
                  @click="telechargerContrat(data)" />
                <span v-else class="text-muted-color">—</span>
              </template>
            </Column>
          </DataTable>
        </template>
      </Card>

      <Card>
        <template #title>Affectations</template>
        <template #content>
          <DataTable :value="affectations" data-key="id" striped-rows size="small" responsive-layout="scroll">
            <Column header="Poste"><template #body="{ data }">#{{ data.idPoste || '—' }}</template></Column>
            <Column header="Service"><template #body="{ data }">#{{ data.idService || '—' }}</template></Column>
            <Column header="Début"><template #body="{ data }">{{ formatDate(data.dateDebut) }}</template></Column>
            <Column header="Fin"><template #body="{ data }">{{ formatDate(data.dateFin) }}</template></Column>
            <Column header="Motif"><template #body="{ data }">{{ libelle(data.motif) }}</template></Column>
            <Column header="Principale"><template #body="{ data }">{{ data.principale ? 'Oui' : 'Non' }}</template></Column>
            <Column header="Statut"><template #body="{ data }"><StatusBadge :valeur="data.statut" /></template></Column>
          </DataTable>
        </template>
      </Card>

      <Card v-if="onboarding">
        <template #title>Checklist d’intégration ({{ onboarding.progression || 0 }} %)</template>
        <template #content>
          <DataTable :value="onboarding.taches || []" data-key="id" striped-rows size="small" responsive-layout="scroll">
            <Column field="libelle" header="Tâche" />
            <Column field="categorie" header="Catégorie"><template #body="{ data }">{{ libelle(data.categorie) }}</template></Column>
            <Column field="responsable" header="Responsable" />
            <Column header="Échéance"><template #body="{ data }">{{ formatDate(data.echeance) }}</template></Column>
            <Column field="obligatoire" header="Obligatoire"><template #body="{ data }">{{ data.obligatoire ? 'Oui' : 'Non' }}</template></Column>
            <Column header="Statut"><template #body="{ data }"><StatusBadge :valeur="data.statut" /></template></Column>
          </DataTable>
        </template>
      </Card>

      <Card>
        <template #title>Pièces du dossier</template>
        <template #content>
          <DataTable :value="documents" data-key="id" striped-rows size="small" responsive-layout="scroll">
            <Column field="typeDocument" header="Type"><template #body="{ data }">{{ libelle(data.typeDocument) }}</template></Column>
            <Column field="titre" header="Titre" />
            <Column field="nomFichier" header="Fichier" />
            <Column header="Validité"><template #body="{ data }">{{ formatDate(data.dateValidite) }}</template></Column>
            <Column header="Statut"><template #body="{ data }"><StatusBadge :valeur="data.statut" /></template></Column>
            <Column header="Action">
              <template #body="{ data }">
                <Button label="Télécharger" icon="pi pi-download" size="small" outlined @click="telecharger(data)" />
              </template>
            </Column>
          </DataTable>
        </template>
      </Card>
    </template>
  </div>
</template>
