<script setup>
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
import PrimeSelect from 'primevue/select'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { besoinApi as api } from '@/modules/besoin/api/besoinApi'
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const router = useRouter()
const notifications = useNotificationStore()

const services = ref([])
const postes = ref([])
const employes = ref([])

const motif = ref('')
const dateSouhaitee = ref(dateDansTroisMois())
const lignes = ref([nouvelleLigne()])
const envoi = ref(false)
const erreurGlobale = ref(null)

function dateDansTroisMois() {
  const date = new Date()
  date.setMonth(date.getMonth() + 3)
  return date.toISOString().slice(0, 10)
}

function nouvelleLigne() {
  return {
    idService: null,
    idPoste: null,
    typeBesoin: 'CREATION',
    idEmployeRemplace: null,
    typeRemplacement: 'DEFINITIF',
    motifRemplacement: '',
    dateDebutRemplacement: '',
    dateFinRemplacement: '',
    intituleDemande: '',
    descriptionPoste: '',
    profilRecherche: '',
    salaireMinEstime: null,
    salaireMaxEstime: null,
    effectif: 1,
    budgetEstime: null,
  }
}

/** Postes du service de la ligne, pour rattacher celle-ci a un poste existant. */
function postesPour(ligne) {
  return postes.value.filter((poste) => !ligne.idService || poste.idService === ligne.idService)
}

/** Employes du service de la ligne, pour identifier la personne remplacee. */
function employesPour(ligne) {
  return employes.value.filter(
    (employe) => !ligne.idService || employe.idService === ligne.idService,
  )
}

const budgetTotal = computed(() =>
  lignes.value.reduce((somme, ligne) => somme + Number(ligne.budgetEstime || 0), 0),
)

function ajouterLigne() {
  lignes.value.push(nouvelleLigne())
}

function supprimerLigne(index) {
  if (lignes.value.length === 1) {
    notifications.erreur('Une demande doit contenir au moins une ligne.')
    return
  }
  lignes.value.splice(index, 1)
}

/** Un poste existant selectionne doit appartenir au service de la ligne (RM-030). */
function surChangementPoste(ligne) {
  if (!ligne.idPoste) {
    return
  }
  const poste = postes.value.find((item) => item.id === ligne.idPoste)
  if (poste && ligne.idService && poste.idService !== ligne.idService) {
    ligne.idService = poste.idService
  }
}

async function chargerReferentiels() {
  try {
    const [listeServices, listePostes, listeEmployes] = await Promise.all([
      api.get('/referentiel/services'),
      api.get('/postes'),
      api.get('/referentiel/employes'),
    ])
    services.value = listeServices
    postes.value = listePostes
    employes.value = listeEmployes
  } catch (e) {
    notifications.erreur(e.message)
  }
}

onMounted(chargerReferentiels)

function corpsLigne(ligne) {
  return {
    idService: ligne.idService,
    idPoste: ligne.idPoste || null,
    typeBesoin: ligne.typeBesoin,
    typeRemplacement: ligne.typeBesoin === 'REMPLACEMENT' ? ligne.typeRemplacement : null,
    idEmployeRemplace: ligne.typeBesoin === 'REMPLACEMENT' ? ligne.idEmployeRemplace : null,
    motifRemplacement: ligne.typeBesoin === 'REMPLACEMENT' ? ligne.motifRemplacement : null,
    dateDebutRemplacement:
      ligne.typeBesoin === 'REMPLACEMENT' ? ligne.dateDebutRemplacement || null : null,
    dateFinRemplacement:
      ligne.typeBesoin === 'REMPLACEMENT' ? ligne.dateFinRemplacement || null : null,
    intituleDemande: ligne.intituleDemande,
    descriptionPoste: ligne.descriptionPoste || null,
    profilRecherche: ligne.profilRecherche || null,
    salaireMinEstime: ligne.salaireMinEstime,
    salaireMaxEstime: ligne.salaireMaxEstime,
    effectif: ligne.effectif,
    budgetEstime: ligne.budgetEstime,
  }
}

async function enregistrer(soumettre) {
  erreurGlobale.value = null
  envoi.value = true
  try {
    const demande = await api.post('/demandes', {
      motif: motif.value.trim(),
      dateSouhaitee: dateSouhaitee.value,
      lignes: lignes.value.map(corpsLigne),
    })

    if (soumettre) {
      await api.post(`/demandes/${demande.id}/soumettre`)
      notifications.succes('La demande a ete soumise au circuit de validation.')
    } else {
      notifications.succes('La demande a ete enregistree en brouillon.')
    }
    router.push({ name: 'demande-detail', params: { id: demande.id } })
  } catch (e) {
    erreurGlobale.value = e.message
  } finally {
    envoi.value = false
  }
}
</script>

<template>
  <div>
    <div class="flex flex-col gap-4 mb-5 md:flex-row md:items-start md:justify-between">
      <div>
        <h1>Nouvelle demande</h1>
        <p class="mt-1 text-sm text-muted-color">
          Une demande peut contenir plusieurs besoins de nature differente. Chaque ligne suivra son
          propre circuit de validation.
        </p>
      </div>
      <PrimeButton
        as="router-link"
        :to="{ name: 'demandes' }"
        label="Annuler"
        severity="secondary"
        variant="outlined"
      />
    </div>

    <div
      v-if="erreurGlobale"
      class="rounded-lg border p-3 text-sm border-red-200 bg-red-50 text-red-700"
    >
      {{ erreurGlobale }}
    </div>

    <section
      class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900"
      style="margin-bottom: 16px"
    >
      <div class="flex items-center justify-between gap-3 border-b border-surface p-4">
        <h2>Entete de la demande</h2>
      </div>
      <div class="p-4">
        <div class="grid grid-cols-1 gap-x-4 gap-y-1 md:grid-cols-2">
          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" for="motif"
              >Motif <span class="text-red-500">*</span></label
            >
            <PrimeInputText
              id="motif"
              v-model="motif"
              class="w-full"
              type="text"
              placeholder="Accroissement d'activite"
            />
          </div>
          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" for="dateSouhaitee"
              >Date souhaitee de recrutement <span class="text-red-500">*</span></label
            >
            <PrimeInputText id="dateSouhaitee" v-model="dateSouhaitee" class="w-full" type="date" />
          </div>
        </div>
      </div>
    </section>

    <section
      v-for="(ligne, index) in lignes"
      :key="index"
      class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900"
      style="margin-bottom: 16px"
    >
      <div class="flex items-center justify-between gap-3 border-b border-surface p-4">
        <h2>Ligne {{ index + 1 }}</h2>
        <PrimeButton
          label="Supprimer la ligne"
          icon="pi pi-trash"
          severity="danger"
          variant="text"
          size="small"
          @click="supprimerLigne(index)"
        />
      </div>
      <div class="p-4">
        <div class="grid grid-cols-1 gap-x-4 gap-y-1 md:grid-cols-2 lg:grid-cols-3">
          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" :for="`service-${index}`"
              >Service <span class="text-red-500">*</span></label
            >
            <PrimeSelect
              :id="`service-${index}`"
              v-model="ligne.idService"
              class="w-full"
              :options="services"
              option-label="nom"
              option-value="id"
              placeholder="Choisir un service"
              @change="ligne.idPoste = null"
            />
          </div>

          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" :for="`type-${index}`"
              >Type de besoin <span class="text-red-500">*</span></label
            >
            <PrimeSelect
              :id="`type-${index}`"
              v-model="ligne.typeBesoin"
              class="w-full"
              :options="[
                { label: 'Creation de poste', value: 'CREATION' },
                { label: 'Remplacement', value: 'REMPLACEMENT' },
              ]"
              option-label="label"
              option-value="value"
            />
          </div>

          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" :for="`poste-${index}`"
              >Poste existant</label
            >
            <PrimeSelect
              :id="`poste-${index}`"
              v-model="ligne.idPoste"
              class="w-full"
              :options="[
                { id: null, intitule: 'Nouveau poste a creer si accepte' },
                ...postesPour(ligne),
              ]"
              option-label="intitule"
              option-value="id"
              @change="surChangementPoste(ligne)"
            />
            <span class="text-xs text-muted-color"
              >Si le poste existe deja, il sera directement reference.</span
            >
          </div>
        </div>

        <template v-if="ligne.typeBesoin === 'REMPLACEMENT'">
          <div class="grid grid-cols-1 gap-x-4 gap-y-1 md:grid-cols-2 lg:grid-cols-3">
            <div class="flex flex-col gap-2 mb-4">
              <label class="text-sm font-semibold text-muted-color" :for="`employe-${index}`"
                >Employe remplace</label
              >
              <PrimeSelect
                :id="`employe-${index}`"
                v-model="ligne.idEmployeRemplace"
                class="w-full"
                :options="[
                  { id: null, nom: 'Employe non identifie', prenom: '', matricule: '' },
                  ...employesPour(ligne),
                ]"
                option-label="nom"
                option-value="id"
              />
            </div>
            <div class="flex flex-col gap-2 mb-4">
              <label class="text-sm font-semibold text-muted-color" :for="`typeRemp-${index}`"
                >Type de remplacement <span class="text-red-500">*</span></label
              >
              <PrimeSelect
                :id="`typeRemp-${index}`"
                v-model="ligne.typeRemplacement"
                class="w-full"
                :options="[
                  { label: 'Definitif', value: 'DEFINITIF' },
                  { label: 'Temporaire', value: 'TEMPORAIRE' },
                ]"
                option-label="label"
                option-value="value"
              />
            </div>
            <div class="flex flex-col gap-2 mb-4">
              <label class="text-sm font-semibold text-muted-color" :for="`motifRemp-${index}`"
                >Motif du remplacement</label
              >
              <PrimeInputText
                :id="`motifRemp-${index}`"
                v-model="ligne.motifRemplacement"
                class="w-full"
                type="text"
              />
            </div>
            <div class="flex flex-col gap-2 mb-4">
              <label class="text-sm font-semibold text-muted-color" :for="`debut-${index}`"
                >Debut du remplacement</label
              >
              <PrimeInputText
                :id="`debut-${index}`"
                v-model="ligne.dateDebutRemplacement"
                class="w-full"
                type="date"
              />
            </div>
            <div class="flex flex-col gap-2 mb-4">
              <label class="text-sm font-semibold text-muted-color" :for="`fin-${index}`"
                >Fin du remplacement</label
              >
              <PrimeInputText
                :id="`fin-${index}`"
                v-model="ligne.dateFinRemplacement"
                class="w-full"
                type="date"
              />
            </div>
          </div>
        </template>

        <div class="grid grid-cols-1 gap-x-4 gap-y-1 md:grid-cols-2">
          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" :for="`intitule-${index}`"
              >Intitule du besoin <span class="text-red-500">*</span></label
            >
            <PrimeInputText
              :id="`intitule-${index}`"
              v-model="ligne.intituleDemande"
              class="w-full"
              type="text"
            />
          </div>
          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" :for="`effectif-${index}`"
              >Effectif <span class="text-red-500">*</span></label
            >
            <PrimeInputText
              :id="`effectif-${index}`"
              v-model.number="ligne.effectif"
              class="w-full"
              type="number"
              min="1"
            />
          </div>
          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" :for="`description-${index}`"
              >Description du poste</label
            >
            <PrimeTextarea
              :id="`description-${index}`"
              v-model="ligne.descriptionPoste"
              class="w-full"
              rows="2"
            />
          </div>
          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" :for="`profil-${index}`"
              >Profil recherche</label
            >
            <PrimeTextarea
              :id="`profil-${index}`"
              v-model="ligne.profilRecherche"
              class="w-full"
              rows="2"
            />
          </div>
          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" :for="`salaireMin-${index}`"
              >Salaire minimum estime (Ar)</label
            >
            <PrimeInputText
              :id="`salaireMin-${index}`"
              v-model.number="ligne.salaireMinEstime"
              class="w-full"
              type="number"
              min="0"
              step="1000"
            />
          </div>
          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" :for="`salaireMax-${index}`"
              >Salaire maximum estime (Ar)</label
            >
            <PrimeInputText
              :id="`salaireMax-${index}`"
              v-model.number="ligne.salaireMaxEstime"
              class="w-full"
              type="number"
              min="0"
              step="1000"
            />
          </div>
          <div class="flex flex-col gap-2 mb-4">
            <label class="text-sm font-semibold text-muted-color" :for="`budget-${index}`"
              >Budget estime (Ar) <span class="text-red-500">*</span></label
            >
            <PrimeInputText
              :id="`budget-${index}`"
              v-model.number="ligne.budgetEstime"
              class="w-full"
              type="number"
              min="0"
              step="1000"
            />
            <span class="text-xs text-muted-color"
              >Verification de la disponibilite budgetaire a l'etape Finance.</span
            >
          </div>
        </div>
      </div>
    </section>

    <section class="rounded-lg border border-surface bg-surface-0 shadow-sm dark:bg-surface-900">
      <div
        class="p-4"
        style="
          display: flex;
          justify-content: space-between;
          align-items: center;
          gap: 12px;
          flex-wrap: wrap;
        "
      >
        <div>
          <PrimeButton
            class="inline-flex items-center justify-center gap-2 border border-surface"
            @click="ajouterLigne"
            >Ajouter une ligne</PrimeButton
          >
          <div class="text-muted-color" style="font-size: 12.5px; margin-top: 8px">
            {{ lignes.length }} ligne(s) · budget total {{ budgetTotal.toLocaleString('fr-FR') }} Ar
          </div>
        </div>
        <div style="display: flex; gap: 10px">
          <PrimeButton
            class="inline-flex items-center justify-center gap-2 border border-surface"
            :disabled="envoi"
            @click="enregistrer(false)"
          >
            Enregistrer en brouillon
          </PrimeButton>
          <PrimeButton
            class="inline-flex items-center justify-center gap-2 bg-primary text-primary-contrast"
            :disabled="envoi"
            @click="enregistrer(true)"
          >
            Enregistrer et soumettre
          </PrimeButton>
        </div>
      </div>
    </section>
  </div>
</template>
