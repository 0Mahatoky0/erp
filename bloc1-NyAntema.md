# Bloc 1 — ERP TOVO (module Recrutement)

> **Auteur du document :** NyAntema
> **Périmètre :** tout le code du *Bloc 1* du projet ERP TOVO (backend Spring Boot + frontend Vue 3).
> **Branche :** `Bloc1` — commit `70f8e88` (merge de `0Mahatoky0/Bloc1`).
> **Objet :** expliquer le **code**, les **fonctionnalités** et les **règles métier** du Bloc 1.

---

## Table des matières

1. [Ce que fait le Bloc 1](#1-ce-que-fait-le-bloc-1)
2. [Architecture et stack technique](#2-architecture-et-stack-technique)
3. [Arborescence du projet](#3-arborescence-du-projet)
4. [Modèle de données](#4-modèle-de-données)
5. [Backend — explication du code](#5-backend--explication-du-code)
6. [Frontend — explication du code](#6-frontend--explication-du-code)
7. [API REST du Bloc 1](#7-api-rest-du-bloc-1)
8. [Catalogue des règles métier (RM-001 → RM-068)](#8-catalogue-des-règles-métier-rm-001--rm-068)
9. [Codes d'erreur](#9-codes-erreur)
10. [Tests et recette](#10-tests-et-recette)
11. [Mise en route](#11-mise-en-route)
12. [Écarts et points d'attention](#12-écarts-et-points-dattention)

---

## 1. Ce que fait le Bloc 1

Le Bloc 1 est le **socle du module Recrutement** de l'ERP TOVO. Il couvre la chaîne complète,
de la demande du manager jusqu'à l'acceptation de la ligne de recrutement :

| Domaine | Fonctionnalité livrée |
|---|---|
| **Sécurité** | Connexion par e-mail + mot de passe, mot de passe BCrypt, jeton JWT HS256 (8 h), séparation des responsabilités, activation/désactivation des comptes |
| **Référentiel** | Départements, services, postes (catalogue officiel), employés |
| **Demande de recrutement** | En-tête + **lignes multiples**, brouillon, soumission, modification, annulation, resoumission après correction |
| **Circuit de validation** | Circuit à 3 étapes (**RH → FINANCE → DIRECTION**) appliqué **ligne par ligne**, 3 décisions possibles (APPROUVE / A_CORRIGER / REFUSE) |
| **Contrôle budgétaire** | Budget de service par exercice, budget disponible = alloué − dépensé − engagé, refus automatique si insuffisant, engagement à l'acceptation |
| **Pilotage** | Tableau de bord par rôle, file de validation, jauges de consommation budgétaire |
| **Traçabilité** | Historique des décisions, vérifications budgétaires, journal des changements de statut (`historique_statut`) |

### 1.1 Principe directeur : la validation est **par ligne**

C'est la règle structurante du Bloc 1 (**RM-003**) :

> Une demande de recrutement est un **en-tête** (motif + date souhaitée) qui contient **une ou
> plusieurs lignes**. Chaque ligne est **validée indépendamment** de ses voisines.
> Le **statut global** de la demande n'est **jamais saisi** : il est **recalculé** par le
> backend à partir du statut de ses lignes.

Conséquence : une demande peut être `FINALISEE_PARTIELLE` (une ligne acceptée, une autre refusée)
ou rester `EN_VALIDATION` alors qu'une de ses lignes est déjà `ACCEPTEE`.

---

## 2. Architecture et stack technique

```
┌──────────────────────────┐        HTTP/JSON + Bearer JWT        ┌────────────────────────────┐
│  frontend/  (port 5173)  │  ─────────────────────────────────▶  │  bakend/  (port 8080)      │
│  Vue 3 + Vite + Pinia    │                                        │  Spring Boot 4.1 / Java 21│
│  Vue Router 5            │  ◀─────────────────────────────────  │  Spring Web MVC + JPA     │
│  Fetch natif (client.js) │        JSON (ErreurApi / ApiError)    │  Spring Security + JWT    │
└──────────────────────────┘                                        │  Flyway + PostgreSQL      │
                                                                   └────────────────────────────┘
```

| Couche | Choix technique | Rôle |
|---|---|---|
| Backend | **Spring Boot 4.1.1**, **Java 21** | socle applicatif |
| Backend | Spring Web MVC | API REST (contrôleurs + DTO records) |
| Backend | Spring Data JPA / Hibernate | mapping objet-relational, repositories |
| Backend | Spring Security + `spring-security-oauth2-jose` | authentification JWT (Nimbus HS256) |
| Backend | Flyway + `flyway-database-postgresql` | versioning du schéma SQL |
| Backend | PostgreSQL | base de données |
| Backend | springdoc-openapi 3.1.0 | documentation interactive Swagger (`/swagger-ui.html`) |
| Frontend | **Vue 3.5** (Composition API, `<script setup>`) | composants et écrans |
| Frontend | **Vite 8** | serveur de dev + build |
| Frontend | **Pinia 4** | stores (session, notifications) |
| Frontend | **Vue Router 5** | navigation + gardes de routes |
| Frontend | CSS natif (variables + 14 sections) | thème, sans framework CSS |

**Organisation en couches (backend)** — règle suivie partout :

```
Contrôleur (web/controller)  →  Service (service/)  →  Repository (repository/)  →  Entité (domain/entity/)
        ↕ DTO (web/dto)                ↕ règles métier        ↕ SQL généré          ↕ colonnes SQL
```

- Le **contrôleur** ne fait que : lire le corps HTTP, appeler le service, retourner un DTO.
- Le **service** porte les **règles métier** et les **transactions** (`@Transactional`).
- Le **repository** ne contient que des requêtes JPA, aucune logique métier.
- Les **DTO** (`record`) isolent l'API du modèle JPA.

---

## 3. Arborescence du projet

```
erp/
├── bloc1-NyAntema.md            ← ce document
├── todo.txt                     ← étapes de réalisation, commandes, tests navigateur
├── test_local.sh                ← orchestrateur de recette (API + navigateur)
├── tests/
│   └── api_bloc1.sh             ← 56 vérifications sur l'API REST (bash + curl + node)
├── bakend/                      ← backend Spring Boot (nom de dossier historique)
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd          ← Maven Wrapper 3.10.0
│   └── src/main/
│       ├── java/com/tovo/erp/
│       │   ├── ErpApplication.java
│       │   ├── common/          → ApiError, BusinessException, GlobalExceptionHandler, Recherche
│       │   ├── config/          → SecurityConfig, CircuitValidationProperties, DemandeProperties, DataInitializer
│       │   ├── domain/entity/   → 12 entités JPA
│       │   ├── domain/enums/    → 8 énumérations
│       │   ├── repository/      → 12 repositories Spring Data
│       │   ├── security/        → JwtService, JwtAuthenticationFilter, JwtProperties, UtilisateurCourant
│       │   ├── service/         → 10 services métier (le cœur des règles)
│       │   └── web/
│       │       ├── controller/  → 8 contrôleurs REST
│       │       └── dto/         → 7 familles de DTO records
│       └── resources/
│           ├── application.properties        (ignoré par git — config locale)
│           ├── exemple.application.properties
│           └── db/
│               ├── migration/V1__bloc1_schema.sql   ← schéma (12 tables)
│               └── donnees_demo.sql                 ← jeu de données de test
└── frontend/
    ├── package.json  vite.config.js  eslint.config.js  .oxlintrc.json  .prettierrc.json
    ├── test_ui.mjs               ← 28 vérifications navigateur (Playwright)
    ├── init-vite/                ← scaffold Vite d'origine, conservé, NON utilisé
    └── src/
        ├── main.js  App.vue
        ├── api/client.js         ← client HTTP unique + ErreurApi
        ├── stores/               → auth.js, notification.js
        ├── router/index.js       ← routes + gardes
        ├── layouts/AppLayout.vue
        ├── components/           → AppSidebar, AppTopbar, StatusBadge, ModalDialog,
        │                           NotificationContainer, EmptyState, HistoriqueValidations
        ├── utils/format.js       → formatage monnaie / dates / libellés
        ├── views/                → 11 écrans
        └── assets/               → base.css (variables) + main.css (14 sections)
```

---

## 4. Modèle de données

### 4.1 Les 12 tables

> ⚠️ La documentation initiale (`todo.txt`, en-tête des scripts SQL) annonce « 11 tables ».
> Le schéma réel en crée **12** : `historique_statut` a été ajoutée par la **RM-065**.

| # | Table | Rôle | Points notables |
|---|---|---|---|
| 1 | `departement` | départements de l'entreprise | `nom` UNIQUE |
| 2 | `service` | services rattachés à un département | FK → `departement` NOT NULL ; `uq_service_departement_nom` |
| 3 | `role` | catalogue des rôles | `nom` UNIQUE |
| 4 | `utilisateur` | comptes + mots de passe | FK → `role` NOT NULL, `departement` nullable ; `email` UNIQUE ; `actif` DEFAULT TRUE |
| 5 | `poste` | catalogue officiel des postes | FK → `service` ; `uq_poste_service_intitule` ; `chk_poste_salaires` (≥ 0 et max ≥ min) |
| 6 | `employe` | personnel | `matricule` UNIQUE, `email` UNIQUE ; `chk_employe_statut` ∈ {ACTIF, INACTIF, PARTI} |
| 7 | `demande_recrutement` | **en-tête** de la demande | `chk_demande_statut` ∈ 7 statuts ; `date_soumission` NULL tant que brouillon |
| 8 | `demande_recrutement_poste` | **lignes** de la demande | 8 statuts ; `episode` (RM-040) ; `etape_courante` (RM-038) ; 6 CHECK métier |
| 9 | `validation` | décisions des validateurs | `episode` ; `chk_validation_etape`, `chk_validation_decision` |
| 10 | `budget_service` | budget alloué par service et par exercice | `uq_budget_service_exercice` ; `montant_depense`, `montant_engage` (RM-051) |
| 11 | `verification_budget` | contrôles financiers | 3 montants NOT NULL ; `chk_verification_budget_decision` ; `episode` |
| 12 | `historique_statut` | **journal** des changements de statut | `id_demande_poste` **NULLABLE** → NULL = changement de statut **global** de la demande |

**Index créés :** `idx_demande_poste_demande`, `idx_demande_poste_service`, `idx_demande_poste_poste`,
`idx_demande_poste_employe_remplace`, `idx_demande_poste_statut`, `idx_validation_demande_date`,
`idx_verification_budget_demande_date`, `idx_historique_statut_demande`.

### 4.2 Les 8 énumérations

| Énumération | Valeurs | Utilisation |
|---|---|---|
| `StatutDemande` | `BROUILLON`, `EN_VALIDATION`, `A_CORRIGER`, `FINALISEE`, `FINALISEE_PARTIELLE`, `REFUSEE`, `ANNULEE` | statut **global**, calculé (RM-041→047) |
| `StatutLigne` | `BROUILLON`, `EN_ATTENTE_RH`, `EN_ATTENTE_FINANCE`, `EN_ATTENTE_DIRECTION`, `A_CORRIGER`, `ACCEPTEE`, `REFUSEE`, `ANNULEE` | statut **d'une ligne**, avec helpers `estTerminal()` et `estEnValidation()` |
| `Decision` | `APPROUVE`, `REFUSE`, `A_CORRIGER` | décision d'un validateur |
| `EtapeValidation` | `RH`, `FINANCE`, `DIRECTION` | étapes du circuit (RM-032) |
| `RoleNom` | `MANAGER`, `RH`, `FINANCE`, `DIRECTION` | rôles applicatifs |
| `StatutEmploye` | `ACTIF`, `INACTIF`, `PARTI` | statut d'un employé |
| `TypeBesoin` | `CREATION`, `REMPLACEMENT` | nature d'une ligne |
| `TypeRemplacement` | `TEMPORAIRE`, `DEFINITIF` | nature d'un remplacement |

### 4.3 Les clés de conception

1. **Relations `@ManyToOne(fetch = EAGER)`** — décision technique documentée : avec
   `spring.jpa.open-in-view=false`, les associations paresseuses provoquaient des
   `LazyInitializationException` lors de la sérialisation JSON des DTO.
2. **ID `IDENTITY`** (PostgreSQL `GENERATED BY DEFAULT AS IDENTITY`) sur toutes les entités.
3. **`episode` sur la ligne** — compteur de resoumission (RM-040). Il sert à distinguer
   « la RH a déjà approved ce tour » d'un tour suivant.
4. **`etape_courante` sur la ligne** — l'étape à laquelle la ligne attend une décision.
   Conservée même quand la ligne passe en `A_CORRIGER` afin de reprendre **exactement**
   au même endroit après correction (RM-038).
5. **Contraintes SQL en défense** — en plus de la validation Java, la base impose les
   invariants (statuts autorisés, `max ≥ min`, `fin ≥ début`, `effectif > 0`, unicité).

---

## 5. Backend — explication du code

### 5.1 Point d'entrée

**`ErpApplication.java`**
```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class ErpApplication { ... }
```
`@SpringBootApplication` active l'auto-configuration, le scan des composants et le MVC.
`@ConfigurationPropertiesScan` permet d'injecter directement des records de configuration
(`CircuitValidationProperties`, `JwtProperties`, `DemandeProperties`) sans `@Component`.

### 5.2 `common/` — le socle transverse

| Classe | Code | Fonctionnalité |
|---|---|---|
| **`ApiError`** | `record ApiError(String code, String message, int status, String chemin)` | **Format d'erreur unique du Bloc 1 (RM-068)**. `message` est affichable tel quel par le front ; `code` sert au tri programmatique. |
| **`BusinessException`** | `RuntimeException` + `code` + `HttpStatus` | Exception métier. Fabriques : `notFound()` → 404 `RESOURCE_INTROUVABLE`, `forbidden()` → 403 `ACCES_REFUSE`, `identifiantsInvalides()` → 401 `IDENTIFIANTS_INVALIDES`. |
| **`GlobalExceptionHandler`** | `@RestControllerAdvice` | Traduit toute exception en `ApiError` : `BusinessException` → code métier ; `MethodArgumentNotValidException` → 400 `DONNEES_INVALIDES` (messages de champs agrégés par `;`) ; `AccessDeniedException` → 403 ; `AuthenticationException` → 401 `NON_AUTHENTIFIE` ; `Exception` → 500 `ERREUR_INTERNE` (loggué). |
| **`Recherche`** | `motif(String)` | Prépare en Java le motif `%terme%` : met en minuscules, **échappe** `\`, `%`, `_` (jokers SQL). Retourne `null` si la recherche est vide. Contourne l'erreur PostgreSQL `lower(bytea) does not exist` sur `concat('%', :recherche, '%')`. |

### 5.3 `config/` — configuration et amorçage

| Classe | Fonctionnalité |
|---|---|
| **`SecurityConfig`** | Déclare le `BCryptPasswordEncoder`, le `JwtEncoder`/`JwtDecoder` Nimbus **HS256** (clé fournie par `JwtService.cleSecrete()`), et **le filtre JWT comme `@Bean`** — point clé : l'instancier ici supprime le cycle de dépendance `SecurityConfig` ↔ `JwtAuthenticationFilter`. `csrf` désactivé (API stateless), `SessionCreationPolicy.STATELESS`, CORS actif. **Seules** `POST /api/auth/login`, `/v3/api-docs/**` et `/swagger-ui/**` sont publics ; tout le reste est `authenticated()`. CORS : origines `http://localhost:*` et `http://127.0.0.1:*`, méthodes GET/POST/PUT/PATCH/DELETE/OPTIONS, headers `*`, credentials autorisés. |
| **`CircuitValidationProperties`** | `@ConfigurationProperties("erp.validation")` — record `(List<String> etapes, BigDecimal seuilDirection)`. Défauts : `RH,FINANCE,DIRECTION` et `0`. La méthode `etapesEnum()` convertit les chaînes en `EtapeValidation`. |
| **`DemandeProperties`** | `@ConfigurationProperties("erp.demande")` — record `(Duration delaiMinimum)`, défaut `ZERO`. Sert à la **RM-014** (délai de planification). |
| **`DataInitializer`** | `CommandLineRunner` **transactionnel et idempotent** : il **sort immédiatement** si `roleRepository.count() > 0`. Sinon il crée le référentiel de démarrage : 4 rôles, 3 départements (Commercial, Production, Support), 3 services (Ventes, Atelier, Ressources Humaines), 4 comptes BCrypt, 3 postes, 3 employés (2 `ACTIF` / 1 `PARTI`), 9 budgets (3 services × exercices année‑1 / année / année+1 : 45 000 000 / 38 000 000 / 22 000 000). Les identifiants des comptes de test sont affichés en console au démarrage. |

### 5.4 `security/` — authentification

| Classe | Fonctionnalité |
|---|---|
| **`JwtProperties`** | `@ConfigurationProperties("erp.securite")` — record `(secret, dureeValidite, emetteur)`. **Contrainte forte : un secret de moins de 32 caractères lève `IllegalStateException` au démarrage.** Durée par défaut 8 h, émetteur `erp-tovo`. |
| **`JwtService`** | `cleSecrete(String)` → `SecretKeySpec` HmacSHA256. `genererJeton()` construit un JWS HS256 avec les claims `iss`, `iat`, `exp`, `sub` (id), `email`, `nom`, `role`, `departement`. |
| **`JwtAuthenticationFilter`** | `OncePerRequestFilter` : lit l'en-tête `Authorization: Bearer …`, **ignore silencieusement** les erreurs de décodage (requête simplement non authentifiée), recharge l'utilisateur **en base** (donc un compte désactivé est rejeté même avec un jeton valide), crée l'autorité `ROLE_<role>` et renseigne les détails de la requête. |
| **`UtilisateurCourant`** | Point d'accès au demandeur : `exiger()` (403 si non authentifié), `optionnel()`, `aLeRole(RoleNom...)`, `exigerRole(RoleNom...)` qui renvoie le message *« Cette operation requiert le role X ou Y (vous etes Z). »* |

### 5.5 `domain/entity/` — les 12 entités

| Entité | Table | Rôle et points clés |
|---|---|---|
| `Departement` | `departement` | `nom` unique |
| `Service` | `service` | appartient à un département ; unicité (département, nom) |
| `Role` | `role` | `nom` unique : MANAGER / RH / FINANCE / DIRECTION |
| `Utilisateur` | `utilisateur` | `email` unique, `motDePasse` **hashé BCrypt**, `actif`, un `role`, un `departement` optionnel |
| `Poste` | `poste` | catalogue officiel ; `salaireMin`/`salaireMax` ; `actif` (désactivation logique, **RM-011**) ; unicité (service, intitulé) |
| `Employe` | `employe` | `matricule` unique, `statut`, `poste` |
| `DemandeRecrutement` | `demande_recrutement` | **en-tête** : `motif`, `dateSouhaitee`, `statut` (calculé), `dateCreation/Modification/Soumission` |
| `DemandeRecrutementPoste` | `demande_recrutement_poste` | **ligne** : service, poste (optionnel), type de besoin, type/motif/dates de remplacement, intitulé, description, profil, salaires estimés, effectif, budget, `statut`, **`episode`**, **`etapeCourante`** |
| `Validation` | `validation` | **décision** : ligne, validateur, étape, décision, commentaire, date, `episode` |
| `Budget` | `budget_service` | `exercice`, `montantAlloue`, `montantDepense`, `montantEngage` + méthode calculée `getMontantDisponible()` = alloué − dépensé − engagé |
| `VerificationBudget` | `verification_budget` | **trace** du contrôle financier : budget demandé, budget disponible, montant estimé, décision, commentaire, `episode` |
| `HistoriqueStatut` | `historique_statut` | **journal** : ancien statut, nouveau statut, auteur, commentaire, date. `id_demande_poste` NULL → statut global |

### 5.6 `repository/` — accès aux données

Points notables :

- **`BudgetRepository.findForMiseAJour`** utilise `@Lock(PESSIMISTIC_WRITE)` : c'est le **verrou pessimiste**
  qui garantit la **RM-056** (deux validations simultanées ne peuvent pas dépasser le budget disponible).
- **`DemandeRecrutementPosteRepository.findFileAttente(etape)`** : requête de la **file de travail** du validateur
  (lignes dont le statut correspond à l'étape demandée).
- **`DemandeRecrutementRepository.rechercher(idUtilisateur, mesDemandes, statut, motif)`** : implémente le
  **périmètre MANAGER (RM-067)** et le filtre de recherche textuelle.
- **`PosteRepository.findEquivalents(service, intitule, exclu)`** : recherche de doublon **insensible à la casse
  et aux espaces** (**RM-010**).
- **`ValidationRepository.findByDemandePosteIdAndEpisodeOrderByDateActionAsc`** : historique d'un **épisode**
  précis, utilisé par le contrôle de décision unique (**RM-040**).
- Requêtes en `join fetch` (`findParDemande`, `rechercher`) pour éviter les requêtes N+1.

### 5.7 `service/` — **le cœur des règles métier**

#### 5.7.1 `LigneRegles` — validation de la saisie d'une ligne

C'est la classe qui porte les **RM-016 → RM-030**, **RM-061**, **RM-062**.

**`construire(demande, source)`** — crée une ligne depuis la saisie :
1. Vérifie que le **service existe** (sinon 404).
2. Copie les champs : type de besoin, type de remplacement, intitulé (trimé), description, profil, salaires, effectif, budget.
3. `controlerSalaires()` puis `controlerEffectifEtBudget()`.
4. According au type : `controlerCreation()` ou `controlerRemplacement()`.
5. `controlerCoherenceService()`.

**`controlerModification(ligne, source)`** — même contrôles sur une ligne modifiée
(intitulé, description, profil, salaires, effectif, budget, et les champs de remplacement si `REMPLACEMENT`).

| Contrôle | Code d'erreur | Règle |
|---|---|---|
| Effectif ≤ 0 | `EFFECTIF_INVALIDE` | RM-016 |
| Budget estimé < 0 | `BUDGET_INVALIDE` | RM-016 |
| Salaire min ou max < 0 | `SALAIRE_INVALIDE` | RM-017 |
| Salaire max < salaire min | `SALAIRE_INVALIDE` | RM-017 |
| `CREATION` avec `typeRemplacement` | `TYPE_BESOIN_INVALIDE` | RM-022 |
| `CREATION` avec `idEmployeRemplace` | `TYPE_BESOIN_INVALIDE` | RM-022 |
| `REMPLACEMENT` sans type | `TYPE_REMPLACEMENT_MANQUANT` | RM-023 |
| `REMPLACEMENT` sans employé **et** sans motif | `MOTIF_REMPLACEMENT_MANQUANT` | RM-026 |
| Remplacement **temporaire** sur employé non `ACTIF` | `EMPLOYE_NON_VALIDE` | RM-027 |
| Remplacement **définitif** sur employé `INACTIF` | `EMPLOYE_NON_VALIDE` | RM-027 |
| Temporaire sans date de début ou de fin | `DATE_REMPLACEMENT_MANQUANTE` | RM-028 |
| Date de fin < date de début | `DATES_REMPLACEMENT_INVALIDES` | RM-029 |
| Définitif sans motif | `MOTIF_REMPLACEMENT_MANQUANT` | RM-029 |
| Poste référencé hors du service de la ligne | `SERVICE_INCOHERENT` | RM-061 |

**Détail important (RM-030)** : si l'employé remplacé occupe un poste, **le poste officiel de cet employé
est repris automatiquement** dans la ligne. C'est ce qui permet, à l'acceptation, de rattacher la ligne
à un poste existant du catalogue.

#### 5.7.2 `StatutService` — calcul du statut global

**Principe (RM-041 → RM-047)** : le statut global **n'est jamais saisi**, il est recalculé.

```
si la demande n'est pas soumise (dateSoumission == null) ou n'a aucune ligne  -> BROUILLON
sinon si au moins une ligne est A_CORRIGER                                   -> A_CORRIGER
sinon si au moins une ligne est BROUILLON ou en attente de validation       -> EN_VALIDATION
sinon (toutes les lignes sont terminées) :
      toutes annulées                                                       -> ANNULEE
      aucune acceptée                                                        -> REFUSEE
      toutes acceptées                                                       -> FINALISEE
      sinon                                                                  -> FINALISEE_PARTIELLE
```

**`recalculer(demande, lignes, auteur, commentaire)`** : calcule le nouveau statut, **ne modifie la demande
que s'il a changé**, met à jour `dateModification`, et **journalise** le changement dans `historique_statut`
avec `id_demande_poste = null` (journal global). C'est la **RM-065**.

#### 5.7.3 `ValidationService` — le circuit de validation

**`decider(idLigne, decision, validateur)`** — ordre **strict** des contrôles :

| # | Contrôle | Erreur | Règle |
|---|---|---|---|
| 1 | La ligne est-elle en validation ? | `LIGNE_NON_VALIDABLE` | RM-039 |
| 2 | `etapeCourante` de la ligne == étape du rôle du validateur ? | 403 `ACCES_REFUSE` | **RM-039** |
| 3 | Le validateur est-il le créateur de la demande ? | 403 `ACCES_REFUSE` | **RM-005** (séparation des responsabilités) |
| 4 | L'étape n'a-t-elle pas déjà été approuvée sur cet épisode ? | `VALIDATION_DEJA_FAITE` | **RM-040** |
| 5 | `montantVerifie` (si renseigné) == `budgetEstime` ? | `MONTANT_INCOHERENT` (400) | **RM-054** |
| 6 | Étape `FINANCE` → contrôle budgétaire | `BUDGET_ABSENT` / `BUDGET_INSUFFISANT` | **RM-053** |

Puis : enregistrement de la décision, application de la transition, mise à jour de la ligne,
**journalisation** (`historique_statut`), **recalcul du statut global**, retour du résumé de ligne.

**Transitions (`appliquerDecision`)** :

| Décision | Effet |
|---|---|
| `REFUSE` | statut `REFUSEE` |
| `A_CORRIGER` | statut `A_CORRIGER` **et `etapeCourante` conservée** → la ligne reprendra **à la même étape** après correction (**RM-038**) |
| `APPROUVE` | si une étape suivante existe → statut correspondant (`EN_ATTENTE_FINANCE`, `EN_ATTENTE_DIRECTION`) et `etapeCourante` mise à jour ; sinon → `ACCEPTEE` et `etapeCourante = null` |

**À l'acceptation (`accepterLigne`)** — deux effets, **dans la même transaction** :
1. `creerPosteSiNecessaire()` (**RM-009** / **RM-010** / **RM-066**) : si la ligne n'a pas de poste, on cherche
   d'abord un **poste équivalent** (même service, intitulé insensible à la casse) ; sinon on **crée** le poste
   à partir de l'intitulé, de la description et de la fourchette de salaire de la demande, et on le rattache à la ligne.
2. `budgetService.engager(ligne)` : **l'engagement budgétaire n'a lieu qu'à l'acceptation** (**RM-051**).

**`etapesRequises(ligne)`** — l'étape `DIRECTION` peut être conditionnée à un seuil :
si `seuilDirection > 0` **et** `budgetEstime < seuil`, `DIRECTION` est retirée du circuit.
Avec la valeur par défaut `0`, **la DIRECTION valide toujours**.

**`fileAttente(validateur)`** — la file du validateur : elle est **vide** si le rôle de l'utilisateur
n'est pas une étape du circuit (par exemple `MANAGER`).

#### 5.7.4 `BudgetService` — contrôle et engagement budgétaire

| Méthode | Fonctionnalité |
|---|---|
| `lister(idService, exercice)` / `obtenir(...)` | consultation des budgets |
| `definirBudget(demande)` | crée ou met à jour le budget d'un (service, exercice) ; montant ≥ 0 (**RM-048/049**) ; l'unicité `(service, exercice)` est garantie en base (`uq_budget_service_exercice`) |
| `verifierLigne(ligne, decision, validateur, commentaire)` | **verrou pessimiste** sur le budget ; `BUDGET_ABSENT` si aucun budget pour le service et l'exercice ; si `decision == APPROUVE` et `montant > disponible` → **`BUDGET_INSUFFISANT`** ; enregistre une ligne dans `verification_budget` (budget demandé, budget disponible, montant estimé, décision, commentaire, épisode) |
| `engager(ligne)` | `montant_engage += budgetEstime` — appelé **à l'acceptation** |
| `liberer(ligne)` | `montant_engage -= budgetEstime` (plancher 0) — libération en cas d'annulation |

**Exercice budgétaire d'une ligne** : c'est **l'année de la `dateSouhaitee`** de la demande (**RM-050**).
**Budget disponible** = `montantAlloue − montantDepense − montantEngage` (**RM-051**).

#### 5.7.5 `DemandeService` — cycle de vie des demandes

| Méthode | Fonctionnalité et règles |
|---|---|
| `lister(lecteur, statut, recherche, mesDemandes)` | **RM-067** : un `MANAGER` est forcé sur ses propres demandes ; les autres rôles voient tout |
| `detail(idDemande, lecteur)` | contrôle de **périmètre en lecture** : un `MANAGER` qui ouvre la demande d'un autre reçoit 403 |
| `creer(auteur, demande)` | contrôle de la date souhaitée ; **au moins une ligne** sinon `DEMANDE_SANS_LIGNE` (**RM-015**) ; statut initial `BROUILLON` ; chaque ligne passe par `LigneRegles.construire()` |
| `modifierEntete(...)` | propriétaire + **brouillon** uniquement (**RM-019**) |
| `ajouterLigne(...)` | propriétaire + brouillon (**RM-015**, **RM-019**) |
| `supprimerLigne(...)` | propriétaire + brouillon |
| `modifierLigne(...)` | **règle centrale RM-019/RM-021** : si la demande est en `BROUILLON` → seule une ligne `BROUILLON` est modifiable ; sinon → **seule une ligne `A_CORRIGER`** est modifiable, et **par son demandeur**. Toute autre tentative → `LIGNE_NON_MODIFIABLE` |
| `soumettre(...)` | propriétaire + brouillon ; **chaque ligne doit être complète** (`LIGNE_INCOMPLETE`) ; toutes les lignes passent à la **première étape du circuit** (`EN_ATTENTE_RH`) avec `dateSoumission` ; la demande passe à `EN_VALIDATION` (**RM-020**) |
| `resoumettreLigne(...)` | propriétaire ; la ligne doit être `A_CORRIGER` sinon `LIGNE_NON_RESOUMISSABLE` ; contrôle de complétude ; **`episode += 1`** (**RM-040**) ; le circuit **reprend à `etapeCourante`** (**RM-038**) |
| `annulerLigne(...)` | propriétaire ; refus si la ligne est déjà `REFUSEE` ou `ANNULEE` (`LIGNE_DEJA_TERMINEE`) ; sinon statut `ANNULEE`, `etapeCourante = null` |

Contrôles internes : `controlerProprietaire()` (403 « Seul le createur de la demande peut la modifier »),
`controlerBrouillon()`, `controlerAccesLecture()`, `controlerLigneComplete()`, `controlerDateSouhaitee()`.

#### 5.7.6 `PosteService`, `UtilisateurService`, `EmployeService`, `AuthService`, `DashboardService`

| Service | Fonctionnalité |
|---|---|
| **`PosteService`** | `lister` (avec le compteur `nbPostesLies`), `creer` (service existant, doublon d'intitulé → `POSTE_DOUBLON`, salaires cohérents), `modifier`, `desactiver` — **jamais de suppression** (**RM-011**) |
| **`UtilisateurService`** | `lister` (filtres rôle / département / recherche / actif), `creer` (email déjà utilisé → `EMAIL_DEJA_UTILISE` ; mot de passe ≥ 6 caractères ; BCrypt ; rôle et département doivent exister), `changerActivation` (active/désactive un compte), `roles()` |
| **`EmployeService`** | `lister` avec filtres statut / service / recherche. **Aucune suppression d'employé n'est exposée.** |
| **`AuthService`** | `connexion(email, motDePasse)` : e-mail **insensible à la casse** → compte **inactif** ⇒ 401 `COMPTE_INACTIF` → mot de passe BCrypt non conforme ⇒ 401 `IDENTIFIANTS_INVALIDES` → retour **jeton + expiration + profil**. `profil()` : renvoie le profil de l'utilisateur courant. |
| **`DashboardService`** | Synthèse **par rôle** : compteurs de demandes par statut, `lignesAValider` (file du rôle, **hors lignes du demandeur lui-même**), lignes acceptées / à corriger, étape du rôle, et agrégats `budgetAlloue` / `budgetEngage` / `budgetDisponible` sur l'exercice. |

### 5.8 `web/controller/` — la surface REST

Les contrôleurs sont **minces** : ils valident le corps (`@Valid`), délèguent au service et
retournent un DTO. Les contrôles de rôle utilisent `utilisateurCourant.exigerRole(RoleNom.X)`.

| Contrôleur | Base | Points d'attention |
|---|---|---|
| `AuthController` | `/api/auth` | `POST /login` public, `GET /me` authentifié |
| `DemandeController` | `/api/demandes` | gestion en-tête + lignes + cycle de vie |
| `ValidationController` | `/api/validation` | file du rôle + décision |
| `PosteController` | `/api/postes` | **création / modification / désactivation réservées au RH** |
| `BudgetController` | `/api/budgets` | **définition du budget réservée à la FINANCE** ; lecture ouverte à tout authentifié |
| `UtilisateurController` | `/api/utilisateurs` | **tout réservé au RH** ; la création reçoit une `Map<String,Object>` non validée (validation métier manuelle) |
| `ReferentielController` | `/api/referentiel` | départements, services, employés — authentifié |
| `DashboardController` | `/api/dashboard` | synthèse |

### 5.9 `web/dto/` — les 7 familles de DTO

Toutes les classes sont des **`record`** Java (immuables, accesseurs typés) :

| DTO | Records |
|---|---|
| `AuthDto` | `Connexion` (`@Email @NotBlank`), `Reponse` (token, expiration, profil), `Profil` (+ factory `Profil.de(Utilisateur)`) |
| `DemandeDto` | `Creation`, `Modification`, `LigneCreation`, `LigneModification`, `Resume`, `LigneResume`, `LigneDetail`, `Detail` (+ `versResume()`) |
| `ValidationDto` | `DecisionRequest` (`@NotNull decision`, `commentaire`, `montantVerifie`), `Historique`, `VerificationBudgetaire`, `ChangementStatut` |
| `PosteDto` | `Liste` (+ `nbPostesLies`), `Creation`, `Modification` |
| `BudgetDto` | `Liste` (+ `montantDisponible`), `MiseAJour` |
| `EmployeDto` | `Liste` |
| `DashboardDto` | `Resume` (16 compteurs et agrégats) |

`LigneCreation` porte les annotations de validation (`@Positive` sur l'effectif, etc.) ;
`DecisionRequest.decision` est `@NotNull` : **une décision sans valeur est rejetée par Bean Validation**.

---

## 6. Frontend — explication du code

### 6.1 `src/main.js` et `src/App.vue`

`main.js` monte l'application : imports de `base.css` + `main.css`, `createApp(App)`,
`createPinia()`, `router`, puis `mount('#app')`.
`App.vue` ne contient que `<RouterView />` et `<NotificationContainer />` : les notifications
sont **hors du layout**, donc visibles sur tous les écrans.

### 6.2 `src/api/client.js` — le client HTTP unique

Point d'entrée unique vers l'API. Fonctions :

- `BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'` (configurable par variable d'environnement).
- **`class ErreurApi extends Error`** : porte `code`, `statut`, `chemin` — le front peut donc
  distinguer une erreur métier (`BUDGET_INSUFFISANT`) d'une erreur technique.
- `definirJeton()` / `obtenirJeton()` / `surJetonExpire(callback)` : gestion du jeton en module.
- **`requete(methode, chemin, corps)`** : ajoute `Accept: application/json`, `Content-Type` si corps,
  `Authorization: Bearer …` si jeton ; **204 → `null`** ; **serveur injoignable** → `ErreurApi("Le serveur est injoignable…", {statut: 0})` ;
  **401 → callback de déconnexion automatique** ; sinon le `message` serveur est remonté tel quel (**RM-068**).
- Façade : `api.get / api.post / api.put / api.delete`.

### 6.3 `src/stores/`

**`stores/auth.js`** (Pinia) — la session :

- Clé `localStorage` : **`erp.token`**.
- State : `token`, `profil`, `chargement`.
- Getters : `connecte`, `role`, `estManager`, `estRh`, `estFinance`, `estDirection`, `initiales`.
- Actions : `connexion()` (POST `/auth/login` puis stockage du jeton), `rechargerProfil()` (GET `/auth/me`),
  `deconnexion()`, `aRole(...roles)`.

**`stores/notification.js`** — messages utilisateur : `ajouter(texte, type, titre, duree = 5000)`,
`succes()` (5 s), `erreur()` (8 s), `fermer(id)`. Identifiant auto-incrémenté pour clés `v-for` stables.

### 6.4 `src/router/index.js` — navigation et gardes

- Toutes les vues sont chargées en **`import()` dynamique** (code splitting).
- `meta.public = true` sur `/connexion` et sur la route 404.
- **`beforeEach`** : (1) si un jeton existe sans profil → `rechargerProfil()` ; (2) route non publique et
  non connecté → redirection vers `/connexion` avec `?suite=<url>` pour revenir après connexion ;
  (3) connecté et sur `/connexion` → redirection vers le tableau de bord ; (4) `meta.roles` supporté
  (mécanisme prévu, non utilisé par les écrans actuels — le filtrage réel se fait dans `AppSidebar`).
- `afterEach` : `document.title = "<titre> · ERP TOVO"`.

### 6.5 `src/layouts/AppLayout.vue` et les composants

| Composant | Fonctionnalité |
|---|---|
| `AppLayout.vue` | Structure : `.page` > `AppSidebar` + `.contenu` > `AppTopbar` + `<main class="page__corps"><RouterView/></main>`. Le titre vient de `route.meta.titre`. Bouton de déconnexion. |
| `AppSidebar.vue` | Logo « T » + marque « ERP TOVO / Recrutement ». **Menu filtré par rôle** : toujours *Tableau de bord*, *Demandes*, *Nouvelle demande*, *File de validation*, *Employes* ; **+ RH** : *Catalogue des postes*, *Utilisateurs* ; **+ FINANCE** : *Budgets*. État actif calculé (le lien *Demandes* reste actif sur `demande-detail`). Pied de page : nom + rôle. |
| `AppTopbar.vue` | Titre, département de l'utilisateur, bouton « Deconnexion ». |
| `StatusBadge.vue` | Props `valeur`, `libelle`, `type`. Deux dictionnaires : `LIBELLES` (26 entrées) et `COULEURS` (neutre / info / primaire / succes / alerte / danger). Rendu `.badge.badge--<couleur>`. |
| `ModalDialog.vue` | Props `titre`, `sousTitre`, `large`, `fermable`. Fermeture au clic sur le fond et à la touche **Échap**. Slots `default` + `actions`. |
| `NotificationContainer.vue` | Liste des notifications `.notification--<type>` avec titre, texte et bouton de fermeture. |
| `EmptyState.vue` | État vide `.etat-vide` : titre, message, slot d'action. |
| `HistoriqueValidations.vue` | Tableau compact **Date / Étape / Décision / Valideur / Commentaire**. État vide « Aucune décision ». Utilise `formatDateHeure` et `libelle`. |

### 6.6 `src/utils/format.js`

| Fonction | Rôle |
|---|---|
| `formatMontant` | `fr-FR` + suffixe **« Ar »** (Ariary) |
| `formatNombre` | formatage numérique localisé |
| `formatDate` / `formatDateHeure` | dates FR |
| `libelle` | transforme `EN_ATTENTE_FINANCE` → « En attente finance » (underscores → espaces, 1re lettre en capitale) |
| `libelles(dict)` | application d'un dictionnaire de libellés |

### 6.7 Les 11 écrans

| Écran | Fonctionnalité et appels API |
|---|---|
| **`ConnexionView.vue`** | 2 colonnes (présentation + formulaire). E-mail / mot de passe pré-remplis (`manager@tovo.mg` / `Manager@2026`) et **4 boutons de comptes de démonstration**. Message d'erreur `.alerte--erreur`. Redirection vers `?suite=` ou le tableau de bord. |
| **`TableauBordView.vue`** | `GET /dashboard` + `GET /demandes` en parallèle. **4 cartes `.stat`** : Demandes, En attente de validation, Lignes acceptées, Budget disponible. **Jauge de consommation** `(engagé + dépensé) / alloué` avec seuils **70 % (alerte)** et **90 % (danger)**. Carte Budget. Liste des 5 dernières demandes. Bouton « Gérer les budgets » (FINANCE). Libellé d'accueil différent par rôle. |
| **`DemandesView.vue`** | `GET /demandes` avec filtres `statut` + `recherche`. Compteur et **budget total cumulé**. Tableau `DEM-0001` / motif / demandeur / date / lignes / budget / statut. `EmptyState` si liste vide. Message de périmètre dépendant du rôle. |
| **`DemandeNouvelleView.vue`** | `GET /referentiel/services` + `/postes` + `/referentiel/employes`. En-tête : motif + date souhaitée (**+3 mois** par défaut). Lignes dynamiques via `nouvelleLigne()` : type de besoin CREATION/REMPLACEMENT, poste filtré par service, employé filtré par service, motif + dates de remplacement, intitulé, description, profil, salaire min/max, effectif, budget. Boutons « Ajouter une ligne », « Enregistrer en brouillon », « Enregistrer et soumettre ». Séquence : `POST /demandes` puis `POST /demandes/{id}/soumettre`. |
| **`DemandeDetailView.vue`** | `GET /demandes/{id}`. En-tête motif + statut. Onglets « Lignes » / « Historique ». Par ligne : service, poste, type de besoin, budget, **étape courante** (badge « Etape : … »), statut. **Actions conditionnelles** : *Soumettre* (demandeur + BROUILLON), *Traiter/valider* (validateur dont le rôle = `etapeCourante`), *Corriger* (demandeur + ligne `A_CORRIGER`), *Resoumettre*, *Annuler*. Modale de décision (`#decision`, `#montantVerifie`, `#commentaire`). Modale de correction (`#corrige-intitule`, `#corrige-effectif`, `#corrige-budget`, `#corrige-salaire-min/max`, `#corrige-profil`, `#corrige-description`). Encart `.carte--accent` « lignes renvoyées à corriger ». Historique via `HistoriqueValidations`. |
| **`ValidationView.vue`** | `GET /validation/file`. Filtre par étape (défaut = rôle courant). Table des lignes à traiter. Modale `#decision-file` (APPROUVE / A_CORRIGER / REFUSE), `#montant-file` (**visible pour la FINANCE**), `#commentaire-file`. `POST /validation/lignes/{id}/decision`. |
| **`PostesView.vue`** | `GET /postes` (filtres `idService`, `actif`, `recherche`) + `GET /referentiel/services`. Bouton « Nouveau poste » (RH). Table avec `nbPostesLies`. Modale `#intitule-form`, `#service-form`, `#description-form`, `#salaire-min/max-form`, `#actif-form`. `POST /postes` ou `PUT /postes/{id}`. `POST /postes/{id}/desactiver`. |
| **`BudgetsView.vue`** | `GET /budgets?exercice=` + services. Sélecteur d'exercice. Totaux alloué / engagé / disponible. Jauge par service (seuils 70 / 90 %). Modale `#service-budget`, `#exercice-form`, `#montant-form`. `PUT /budgets` (FINANCE). |
| **`EmployesView.vue`** | `GET /referentiel/employes` (filtres `statut`, `idService`, `recherche`). Table matricule / nom / poste / service / date d'embauche / statut. |
| **`UtilisateursView.vue`** | `GET /utilisateurs` + `/referentiel/departements` + `/utilisateurs/roles`. Filtres recherche + rôle. Table nom / e-mail / rôle / département / statut + bouton d'activation. Modale `#nom-form`, `#email-form`, `#mdp-form`, `#role-form`, `#departement-form`. `POST /utilisateurs`, `POST /utilisateurs/{id}/activation?actif=`. |
| **`IntrouvableView.vue`** | « Page introuvable » + bouton de retour au tableau de bord. |

### 6.8 Le thème (`src/assets/`)

- **`base.css`** (125 lignes) : les **variables CSS** — `--fond #f4f6f8`, `--carte #fff`,
  `--sidebar #12302d`, `--primaire #0f766e`, `--bordure #e3e8ed`, nuances succès / alerte / danger / info,
  rayons, ombres, `--largeur-sidebar 220px`, `--hauteur-topbar 64px`, police Inter ;
  remise à zéro, typographie, `:focus-visible`, utilitaires `.texte-discret`, `.texte-secondaire`, `.mono`.
- **`main.css`** (913 lignes) : 14 sections — mise en page (`.page`, `.sidebar*`, `.topbar*`),
  cartes (`.carte`, `.grille--2/3/4` avec breakpoints 1100 px / 700 px, `.stat*`),
  tableaux (`.tableau*`, `.tableau--compacte`), badges, boutons, formulaires (`.champ*`, `.saisie`,
  `.grille-champs--3` breakpoint 800 px, `.barre-filtres`), modale, notifications,
  page de connexion (`.comptes-demo*` breakpoint 900 px), états (`.etat-vide`, `.chargement`, `.alerte*`),
  détails (`.liste-definitions`, `.onglets`, `.fil-ariane`, `.jauge`, `.ligne-liste`).

La palette est reprise de la maquette « Catalogue des postes (RH).png ».

---

## 7. API REST du Bloc 1

Base : `http://localhost:8080/api`. Authentification : `Authorization: Bearer <JWT>`.
Swagger : `http://localhost:8080/swagger-ui.html` · OpenAPI : `/v3/api-docs`.

### 7.1 Authentification

| Méthode | Chemin | Accès | Description |
|---|---|---|---|
| `POST` | `/auth/login` | public | Connexion (e-mail + mot de passe) → jeton + expiration + profil |
| `GET` | `/auth/me` | authentifié | Profil de l'utilisateur courant |

### 7.2 Demandes de recrutement

| Méthode | Chemin | Description |
|---|---|---|
| `GET` | `/demandes?statut&recherche&mesDemandes` | Liste — MANAGER limité à ses demandes |
| `GET` | `/demandes/{id}` | Détail (lignes + historique) — contrôle de périmètre |
| `POST` | `/demandes` | Création (en-tête + lignes) → `BROUILLON` |
| `PUT` | `/demandes/{id}` | Modification de l'en-tête (brouillon) |
| `POST` | `/demandes/{id}/lignes` | Ajout d'une ligne (brouillon) |
| `PUT` | `/demandes/{id}/lignes/{idLigne}` | Modification d'une ligne (brouillon **ou** `A_CORRIGER`) |
| `DELETE` | `/demandes/{id}/lignes/{idLigne}` | Suppression d'une ligne (brouillon) |
| `POST` | `/demandes/{id}/soumettre` | Soumission → `EN_VALIDATION` |
| `POST` | `/demandes/{id}/lignes/{idLigne}/resoumettre` | Resoumission après correction (épisode + 1) |
| `POST` | `/demandes/{id}/lignes/{idLigne}/annuler` | Annulation d'une ligne |

### 7.3 Validation, postes, budgets

| Méthode | Chemin | Rôle | Description |
|---|---|---|---|
| `GET` | `/validation/file` | tous | File de validation du rôle courant |
| `POST` | `/validation/lignes/{idLigne}/decision` | étape de la ligne | Décision (APPROUVE / REFUSE / A_CORRIGER) + commentaire + montant vérifié |
| `GET` | `/postes?idService&actif&recherche` | tous | Catalogue des postes |
| `POST` | `/postes` | **RH** | Création d'un poste |
| `PUT` | `/postes/{id}` | **RH** | Modification |
| `POST` | `/postes/{id}/desactiver` | **RH** | Désactivation (jamais de suppression) |
| `GET` | `/budgets?idService&exercice` | tous | Liste des budgets |
| `GET` | `/budgets/{idService}/{exercice}` | tous | Détail d'un budget |
| `PUT` | `/budgets` | **FINANCE** | Définition du montant alloué |

### 7.4 Référentiel, utilisateurs, tableau de bord

| Méthode | Chemin | Rôle | Description |
|---|---|---|---|
| `GET` | `/referentiel/departements` | tous | Départements |
| `GET` | `/referentiel/services?idDepartement` | tous | Services |
| `GET` | `/referentiel/employes?statut&idService&recherche` | tous | Employés |
| `GET` | `/utilisateurs?role&idDepartement&recherche&actif` | **RH** | Liste des comptes |
| `GET` | `/utilisateurs/roles` | **RH** | Liste des rôles |
| `POST` | `/utilisateurs` | **RH** | Création d'un compte |
| `POST` | `/utilisateurs/{id}/activation?actif=` | **RH** | Activation / désactivation |
| `GET` | `/dashboard?exercice` | tous | Synthèse par rôle |

---

## 8. Catalogue des règles métier (RM-001 → RM-068)

| Règle | Intitulé | Implémentation |
|---|---|---|
| RM-001 | Connexion par e-mail + mot de passe | `AuthService.connexion`, `AuthController` |
| RM-002 | Message d'erreur explicite à la connexion | `GlobalExceptionHandler` → `ApiError` |
| RM-003 | Une ligne = une décision ; statut global calculé | `StatutService`, `demande_recrutement_poste` |
| RM-004 | Gestion des utilisateurs et des employés | `UtilisateurService`, `EmployeService` |
| RM-005 | Séparation des responsabilités | `ValidationService.controlerSeparationDesResponsabilites` → 403 |
| RM-006 → RM-018 | Saisie d'une ligne (service, type, intitulé, effectif, budget, salaires, profil, description) | `LigneRegles.construire` |
| RM-010 | Doublons de poste ou de demande (casse + espaces insensibles) | `PosteRepository.findEquivalents`, `ServiceRepository.findEquivalents` |
| RM-011 | Pas de suppression d'un poste (désactivation) | `PosteService.desactiver` |
| RM-012 | Création d'une demande | `DemandeService.creer` |
| RM-014 | Date souhaitée ≥ `now + délaiMinimum` | `DemandeService.controlerDateSouhaitee` |
| RM-015 | Au moins une ligne par demande | `DEMANDE_SANS_LIGNE` |
| RM-016 → RM-018 | Contrôles effectif / budget / salaires | `LigneRegles` |
| RM-019 | Demande soumise = non modifiable | `controlerBrouillon`, `controlerLigneModifiable` |
| RM-020 | Ligne soumise = complète | `controlerLigneComplete` → `LIGNE_INCOMPLETE` |
| RM-021 | Seule une ligne `A_CORRIGER` est modifiable après soumission | `controlerLigneModifiable` |
| RM-022 | `CREATION` sans remplacement | `controlerCreation` |
| RM-023 → RM-026 | Remplacement : type, employé, motif | `controlerRemplacement` |
| RM-027 | Statut de l'employé remplacé cohérent | `controlerStatutEmployeRemplace` |
| RM-028, RM-029 | Dates et motif du remplacement | `controlerDatesRemplacement` |
| RM-030 | Reprise du poste officiel de l'employé remplacé | `controlerRemplacement` |
| RM-032 | Circuit RH → FINANCE → DIRECTION | `CircuitValidationProperties`, `ValidationService` |
| RM-034 | Seuil de DIRECTION configurable | `etapesRequises()` |
| RM-037 | Ligne `A_CORRIGER` modifiable par le demandeur | `controlerLigneModifiable` |
| RM-038 | Resoumission : reprise **à la même étape** | `resoumettreLigne` + `etapeCourante` conservée |
| RM-039 | Seul le rôle de l'étape courante peut décider | `controlerEtape` → 403 |
| RM-040 | Un épisode = une validation par étape | `episode` + `controlerDecisionUnique` |
| RM-041 → RM-047 | Calcul du statut global | `StatutService.calculerStatutGlobal` |
| RM-048, RM-049 | Définition du budget par service et exercice | `BudgetService.definirBudget` |
| RM-050 | Exercice budgétaire = année de la date souhaitée | `BudgetService.exerciceDe` |
| RM-051 | Disponible = alloué − dépensé − engagé ; engagement à l'acceptation | `Budget.getMontantDisponible()`, `BudgetService.engager` |
| RM-053 | Refus automatique si budget insuffisant | `BUDGET_INSUFFISANT` |
| RM-054 | Montant vérifié facultatif mais doit correspondre | `controlerMontantVerifie` → `MONTANT_INCOHERENT` |
| RM-056 | Verrou pessimiste sur le budget | `BudgetRepository.findForMiseAJour` (`PESSIMISTIC_WRITE`) |
| RM-057 | Interface responsive | breakpoints 1100 / 900 / 800 / 700 px, test 420 px |
| RM-061 | Service de la ligne = service du poste | `controlerCoherenceService` → `SERVICE_INCOHERENT` |
| RM-062 | Doublons de poste interdits | `PosteService.creer` → `POSTE_DOUBLON` |
| RM-063 | Seules les valeurs d'énumération autorisées | `chk_*` SQL + enums Java |
| RM-065 | Journal des changements de statut | `HistoriqueStatut` + `StatutService.recalculer` |
| RM-066 | Poste créé à l'acceptation, dans la même transaction | `creerPosteSiNecessaire` |
| RM-067 | Périmètre MANAGER limité à ses demandes | `DemandeService.lister` / `controlerAccesLecture` / `controlerProprietaire` |
| RM-068 | Format d'erreur unique `{code, message, status, chemin}` | `ApiError` + `GlobalExceptionHandler` + `ErreurApi` (front) |

---

## 9. Codes d'erreur

### 9.1 Structure de la réponse d'erreur

```json
{
  "code": "BUDGET_INSUFFISANT",
  "message": "Budget insuffisant : le besoin de 900000000 depasse le budget disponible de 38000000 pour le service Ventes sur l'exercice 2026.",
  "status": 400,
  "chemin": "/api/validation/lignes/12/decision"
}
```

### 9.2 Codes métier les plus fréquents

| Code | HTTP | Situation |
|---|---|---|
| `RESOURCE_INTROUVABLE` | 404 | Ressource inexistante |
| `ACCES_REFUSE` | 403 | Rôle insuffisant, périmètre, séparation des responsabilités |
| `IDENTIFIANTS_INVALIDES` | 401 | Mot de passe incorrect |
| `COMPTE_INACTIF` | 401 | Compte désactivé |
| `NON_AUTHENTIFIE` | 401 | Jeton absent / invalide / expiré |
| `DONNEES_INVALIDES` | 400 | Échec de Bean Validation (messages agrégés par `;`) |
| `ERREUR_INTERNE` | 500 | Exception inattendue (logguée côté serveur) |
| `EFFECTIF_INVALIDE`, `BUDGET_INVALIDE`, `SALAIRE_INVALIDE` | 400 | Contrôles de saisie d'une ligne |
| `TYPE_BESOIN_INVALIDE`, `TYPE_REMPLACEMENT_MANQUANT`, `MOTIF_REMPLACEMENT_MANQUANT` | 400 | Incohérence type de besoin / remplacement |
| `DATE_REMPLACEMENT_MANQUANTE`, `DATES_REMPLACEMENT_INVALIDES` | 400 | Dates de remplacement |
| `EMPLOYE_NON_VALIDE`, `SERVICE_INCOHERENT` | 400 | Cohérence employé / service |
| `DEMANDE_SANS_LIGNE`, `DEMANDE_NON_MODIFIABLE`, `LIGNE_INCOMPLETE`, `LIGNE_NON_MODIFIABLE` | 400 | Cycle de vie de la demande |
| `LIGNE_NON_RESOUMISSABLE`, `LIGNE_DEJA_TERMINEE`, `LIGNE_NON_VALIDABLE` | 400 | Transitions refusées |
| `VALIDATION_DEJA_FAITE`, `ETAPE_INCONNUE`, `DECISION_INCONNUE` | 400 | Circuit de validation |
| `MONTANT_INCOHERENT` | 400 | Montant vérifié ≠ montant soumis |
| `BUDGET_ABSENT`, `BUDGET_INSUFFISANT` | 400 | Contrôle budgétaire |
| `POSTE_DOUBLON` | 400 | Doublon d'intitulé de poste |
| `EMAIL_DEJA_UTILISE` | 400 | E-mail déjà utilisé |
| `DATE_SOUHAITEE_INVALIDE` | 400 | Date souhaitée trop courte |

---

## 10. Tests et recette

### 10.1 Les trois scripts de recette

| Script | Niveau | Contenu | Résultat attendu |
|---|---|---|---|
| `tests/api_bloc1.sh` (372 l.) | API REST — bash + curl + node | 14 sections : connexion, référentiels, catalogue des postes, demande multi-lignes, règles de saisie, soumission, circuit, contrôle budgétaire, budget insuffisant, décision finale, séparation des responsabilités, correction/resoumission, périmètre MANAGER, tableau de bord, utilisateurs. Lecteur JSON **embarqué en Node** (opérations `get`, `len`, `has`, `findid`, `findkey`, `hasid`, `contains`) pour **n dépendre ni de `jq` ni de `python`**. | **56 vérifications OK**, 0 échec, code retour 0 |
| `frontend/test_ui.mjs` (268 l.) | Navigateur — Playwright | 11 sections : connexion → tableau de bord → liste des demandes → création d'une demande → validation RH → validation FINANCE (avec jauge et montant vérifié) → validation DIRECTION → catalogue des postes → employés & utilisateurs → correction d'une ligne → responsive 420 px. Collecte des **erreurs console** et des **réponses API ≥ 400** ; captures dans `/tmp/opencode/captures`. | **28 vérifications OK**, 0 échec |
| `test_local.sh` (193 l.) | Orchestrateur | Vérifie les pré-requis (`java`, `node`, `npm`, `curl`, `psql`, `node_modules`, `application.properties` — code 2 si manquant), **démarre** backend (`./mvnw -o spring-boot:run`, log `/tmp/backend-test-local.log`, attente 90 s) et frontend (`npx vite --port 5173 --host`, log `/tmp/frontend-test-local.log`, attente 40 s), lance les deux scripts, affiche un récapitulatif. Supporte `--no-start`. | code 0 |

### 10.2 Parcours métier de référence

```
MANAGER                     RH                     FINANCE                  DIRECTION
   │                         │                        │                        │
   ├─ Nouvelle demande (BROUILLON)                   │                        │
   ├─ 2 lignes (CREATION + REMPLACEMENT)             │                        │
   ├─ Soumettre ─────────────▶ EN_VALIDATION (étape RH)                     │
   │                         ├─ Contrôle de saisie + doublons                 │
   │                         ├─ APPROUVE ─────────────▶ étape FINANCE         │
   │                         │                        ├─ Vérif. budget (dispo.) │
   │                         │                        ├─ APPROUVE ───────────▶ étape DIRECTION
   │                         │                        │                        ├─ APPROUVE
   │                         │                        │                        └─ ACCEPTEE
   │                         │                        │                           + poste créé si besoin
   │                         │                        │                           + budget engagé
   │                         │                        │
   │   ◀── A_CORRIGER ───────┤ (reprise à la MÊME étape, épisode + 1)
   ├─ Corriger la ligne ─────▶ resoumettre ──▶ reprise du circuit
```

### 10.3 Checklist de recette

- [ ] `./mvnw -o compile` sans erreur
- [ ] `npm run build` sans erreur
- [ ] `npm run lint` sans erreur
- [ ] `./tests/api_bloc1.sh` → 56 vérifications OK
- [ ] `cd frontend && node test_ui.mjs` → 28 vérifications OK
- [ ] Parcours manuel complet (connexion → demande → validation → correction → poste → budgets)
- [ ] Statut global cohérent à chaque étape
- [ ] Journal des statuts et historique des décisions consultables sur le détail d'une demande

---

## 11. Mise en route

### 11.1 Pré-requis

Java 21 · Node.js 20+ (le `package.json` exige `^22.18.0 || >=24.12.0`) · PostgreSQL 14+ · Maven Wrapper fourni.

### 11.2 Base de données

```bash
cp bakend/src/main/resources/exemple.application.properties \
   bakend/src/main/resources/application.properties
# renseigner spring.datasource.url / username / password
PGPASSWORD=postgres psql -h localhost -U postgres -d erp -c '\dt'
```

Les tables sont créées automatiquement par **Flyway** au démarrage (`V1__bloc1_schema.sql`),
puis les données de démarrage sont insérées par `DataInitializer`.
Le script `bakend/src/main/resources/db/donnees_demo.sql` permet de recharger un jeu de test
**idempotent** (`ON CONFLICT DO NOTHING` + remise en état des séquences) :

```bash
PGPASSWORD=postgres psql -h localhost -U postgres -d erp \
    -f bakend/src/main/resources/db/donnees_demo.sql
```

### 11.3 Backend (port 8080)

```bash
cd bakend
./mvnw -o compile
./mvnw -o spring-boot:run
# arrêter : pkill -f "[c]om.tovo.erp.ErpApplication"
```

Swagger : <http://localhost:8080/swagger-ui.html>

### 11.4 Frontend (port 5173)

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
npm run build
npm run lint
npm run format
```

### 11.5 Comptes de démonstration

| Rôle | E-mail | Mot de passe | Statut |
|---|---|---|---|
| MANAGER | `manager@tovo.mg` | `Manager@2026` | actif |
| RH | `rh@tovo.mg` | `Rh@2026` | actif |
| FINANCE | `finance@tovo.mg` | `Finance@2026` | actif |
| DIRECTION | `direction@tovo.mg` | `Direction@2026` | actif |
| MANAGER | `ancien.manager@tovo.mg` | `Manager@2026` | **désactivé** (connexion refusée) |

Les mots de passe sont stockés **hachés en BCrypt** (jamais en clair).

---

## 12. Écarts et points d'attention

Relevés lors de la lecture du code, **aucun n'a été corrigé** :

| # | Constat | Impact / correction proposée |
|---|---|---|
| 1 | « **11 tables** » annoncé (en-tête de `V1__bloc1_schema.sql`, de `donnees_demo.sql`, `todo.txt`, message du commit `6d6236b`) alors que le schéma en crée **12** (`historique_statut`, RM-065) | Corriger la documentation en « 12 tables » |
| 2 | `HistoriqueValidations.vue` : `{{ ligne.etapeLible \|\| ligne.etapeLisible }}` — `etapeLible` **n'existe pas** | Le fallback fonctionne, mais produit un avertissement Vue en développement → remplacer par `ligne.etapeLisible` |
| 3 | La base n'interdit **pas** deux approbations de la même étape sur le même épisode | Le contrôle est uniquement applicatif (`controlerDecisionUnique`) → ajouter un `UNIQUE (id_demande_poste, etape, episode)` partiel ou un index unique filtré |
| 4 | Secret JWT en clair dans `exemple.application.properties` | À remplacer à l'installation (le code impose ≥ 32 caractères) |
| 5 | `UtilisateurController.creer` reçoit une `Map<String,Object>` non validée | unlike les autres DTO typés ; la validation est manuelle dans `UtilisateurService` → créer un `UtilisateurDto.Creation` |
| 6 | **Aucun test unitaire métier** | La seule classe JUnit est `ErpApplicationTests.contextLoads()` ; la couverture repose sur les deux scripts de recette → extraire des tests sur `LigneRegles`, `StatutService`, `ValidationService` |
| 7 | `erp.demande.delai-minimum = 0d` par défaut | Le contrôle **RM-014** (`dateSouhaitee ≥ now + délai`) est **inactif** → fixer une valeur (ex. `30d`) |
| 8 | `frontend/README.md` décrit encore le scaffold Vite d'origine | À réécrire pour l'application ERP TOVO |
| 9 | `frontend/.eslintcache` présent dans l'arborescence | Artefact de build (ignoré par git) → à exclure |
| 10 | `frontend/init-vite/` (scaffold Vite complet) conservé mais non utilisé | Exclusion ESLint déjà en place ; à supprimer ou documenter comme référence de styles |

---

## Annexe — chaîne de commits de la branche `Bloc1`

```
70f8e88 Merge pull request #1 from 0Mahatoky0/Bloc1
f5eb61e test_moi
6d6236b feat(backend): ajoute le jeu de donnees de test en SQL pour les 11 tables
650a5d9 docs: remplace todo par les etapes, les commandes et les tests navigateur
b39aba6 test(api): ajoute le test fonctionnel de l'API en script shell
724458e chore(frontend): conserve le scaffold Vite d'origine
dac1657 test(frontend): ajoute le parcours de verification navigateur
daa33b8 feat(frontend): ajoute les ecrans du bloc 1
80faad2 feat(frontend): ajoute la navigation, le layout et les composants
92067b4 feat(frontend): ajoute le theme, le client API et les stores
54d9f6c chore(backend): retire le controleur de test
797a4d8 feat(backend): expose l API REST du bloc 1
3546892 feat(backend): ajoute les services metier du bloc 1
d1565cb feat(backend): ajoute le schema Flyway et les donnees de demarrage
5cb2c45 feat(backend): configure la securite JWT, le CORS et le circuit de validation
bf01813 feat(backend): ajoute les repositories et les utilitaires communs
efd2e0e feat(backend): ajoute le modele de donnees du bloc 1
f20beaf docs: ajoute la specification fonctionnelle du bloc 1
64a2b40 init frontend
79d8f9d fix application . properties
```
