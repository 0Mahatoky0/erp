# ARCHITECTURE_MODULES.md — Découpage fonctionnel de l'ERP RH

> Complément de `ARCHITECTURE.md` (règles de code). Ce document décrit **quoi est dans quel module, qui possède quelle donnée, qui appelle qui, et comment les processus traversent les modules**. Il ne contient aucun code.
> Pour les développeurs et les agents IA : en cas de doute sur « où mettre cette fonctionnalité », ce document fait foi.

---

## 1. Correspondance blocs → modules

| Bloc | Module (package) | Responsabilité en une phrase |
|---|---|---|
| 1. Expression du besoin | `besoin` | Référentiels organisation (départements, postes, sites) et demande de recrutement validée par workflow |
| 2. Recrutement | `recrutement` | Offre d'emploi, candidatures, présélection, entretiens, évaluations, décision |
| 3. Sélection et embauche | `embauche` | Proposition d'embauche, négociation avec le candidat, déclenchement de la création du salarié |
| 4. Contrat et onboarding | `contrat` | Contrats successifs, affectations, avenants, checklist d'intégration |
| 5. Dossier administratif | `employe` | Identité, coordonnées, matricule, documents et leur validité, missions, alertes, confidentialité |
| 6. Temps de travail et pointage | `temps` | Régimes, horaires, plannings, rôles de planning, pointage, anomalies, temps réellement travaillé |
| 7. Congés et absences | `conges` | Droits, soldes, demandes, contrôles, planning de congés, absences et justificatifs |
| Transverse | `iam` | Utilisateurs, rôles, permissions |
| Transverse | `shared` | Technique : sécurité, audit, exceptions, configuration, paramètres |

Il n'y a **pas de module paie** dans le périmètre. Les modules `temps`, `conges` et `contrat` exposent les données dont une future paie aura besoin (voir §9).

---

## 2. Graphe de dépendances

Notation : **A → B** signifie « A appelle les services de B ». Un module n'appelle que des modules situés plus bas.

```
Niveau 4 (haut)   conges
                    ├──→ temps, contrat, employe, besoin
Niveau 3          temps                       embauche
                    ├──→ contrat, employe,      ├──→ recrutement, besoin,
                    │    besoin                 │    employe, contrat
Niveau 2          contrat                     recrutement
                    ├──→ employe, besoin, iam   └──→ besoin
Niveau 1          employe          besoin
                    (aucune          └──→ iam
                     dépendance)
Niveau 0 (base)   iam            shared (utilisable par tous)
```

Lecture par module :

| Module | Appelle | Est appelé par |
|---|---|---|
| `iam` | — | besoin, contrat |
| `besoin` | iam | recrutement, embauche, contrat, temps, conges |
| `employe` | — | embauche, contrat, temps, conges |
| `recrutement` | besoin | embauche |
| `contrat` | employe, besoin, iam | embauche, temps, conges |
| `embauche` | recrutement, besoin, employe, contrat | — (sommet du parcours d'embauche) |
| `temps` | contrat, employe, besoin | conges |
| `conges` | temps, contrat, employe, besoin | — (sommet) |

**Pourquoi ce sens :**
- Un module de base (`employe`, `besoin`, `iam`) ne connaît pas ceux qui l'utilisent.
- Un processus qui traverse plusieurs modules est **orchestré par le module le plus haut impliqué** (embauche pour l'embauche, conges pour les congés).
- Aucune notification inversée : le module du dessus **lit à la demande** ce dont il a besoin (voir §6.2).

Toute nouvelle dépendance doit être ajoutée ici et justifiée en pull request. Le test d'architecture interdit les cycles.

---

## 3. Propriétaire de chaque donnée (une seule source de vérité)

| Objet métier | Module propriétaire | Remarques |
|---|---|---|
| Departement, Poste, Site | `besoin` | Un **Poste existe indépendamment de l'employé** (jamais de lien direct employé ↔ poste hors affectation) |
| DemandeRecrutement, Validation (étapes du workflow) | `besoin` | |
| OffreEmploi, Candidat, Candidature, Entretien, EvaluationCandidat | `recrutement` | Le Candidat n'est **pas** un Employé |
| PropositionEmbauche, VersionProposition, ReponseCandidat | `embauche` | |
| Employe, matricule, identité, coordonnées | `employe` | Créé à l'acceptation de l'embauche |
| DocumentAdministratif (et validité), Mission, Alerte | `employe` | Y compris le contrat signé (pièce jointe) |
| Contrat, Avenant/Renouvellement, Affectation, Salaire (par période) | `contrat` | **Historique conservé, jamais écrasé** |
| Onboarding, OnboardingTache | `contrat` | Voir §4.4 |
| RegimeTravail, Horaire, PlanningTravail, RolePlanning, Suppleance, CalendrierFerie | `temps` | |
| Pointage, AnomaliePointage, TempsTravaille, HeureSupplementaire | `temps` | |
| TypeConge, DroitConge, SoldeConge, DemandeConge, Absence, Justificatif, PlanningConge | `conges` | |
| Utilisateur, Role, Permission | `iam` | Lié à l'employé par un identifiant, pas par une relation JPA |

**Historique de carrière** : pas de table dédiée. C'est une lecture ordonnée des contrats, avenants, affectations et changements de salaire du module `contrat`.

---

## 4. Fiches des modules

### 4.1 `besoin` — Expression du besoin en personnel

**Rôle :** un manager exprime un besoin (création ou remplacement d'un poste). La demande suit un workflow de validation, puis le recrutement est ouvert.

**Entités :** Departement, Poste, Site (référentiels) ; DemandeRecrutement ; Validation (une ligne par étape : étape, validateur, décision, commentaire, date).

**Contenu d'une demande :** poste, département, effectif demandé, motif (création / remplacement, avec la personne remplacée si applicable), budget, date souhaitée.

**Workflow (machine à états) :**
`Brouillon → Soumise (manager) → Revue RH → Validation Finance → Validation Direction → Revue finale RH → Recrutement ouvert`
Un refus à n'importe quelle étape renvoie la demande au manager (statut « refusée » ou « à corriger », avec commentaire obligatoire). Les états finaux : « recrutement ouvert », « annulée », « clôturée (pourvue) ».

**Services publics :** consulter une demande, lister les demandes validées prêtes à l'ouverture, consulter/lister départements, postes, sites, clôturer une demande (appelé par `embauche` quand le poste est pourvu).

**Règles :**
- Qui valide chaque étape est déterminé par les **rôles `iam`** (manager du département, RH, finance, direction).
- Le poste est un référentiel : on peut le créer, le geler ou le supprimer sans toucher à un employé.
- Le « budget » est une information saisie et contrôlée par la finance ; aucun calcul de masse salariale (pas de module paie).

---

### 4.2 `recrutement` — Du besoin validé aux candidatures

**Rôle :** transformer une demande validée en offre, publier, recevoir des candidatures, présélectionner, organiser entretiens/tests, conserver les évaluations et la décision.

**Entités :** OffreEmploi, Candidat, Candidature, Entretien, EvaluationCandidat (+ critères de sélection).

**Cycle de vie d'une candidature :**
`Reçue → Présélectionnée / Écartée → Entretien(s) / Test(s) → Évaluée → Retenue / Refusée`
La **décision finale** est conservée avec son auteur, sa date et son motif.

**Mode de sélection** (choisi par offre) : trois niveaux possibles, du plus simple au plus avancé — sélection manuelle par RH, grille de notation à critères pondérés, filtres éliminatoires (questions bloquantes). Démarrer par manuel + grille ; le champ « mode de sélection » reste sur l'offre pour évoluer.

**Services publics :** créer une offre depuis une demande validée, consulter une offre, consulter une candidature (et le candidat retenu), marquer une candidature comme « retenue ».

**Règles :**
- Une offre référence l'identifiant de la demande (`besoin`), pas l'entité.
- Un candidat peut postuler à plusieurs offres ; une candidature appartient à une seule offre.
- Les données des candidats non retenus ont une durée de conservation à définir (confidentialité).

---

### 4.3 `embauche` — Proposition, négociation, déclenchement de l'embauche

**Rôle :** le candidat retenu reçoit une proposition d'embauche ; il l'accepte, la refuse ou demande un ajustement ; une fois tout validé, le salarié est créé.

**Entités :** PropositionEmbauche, VersionProposition (chaque ajustement crée une **nouvelle version**, les anciennes sont conservées), ReponseCandidat.

**Contenu d'une proposition :** poste, département, site/lieu de travail, type de contrat et durée, période d'essai, date de début, rémunération (montant, périodicité, avantages), horaires et durée de travail, supérieur hiérarchique, congés, délai de réponse, pièces à fournir (voir §11).

**Workflow :**
`Brouillon → Envoyée → [Acceptée | Refusée | Ajustement demandé]`
- *Ajustement demandé* : RH étudie, crée une nouvelle version, renvoie → boucle jusqu'à acceptation ou refus.
- *Acceptée* → **validation finale** (pièces fournies, accord final du candidat sur la dernière version) → *Embauche confirmée*.

**Orchestration à la confirmation** (ce module est au sommet, il appelle les autres dans cet ordre, dans une seule transaction) :
1. `employe` : créer le dossier salarié et générer le matricule (avec référence à la candidature d'origine) ;
2. `contrat` : créer le premier contrat (essai ou selon la proposition) et l'affectation initiale ;
3. `contrat` : démarrer l'onboarding (checklist) ;
4. `besoin` : clôturer la demande si l'effectif demandé est atteint.
En cas d'échec d'une étape, tout est annulé (transaction unique) : pas de salarié sans contrat.

**Règles :**
- **Candidat ≠ Employé** : le candidat reste dans `recrutement` ; l'employé est une nouvelle fiche créée ici.
- La proposition est figée à la validation (snapshot des conditions acceptées, conservé comme preuve).

---

### 4.4 `contrat` — Contrats, affectations, onboarding

**Rôle :** gérer la succession des contrats d'un salarié et ses affectations, puis son intégration.

**Entités :** Contrat, Avenant/Renouvellement, Affectation, Onboarding, OnboardingTache.

**Principe d'historisation (non négociable) :** on ne stocke **jamais** « le type de contrat actuel » comme un champ de l'employé. Chaque contrat est une ligne avec ses dates ; le contrat courant est celui valide à la date demandée. Même principe pour les affectations (poste, département, site, supérieur, dates) et le salaire (valeur + date d'effet, portée par le contrat ou l'avenant).

**Succession de contrats (règles données par le métier, à valider juridiquement, voir §10) :**
- **CE** (contrat d'essai) : maximum 6 mois, renouvelable une fois (1 an au total) ;
- **CDD** : maximum 2 ans, renouvelable une fois ;
- puis **CDI**.
Le service de contrat **refuse** toute création ou tout renouvellement qui viole ces règles. Les durées et le nombre de renouvellements sont des **paramètres** (§8), pas des constantes.

**Onboarding :** à la confirmation de l'embauche, une checklist est générée à partir d'un modèle. Tâches typiques : signature du contrat, badge, compte (via `iam`), matériel, formation, **inscription à l'état de paie**. Chaque tâche a un responsable, une échéance, un statut. L'onboarding est « terminé » quand toutes les tâches obligatoires sont faites. Le document signé est ajouté au dossier via `employe`.

**Inscription à l'état de paie :** pas de module paie. L'état de paie périodique = la **liste des contrats actifs sur la période**, fournie par ce module. La tâche d'onboarding « inscription paie » consiste à vérifier que le contrat est actif et complet (salaire, date d'effet).

**Services publics :** contrat courant d'un employé (à une date), historique des contrats, affectation courante et historique, salaire à une date, liste des contrats actifs sur une période, démarrer/consulter un onboarding, terminer un contrat (voir §10).

**Règles :**
- `contrat` appelle `employe` (l'employé existe) et `besoin` (poste, département, site existent) ; il ne stocke que leurs identifiants.
- Si le module grossit, l'onboarding peut devenir un module séparé au-dessus de `contrat` sans rien casser.

---

### 4.5 `employe` — Dossier administratif

**Rôle :** centraliser identité, coordonnées, documents, missions et alertes ; garantir la confidentialité.

**Entités :** Employe (matricule, identité, coordonnées, date de naissance, lien optionnel vers le compte utilisateur), DocumentAdministratif (type, fichier, date d'émission, **date de validité**), Mission + SuiviMission, Alerte.

**Documents et alertes :** chaque type de document a une règle de validité (ex. carte de résidence, carapanondro, autres pièces). Une tâche planifiée calcule des **alertes** (document expirant dans N jours, approche de l'âge de retraite) et les stocke ; l'écran RH les lit. Pas de notification poussée entre modules : les alertes sont des données lues par l'écran (et éventuellement envoyées par e-mail depuis ce module).

**Missions :** on trace les missions confiées et leur suivi avec deux indicateurs repris de votre cahier : *Manao asa ve* (le travail est-il réalisé ?) et *Miassa tsara ve* (est-il bien fait ?). *Hypothèse à confirmer : ce sont des indicateurs de suivi saisis par le responsable, pas un module d'évaluation de performance complet.*

**Confidentialité et accès (règle forte) :**
- **Seul le rôle RH** accède aux dossiers complets. Les autres rôles n'accèdent qu'à une vue minimale (nom, matricule, poste) fournie par un service dédié.
- Écran et API **spécifiques RH** (préfixe d'API distinct, contrôle de rôle sur chaque endpoint).
- **Chaque consultation et chaque modification** d'un dossier est écrite dans le **journal d'audit** (`shared`) : qui, quand, quoi, avant/après.

**Fiche salarié complète :** contrats, affectations, salaires et carrière appartiennent à `contrat`. Pour l'écran « dossier complet », le **front Vue** appelle les deux API (employe + contrat) et assemble la page. Aucun appel `employe → contrat` n'est créé.

**Services publics :** créer un employé (génère le matricule), consulter un employé, résumés par lot d'identifiants (pour les listes d'autres modules), ajouter/consulter des documents, lier un compte utilisateur.

**Règles :** le matricule est généré par ce module (unique, jamais réutilisé, format paramétrable).

---

### 4.6 `temps` — Temps de travail et pointage

**Rôle :** définir quand et où chacun travaille, enregistrer ce qui s'est réellement passé, calculer le temps travaillé et détecter les anomalies.

**Entités :** RegimeTravail (durée hebdomadaire, jours travaillés), Horaire (plages, pauses), PlanningTravail (qui, quand, où, quel rôle), RolePlanning (fonction tenue dans le planning, ex. responsable d'équipe, caisse), Suppleance (qui peut remplacer qui : compatibilité entre rôles), CalendrierFerie, Pointage (entrée/sortie), AnomaliePointage, TempsTravaille, HeureSupplementaire.

**Planning :** tout salarié a un régime et un planning, **y compris les CDI**. Le régime vient de l'affectation/contrat (`contrat`) ; le planning peut être défini par modèle (semaine type) avec exceptions par jour.

**Pointage et calculs :**
- Sources de pointage possibles : badge, saisie manuelle par le responsable, mobile ; le module expose un point d'entrée unique d'enregistrement.
- **Temps réellement travaillé** = sorties − entrées − pauses, comparé au planning.
- **Heures supplémentaires** : détectées au-delà du planning/seuil légal, **soumises à validation du responsable** avant d'être retenues, classées par catégorie (jour, nuit, dimanche/férié).
- **Anomalies** : entrée sans sortie, retard, départ anticipé, pointage hors planning, doublon, jour planifié sans aucun pointage.

**Sorties pour les autres modules :** jours planifiés dans une période, jours sans pointage, couverture d'un rôle à une date, remplaçants compatibles d'un employé, variables de temps pour la paie (§9).

**Règles :**
- `temps` **ne connaît pas les congés** : il signale « jour planifié sans pointage » ; c'est `conges` qui qualifie ensuite (congé validé, absence justifiée, absence injustifiée).
- Les seuils légaux, majorations et catégories d'heures sont des **paramètres** (§8) — les sources trouvées divergent (§11).

---

### 4.7 `conges` — Congés et absences

**Rôle :** gérer droits, soldes, demandes, contrôles, planning de congés, absences et justificatifs.

**Entités :** TypeConge (annuel, maladie, maternité, sans solde…), DroitConge, SoldeConge (avec mouvements : acquisition, consommation, ajustement), DemandeConge (+ historique de validation), Absence, Justificatif, PlanningConge.

**Droits et solde :**
- Acquisition : 30 jours/an soit **2,5 jours par mois** de service.
- **Plafond : le solde ne dépasse jamais 90 jours** (règle donnée par le métier ; interprétée comme solde cumulé maximal — *à confirmer*).
- Le solde est un **journal de mouvements** (pas une simple valeur écrasée) pour pouvoir l'expliquer et le corriger.

**Valorisation en temps réel :** pour chaque solde on affiche sa valeur en argent = **salaire brut × nombre de jours non pris ÷ 30** (salaire lu dans `contrat`). La même formule donne l'**indemnité de congés non pris** lorsque le salarié quitte l'entreprise (voir §10).

**Flux d'une demande :** `Brouillon → Soumise → Contrôles automatiques → Validation manager → Validation RH → Acceptée / Refusée` (annulation possible avant la date).

**Contrôles automatiques à la soumission (dans cet ordre) :**

| # | Contrôle | Source de l'information | Résultat |
|---|---|---|---|
| 1 | Droit de partir (type de congé autorisé, ancienneté, situation contractuelle, période fermée) | `contrat`, paramètres | Bloquant |
| 2 | Solde disponible suffisant | `conges` | Bloquant |
| 3 | Chevauchement avec ses propres congés/absences | `conges` | Bloquant |
| 4 | Jours réellement ouvrés de la période (planning, jours fériés) | `temps` | Calcul du nombre de jours |
| 5 | **Couverture** : pour chaque rôle tenu par l'employé, existe-t-il un remplaçant compatible, disponible (ni en congé, ni déjà remplaçant) ? | `temps` (rôles, suppléances) + `conges` (qui est absent) | Avertissement ou bloquant selon la règle du département |
| 6 | Seuil minimal de présence du département à ces dates | `conges` + `contrat` (effectif affecté) | Avertissement ou bloquant |

Les contrôles 5 et 6 répondent aux questions « qui peut remplacer qui », « que se passe-t-il quand une personne part » et « a-t-il le droit de partir ».

**Planning de congés :** deux modes à prévoir.
- **Par saisie** (version 1) : chaque salarié propose, le manager arbitre avec l'aide des contrôles de couverture.
- **Automatique** (version 2) : le système *propose* un planning (rotation, ancienneté, contraintes de couverture) ; un humain valide toujours.

**Absences :** `conges` rapproche les « jours planifiés sans pointage » (fournis par `temps`) de ses propres congés validés : ce qui reste est une absence à justifier (justificatif, délai), puis qualifiée en payée/non payée. Cet écran de rapprochement, qui superpose planning et congés, est porté par ce module.

**Règles :** `conges` est le sommet du graphe : il lit `temps`, `contrat`, `employe`, `besoin` ; personne ne l'appelle.

---

### 4.8 `iam` et `shared`

**`iam` :** utilisateurs, rôles (EMPLOYE, MANAGER, RH, FINANCE, DIRECTION, ADMIN), permissions. Rôles pouvant être limités à un département (un manager ne voit que son équipe). Authentification JWT.

**`shared` :** sécurité technique, journal d'audit, gestion des erreurs, paramètres de configuration (§8), utilitaires. **Aucune notion métier RH.**

---

## 5. Parcours transversaux (qui orchestre quoi)

| Parcours | Orchestrateur | Appels dans l'ordre |
|---|---|---|
| Du besoin à l'offre | `recrutement` | lit la demande validée (`besoin`) → crée l'offre |
| Candidat retenu → salarié | `embauche` | `recrutement` (candidature retenue) → `employe` (dossier + matricule) → `contrat` (contrat + affectation) → `contrat` (onboarding) → `besoin` (clôture) |
| Onboarding : création du compte | `contrat` | `iam` (créer le compte) → `employe` (lier le compte) |
| Demande de congé | `conges` | `contrat` (droit, salaire) → `temps` (jours ouvrés, couverture) → `employe` (identité) |
| Rapprochement absences | `conges` | `temps` (jours sans pointage) → qualification locale |
| Fiche salarié complète | **Front Vue** | API `employe` + API `contrat` |

---

## 6. Règles de conception

### 6.1 Trois identités distinctes
**Candidat** (`recrutement`), **Employé** (`employe`), **Utilisateur** (`iam`) sont trois objets liés **par identifiants**. Jamais fusionnés dans une même table.

### 6.2 Lire à la demande, pas notifier
Quand un module du dessus a besoin d'une information, il **l'interroge**. Exemples : `conges` demande le salaire à `contrat` ; `temps` demande le régime à `contrat`. Les modules du bas n'ont aucune connaissance de ceux du haut.

### 6.3 Historiser par période de validité
Contrat, affectation, salaire, régime de travail : chaque ligne a une date de début et de fin. On **ajoute**, on ne remplace pas. Les corrections se font par ajout d'une ligne corrective.

### 6.4 Workflows
Chaque workflow (besoin, proposition d'embauche, demande de congé, onboarding) est une **machine à états simple** dans son module : un statut, une table de transitions autorisées dans le service, une table d'historique des validations. **Pas de moteur de workflow générique** dans `shared`.

### 6.5 Données figées
Quand une valeur doit rester celle d'un instant donné (conditions de la proposition acceptée, salaire sur un document), on **copie** la valeur dans le module concerné (snapshot).

---

## 7. Sécurité, confidentialité, audit

| Rôle | Accès principal |
|---|---|
| EMPLOYE | Ses propres données : planning, pointages, soldes, demandes de congé, documents |
| MANAGER | Son équipe : demandes de besoin, validation congés/heures supplémentaires, plannings |
| RH | Tous les modules ; **seul** accès au dossier administratif complet |
| FINANCE | Validation finance des demandes de recrutement ; budget |
| DIRECTION | Validation direction des demandes de recrutement ; consultation |
| ADMIN | Paramétrage technique, comptes |

- Les données de salaire, d'identité et de dossier ne sortent **jamais** vers un rôle non autorisé (contrôle côté serveur, pas seulement côté interface).
- Le **journal d'audit** (`shared`) trace consultations et modifications des données sensibles (dossier, salaire, contrat, validations).
- Aucune donnée personnelle ni secret dans les journaux techniques.

---

## 8. Paramétrage (règles métier modifiables sans redéploiement)

Les valeurs suivantes sont des **paramètres** (table de configuration gérée dans `shared`, avec date d'effet et historique), jamais écrites en dur :

- durées maximales et renouvellements : essai, CDD ;
- durée légale de travail (hebdomadaire, journalière), seuils d'heures supplémentaires, catégories et taux de majoration ;
- acquisition de congés (jours par mois), plafond de solde (90), diviseur de valorisation (30) ;
- délais d'alerte des documents et âge de retraite ;
- règles de couverture minimale par département/rôle ;
- format du matricule ;
- modèles de checklist d'onboarding.

---

## 9. Données de paie (sans module paie)

Une future paie (ou un export vers un outil externe) lira, sans modification des modules existants :

| Donnée | Fournie par |
|---|---|
| Liste des salariés payés sur la période (contrats actifs) | `contrat` |
| Salaire de base et avenants à une date | `contrat` |
| Heures normales, heures supplémentaires par catégorie, retards | `temps` |
| Jours de congés payés pris, absences non payées | `conges` |
| Indemnité de congés non pris | `conges` |

Ces fonctions sont exposées dès maintenant sous forme de services de lecture ; aucune logique de calcul de paie n'est introduite.

---

## 10. Hypothèses et questions ouvertes

1. **Départ du salarié (hors des 7 blocs).** Fin de contrat, solde de tout compte, désactivation du compte : le parcours doit être orchestré **au-dessus de `conges`** (qui calcule l'indemnité de congés). Recommandation : un futur module `sortie` au sommet du graphe. D'ici là, `contrat` sait « terminer un contrat » et `conges` calcule l'indemnité ; RH enchaîne les deux à la main.
2. **Plafond de 90 jours** : solde cumulé maximal (hypothèse retenue) ou maximum par an ? À confirmer.
3. **Missions** (*Miassa tsara ve / Manao asa ve*) : indicateurs de suivi simples (hypothèse) ou évaluation complète ?
4. **Règles juridiques** (essai, CDD, heures supplémentaires, congés) : les règles ci-dessus viennent de votre cahier et de guides non officiels. **À faire valider par un juriste / l'inspection du travail** avant mise en production.
5. **Document « carapanondro »** : traité comme un type de document à échéance, à définir avec les RH.
6. **Qui est le manager d'un employé ?** Hypothèse : le supérieur hiérarchique figure dans l'**affectation** (`contrat`).
7. **Site / lieu de travail** : placé dans le référentiel `besoin` ; à ajuster si un module dédié « organisation » est souhaité plus tard.

---

## 11. Éléments recherchés

### 11.1 Éléments utiles d'un contrat de travail / d'une proposition d'embauche
D'après les guides consultés (à vérifier sur le Code du travail malgache, loi n° 2003-044) :
- identité et adresse de l'employeur et du salarié ;
- date de conclusion et **date d'effet** ;
- nature du travail / poste et **classification** (catégorie professionnelle) ;
- **lieu de travail** ;
- type et **durée** du contrat (terme pour un CDD) ;
- **période d'essai** : doit être écrite ; durée selon la catégorie ; renouvelable une seule fois ;
- **salaire** (montant, périodicité, éléments variables, avantages) ;
- **conditions de travail** : horaires, durée hebdomadaire, repos ;
- congés, préavis ;
- référence au Code du travail / à la convention collective applicable ;
- affiliation sociale (CNaPS, organisme de santé) ;
- signature des deux parties ; contrat écrit.

À ajouter dans une proposition : délai de réponse, supérieur hiérarchique, pièces à fournir.

### 11.2 Heures supplémentaires
- Durée légale citée par les guides : **40 h/semaine**, **8 h/jour** maximum, repos hebdomadaire de 24 h ; un guide indique que la durée légale ne s'applique pas aux cadres.
- Sont comptées comme heures supplémentaires : au-delà du seuil hebdomadaire ou journalier et le travail un jour de repos.
- **Majorations : les sources divergent** (25 % / 50 % pour l'une ; 30 % les 8 premières heures puis 50 %, et 100 % nuit/dimanche/férié pour d'autres). Un guide signale aussi un contingent annuel et une autorisation de l'inspection du travail.
- **Décision d'architecture :** seuils, catégories et taux sont des **paramètres** (§8), validés par un juriste ; le moteur de `temps` classe les heures par catégorie sans valeur codée en dur.

### 11.3 Congés
Les guides confirment **2,5 jours ouvrables par mois**, soit 30 jours par an. Le plafond de 90 jours est une règle interne propre à votre cahier (non retrouvée dans les sources).

### Sources
- africarrieres.com : guides « Heures de travail et congés », « Code du travail », « Contrats de travail » (non officiels)
- rivermate.com : « Éléments essentiels du contrat de travail » Madagascar
- wageindicator.org : fiches sur le temps de travail
- doc-du-juriste.com : fiches sur le droit du travail malgache (essai, CDD, heures supplémentaires)
- Texte de référence : Loi n° 2003-044 portant Code du travail (assemblee-nationale.mg)

---

## 12. Ordre de construction suggéré

| Phase | Modules | Objectif |
|---|---|---|
| 0 | `shared`, `iam`, CI, tests d'architecture, paramétrage | Socle |
| 1 | `besoin`, `employe` | Référentiels et dossier (aucune dépendance entre eux, parallélisables) |
| 2 | `recrutement`, `contrat` | Offres/candidatures ; contrats/affectations |
| 3 | `embauche` | Parcours complet besoin → salarié |
| 4 | `temps` | Plannings, pointage, anomalies |
| 5 | `conges` | Congés, contrôles de couverture, absences |

Répartition indicative pour 5 développeurs : (A) `shared` + `iam` + `employe` ; (B) `besoin` + `recrutement` ; (C) `contrat` + `embauche` ; (D) `temps` ; (E) `conges` + socle front Vue. Un module = un propriétaire ; relecture croisée obligatoire sur les dépendances inter-modules.

---

## 13. Lien avec les règles de code

Les règles d'implémentation (structure `controller / service / repository / model / dto`, appels uniquement par les services et DTO, pas de relation JPA entre modules, pas de cycle, tests ArchUnit) sont dans `ARCHITECTURE.md`. En cas de conflit entre les deux documents, signaler l'écart et corriger le document en tort par ADR.
