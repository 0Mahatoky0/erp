<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import PrimeButton from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Message from 'primevue/message'

import { recrutementApi as api, ErreurApi } from '@/modules/recrutement/api/recrutementApi'
import { useAuthStore } from '@/shared/store/useAuthStore'

const auth = useAuthStore()
const router = useRouter()

const lignes = ref([])
const chargement = ref(false)
const erreur = ref('')

async function charger() {
  chargement.value = true
  erreur.value = ''
  try {
    lignes.value = await api.get('/demandes/lignes/acceptees')
  } catch (cause) {
    erreur.value =
      cause instanceof ErreurApi ? cause.message : 'Impossible de charger les demandes acceptées.'
  } finally {
    chargement.value = false
  }
}

function creerOffre(ligne) {
  router.push({ name: 'recrutement-offres', query: { idDemandePoste: ligne.id } })
}

onMounted(charger)
</script>

<template>
  <section class="recrutement-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Recrutement · Étape 1</p>
        <h1>Demandes acceptées</h1>
        <p class="description">
          Une ligne apparaît ici dès la première approbation et reste suivie pendant les
          validations suivantes.
        </p>
      </div>
      <PrimeButton label="Actualiser" icon="pi pi-refresh" :loading="chargement" @click="charger" />
    </header>

    <Message v-if="erreur" severity="error" :closable="false" class="mb-3">{{ erreur }}</Message>

    <div class="liste-wrapper">
      <DataTable
        :value="lignes"
        :loading="chargement"
        dataKey="id"
        stripedRows
        rowHover
        paginator
        :rows="10"
        :rowsPerPageOptions="[10, 25, 50]"
      >
        <template #empty>
          <div class="vide">
            <i class="pi pi-inbox" />
            <span>Aucune demande acceptée disponible.</span>
          </div>
        </template>

        <Column field="intituleDemande" header="Demande" sortable>
          <template #body="{ data }">
            <div class="flex flex-column">
              <strong>{{ data.intituleDemande }}</strong>
              <small class="muted">Demande #{{ data.idDemande }}</small>
            </div>
          </template>
        </Column>

        <Column header="Service / Département">
          <template #body="{ data }">
            {{ data.service }} · {{ data.departement }}
          </template>
        </Column>

        <Column field="profilRecherche" header="Profil recherché">
          <template #body="{ data }">
            <span :class="{ muted: !data.profilRecherche }">
              {{ data.profilRecherche || 'Profil non précisé' }}
            </span>
          </template>
        </Column>

        <Column field="effectif" header="Effectif" sortable style="width: 7rem" />

        <Column header="Étape actuelle">
          <template #body="{ data }">
            {{ data.etapeCourante || 'Terminée' }}
          </template>
        </Column>

        <Column header="Statut">
          <template #body>
            <Tag value="Approbation reçue" severity="success" icon="pi pi-check" />
          </template>
        </Column>

        <Column v-if="auth.estRh || auth.estDirection" header="" style="width: 10rem">
          <template #body="{ data }">
            <PrimeButton
              label="Créer une offre"
              icon="pi pi-plus"
              size="small"
              outlined
              @click="creerOffre(data)"
            />
          </template>
        </Column>
      </DataTable>
    </div>
  </section>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.eyebrow {
  margin: 0 0 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--p-primary-color);
}

h1 {
  margin: 0 0 0.35rem;
  font-size: 1.6rem;
  color: var(--p-text-color);
}

.description {
  margin: 0;
  max-width: 42rem;
  color: var(--p-text-muted-color);
}

.liste-wrapper {
  background: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  border-radius: 0.75rem;
  overflow: hidden;
}

.muted {
  color: var(--p-text-muted-color);
}

.vide {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem 0;
  color: var(--p-text-muted-color);
}

.vide i {
  font-size: 2rem;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }
}
</style>