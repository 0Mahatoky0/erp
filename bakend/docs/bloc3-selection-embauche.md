# Bloc 3 — Sélection et embauche

> Document de conception et de justification du Bloc 3.
> Il décrit le code livré, les fonctionnalités, les décisions prises et les
> écarts connus par rapport aux documents normatifs `.md`.
>
> Ce document ne remplace pas `OUR_IA.md`, `ARCHITECTURE_BAKEND.md` ou
> `ARCHITECTURE_FRONTEND.md`. En cas de contradiction, ces documents restent la
> référence et l'écart doit être corrigé ou arbitré.

---

## 1. Objet et périmètre

Le Bloc 3 couvre le passage d'une **candidature retenue** à un **salarié créé**,
avec :

- la proposition d'embauche et ses versions ;
- la négociation avec le candidat : acceptation, refus, contre-proposition ;
- le lien public sécurisé par token ;
- la validation interne et la dérogation salariale Finance / Direction ;
- la création atomique du dossier salarié, du premier contrat, de l'affectation
  et de l'onboarding ;
- la génération du PDF du contrat ;
- le dépôt des documents du dossier salarié ;
- le front de gestion des propositions et du dossier d'embauche.

Modules concernés :

| Module | Rôle dans le Bloc 3 |
| --- | --- |
| `embauche` | Propriétaire de la proposition, du token et de l'orchestration de l'acceptation |
| `contrat` | Contrat, affectation, onboarding, PDF |
| `employe` | Dossier salarié et documents |
| `recrutement` | Lecture de la candidature retenue, mise à jour de son statut |
| `besoin` | Lecture du poste et de la ligne de besoin, mise à jour du nombre de pourvus |
| `iam` | Auteur des actions et contrôle des rôles |
| `shared` | Exceptions métier, références, sécurité courante |

---

## 2. Flux fonctionnel

```text
Candidature RETENUE
        |
        v
Proposition BROUILLON
        |  valider (RH / Finance / Direction si dérogation)
        v
Proposition VALIDEE_INTERNE
        |  envoyer  -> génère un token et une date d'expiration
        v
Proposition ENVOYEE
        |
        +--> ACCEPTEE       -> création salarié + contrat + affectation + onboarding
        |
        +--> REFUSEE        -> candidature REFUSEE_PAR_CANDIDAT
        |
        +--> CONTRE_PROPOSEE -> nouvelle version BROUILLON, ancienne RETIREE
        |
        +--> EXPIREE        -> job planifié ou tentative après échéance
        |
        +--> RETIREE        -> retrait par l'entreprise
```

L'acceptation est **idempotente** : si la proposition est déjà `ACCEPTEE`, le
service renvoie le résultat existant au lieu de recréer un salarié ou un contrat.

---

## 3. Code backend

### 3.1 Organisation des packages

```text
com.tovo.erp.embauche/
├── controller/   PropositionController, PropositionPubliqueController
├── dto/          PropositionDtos
├── model/        PropositionEmbauche, VersionProposition, ReponseCandidat,
│                 HistoriqueProposition, enums
├── repository/   PropositionEmbaucheRepository, VersionPropositionRepository,
│                 ReponseCandidatRepository, HistoriquePropositionRepository
└── service/      PropositionService, EmbaucheService, EmbaucheProperties

com.tovo.erp.contrat/
├── controller/   ContratController
├── dto/          ContratDtos
├── model/        Contrat, Affectation, Onboarding, OnboardingTache, enums
├── repository/   ContratRepository, AffectationRepository,
│                 OnboardingRepository, OnboardingTacheRepository
└── service/      ContratService, ContratPdfService

com.tovo.erp.employe/
├── controller/   EmployeController
├── dto/          DossierEmbaucheRequest, EmployeEmbaucheDto,
│                 DocumentEmbaucheRequest, DocumentEmbaucheDto,
│                 DocumentEmbaucheFichier
├── model/        DocumentEmploye, enums
├── repository/   DocumentEmployeRepository
└── service/      EmployeService
```

### 3.2 Classes principales

| Classe | Responsabilité |
| --- | --- |
| `PropositionService` | Cycle de vie de la proposition, versions, token, expiration, historique, réponse candidat, job d'expiration |
| `EmbaucheService` | Orchestration atomique de l'acceptation : employé, contrat, affectation, onboarding, candidature, besoin |
| `EmbaucheProperties` | Paramètres métier : validité, délai minimum, salaire minimum, fréquence du job |
| `ContratService` | Création du premier contrat, de l'affectation et de l'onboarding ; lecture des contrats |
| `ContratPdfService` | Génération d'un PDF 1.4 minimal sans dépendance externe |
| `EmployeService` | Création / réactivation du dossier `PRE_EMBAUCHE`, documents et calcul SHA-256 |
| `PropositionController` | API interne des propositions |
| `PropositionPubliqueController` | API publique par token, sans authentification |
| `ContratController` | Consultation des contrats, affectations, onboarding et téléchargement PDF |
| `EmployeController` | Dossier salarié et documents |

### 3.3 Endpoints

#### Propositions — usage interne

| Méthode | Chemin | Rôles autorisés | Usage |
| --- | --- | --- | --- |
| `POST` | `/api/embauche/propositions` | RH, DIRECTION | Créer un brouillon |
| `GET` | `/api/embauche/propositions?statut=` | RH, DIRECTION, MANAGER | Lister / filtrer |
| `GET` | `/api/embauche/propositions/{id}` | RH, DIRECTION, MANAGER | Détail complet |
| `PUT` | `/api/embauche/propositions/{id}` | RH, DIRECTION | Modifier un brouillon |
| `POST` | `/api/embauche/propositions/{id}/valider` | RH, FINANCE, DIRECTION | Validation interne |
| `POST` | `/api/embauche/propositions/{id}/envoyer` | RH, DIRECTION | Générer le token et envoyer |
| `POST` | `/api/embauche/propositions/{id}/retirer` | RH, DIRECTION | Retirer la proposition |
| `POST` | `/api/embauche/propositions/{id}/prolonger` | RH, DIRECTION | Prolonger l'expiration |
| `POST` | `/api/embauche/propositions/{id}/repondre` | RH, DIRECTION | Saisir une réponse reçue par email / papier |

#### Propositions — usage public

| Méthode | Chemin | Usage |
| --- | --- | --- |
| `GET` | `/api/embauche/propositions/token/{token}` | Consulter la proposition |
| `POST` | `/api/embauche/propositions/token/{token}/accepter` | Accepter |
| `POST` | `/api/embauche/propositions/token/{token}/refuser` | Refuser |
| `POST` | `/api/embauche/propositions/token/{token}/contre-proposer` | Faire une contre-proposition |

#### Contrats et dossier salarié

| Méthode | Chemin | Rôles autorisés | Usage |
| --- | --- | --- | --- |
| `GET` | `/api/contrats/{id}` | RH, DIRECTION, MANAGER | Détail du contrat |
| `GET` | `/api/contrats/employe/{idEmploye}` | RH, DIRECTION, MANAGER | Contrats d'un salarié |
| `GET` | `/api/contrats/employe/{idEmploye}/affectations` | RH, DIRECTION, MANAGER | Affectations |
| `GET` | `/api/contrats/employe/{idEmploye}/onboarding` | RH, DIRECTION, MANAGER | Onboarding et tâches |
| `GET` | `/api/contrats/{id}/document` | RH, DIRECTION | PDF du contrat |
| `GET` | `/api/employes/{id}/dossier` | RH, DIRECTION, MANAGER | Dossier d'embauche |
| `GET` | `/api/employes/{id}/documents` | RH, DIRECTION, MANAGER | Liste des documents |
| `POST` | `/api/employes/{id}/documents` | RH, DIRECTION | Ajouter un document |
| `GET` | `/api/employes/documents/{idDocument}` | RH, DIRECTION, MANAGER | Télécharger un document |

### 3.4 Modèle de données

Migration : `src/main/resources/db/migration/V4__bloc3_proposition_embauche.sql`.

| Table | Contenu |
| --- | --- |
| `proposition_embauche` | Conditions proposées, statut, token, dates, identifiants créés |
| `version_proposition` | Snapshot texte de chaque version de la proposition |
| `reponse_candidat` | Réponses du candidat, preuve, IP, empreinte |
| `historique_proposition` | Journal des transitions de statut et de leur auteur |
| `compteur_reference` | Compteur annuel des références `PE-...` et `CT-...` |
| `contrat` | Premier contrat, état `CREE`, salaire, dates, PDF |
| `affectation` | Poste, département, service, manager, lieu, dates |
| `onboarding` | Parcours d'intégration et progression |
| `onboarding_tache` | Checklist d'intégration et dépendances |
| `document_employe` | Documents binaires, hash SHA-256, validité, confidentialité |

La migration modifie aussi :

- `employe` : matricule nullable, identité, `id_candidat`, dates, statut ;
- `candidat` : CIN, date de naissance, statut ;
- `candidature` : nouveaux statuts d'embauche ;
- `demande_recrutement_poste` : `nombre_pourvus`.

### 3.5 Transitions autorisées

| Statut actuel | Action | Statut suivant |
| --- | --- | --- |
| `BROUILLON` | `valider` | `VALIDEE_INTERNE` |
| `BROUILLON` | `retirer` | `RETIREE` |
| `VALIDEE_INTERNE` | `envoyer` | `ENVOYEE` |
| `VALIDEE_INTERNE` | `retirer` | `RETIREE` |
| `ENVOYEE` | accepter | `ACCEPTEE` |
| `ENVOYEE` | refuser | `REFUSEE` |
| `ENVOYEE` | contre-proposer | `CONTRE_PROPOSEE` puis nouvelle version `BROUILLON` |
| `ENVOYEE` | échéance dépassée | `EXPIREE` |
| `ENVOYEE` | `retirer` | `RETIREE` |

Toute autre transition produit une `BusinessException` avec un code explicite.

### 3.6 Règles métier implémentées

- La candidature doit être `RETENUE` (ou déjà `ACCEPTEE`) pour créer ou accepter
  une proposition.
- Une seule proposition active est autorisée par candidature.
- La ligne de besoin doit encore avoir de la capacité (`nombrePourvus < effectif`).
- Le salaire doit être supérieur ou égal au minimum légal.
- Un salaire hors fourchette du poste exige une dérogation Finance / Direction ;
  seul un utilisateur Finance ou Direction peut valider cette dérogation.
- La date de début doit respecter un délai minimum configurable.
- Un CDD doit avoir une durée strictement positive.
- Le token public est unique, généré à l'envoi et expire après un délai
  configurable.
- L'acceptation vérifie l'expiration avant toute création.
- L'acceptation est idempotente et transactionnelle : aucune embauche partielle.
- Chaque changement de statut est historisé.
- Chaque modification de proposition crée une version texte conservée.
- Le dossier salarié est créé ou réactivé en `PRE_EMBAUCHE`.
- Le contrat est créé en `CREE`, avec PDF et hash de document.
- L'onboarding est généré avec une checklist standard.
- Le nombre de pourvus de la ligne de besoin est incrémenté.

### 3.7 Orchestration de l'acceptation

`EmbaucheService.accepter(...)` exécute, dans une seule transaction :

1. vérification du statut et de l'expiration ;
2. lecture de la candidature et de la ligne de besoin ;
3. création / réactivation du dossier `employe` ;
4. création du contrat, de l'affectation et de l'onboarding ;
5. génération et dépôt du PDF du contrat ;
6. incrémentation du besoin ;
7. passage de la candidature à `EMBAUCHEE` ;
8. passage de la proposition à `ACCEPTEE` ;
9. enregistrement de la réponse et de l'historique.

---

## 4. Frontend

Module : `frontend/src/modules/embauche/`.

### 4.1 Routes

| Route | Vue | Rôles |
| --- | --- | --- |
| `/embauche/propositions/token/:token` | `PropositionPubliqueView.vue` | Public |
| `/admin/embauche/propositions` | `PropositionListeView.vue` | RH, DIRECTION, MANAGER, FINANCE |
| `/admin/embauche/propositions/nouvelle` | `PropositionSaisieView.vue` | RH, DIRECTION |
| `/admin/embauche/propositions/:id/modifier` | `PropositionSaisieView.vue` | RH, DIRECTION |
| `/admin/embauche/propositions/:id` | `PropositionDetailView.vue` | RH, DIRECTION, MANAGER, FINANCE |
| `/admin/embauche/employes/:id` | `DossierEmbaucheView.vue` | RH, DIRECTION, MANAGER, FINANCE |

Toutes les vues sont chargées en lazy loading.

### 4.2 Fonctionnalités des vues

| Vue | Fonctionnalités |
| --- | --- |
| `PropositionListeView` | Liste paginée, filtre par statut, création, ouverture |
| `PropositionSaisieView` | Sélection d'une candidature retenue, poste, type de contrat, salaire, dates, manager, dérogation |
| `PropositionDetailView` | Actions du cycle de vie, lien public, historique, versions, réponses, contrat, onboarding, PDF |
| `PropositionPubliqueView` | Consultation par token, acceptation, refus, contre-proposition |
| `DossierEmbaucheView` | Dossier salarié, contrats, affectations, onboarding, documents, téléchargements |

### 4.3 Intégration partagée

- `router/index.js` agrège `embaucheRoutes`.
- `AppSidebar.vue` ajoute la section « Embauche » selon les rôles.
- `StatusBadge.vue` connaît les statuts du Bloc 3.
- `shared/api/axios.js` fournit `telecharger(...)` pour les PDF et documents.
- Le dossier salarié assemble les données de `employe` et de `contrat` côté
  front, conformément à la règle « pas d'appel backend `employe -> contrat` ».

---

## 5. Tests et vérifications

Commandes :

```bash
cd bakend && mvn test
cd frontend && npm run build
cd frontend && npm run lint
```

Tests ajoutés :

| Test | Couverture |
| --- | --- |
| `PropositionServiceTest` | Candidature non retenue, proposition active existante, salaire minimum, CDD sans durée, dérogation, envoi non validé, refus public |
| `EmbaucheServiceTest` | Idempotence, proposition non envoyée, expiration avant acceptation |
| `ContratPdfServiceTest` | Génération d'un PDF lisible et valide |

Résultat constaté : `27` tests backend passent. Le build et le lint front
passent également.

En test, H2 est utilisé avec `ddl-auto=create-drop`. En production, la
configuration par défaut est `ddl-auto=validate` : la migration Flyway doit donc
rester alignée avec les entités JPA.

---

## 6. Choix, conformité et justification

### 6.1 Choix conformes aux `.md`

| Choix | Pourquoi ce choix plutôt qu'une alternative |
| --- | --- |
| Orchestration dans `embauche` | Le `.md` désigne `embauche` comme module sommet du parcours ; il appelle `employe`, `contrat`, `besoin` et `recrutement` |
| Une seule transaction pour l'acceptation | Évite un salarié sans contrat ou un contrat sans onboarding |
| Services retournant des DTO | Respecte R2 ; les controllers ne manipulent pas les entités |
| `@Transactional` sur les services | Respecte R6 ; aucune transaction dans les controllers |
| Pas de nouvelle relation JPA entre `embauche` et `contrat` | Les liens sont des identifiants (`idEmploye`, `idContrat`), pas des `@ManyToOne` |
| Front en lazy loading | Respecte la règle de chargement des routes |
| Appels HTTP via l'instance Axios partagée | Respecte la centralisation du client HTTP |
| Dossier complet assemblé par le front | Évite un appel backend `employe -> contrat`, interdit par le `.md` |

### 6.2 Choix de conception assumés

| Sujet | Choix implémenté | Alternative écartée | Raison |
| --- | --- | --- | --- |
| Préfixe API | `/api` | `/api/v1` | `OUR_IA.md` harmonise explicitement sur `/api`, et le code existant utilise déjà `/api` |
| Validation interne | Statut `VALIDEE_INTERNE` | Passage direct `BROUILLON -> ENVOYEE` | Séparer la validation RH / Finance de l'envoi au candidat |
| Type de contrat de la proposition | Enum `TypeContratEmbauche` | Réutiliser `contrat.TypeContrat` | Éviter un couplage direct au modèle du module `contrat` ; la conversion est faite au moment de l'orchestration |
| Accès candidat | Token public | Compte candidat authentifié | Le candidat n'a pas de compte `iam` dans le périmètre ; le token sert de preuve d'accès |
| Token dans le détail RH | `tokenAcceptation` exposé | Token caché et renvoyé une seule fois | Permet à RH de copier / renvoyer le lien ; à reconsidérer si le token devient un secret sensible |
| Acceptation rejouée | Résultat idempotent | Erreur « déjà acceptée » | Un double clic ou un retry ne doit pas créer un second salarié |
| Statut du salarié | `PRE_EMBAUCHE` | `ACTIF` immédiatement | Le salarié n'est pas encore pleinement intégré avant la signature |
| Contrat | `CREE` avant signature | Contrat directement `SIGNE` | Le PDF est généré en attente de signature ; la signature est une étape ultérieure |
| PDF | Générateur PDF 1.4 maison | Bibliothèque PDF externe | Aucune dépendance supplémentaire ; suffisant pour un document lisible et archivable |
| Checklist onboarding | Modèle codé dans `ContratService` | Modèle paramétrable en base | Livraison rapide du parcours standard ; la paramétrabilité est reportée |
| DTO | Records regroupés dans `PropositionDtos` | Un fichier par DTO | Garder les objets du même domaine au même endroit |

### 6.3 Écarts connus par rapport aux `.md`

Ces points sont **non conformes** ou partiellement conformes. Ils sont documentés
pour être arbitrés et corrigés, pas pour être présentés comme validés.

| Écart | Ce qui est fait | Ce que le `.md` demande | Pourquoi ce choix a été fait | Correction recommandée |
| --- | --- | --- | --- | --- |
| Matricule | `null` jusqu'à la signature (docx `RG-B3-06` / `RG-B4-06`) | Matricule généré à l'acceptation (`OUR_IA`) | Suivre le cycle de vie du contrat : le matricule est considéré comme attribué à la signature | Arbitrer le conflit `.md` / docx, puis générer le matricule soit à l'acceptation, soit à la signature |
| Confidentialité | `DIRECTION` et `MANAGER` peuvent lire le dossier complet ; route front `FINANCE` | Seul RH lit le dossier complet ; les autres ont une vue minimale | Réutilisation du contrôle d'accès existant et besoin opérationnel de consultation | Restreindre le dossier complet à RH, créer une vue minimale, retirer `FINANCE` de la route front |
| Audit | Aucun journal d'audit pour les consultations / modifications | Chaque consultation et modification sensible doit être auditée | Le module `shared` ne fournit pas encore d'audit | Ajouter un service d'audit partagé et l'appeler sur les accès au dossier |
| Imports entre modules | `embauche` importe `contrat.model.TypeContrat` et `recrutement.model.CandidatureStatut` ; `contrat` importe `employe.model.TypeDocumentEmploye` | Un module ne peut utiliser que les `service` et `dto` des autres modules | Éviter de créer des DTO ou constantes supplémentaires pour des enums | Remplacer par des DTO / méthodes de service ou déplacer les enums partagés dans un module de base |
| Dépendance `employe -> besoin` | `Employe` a un lien vers `Poste` ; `EmployeService` utilise `PosteRepository` | `employe` ne dépend d'aucun module | Dette existante antérieure au Bloc 3 | Supprimer le lien direct et passer par `Affectation` / `contrat` |
| ArchUnit | Aucun test `ArchitectureTest` | Tests ArchUnit obligatoires et non désactivables | Le projet n'en avait pas ; l'ajouter aurait bloqué la CI sur les violations existantes | Ajouter ArchUnit, corriger les violations, puis l'intégrer à la CI |
| `@PreAuthorize` | Contrôle manuel via `UtilisateurCourant.exigerRole(...)` | `@PreAuthorize` sur chaque endpoint | Cohérence avec le code existant | Migrer les endpoints vers `@PreAuthorize` |
| Règles de succession des contrats | Seule la création du premier contrat est implémentée | CE ≤ 6 mois, CDD ≤ 2 ans, renouvellement encadré | Le Bloc 3 ne couvre que le premier contrat ; le renouvellement relève du Bloc 4 | Ajouter `CE` et les contrôles de durée / renouvellement dans `contrat` |
| Structure front | Pas de dossier `store/` ni `components/` dans `embauche` | Structure de module strictement identique | Le module n'a pas eu besoin d'état Pinia ni de composants réutilisables | Créer les dossiers et y déplacer les éléments quand le module grossit |
| API du module front | `embaucheApi.js` est un alias de l'instance Axios ; les vues appellent directement `api.get/post` | Fonctions d'appel dans `api/<module>Api.js` | Éviter des wrappers pour chaque endpoint | Créer des fonctions nommées (`creerProposition`, `envoyerProposition`, etc.) |
| Nommage DTO | `Creation`, `Refus`, `ContreProposition`, etc. | `<Action><Nom>Request` | Regrouper les DTO dans `PropositionDtos` | Renommer les records ou les séparer si le volume augmente |
| API RH distincte | Réutilisation de `/api/employes` | Préfixe d'API distinct pour l'écran / l'API RH | Intégration avec les endpoints existants | Ajouter un préfixe ou un contrôleur dédié au dossier RH |

### 6.4 Conflit documentaire à arbitrer

`ARCHITECTURE_BAKEND.md` indique `/api/v1/<ressource>`, tandis que
`OUR_IA.md` indique explicitement `/api` et que le code existant utilise `/api`.
Le code suit `OUR_IA.md`. Il faut mettre à jour `ARCHITECTURE_BAKEND.md` ou
revenir à `/api/v1` partout, mais pas laisser les deux versions coexister.

De même, le matricule est généré à l'acceptation dans `OUR_IA.md`, mais à la
signature dans le cahier détaillé. Le code suit le cahier détaillé. C'est un
arbitrage métier à valider formellement.

---

## 7. Plan de mise en conformité

### Priorité 1 — sécurité et règles métier

1. Restreindre le dossier complet au rôle RH.
2. Créer une vue minimale pour les autres rôles.
3. Ajouter l'audit des consultations et modifications.
4. Arbitrer le matricule : acceptation ou signature.
5. Ajouter les règles CE / CDD dans `contrat`.

### Priorité 2 — architecture

1. Supprimer les imports `model` entre modules.
2. Corriger la dépendance `employe -> besoin`.
3. Ajouter `ArchitectureTest` (ArchUnit).
4. Remplacer les contrôles manuels par `@PreAuthorize`.

### Priorité 3 — front et nommage

1. Créer les fonctions de l'API `embauche`.
2. Ajouter `store/` et `components/` si nécessaire.
3. Renommer les DTO selon `<Action><Nom>Request`.
4. Aligner la route front `FINANCE` avec les droits backend.

---

## 8. Limites fonctionnelles connues

- Pas d'envoi d'email réel : le lien token est copié manuellement par RH.
- Pas de signature électronique du contrat.
- Pas de gestion des renouvellements / avenants dans le Bloc 3.
- Pas de modèles d'onboarding configurables en base.
- Pas d'alertes de validité des documents dans ce bloc.
- Pas de création automatique du compte `iam` à l'onboarding.
- Paramètres `relanceJours` et `maxTentativesPdf` présents mais non utilisés.

---

## 9. Exploitation et intégration

Paramètres disponibles :

```properties
erp.embauche.validite-proposition-jours=7
erp.embauche.delai-minimum-jours=3
erp.embauche.relance-jours=3
erp.embauche.salaire-minimum-legal=500000
erp.embauche.max-tentatives-pdf=3
erp.embauche.verification-expiration-ms=3600000
erp.demo.bloc3.enabled=true
```

Données de démonstration :

- `Bloc3DemoDataInitializer` crée au démarrage 8 propositions couvrant tous les
  statuts du cycle de vie, avec candidats, candidatures, offres, demandes et
  validations associées.
- Le scénario `ACCEPTEE` crée aussi le dossier employé, le contrat et
  l'onboarding.
- Le seeder ne s'exécute que si aucune proposition n'existe et si les comptes
  de base (`rh@tovo.mg`, `manager@tovo.mg`, `finance@tovo.mg`,
  `direction@tovo.mg`) sont présents.
- Le désactiver avec `ERP_DEMO_BLOC3=false` ou
  `erp.demo.bloc3.enabled=false`.

Points d'attention :

- `V4__bloc3_proposition_embauche.sql` recrée les tables du Bloc 3 et supprime
  les tables d'esquisse `V3`. Elle suppose qu'aucune donnée métier Bloc 3
  exploitable n'existe déjà.
- En production, `ddl-auto=validate` : toute modification d'entité doit être
  accompagnée de la migration correspondante.
- Les tests backend utilisent H2 ; la migration réelle cible PostgreSQL.
- Le job d'expiration est activé par `@EnableScheduling` et s'exécute par défaut
  toutes les heures.
