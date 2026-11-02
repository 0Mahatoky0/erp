<script setup>
import PrimeButton from 'primevue/button'
import PrimeInputText from 'primevue/inputtext'
import PrimeTextarea from 'primevue/textarea'
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
  return employes.value.filter((employe) => !ligne.idService || employe.idService === ligne.idService)
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
    dateDebutRemplacement: ligne.typeBesoin === 'REMPLACEMENT' ? ligne.dateDebutRemplacement || null : null,
    dateFinRemplacement: ligne.typeBesoin === 'REMPLACEMENT' ? ligne.dateFinRemplacement || null : null,
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
    <div class="page__entete">
      <div>
        <h1>Nouvelle demande</h1>
        <p class="page__description">
          Une demande peut contenir plusieurs besoins de nature differente. Chaque ligne suivra
          son propre circuit de validation.
        </p>
      </div>
      <RouterLink class="bouton bouton--secondaire" :to="{ name: 'demandes' }">Annuler</RouterLink>
    </div>

    <div v-if="erreurGlobale" class="alerte alerte--erreur">{{ erreurGlobale }}</div>

    <section class="carte" style="margin-bottom: 16px">
      <div class="carte__entete"><h2>Entete de la demande</h2></div>
      <div class="carte__corps">
        <div class="grille-champs">
          <div class="champ">
            <label class="champ__libelle" for="motif">Motif <span class="champ__obligatoire">*</span></label>
            <PrimeInputText id="motif" v-model="motif" class="saisie" type="text" placeholder="Accroissement d'activite" />
          </div>
          <div class="champ">
            <label class="champ__libelle" for="dateSouhaitee">Date souhaitee de recrutement <span class="champ__obligatoire">*</span></label>
            <PrimeInputText id="dateSouhaitee" v-model="dateSouhaitee" class="saisie" type="date" />
          </div>
        </div>
      </div>
    </section>

    <section
      v-for="(ligne, index) in lignes"
      :key="index"
      class="carte"
      style="margin-bottom: 16px"
    >
      <div class="carte__entete">
        <h2>Ligne {{ index + 1 }}</h2>
        <PrimeButton class="bouton bouton--fantome bouton--petit" @click="supprimerLigne(index)">
          Supprimer la ligne
        </PrimeButton>
      </div>
      <div class="carte__corps">
        <div class="grille-champs grille-champs--3">
          <div class="champ">
            <label class="champ__libelle" :for="`service-${index}`">Service <span class="champ__obligatoire">*</span></label>
            <select :id="`service-${index}`" v-model="ligne.idService" class="saisie" @change="ligne.idPoste = null">
              <option :value="null" disabled>Choisir un service</option>
              <option v-for="service in services" :key="service.id" :value="service.id">{{ service.nom }}</option>
            </select>
          </div>

          <div class="champ">
            <label class="champ__libelle" :for="`type-${index}`">Type de besoin <span class="champ__obligatoire">*</span></label>
            <select :id="`type-${index}`" v-model="ligne.typeBesoin" class="saisie">
              <option value="CREATION">Creation de poste</option>
              <option value="REMPLACEMENT">Remplacement</option>
            </select>
          </div>

          <div class="champ">
            <label class="champ__libelle" :for="`poste-${index}`">Poste existant</label>
            <select :id="`poste-${index}`" v-model="ligne.idPoste" class="saisie" @change="surChangementPoste(ligne)">
              <option :value="null">Nouveau poste (a creer a l'acceptation)</option>
              <option v-for="poste in postesPour(ligne)" :key="poste.id" :value="poste.id">{{ poste.intitule }}</option>
            </select>
            <span class="champ__aide">Si le poste existe deja, il sera directement reference.</span>
          </div>
        </div>

        <template v-if="ligne.typeBesoin === 'REMPLACEMENT'">
          <div class="grille-champs grille-champs--3">
            <div class="champ">
              <label class="champ__libelle" :for="`employe-${index}`">Employe remplace</label>
              <select :id="`employe-${index}`" v-model="ligne.idEmployeRemplace" class="saisie">
                <option :value="null">Employe non identifie</option>
                <option v-for="employe in employesPour(ligne)" :key="employe.id" :value="employe.id">
                  {{ employe.nom }} {{ employe.prenom }} ({{ employe.matricule }})
                </option>
              </select>
            </div>
            <div class="champ">
              <label class="champ__libelle" :for="`typeRemp-${index}`">Type de remplacement <span class="champ__obligatoire">*</span></label>
              <select :id="`typeRemp-${index}`" v-model="ligne.typeRemplacement" class="saisie">
                <option value="DEFINITIF">Definitif</option>
                <option value="TEMPORAIRE">Temporaire</option>
              </select>
            </div>
            <div class="champ">
              <label class="champ__libelle" :for="`motifRemp-${index}`">Motif du remplacement</label>
              <PrimeInputText :id="`motifRemp-${index}`" v-model="ligne.motifRemplacement" class="saisie" type="text" />
            </div>
            <div class="champ">
              <label class="champ__libelle" :for="`debut-${index}`">Debut du remplacement</label>
              <PrimeInputText :id="`debut-${index}`" v-model="ligne.dateDebutRemplacement" class="saisie" type="date" />
            </div>
            <div class="champ">
              <label class="champ__libelle" :for="`fin-${index}`">Fin du remplacement</label>
              <PrimeInputText :id="`fin-${index}`" v-model="ligne.dateFinRemplacement" class="saisie" type="date" />
            </div>
          </div>
        </template>

        <div class="grille-champs">
          <div class="champ">
            <label class="champ__libelle" :for="`intitule-${index}`">Intitule du besoin <span class="champ__obligatoire">*</span></label>
            <PrimeInputText :id="`intitule-${index}`" v-model="ligne.intituleDemande" class="saisie" type="text" />
          </div>
          <div class="champ">
            <label class="champ__libelle" :for="`effectif-${index}`">Effectif <span class="champ__obligatoire">*</span></label>
            <PrimeInputText :id="`effectif-${index}`" v-model.number="ligne.effectif" class="saisie" type="number" min="1" />
          </div>
          <div class="champ">
            <label class="champ__libelle" :for="`description-${index}`">Description du poste</label>
            <PrimeTextarea :id="`description-${index}`" v-model="ligne.descriptionPoste" class="saisie" rows="2" />
          </div>
          <div class="champ">
            <label class="champ__libelle" :for="`profil-${index}`">Profil recherche</label>
            <PrimeTextarea :id="`profil-${index}`" v-model="ligne.profilRecherche" class="saisie" rows="2" />
          </div>
          <div class="champ">
            <label class="champ__libelle" :for="`salaireMin-${index}`">Salaire minimum estime (Ar)</label>
            <PrimeInputText :id="`salaireMin-${index}`" v-model.number="ligne.salaireMinEstime" class="saisie" type="number" min="0" step="1000" />
          </div>
          <div class="champ">
            <label class="champ__libelle" :for="`salaireMax-${index}`">Salaire maximum estime (Ar)</label>
            <PrimeInputText :id="`salaireMax-${index}`" v-model.number="ligne.salaireMaxEstime" class="saisie" type="number" min="0" step="1000" />
          </div>
          <div class="champ">
            <label class="champ__libelle" :for="`budget-${index}`">Budget estime (Ar) <span class="champ__obligatoire">*</span></label>
            <PrimeInputText :id="`budget-${index}`" v-model.number="ligne.budgetEstime" class="saisie" type="number" min="0" step="1000" />
            <span class="champ__aide">Verification de la disponibilite budgetaire a l'etape Finance.</span>
          </div>
        </div>
      </div>
    </section>

    <section class="carte">
      <div class="carte__corps" style="display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap">
        <div>
          <PrimeButton class="bouton bouton--secondaire" @click="ajouterLigne">Ajouter une ligne</PrimeButton>
          <div class="texte-discret" style="font-size: 12.5px; margin-top: 8px">
            {{ lignes.length }} ligne(s) · budget total {{ budgetTotal.toLocaleString('fr-FR') }} Ar
          </div>
        </div>
        <div style="display: flex; gap: 10px">
          <PrimeButton class="bouton bouton--secondaire" :disabled="envoi" @click="enregistrer(false)">
            Enregistrer en brouillon
          </PrimeButton>
          <PrimeButton class="bouton bouton--principal" :disabled="envoi" @click="enregistrer(true)">
            Enregistrer et soumettre
          </PrimeButton>
        </div>
      </div>
    </section>
  </div>
</template>
