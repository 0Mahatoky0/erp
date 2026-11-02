# ARCHITECTURE_FRONTEND.md — ERP RH (Vue.js 3 / PrimeVue)

> Document normatif pour les développeurs **et les agents IA**.
> **DOIT / NE DOIT PAS** = règle obligatoire. En cas de doute, appliquer ce document sans improviser.
> Ce document est le pendant Frontend du document `ARCHITECTURE.md` (Spring Boot).

---

## 1. Principe

Un **front modulaire simple** sous **Vue.js 3 (Composition API avec `<script setup>`)** et **PrimeVue**, aligné sur les domaines métier du backend. 

Chaque module regroupe ses vues, ses composants métier, son store Pinia, ses routes et ses appels API. Les composants UI d'infrastructure et les configurations globales résident dans `shared/`.

---

## 2. Structure du Projet (`src/`)

```
src/
├── assets/             # Styles globaux, variables CSS, tailwind.css
├── shared/             # Technique et UI transverses (auth, axios, components UI)
├── router/             # Agrégateur des routes de modules
│   └── index.js
├── modules/
│   ├── iam/            # Authentification, gestion des rôles
│   ├── employe/        # Dossiers et vie des employés
│   ├── recrutement/    # Offres, candidatures, entretiens
│   ├── besoin/         # Besoins en recrutement
│   ├── embauche/       # Intégration et passage candidat -> employé
│   ├── contrat/        # Contrats de travail
│   ├── temps/          # Temps de travail et pointages
│   └── conges/         # Demandes et soldes de congés
├── App.vue
└── main.js             # Initialisation Vue, PrimeVue, Pinia, Router
```

---

## 3. Structure d'un Module Frontend

Structure strictement identique au sein de **chaque** module sous `src/modules/<module>/` :

```
<module>/
├── api/                # Client Axios spécifique au module
├── components/         # Composants visuels propres au module
├── views/              # Pages principales routées
├── store/              # Store Pinia du module
└── routes.js           # Export des routes du module
```

---

## 4. Rôle de chaque dossier

| Dossier | Contenu | Importable par un autre module ? |
|---|---|---|
| `api` | Fonctions d'appel HTTP vers les endpoints Spring Boot | **Non** |
| `components` | Composants spécifiques au métier du module | **Non** |
| `views` | Pages principales associées aux routes | **Non** |
| `store` | Gestion de l'état local du module via Pinia | **Non** |
| `routes.js` | Définition des routes du module | **Oui** (par `router/index.js` uniquement) |
| `shared` | Client HTTP principal, Layouts, composables globaux, composants UI génériques | **Oui**, par tous les modules |

---

## 5. Règles obligatoires

**R1 — Isolation stricte inter-modules :**
Un module ne doit **jamais** importer un composant, un store Pinia ou une fonction API provenant d'un autre module. 
```javascript
// KO - Interdit
import { useEmployeStore } from '@/modules/employe/store/employeStore';

// OK - Passer par la navigation / route ou les composants/stores génériques dans shared/
```

**R2 — Utilisation prioritaire des composants PrimeVue :**
Toutes les interactions complexes de grille de données, formulaires et éléments interactifs DOIVENT utiliser la suite PrimeVue.
* Tableaux de données : `<DataTable>`, `<Column>`
* Dialogue/Modale : `<Dialog>`, `<ConfirmDialog>`
* Notifications : `<Toast>`
* Formulaires : `<InputText>`, `<Dropdown>`, `<DatePicker>`, `<Button>`, `<Tag>`

**R3 — Stylos et Layouts avec Tailwind CSS :**
Tailwind CSS est réservé à la disposition (`flex`, `grid`, `padding`, `margin`, `gap`). Le style visuel des composants interactifs est délégué au thème PrimeVue.

**R4 — Centralisation du Client HTTP (`shared/api/axios.js`) :**
Tous les appels API utilisent l'instance Axios configurée dans `shared/` qui gère :
* L'URL de base (`/api/v1`)
* L'injection automatique du token JWT dans l'en-tête `Authorization`
* La gestion globale des erreurs (ex: 401 Unauthorized -> Redirection login)

**R5 — Lazy Loading obligatoire des routes :**
Toutes les vues enregistrées dans `routes.js` doivent utiliser l'import dynamique :
```javascript
// OK
component: () => import('./views/ListeCandidatsView.vue')
```

---

## 6. Thème et UI Centralisés (PrimeVue)

La configuration du thème et de la charte couleur se fait exclusivement à la racine dans `main.js` via le système de Presets PrimeVue.

```javascript
// main.js
import { createApp } from 'vue';
import PrimeVue from 'primevue/config';
import Aura from '@primevue/themes/aura';
import { definePreset } from '@primevue/themes';
import App from './App.vue';

// Personnalisation des couleurs principales de l'application ERP
const ErpRhTheme = definePreset(Aura, {
    semantic: {
        primary: {
            50: '{indigo.50}',
            100: '{indigo.100}',
            500: '{indigo.500}', // Couleur d'action principale
            600: '{indigo.600}',
            700: '{indigo.700}'
        }
    }
});

const app = createApp(App);

app.use(PrimeVue, {
    theme: {
        preset: ErpRhTheme,
        options: {
            prefix: 'p',
            darkModeSelector: '.app-dark'
        }
    }
});
```

---

## 7. Conventions de Nommage

| Élément | Convention | Exemple |
|---|---|---|
| Module | Minuscule, kebab-case | `recrutement` |
| Vue (Page) | PascalCase + suffixe `View` | `ListeCandidatsView.vue` |
| Composant | PascalCase | `CardCandidat.vue` |
| Store Pinia | camelCase + préfixe `use` + suffixe `Store` | `useRecrutementStore.js` |
| Fichier API | camelCase + suffixe `Api` | `recrutementApi.js` |

---

## 8. Checklist de Pull Request Frontend

- [ ] Aucun import direct vers un dossier interne d'un autre module (`modules/x` ne doit pas importer `modules/y`).
- [ ] Les routes du module utilisent le Lazy Loading.
- [ ] Les formulaires et tableaux réutilisent la bibliothèque PrimeVue.
- [ ] Tous les appels API passent par l'instance Axios partagée.
- [ ] Le store Pinia ne conserve que l'état nécessaire au module.

---

## 9. Instructions pour les agents IA

**Toujours :**
1. Créer ou modifier le code dans le module concerné sous `src/modules/<module>/`.
2. Utiliser l'API Composition de Vue 3 (`<script setup>`).
3. Utiliser les composants PrimeVue pour l'UI.
4. Consulter l'instance Axios partagée dans `shared/api/axios.js` pour les appels backend.

**Jamais :**
- Créer un import croisé entre deux modules (`modules/A` -> `modules/B`).
- Écrire des styles CSS custom si PrimeVue ou Tailwind fournissent l'équivalent.
- Mettre des tokens d'authentification ou des appels `fetch` direct hors de `shared/api`.
- Ignorer le typage DTO provenant du backend Spring Boot.