# Bloc 4 — Contrat et onboarding

Ce bloc complète le cycle ouvert par l'acceptation d'une proposition d'embauche
(Bloc 3). Il applique la section 6 du cahier de conception SIRH.

## 1. Cycle de vie du contrat

```
CREE ──signature──> SIGNE ──validation RH──> VALIDE
  │                   │
  └──── annulation ───┴──── annulation avant début (rétractation)
```

- `CREE` : contrat généré à l'acceptation, PDF provisoire, aucun matricule.
- `SIGNE` : les deux dates de signature sont renseignées. Le matricule est
  attribué au salarié (RG-B4-06) et le parcours d'intégration est créé
  (RG-B4-08). Le PDF est régénéré puis figé.
- `VALIDE` : validation RH possible seulement si toutes les tâches bloquantes
  sont faites (RG-B4-09). Si la date de début est arrivée, le contrat passe en
  exécution `EN_COURS`.
- `ANNULE` : possible sur un contrat `CREE`, ou sur un contrat `SIGNE`
  uniquement avant la date de début (RG-B4-16, rétractation).

Les contrats successifs ne s'écrasent jamais. Le contrat courant est celui dont
l'exécution est `EN_COURS` (RG-B4-03). Les états d'exécution sont `A_VENIR`,
`EN_COURS`, `TERMINE`, `ROMPU`, `SUSPENDU`.

## 2. Signature et onboarding

La signature est **séparée** : le salarié signe depuis son compte candidat
(`POST /api/contrats/mes-contrats/{id}/signer`) et l'employeur depuis le module
RH (`POST /api/contrats/{id}/signer-employeur`). Chaque partie produit une date,
une empreinte SHA-256 horodatée et l'adresse IP de la demande. Le contrat ne
passe `SIGNE` que lorsque les **deux** signatures sont présentes. Cette
transition déclenche :

1. l'attribution du matricule au format `M` + année + séquence sur 4 chiffres
   (les séparateurs sont retirés, ex. `M20260042`) ;
2. la création de l'onboarding et de sa checklist (modèle choisi par type de
   contrat, catégorie de poste et site, avec repli `STANDARD`) ;
3. la génération du PDF signé.

Le contrat ne met volontairement pas à jour `proposition_embauche.id_onboarding_cree`
afin de ne pas créer de dépendance d'écriture `contrat` → `embauche`. Le lien
reste consultable dans l'autre sens via `contrat.id_proposition`.

La tâche `ADMIN_CONTRAT_SIGNE` est automatiquement marquée faite, puisque la
signature est précisément l'événement qui la réalise. Les autres tâches
bloquantes doivent être terminées avant la validation RH :

| Tâche | Responsable | Échéance | Bloquante |
|---|---|---|---|
| Contrat signé par les deux parties | RH | J-3 | oui |
| Dossier administratif complet | RH / Salarié | J+7 | oui |
| Visite médicale d'embauche | RH | J+15 | non |
| Inscription à la paie | Paie | J-2 | oui |
| Compte et messagerie | IT | J-2 | oui |
| Ordinateur, téléphone, fournitures | Services généraux / IT | J-1 | non |
| Badge, accès et pointage | Sécurité | J-1 | oui |
| Formation d'accueil et sécurité | RH / Manager | J+3 | non |
| Règlement intérieur et accusé | RH | J+1 | oui |
| Présentation d'équipe et parrain | Manager | J+1 | non |
| Point de fin de période d'essai | Manager | J+30 | non |

La progression est le pourcentage de tâches applicables terminées. Le parcours
passe `TERMINE` lorsque toutes les tâches obligatoires sont faites ou non
applicables, `BLOQUE` si une tâche est bloquée, `EN_COURS` s'il a démarré.

## 3. CDD, renouvellement et transformation

- Un CDD ou `CDD_RENOUVELLEMENT` doit avoir une date de fin.
- Le renouvellement crée un **nouveau** contrat `CDD_RENOUVELLEMENT` avec
  `id_contrat_precedent`, un rang incrémenté et une date de début égale au
  lendemain de la fin précédente (continuité, RG-B4-03).
- Le cumul est plafonné à 24 mois et à un renouvellement par défaut
  (RG-B4-12). Les valeurs sont configurables (`erp.contrat.*`).
- La transformation en CDI crée également un nouveau contrat, sans date de fin,
  en continuité du CDD.
- Une affectation de succession est créée en `PLANIFIEE` et devient principale
  à l'activation du nouveau contrat ; l'ancienne affectation est terminée.

## 4. Avenants

Un avenant ne modifie jamais le contrat d'origine. Il naît `CREE`, devient
`SIGNE` puis `VALIDE`. Les changements ne sont appliqués qu'à la **date
d'effet** : `date_application` est renseignée par le planificateur
(`AvenantService.appliquerEcheances`) lorsque cette date est atteinte. Les
valeurs avant/après sont conservées dans `anciennes_valeurs` et
`nouvelles_valeurs`, ainsi que dans les colonnes typées (`nouveau_salaire`,
`nouvelle_date_fin`, etc.).
Types supportés : `SALAIRE`, `POSTE`, `HORAIRES`, `LIEU`, `DUREE`, `AUTRE`.
Un avenant de type `POSTE` remplace l'affectation principale : l'ancienne est
close la veille de la date d'effet et une nouvelle affectation est créée avec
`nouveau_poste_id`, `nouveau_departement_id`, `nouveau_service_id` et
`nouveau_manager_id`.

## 5. Échéances et alertes

`ContratEcheanceJob` exécute périodiquement `ContratService.traiterEcheances` :

- activation des contrats validés dont la date de début est arrivée ;
- clôture des contrats dont la date de fin est dépassée ;
- passage du salarié en `NON_PRIS_POSTE` après le délai configurable
  (RG-B4-17, 3 jours par défaut).

`GET /api/contrats/alertes` calcule à la demande :

- contrats non signés à l'approche ou après la date de début ;
- contrats signés mais non validés alors qu'ils devraient démarrer ;
- fins de CDD à J-30 / J-15 / J-7 et CDD expirés ;
- fins de période d'essai à J-15 / J-7 (ou dépassées sans décision tracée) ;
- onboardings en retard par rapport à leur date cible.

Les alertes sont transformées en notifications internes idempotentes
(`GET /api/contrats/notifications`). Le planificateur génère aussi les relances
de tâches en retard, les escalades à J+3 et les alertes de tâches bloquantes.
Aucun envoi externe n'est réalisé : la table `notification` sert de boîte
d'envoi consultable et pourra être lue par un connecteur email/SMS.

La décision de fin de période d'essai (`POST /api/contrats/{id}/decision-fin-essai`)
est tracée sur le contrat (`CONFIRMATION` ou `RUPTURE`). Une rupture clôture le
contrat et l'affectation, puis désactive le salarié.

## 6. API REST

| Méthode | Chemin | Rôles | Usage |
|---|---|---|---|
| GET | `/api/contrats` | RH, Direction, Manager | Liste filtrable par `etat` |
| GET | `/api/contrats/actifs` | RH, Direction, Manager | Contrats en exécution |
| GET | `/api/contrats/alertes` | RH, Direction, Manager | Alertes calculées |
| GET | `/api/contrats/{id}` | RH, Direction, Manager | Détail |
| GET | `/api/contrats/employe/{id}` | RH, Direction, Manager | Historique du salarié |
| GET | `/api/contrats/employe/{id}/courant` | RH, Direction, Manager | Contrat courant |
| GET | `/api/contrats/employe/{id}/affectations` | RH, Direction, Manager | Affectations |
| GET | `/api/contrats/employe/{id}/onboarding` | RH, Direction, Manager | Parcours (null si absent) |
| GET | `/api/contrats/onboarding` | RH, Direction, Manager | Tous les parcours |
| PATCH | `/api/contrats/taches/{id}` | RH, Direction, Manager | Mise à jour d'une tâche |
| POST | `/api/contrats/{id}/signer` | RH, Direction | Signature employeur (compatibilité) |
| POST | `/api/contrats/{id}/signer-employeur` | RH, Direction | Signature employeur |
| POST | `/api/contrats/{id}/signer-salarie` | Candidat | Signature salarié (contrôle de propriété) |
| GET | `/api/contrats/mes-contrats` | Candidat | Contrats du candidat connecté |
| GET | `/api/contrats/mes-contrats/{id}` | Candidat | Détail d'un contrat du candidat |
| POST | `/api/contrats/mes-contrats/{id}/signer` | Candidat | Signature depuis l'espace candidat |
| POST | `/api/contrats/{id}/decision-fin-essai` | RH, Direction | Décision de fin d'essai |
| GET | `/api/contrats/notifications` | RH, Direction, Manager | Notifications internes |
| PATCH | `/api/contrats/notifications/{id}/lire` | RH, Direction, Manager | Marquer une notification lue |
| POST | `/api/contrats/{id}/valider` | RH, Direction | Validation RH |
| POST | `/api/contrats/{id}/annuler` | RH, Direction | Annulation / rétractation |
| POST | `/api/contrats/{id}/renouveler` | RH, Direction | Renouvellement CDD |
| POST | `/api/contrats/{id}/transformer-cdi` | RH, Direction | Transformation CDI |
| POST | `/api/contrats/{id}/avenants` | RH, Direction | Créer un avenant |
| POST | `/api/contrats/avenants/{id}/signer` | RH, Direction | Signer un avenant |
| POST | `/api/contrats/avenants/{id}/valider` | RH, Direction | Valider un avenant |
| POST | `/api/contrats/employe/{id}/affectations` | RH, Direction, Manager | Nouvelle affectation |
| POST | `/api/contrats/echeances/traiter` | RH, Direction | Forcer un passage |
| GET | `/api/contrats/{id}/document` | RH, Direction | PDF du contrat |

## 7. Frontend

Nouveau module `frontend/src/modules/contrat` :

- `/admin/contrats` : liste, filtre par état, alertes graves ;
- `/admin/contrats/onboarding` : suivi des parcours et retards ;
- `/admin/contrats/:id` : détail, signature employeur, validation, annulation,
  décision de fin d'essai, avenants, notifications, renouvellement,
  transformation CDI et checklist d'intégration.

L'espace candidat (`EspaceCandidatView`) expose « Mes contrats » et permet au
salarié de signer son contrat avec son nom de signataire. Le dossier d'embauche
existant reste accessible et tolère désormais l'absence d'onboarding (il
apparaît à la signature).

## 8. Conformité architecture

Le module `contrat` n'accède plus aux repositories ou modèles des modules
`recrutement`, `embauche` et `employe` :

- `ContratService` utilise `PosteService`/`PosteDto` et les méthodes DTO de
  `EmployeService` ;
- les contrats du candidat sont orchestrés par
  `embauche.service.ContratCandidatService` ;
- les vérifications de rôle sont portées par `@PreAuthorize` ;
- les listes sont chargées par requêtes batch (`findByIdContratIn`,
  `findByIdInAndStatut`) pour éviter les N+1 inter-modules.

Points restants hors périmètre Bloc 4 : les relations JPA inter-modules
existantes (ex. `Employe.poste`) et l'accès à `iam.model.Utilisateur` pour
l'identité de l'utilisateur connecté.

## 9. Vérifications

```bash
cd bakend && ./mvnw test
cd frontend && npm run build && npm run lint
```
