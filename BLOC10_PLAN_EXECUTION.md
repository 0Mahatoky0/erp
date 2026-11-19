# Bloc 10 — Plan d’exécution (5 développeurs)

## État

Le bloc 10 « Carrière et mobilité » est considéré complet côté backend/frontend :
historique salarial et contraintes temporelles, éligibilité et dérogations, workflow de validation séquentiel, événement d’application, propagation inter-modules, confidentialité, contrôle d’intégrité et interface.

Commits de référence : `c6e0102`, `430a69a`, `2a2ea96`, `153a3c4`, `b960c49`, `c6a1ec1`, `d6abcf4`, `85fc675`, `0d55ec5`.

## Règles communes

1. Respecter `OUR_IA.md` : monolithe modulaire Spring Boot + Vue ; pas de dépendance `controller -> model/repository`.
2. Un module ne consomme d’un autre module que ses `service`, `dto` et éventuels événements `shared`.
3. Ne jamais créer de relation JPA inter-modules.
4. Toute lecture de salaire ou de sanction doit être audité et filtrée selon le rôle.
5. Chaque évolution fait l’objet d’un commit dédié et de tests ciblés.
6. Ne pas inclure les changements non liés au bloc 10 actuellement présents dans l’arbre de travail (rupture, bulletins solde, postes, sidebar, router, migrations `V35`/`V36`).

## Répartition

### Dev 1 — Socle métier et historique temporel

Responsable : migrations, contraintes, historique salarial, reconstitution.

- Vérifier `V37__bloc10_integrite_temporelle.sql` : unicité bi-temporelle, plages chevauchantes, triggers, colonnes `ancienne_situation`/`nouvelle_situation`.
- Maintenir `CarriereService.enregistrerSalaire(...)` : motif valide, montant ≥ 0, date requise, preuve contractuelle obligatoire.
- Maintenir `carriereALaDate(...)` : poste, service, département, manager, catégorie, échelon, taux d’activité, régime de travail et salaire selon `connu_le`.
- Maintenir `rapportIntegrite()` : chevauchements, trous, lignes hors contrat, mouvements sans historique, incohérences paie.
- Tests associés : `CarriereServiceTest`, `MouvementCarriereServiceTest`.

### Dev 2 — Workflow de validation et sécurité

Responsable : états, circuit séquentiel, séparation des tâches, avenants.

- Maintenir `MouvementCarriereService.valider(...)` : rôle de l’étape courante, `etapeCourante`, `idValidateur`, `dateDecision`, passage final à `VALIDE`.
- Maintenir `refuser`, `preparerAvenant`, `signerAvenant`, `refuserParSalarie`, `planifier`, `annuler`.
- Vérifier la règle de dérogation direction et le refus d’auto-déclaration.
- Vérifier le garde-fou `exigeAvenantSigne` avant application.
- Vérifier les `@PreAuthorize` du `MouvementCarriereController`.
- Tests associés : transitions de statut, rôle courant, séparation des tâches, baisses de salaire, mutations géographiques/temporaires.

### Dev 3 — Événements et intégration inter-modules

Responsable : `MouvementAppliqueEvent` et consommateurs.

- Maintenir la publication atomique après `mouvements.save(...)` dans `MouvementCarriereService.appliquer(...)`.
- Vérifier les consommateurs :
  - `MouvementAppliqueBesoinListener` : demande de remplacement.
  - `MouvementAppliqueEmployeListener` : mission de prise en charge.
  - `MouvementAppliqueEvaluationListener` : besoins de formation `MOBILITE`.
  - `MouvementAppliqueTempsListener` : régime et planning.
  - `MouvementAppliqueCongeListener` : validation des congés.
  - `MouvementAppliquePaieListener` : ajustement des bulletins.
  - `MouvementAppliqueIamListener` : rôle `MANAGER`.
- Maintenir `ManagerAffectationService` pour éviter le cycle IAM → contrat.
- Ne jamais introduire d’import direct vers un `model`/`repository` externe.
- Test obligatoire : `./mvnw -q -Dtest=ArchitectureTest test`.

### Dev 4 — Frontend et expérience utilisateur

Responsable : `CarriereView.vue` et `carriereApi.js`.

- Couvrir les 5 types : `PROMOTION`, `MUTATION`, `DETACHEMENT`, `CHANGEMENT_POSTE`, `CHANGEMENT_SALAIRE`.
- Couvrir les types de mutation : `INTERNE_SERVICE`, `INTER_DEPARTEMENT`, `GEOGRAPHIQUE`, `TEMPORAIRE`, `DETACHEMENT`.
- Afficher l’étape courante du circuit et les actions conditionnées par statut.
- Afficher les champs enrichis de la reconstitution : taux, régime, statut d’affectation, salaire filtré.
- Empêcher l’affichage des salaires et sanctions non autorisés (le backend filtre déjà).
- Vérifier : `npm run build`, `npx oxlint src/modules/carriere`, `npx eslint src/modules/carriere`.

### Dev 5 — Qualité, confidentialité et recette

Responsable : tests, audit, contrôle nocturne, documentation.

- Maintenir les tests de confidentialité : manager ne voit ni salaire ni sanction ; RH voit la frise complète.
- Vérifier les audits `LECTURE_HISTORIQUE_SALAIRE`, `RAPPORT_INTEGRITE_CARRIERE` et les audits de propagation.
- Maintenir `MouvementCarriereJob` : mouvements planifiés à `00:10`, contrôle d’intégrité à `02:30`.
- Vérifier `BulletinCoherenceService.ecarts()` : comparaison bulletin/historique sur periodes passées.
- Lancer la batterie complète avant recette :
  - `./mvnw -q -Dtest=CarriereServiceTest,MouvementCarriereServiceTest,ArchitectureTest test`
  - `npm run build`
- Conserver ce MD à jour après chaque commit bloc 10.

## Ordre d’exécution

1. Dev 1 valide la migration et l’historique salarial.
2. Dev 2 valide le workflow et les règles d’éligibilité.
3. Dev 3 branche la publication d’événement et les consommateurs.
4. Dev 4 aligne l’interface et vérifie le build frontend.
5. Dev 5 exécute les tests, contrôle les audits, la confidentialité et le rapport d’intégrité.
6. Revue commune du diff complet du bloc 10 avant passage aux blocs 11/12.

## Définition de terminé

- Tous les tests ciblés passent.
- `ArchitectureTest` passe sans nouvelle violation.
- `npm run build` passe.
- Aucun changement hors bloc 10 n’est inclus dans les commits.
- Le MD de répartition correspond au code livré.
