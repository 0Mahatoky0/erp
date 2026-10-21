# Contrat d'intégration du bloc 2

Le bloc 2 (recrutement) est développé indépendamment du bloc 1 (création du
besoin). Il ne modifie aucune table, colonne ou contrainte du bloc 1.

## Dépendances V1

La migration `V2__bloc2_recrutement.sql` attend les tables suivantes :

| Table V1 | Colonne attendue | Utilisation |
| --- | --- | --- |
| `demande_recrutement_poste` | `id_demande_poste` | rattacher une offre à une demande |
| `utilisateur` | `id_utilisateur` | créateur, organisateur, évaluateur et décideur |

Les deux colonnes doivent être des clés primaires ou des colonnes couvertes
par une contrainte `UNIQUE`, afin de recevoir les clés étrangères de V2.

## Autorisation de création d'une offre

La base ne force pas le statut de la demande V1, car ce statut appartient au
modèle métier du bloc 1. Le service Spring Boot doit vérifier que la demande
référencée est `ACCEPTEE` avant d'insérer une ligne dans `offre_emploi`.

Une demande absente, non accessible ou ayant un autre statut doit produire une
erreur métier explicite. Le contrôle ne doit pas être remplacé par une valeur
par défaut.

## Stratégie de développement indépendant

- Les tests du bloc 2 utilisent des fixtures V1 minimales avec les deux tables
  et les colonnes ci-dessus.
- Les tests ne copient pas la logique métier du bloc 1 ; ils simulent seulement
  son contrat d'intégration.
- Les tables V2 ont le préfixe de contraintes et d'index `b2_` afin de limiter
  les collisions lors de l'intégration.
- Les binaires de documents et les réponses de tests restent dans un stockage
  séparé ; la base conserve uniquement leurs clés durables.
- La fusion avec le bloc 1 est validée par une migration Flyway complète et par
  des tests d'intégration des clés étrangères.
