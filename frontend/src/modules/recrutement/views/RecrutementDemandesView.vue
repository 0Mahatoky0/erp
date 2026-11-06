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
  // Redirection vers la vue de saisie de l'offre en passant l'ID du besoin validé en query param
  router.push({ 
    name: 'recrutement-offre-saisie', 
    query: { idDemandePoste: ligne.id } 
  })
}

onMounted(charger)
</script>

<template>
  <section class="mx-auto w-full max-w-5xl p-4 md:p-6">
    <header class="mb-6 flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
      <div>
        <p class="mb-1 text-xs font-semibold uppercase tracking-wider text-primary">Recrutement · Étape 1</p>
        <h1 class="m-0 text-2xl font-bold">Demandes acceptées</h1>
        <p class="mt-1 text-sm text-muted-color">
          Une ligne apparaît ici dès la première approbation et reste suivie pendant les
          validations suivantes.
        </p>
      </div>
      <PrimeButton label="Actualiser" icon="pi pi-refresh" :loading="chargement" @click="charger" />
    </header>

    <Message v-if="erreur" severity="error" :closable="false" class="mb-3">{{ erreur }}</Message>

    <div class="flex flex-col gap-3">
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
          <div class="rounded-lg border border-dashed border-surface p-8 text-center text-muted-color">
            <i class="pi pi-inbox text-2xl mb-2" />
            <p class="m-0">Aucune demande acceptée disponible.</p>
          </div>
        </template>

        <Column field="intituleDemande" header="Demande" sortable>
          <template #body="{ data }">
            <div class="flex flex-col">
              <strong>{{ data.intituleDemande }}</strong>
              <small class="text-muted-color">Demande #{{ data.idDemande }}</small>
            </div>
          </template>
        </Column>

        <Column header="Service / Département">
          <template #body="{ data }">
            {{ data.service }} · {{ data.departement || '-' }}
          </template>
        </Column>

        <Column field="profilRecherche" header="Profil recherché">
          <template #body="{ data }">
            <span :class="{ 'text-muted-color italic': !data.profilRecherche }">
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