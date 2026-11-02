# Éléments partagés du frontend

Ce dossier contient les composants, stores, utilitaires et services réutilisables par les modules frontend.

Alias utilisé :

```js
@/shared
```

## Composants partagés

### `EmptyState.vue`

Affiche un état vide avec un titre, un message et un slot optionnel.

```vue
<EmptyState
  titre="Aucune demande"
  message="Aucune demande disponible."
>
  <PrimeButton label="Créer une demande" />
</EmptyState>
```

Props :

- `titre` : titre affiché.
- `message` : message explicatif.

### `ModalDialog.vue`

Encapsule le composant PrimeVue `Dialog`.

```vue
<ModalDialog
  titre="Modifier le poste"
  sous-titre="Saisissez les nouvelles informations."
  :large="true"
  @fermer="fermer"
>
  <!-- contenu -->
  <template #actions>
    <PrimeButton label="Annuler" @click="fermer" />
  </template>
</ModalDialog>
```

Props :

- `titre` : titre obligatoire.
- `sousTitre` : sous-titre optionnel.
- `large` : élargit la modale.
- `fermable` : autorise la fermeture.

Événement :

- `fermer`

Slots :

- contenu principal ;
- `actions` pour le pied de la modale.

### `StatusBadge.vue`

Affiche un statut avec le composant PrimeVue `Tag`.

```vue
<StatusBadge valeur="EN_VALIDATION" />
<StatusBadge valeur="ACCEPTEE" />
<StatusBadge valeur="ACTIF" libelle="Disponible" />
```

Props :

- `valeur` : valeur technique du statut.
- `libelle` : libellé personnalisé.
- `type` : réservé pour les besoins futurs.

Les statuts connus sont automatiquement traduits et associés à une couleur.

### `HistoriqueValidations.vue`

Affiche l’historique des décisions d’une ligne de demande.

```vue
<HistoriqueValidations :validations="validations" />
```

Prop :

- `validations` : tableau des validations.

Chaque élément peut contenir :

- `dateAction`
- `etape`
- `decision`
- `utilisateur`
- `commentaire`

### `NotificationContainer.vue`

Affiche les notifications globales avec PrimeVue `Toast`.

Il doit normalement être placé une seule fois dans la structure principale de l’application :

```vue
<NotificationContainer />
```

Les notifications sont envoyées via `useNotificationStore`.

### `AppSidebar.vue`

Affiche la navigation principale de l’application.

Fonctionnalités :

- utilise PrimeVue `Menu` et `Avatar` ;
- affiche les liens selon le rôle de l’utilisateur ;
- met en évidence la route active ;
- affiche les informations de l’utilisateur connecté.

```vue
<AppSidebar />
```

### `AppTopbar.vue`

Affiche la barre supérieure de l’application.

Fonctionnalités :

- affiche le titre de la page ;
- affiche le rôle et le département de l’utilisateur ;
- fournit le bouton de déconnexion ;
- utilise PrimeVue `Toolbar`, `Tag` et `Button`.

```vue
<AppTopbar
  titre="Demandes"
  @deconnexion="deconnexion"
/>
```

Props :

- `titre` : titre obligatoire.

Événement :

- `deconnexion`

### `IntrouvableView.vue`

Vue affichée lorsqu’une route n’existe pas.

Elle affiche un message d’erreur et un bouton permettant de revenir au tableau de bord.

## Layout partagé

### `AppLayout.vue`

Structure principale des pages authentifiées.

Contient :

- `AppSidebar` ;
- `AppTopbar` ;
- `RouterView` ;
- la gestion de la déconnexion ;
- le titre dynamique basé sur les métadonnées de route.

Utilisation recommandée :

```js
{
  path: '/',
  component: () => import('@/shared/layouts/AppLayout.vue'),
  children: [
    // routes protégées
  ],
}
```

## Stores partagés

### `useAuthStore.js`

Store Pinia de l’authentification.

```js
import { useAuthStore } from '@/shared/store/useAuthStore'

const auth = useAuthStore()
```

Propriétés principales :

- `token`
- `profil`
- `chargement`

Getters :

- `connecte`
- `role`
- `estManager`
- `estRh`
- `estFinance`
- `estDirection`
- `initiales`

Actions :

- `connexion(email, motDePasse)`
- `rechargerProfil()`
- `deconnexion()`
- `aRole(...roles)`

### `useNotificationStore.js`

Store Pinia des notifications.

```js
import { useNotificationStore } from '@/shared/store/useNotificationStore'

const notifications = useNotificationStore()

notifications.succes('Demande enregistrée.')
notifications.erreur('Impossible de charger les données.')
notifications.ajouter('Information disponible.', 'info')
```

Actions :

- `ajouter(texte, type, titre, duree)`
- `succes(texte, titre)`
- `erreur(texte, titre)`
- `fermer(id)`

Types courants :

- `info`
- `succes`
- `erreur`
- `warn`

## API partagée

### `axios.js`

Centralise les appels HTTP vers le backend.

```js
import { api } from '@/shared/api/axios'

const demandes = await api.get('/demandes')
await api.post('/demandes', donnees)
await api.put(`/demandes/${id}`, donnees)
await api.patch(`/demandes/${id}`, donnees)
await api.delete(`/demandes/${id}`)
```

Méthodes disponibles :

- `api.get(chemin)`
- `api.post(chemin, corps)`
- `api.put(chemin, corps)`
- `api.patch(chemin, corps)`
- `api.delete(chemin)`

Fonctions liées au token :

- `definirJeton(valeur)`
- `obtenirJeton()`
- `surJetonExpire(callback)`

Erreur spécialisée :

```js
import { ErreurApi } from '@/shared/api/axios'
```

L’URL de base utilise `VITE_API_URL`. La valeur par défaut est :

```text
http://localhost:8080/api
```

## Utilitaires partagés

### `format.js`

```js
import {
  formatMontant,
  formatNombre,
  formatDate,
  formatDateHeure,
  libelle,
  libelles,
} from '@/shared/utils/format'
```

Fonctions :

- `formatMontant(montant)` : formatte un montant en ariary.
- `formatNombre(nombre)` : formatte un nombre sans devise.
- `formatDate(date)` : formatte une date.
- `formatDateHeure(date)` : formatte une date avec l’heure.
- `libelle(valeur)` : transforme une valeur technique en texte lisible.
- `libelles(dictionnaire)` : crée une fonction de traduction personnalisée.

Exemple :

```js
formatMontant(125000) // "125 000 Ar"
formatDate('2026-01-15') // "15/01/2026"
libelle('EN_VALIDATION') // "En validation"
```

## Règles d’utilisation

1. Réutiliser les éléments de `shared` avant de créer un composant similaire.
2. Ne pas dupliquer les modales, badges, notifications ou états vides dans les modules métier.
3. Importer localement les composants PrimeVue utilisés.
4. Ne pas enregistrer globalement les composants PrimeVue dans `main.js`.
5. Garder les composants spécifiques dans leur module métier.
6. Placer dans `shared` uniquement les éléments génériques et réutilisables.
7. Garder les appels HTTP spécifiques dans l’API du module concerné.
8. Utiliser `shared/api/axios.js` pour les appels HTTP génériques et la gestion du token.
