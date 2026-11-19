# Répartition des tâches — Finitions des blocs 8 à 12 (ERP SIRH Tovo)

## 1. Présentation du projet

### Objectif

Organiser le travail restant sur les blocs **8, 9, 10, 11 et 12** afin de corriger les écarts avec le cahier des charges, compléter les tests et préparer une recette propre avant de considérer ces blocs comme terminés.

Chaque bloc a déjà une base commitée. Il ne s’agit pas de tout refaire, mais de **compléter, corriger et fiabiliser** ce qui existe.

### Technologies

| Partie | Technologie |
|---|---|
| Backend | Java 21, Spring Boot, Spring Data JPA, Flyway, PostgreSQL |
| Frontend | Vue 3, PrimeVue, Tailwind CSS, JavaScript |
| Sécurité | JWT, `@PreAuthorize`, rôles `RH`, `DIRECTION`, `MANAGER`, `FINANCE`, `EMPLOYE` |
| Tests | JUnit, Mockito, ArchUnit, Maven |
| Architecture | Monolithe modulaire ; un module ne consomme que les `service`/`dto` des autres modules |

### Architecture générale

| Bloc | Domaine | Module principal |
|---|---|---|
| 8 | Paie et fiche de paie | `paie` |
| 9 | Évaluation, compétences et formation | `evaluation` |
| 10 | Carrière et mobilité | `carriere` |
| 11 | Discipline | `discipline` |
| 12 | Rupture et offboarding | `rupture` |

### État actuel des commits

| Bloc | Commits déjà faits |
|---|---|
| 8 | `9b8f498` |
| 9 | `48922d8` |
| 10 | `6e0a21f`, `0afd459`, `8d9d832`, `c6e0102`, `430a69a`, `2a2ea96`, `153a3c4`, `b960c49`, `c6a1ec1`, `d6abcf4`, `85fc675`, `0d55ec5`, `76a3662` |
| 11 | `215d068`, `3babf6c` |
| 12 | `68cabcc`, `bc2a209`, `76f4f6f`, `a6115d9` |

> Les quatre commits bloc 12 viennent d’être ajoutés : capacité des postes, socle rupture, solde de tout compte et pilotage de l’offboarding.

---

## 2. Dev 1 — Bloc 8 : Paie et fiche de paie

### Objectif

Rendre le moteur de paie réellement reproductible, conforme au cahier et auditable, y compris le bulletin `SOLDE_TOUT_COMPTE` utilisé par le bloc 12.

### A. Backend à améliorer

- [ ] Persister un **instantané réel des variables** lors de la consolidation ; aujourd’hui `consolider(...)` ne change que le statut et la date.
- [ ] Faire relire le calcul à cet instantané au lieu des données courantes des autres modules (`RG-B8-05`).
- [ ] Corriger `CarriereService.salairesBaseParIds(...)` : lire l’historique **bi-temporel**, pas seulement `date_fin IS NULL`.
- [ ] Appliquer le prorata quand le salaire change en cours de mois.
- [ ] Élargir la population de calcul : actifs, `EN_PREAVIS`, entrées et sorties dans le mois.
- [ ] Ajouter le champ établissement à `PeriodePaie` si nécessaire.
- [ ] Renforcer `valider(...)` :
  - [ ] écart > 20 % ;
  - [ ] salaire minimum ;
  - [ ] compte de paiement renseigné ;
  - [ ] actif sans bulletin ;
  - [ ] salarié sorti sans solde.
- [ ] Corriger l’ajustement manuel : `enregistrerRetenue(...)` utilise aujourd’hui le champ `avance` alors que `ajuster(...)` force `RetenueManuelle` à zéro.
- [ ] Ajouter l’endpoint `POST /bulletins/{id}/rectifier`.
- [ ] Ajouter le suivi virement : statut `REJETE`, incident, fichier de virement et export comptable.
- [ ] Séparer clairement les rôles calcul / validation / paiement.

### B. API à aligner

| Méthode | Route actuelle | Route cible du cahier |
|---|---|---|
| POST | `/periodes/calcul?mois=...` | `/periodes/{id}/calculer` |
| POST | `/periodes/consolider?mois=...` | `/periodes/{id}/consolider` |
| POST | `/periodes/valider?mois=...` | `/periodes/{id}/valider` |
| GET | déjà présent | `/bulletins/{id}/lignes` |
| POST | à ajouter | `/bulletins/{id}/rectifier` |

### C. Fichiers concernés

- `bakend/src/main/java/com/tovo/erp/paie/service/BulletinPaieService.java`
- `bakend/src/main/java/com/tovo/erp/paie/controller/BulletinPaieController.java`
- `bakend/src/main/java/com/tovo/erp/paie/controller/PaieController.java`
- `bakend/src/main/java/com/tovo/erp/paie/dto/BulletinPaieDtos.java`
- `bakend/src/main/java/com/tovo/erp/paie/model/PeriodePaie.java`
- `bakend/src/main/java/com/tovo/erp/contrat/service/CarriereService.java`
- `bakend/src/main/resources/db/migration/V31__bloc8_moteur_regles_fiche_paie.sql`
- Nouvelle migration `V38__...` pour l’instantané des variables
- `bakend/src/test/java/com/tovo/erp/paie/service/BulletinPaieServiceTest.java`
- `frontend/src/modules/paie/views/PaieView.vue`

### Résultat attendu

Le spécimen du cahier donne toujours **brut 1 242 000 Ar**, **IRSA 146 932 Ar**, **net 970 228 Ar**, **coût employeur 1 465 560 Ar**, et un recalcul après modification des données sources redonne exactement le même résultat.

---

## 3. Dev 2 — Bloc 9 : Évaluation, compétences et formation

### Objectif

Compléter les règles de confidentialité, les alertes et les tests d’acceptation du bloc 9.

### A. Backend à améliorer

- [ ] Ajouter le test exact du cahier : **4,25 / 4,04 / 4,14** ; le test actuel utilise `5.00 / 5.00 / 4.00 / 4.80`.
- [ ] Journaliser toute consultation d’évaluation dans `JournalAuditService`.
- [ ] Vérifier le conflit « proche déclaré » entre évalué et évaluateur.
- [ ] Ajouter les jobs et relances :
  - [ ] `J-60` et `J-30` avant clôture de campagne ;
  - [ ] relance hebdomadaire ;
  - [ ] alerte manager `J+90` après entretien.
- [ ] Ajouter un seuil de coût formation déclenchant la validation `DIRECTION` dans `verifierBudget(...)`.
- [ ] Aligner les routes du cahier :
  - [ ] `POST /campagnes` ;
  - [ ] `POST /objectifs` ;
  - [ ] `POST /evaluations/{id}/avancer` ;
  - [ ] `GET /employes/{id}/ecarts` ;
  - [ ] `POST /besoins-formation` ;
  - [ ] `POST /sessions/{id}/inscriptions`.

### B. Frontend à améliorer

- [ ] Afficher clairement les écarts de compétences générés à la clôture.
- [ ] Afficher l’état des inscriptions liste d’attente / confirmée.
- [ ] Empêcher l’affichage des notes d’un collaborateur non autorisé.

### C. Fichiers concernés

- `bakend/src/main/java/com/tovo/erp/evaluation/service/EvaluationService.java`
- `bakend/src/main/java/com/tovo/erp/evaluation/service/CampagneService.java`
- `bakend/src/main/java/com/tovo/erp/evaluation/service/FormationService.java`
- `bakend/src/main/java/com/tovo/erp/evaluation/controller/EvaluationController.java`
- `bakend/src/main/java/com/tovo/erp/evaluation/controller/CampagneController.java`
- `bakend/src/main/java/com/tovo/erp/evaluation/controller/FormationController.java`
- `bakend/src/test/java/com/tovo/erp/evaluation/service/EvaluationServiceTest.java`
- `bakend/src/test/java/com/tovo/erp/evaluation/service/FormationServiceTest.java`
- `frontend/src/modules/evaluation/views/EvaluationView.vue`
- `frontend/src/modules/evaluation/views/FormationView.vue`

### Résultat attendu

Les calculs du cahier sont prouvés par test, les consultations sensibles sont auditées et les campagnes ne peuvent plus rester oubliées sans relance.

---

## 4. Dev 3 — Bloc 10 : Carrière et mobilité

### Objectif

Finaliser les derniers ajustements du bloc 10 et maintenir la documentation livrée.

### A. Backend à corriger

- [ ] Remplacer le cron du contrôle d’intégrité dans `MouvementCarriereJob.java` :
  - actuel : `0 30 2 * * *` ;
  - cible cahier : `0 30 3 * * *`.
- [ ] Ajouter un test `rapportIntegriteSignaleUnEcartDeSalaireBulletin`.
- [ ] Vérifier l’idempotence des consommateurs d’événements si un mouvement est réappliqué.
- [ ] Garder `ArchitectureTest` vert après chaque ajout de listener.

### B. Documentation et recette

- [ ] Mettre à jour `BLOC10_PLAN_EXECUTION.md` avec le bon horaire `03:30`.
- [ ] Exécuter la batterie complète bloc 10 :
  - `./mvnw -q -Dtest=CarriereServiceTest,MouvementCarriereServiceTest,ArchitectureTest test`
- [ ] Vérifier dans l’UI que :
  - [ ] le manager ne voit ni salaire ni sanction ;
  - [ ] le RH voit la frise complète ;
  - [ ] l’étape courante du circuit s’affiche.

### C. Fichiers concernés

- `bakend/src/main/java/com/tovo/erp/carriere/service/MouvementCarriereJob.java`
- `bakend/src/test/java/com/tovo/erp/carriere/service/MouvementCarriereServiceTest.java`
- `BLOC10_PLAN_EXECUTION.md`
- `frontend/src/modules/carriere/views/CarriereView.vue`

### Résultat attendu

Le bloc 10 est totalement conforme au cahier, notamment le contrôle nocturne à **03:30**, et le MD de répartition bloc 10 ne contient plus d’information obsolète.

---

## 5. Dev 4 — Bloc 11 : Discipline

### Objectif

Durcir les règles de prescription, les alertes et la confidentialité du dossier disciplinaire.

### A. Backend à améliorer

- [ ] Remplacer la prescription codée en dur à **2 mois** par un paramètre configurable, valeur cible **90 jours**.
- [ ] Exiger explicitement le rôle `DIRECTION` pour toute dérogation de prescription.
- [ ] Ajouter :
  - [ ] alerte `J-15` avant prescription ;
  - [ ] job de délai de réponse à la demande d’explication ;
  - [ ] revue mensuelle du journal d’accès.
- [ ] Ajouter des générateurs de lettres :
  - [ ] demande d’explication ;
  - [ ] convocation ;
  - [ ] notification de sanction.
- [ ] Vérifier la confidentialité :
  - [ ] chiffrement au repos des pièces ;
  - [ ] filigrane sur les exports ;
  - [ ] masquage dans les recherches générales ;
  - [ ] contrôle de volume anormal de consultations.

### B. Frontend à améliorer

- [ ] Afficher la date de prescription restante.
- [ ] Afficher le statut de remise de la demande d’explication.
- [ ] Masquer les sanctions sensibles selon le rôle connecté.

### C. Fichiers concernés

- `bakend/src/main/java/com/tovo/erp/discipline/service/IncidentService.java`
- `bakend/src/main/java/com/tovo/erp/discipline/service/ProcedureService.java`
- `bakend/src/main/java/com/tovo/erp/discipline/service/SanctionService.java`
- `bakend/src/main/java/com/tovo/erp/discipline/service/PieceService.java`
- `bakend/src/main/java/com/tovo/erp/discipline/service/JournalDisciplineService.java`
- `bakend/src/main/resources/db/migration/V34__bloc11_discipline.sql`
- `bakend/src/test/java/com/tovo/erp/discipline/service/DisciplineServiceTest.java`
- `frontend/src/modules/discipline/views/DisciplineView.vue`

### Résultat attendu

Le dossier disciplinaire est conforme, la prescription est configurable et les accès sensibles sont tracés et visibles.

---

## 6. Dev 5 — Bloc 12 : Rupture et offboarding

### Objectif

Corriger les écarts bloquants du bloc 12 fraîchement commité et harmoniser les contrats d’API avec le cahier.

### A. Backend à corriger

- [ ] Ajouter les routes conformes au cahier tout en gardant les anciennes comme alias temporaires :

| Méthode | Route cahier | Route actuelle |
|---|---|---|
| POST | `/ruptures` | `/api/rupture/dossiers` |
| POST | `/ruptures/{id}/notifier` | `/api/rupture/dossiers/{id}/notifier` |
| POST | `/ruptures/{id}/arreter-droits` | `/api/rupture/dossiers/{id}/arreter-droits` |
| POST | `/ruptures/{id}/cloturer` | `/api/rupture/dossiers/{id}/cloturer` |
| GET | `/ruptures/{id}/solde` | `/api/rupture/dossiers/{id}/solde` |

- [ ] Exiger une pièce justificative et une date de demande distincte à la création.
- [ ] Pour `DEMISSION`, exiger la lettre du salarié.
- [ ] Vérifier les étapes obligatoires des licenciements économique, inaptitude et insuffisance professionnelle.
- [ ] Interdire `notifier(...)` tant que le dossier n’est pas `VALIDE`.
- [ ] Calculer le préavis selon catégorie, ancienneté et type de contrat ; ne plus écraser `dateSortieSouhaitee`.
- [ ] Remplacer `primesDues = 0` et `avancesRestantes = 0` par les valeurs réelles.
- [ ] Ajouter un endpoint pour passer le matériel en `RESTITUE`, `PERDU` ou `RETENU`.
- [ ] Ajouter les états matériel `CONFORME`, `DEGRADE`, `MANQUANT`.
- [ ] Ne pas générer de bulletin avec un net négatif : créer une créance suivie séparément.
- [ ] Déduire les cotisations du solde ou documenter explicitement le montant `netAvantCotisations`.
- [ ] Aligner les statuts solde : `BROUILLON`, `VALIDE`, `PAYE`, `SIGNE`.
- [ ] Vérifier `clauseDedit` et la preuve d’accord écrit avant d’appliquer le dédit formation.
- [ ] Remplacer les documents `.txt` par des gabarits PDF signés avec hash.
- [ ] Ajouter attestations sociales et chômage.
- [ ] Enregistrer la confirmation IT horodatée par accès et utiliser `accesCoffreMois`.
- [ ] Archiver mensuellement, sur la base de `CLOTUREE + durée légale depuis la sortie`, puis anonymiser.
- [ ] Ajouter les jobs :
  - [ ] `FIN_CDD` à `J-30` ;
  - [ ] dossier `RETRAITE` à 12 mois ;
  - [ ] rupture automatique `DECES`.
- [ ] Remplacer les retours `Map<String, Object>` par des DTOs.
- [ ] Ne plus exposer d’entités JPA dans les méthodes publiques des services.

### B. Frontend à corriger

- [ ] Corriger les payloads :
  - [ ] `satisfaction` doit être un entier, pas `SATISFAIT`/`NEUTRE`/`INSATISFAIT` ;
  - [ ] `preuveRemise` → `referencePreuve` ;
  - [ ] `preuve` → `commentaire` ;
  - [ ] colonnes documents `type`/`gabaritVersion` → `code`/`hashSha256`.
- [ ] Ajouter l’UI de gestion du matériel : restitué, perdu, retenu.
- [ ] Afficher les créances quand le solde est négatif.

### C. Fichiers concernés

- `bakend/src/main/java/com/tovo/erp/rupture/controller/RuptureController.java`
- `bakend/src/main/java/com/tovo/erp/rupture/service/DossierRuptureService.java`
- `bakend/src/main/java/com/tovo/erp/rupture/service/SoldeRuptureService.java`
- `bakend/src/main/java/com/tovo/erp/rupture/service/RuptureArchiveJob.java`
- `bakend/src/main/java/com/tovo/erp/rupture/dto/RuptureDtos.java`
- `bakend/src/main/java/com/tovo/erp/paie/service/BulletinPaieService.java`
- `bakend/src/main/resources/db/migration/V35__bloc12_rupture.sql`
- `bakend/src/main/resources/db/migration/V36__bulletin_solde_tout_compte.sql`
- `bakend/src/test/java/com/tovo/erp/rupture/service/DossierRuptureServiceTest.java`
- `bakend/src/test/java/com/tovo/erp/rupture/service/SoldeRuptureServiceTest.java`
- `frontend/src/modules/rupture/views/RuptureView.vue`
- `frontend/src/modules/rupture/api/ruptureApi.js`

### Résultat attendu

Le dossier de rupture est complet, le solde ne peut pas produire de bulletin négatif, l’offboarding est tracé et les payloads frontend/backend correspondent exactement.

---

## 7. Tâches communes aux cinq personnes

Ces éléments sont à vérifier une seule fois et réutilisés par tous.

### À vérifier avant le développement

- [x] Dépôt GitHub présent.
- [x] Arborescence backend/frontend en place.
- [x] Routage Vue et contrôleurs Spring en place.
- [x] Réponses JSON uniformisées côté backend et appels centralisés côté frontend via `axios`.
- [x] Fonctions JavaScript communes présentes.
- [x] Styles communs présents.
- [ ] Ajouter officiellement le module `rupture` dans `OUR_IA.md`.
- [ ] Vérifier que les cinq développeurs peuvent lancer backend + frontend + Flyway.

### Fichiers communs à ne modifier qu’après concertation

| Fichier | Rôle |
|---|---|
| `OUR_IA.md` | Règles d’architecture et conventions |
| `bakend/src/test/java/com/tovo/erp/ArchitectureTest.java` | Liste des modules et interdictions |
| `bakend/src/main/resources/application.properties` | Paramètres et crons |
| `frontend/src/router/index.js` | Routes globales |
| `frontend/src/shared/components/AppSidebar.vue` | Menu global |
| `frontend/src/shared/api/axios.js` | Appels API communs |

**Important :** aucun développeur ne doit recréer individuellement un système de réponse, de routage ou de style déjà présent.

---

## 8. Règles communes de collaboration

- [ ] Chaque développeur travaille principalement sur son bloc.
- [ ] Un commit = une fonctionnalité ou une correction cohérente.
- [ ] Ne jamais committer les documents `.docx` ou `fiche-paie.jpeg` présents à la racine.
- [ ] Ne jamais dépendre d’un `model` ou `repository` externe à son module.
- [ ] Toute lecture sensible doit être auditée : salaire, sanction, dossier disciplinaire, solde, rupture.
- [ ] Toute modification de schéma passe par une migration Flyway : pas de modification manuelle de la base.
- [ ] Flyway est configuré avec `spring.flyway.out-of-order=true` car `V35`/`V36` ont été ajoutés après `V37`.
- [ ] Chaque développeur teste son bloc avant l’intégration.
- [ ] Les cinq développeurs réalisent ensemble la recette finale.

### Attention aux opérations sensibles

| Opération | Signification |
|---|---|
| Rectifier un bulletin | Corriger un bulletin calculé sans changer l’historique des règles |
| Désactiver un accès | Empêcher la connexion sans supprimer le dossier |
| Archiver une rupture | Conserver les données légales puis anonymiser |
| Clôturer une rupture | Terminer le contrat, le solde, les documents et l’offboarding |

---

## 9. Critères de réussite globaux

- [ ] Les modules métier concernés par les blocs 8 à 12 sont verts.
- [ ] `ArchitectureTest` passe sans nouvelle violation.
- [ ] Les exemples exacts des cahiers bloc 8, bloc 9 et bloc 12 sont couverts par des tests.
- [ ] Les jobs et crons respectent les horaires du cahier.
- [ ] Les API sensibles sont protégées par rôle et auditées.
- [ ] Le frontend ne casse pas les payloads backend.
- [ ] `npm run build` passe.
- [ ] La batterie Maven complète passe.
- [ ] Ce MD est mis à jour après chaque finition.

## 10. Définition de terminé

Un bloc est terminé seulement si :

1. les écarts listés pour ce bloc sont tous cochés ;
2. les tests du bloc passent ;
3. le build frontend passe si le bloc a une UI ;
4. aucun commit ne mélange deux blocs ;
5. la documentation de répartition est à jour.
