# ARCHITECTURE_BAKEND.md — ERP RH (Spring Boot / Vue.js)

> Document normatif pour les développeurs **et les agents IA**.
> **DOIT / NE DOIT PAS** = règle obligatoire. En cas de doute, appliquer ce document sans improviser.

---

## 1. Principe

Un **monolithe modulaire simple** : une application Spring Boot, découpée en modules métier. Chaque module contient les mêmes dossiers de base. Les modules s'appellent **directement par leurs services**. Pas d'événements, pas de couches supplémentaires.

---

## 2. Structure

```
com.entreprise.erprh/
├── ErpRhApplication.java
├── shared/          # technique commun : sécurité, exceptions, config, utilitaires
├── iam/             # utilisateurs, rôles, permissions
├── employe/
├── recrutement/
├── besoin/
├── embauche/
├── contrat/
├── temps/
└── conges/
```
(Adapter la liste aux modules réels.)

Structure identique dans **chaque** module :

```
<module>/
├── controller/      # API REST
├── service/         # logique métier (classes concrètes, @Service)
├── repository/      # Spring Data JPA
├── model/           # entités JPA, enums
└── dto/             # objets d'entrée/sortie
```

Pas de dossier `controller/`, `service/`, `repository/` à la racine. Pas d'interface + implémentation : une classe `EmployeService` suffit.

---

## 3. Rôle de chaque dossier

| Dossier | Contenu | Utilisable par un autre module ? |
|---|---|---|
| `controller` | Endpoints REST, validation d'entrée. **Aucune logique métier.** | Non |
| `service` | Logique métier, `@Transactional`, appel des repositories | **Oui** |
| `repository` | Accès base de données | **Non** |
| `model` | Entités JPA | **Non** |
| `dto` | Objets échangés avec le front et entre modules | **Oui** |
| `shared` | Technique uniquement (aucune notion métier RH) | Oui, par tous |

**Ce qu'un module peut utiliser d'un autre module : ses `service` et ses `dto`. Rien d'autre.**

---

## 4. Règles obligatoires

**R1 — On appelle le service de l'autre module, jamais son repository ni son entité.**
```java
// OK
private final EmployeService employeService;
EmployeDto e = employeService.trouverParId(id);

// KO
import com.entreprise.erprh.employe.repository.EmployeRepository;
import com.entreprise.erprh.employe.model.Employe;
```

**R2 — Les services publics retournent des DTO, jamais des entités JPA.** Un controller manipule uniquement des DTO.

**R3 — Pas de relation JPA entre modules.** On stocke l'identifiant (`Long employeId`), pas de `@ManyToOne Employe` dans `paie`. La clé étrangère en base reste autorisée.

**R4 — Pas de dépendance circulaire entre modules.** Si `paie` appelle `employe`, alors `employe` n'appelle pas `paie` (ni indirectement). Voir §5.

**R5 — Controller fin** : il valide, appelle un service, retourne un DTO.

**R6 — `@Transactional` sur les méthodes de `service`**, jamais sur les controllers ni les repositories. Lecture seule : `readOnly = true`. Un appel à un autre service rejoint la même transaction.

**R7 — Pas de logique métier dans `shared`**, et `shared` ne dépend d'aucun module métier.

**R8 — Pas de N+1 entre modules.** Pour afficher des données d'un autre module sur une liste, utiliser une méthode batch, pas une boucle d'appels.
```java
Map<Long, EmployeResumeDto> employes = employeService.trouverResumesParIds(ids); // 1 appel
```

---

## 5. Sens des dépendances (à respecter pour éviter les cycles)

```
iam      ←  besoin, contrat
besoin   ←  recrutement, embauche, contrat, temps, conges
employe  ←  embauche, contrat, temps, conges
recrutement ← embauche
contrat  ←  embauche, temps, conges
temps    ←  conges
```
« A ← B » : **B peut appeler A**. Les modules de base (`iam`, `employe`) n'appellent **jamais** les modules au-dessus.

**Comment gérer un besoin dans le « mauvais sens » (sans événement) :**

1. **L'orchestration se fait dans le module du dessus.** Le module qui déclenche le processus appelle les autres.
   Exemple : l'acceptation d'une proposition est traitée par `embauche`, qui appelle `employeService` (dossier et matricule) puis `contratService` (contrat et affectation). `employe` n'a pas besoin de connaître `contrat`. Le détail des modules et des orchestrations est dans `ARCHITECTURE_MODULES.md`.
2. **On lit à la demande plutôt que de notifier.** `paie` interroge `employeService` quand elle calcule un bulletin, au lieu qu'`employe` prévienne `paie`.
3. **Si deux modules ont vraiment besoin l'un de l'autre**, la fonctionnalité commune doit descendre dans un module de base (ou être regroupée dans un seul module). Ne pas utiliser `@Lazy` ni injection par setter pour masquer un cycle.
4. Tout nouveau lien de dépendance est ajouté à ce schéma dans la PR.

---

## 6. Listes et recherches multi-modules

- Affichage de quelques champs d'un autre module : méthode batch (R8).
- Donnée à figer au moment de l'action (nom, poste, salaire sur un bulletin) : copier la valeur dans la table du module (snapshot).
- Filtre/tri/rapport sur plusieurs modules : vue SQL en lecture seule (entité `@Immutable`, `Repository<>` sans `save`), placée dans le module qui l'utilise. Créée par migration Flyway.

---

## 7. Base de données

- Une seule base, un seul dossier de migrations Flyway (`db/migration`), versions croissantes. Une migration appliquée n'est jamais modifiée.
- Pas de préfixe obligatoire sur les noms de tables.
- Contraintes FK en base recommandées (R3 ne concerne que le mapping JPA).

---

## 8. Conventions

| Élément | Convention | Exemple |
|---|---|---|
| Package module | minuscule, sans underscore | `tempsconges` |
| Service | `<Nom>Service` | `EmployeService` |
| Controller | `<Nom>Controller` | `EmployeController` |
| Repository | `<Entité>Repository` | `EmployeRepository` |
| DTO entrée / sortie | `<Action><Nom>Request` / `<Nom>Dto` | `CreerEmployeRequest`, `EmployeDto` |
| Endpoint | `/api/v1/<ressource>` | `/api/v1/employes/{id}` |

DTO : de préférence des `record`, validation Jakarta (`@NotNull`, `@Size`) sur les DTO d'entrée. Autorisation (`@PreAuthorize`) sur les endpoints. Aucune donnée personnelle ou secret dans les logs.

---

## 9. Vérification automatique (ArchUnit)

Ces tests DOIVENT passer en CI. Ne jamais les désactiver ou les affaiblir pour faire passer un build.

```java
class ArchitectureTest {

    private static final String BASE = "com.entreprise.erprh";
    private static final List<String> MODULES = List.of(
            "iam", "besoin", "employe", "recrutement",
            "embauche", "contrat", "temps", "conges");

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);

    @Test
    void un_module_ne_touche_ni_repository_ni_model_ni_controller_des_autres() {
        for (String m : MODULES) {
            String p = BASE + "." + m;
            noClasses().that().resideOutsideOfPackage(p + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            p + ".repository..", p + ".model..", p + ".controller..")
                    .check(classes);
        }
    }

    @Test
    void pas_de_cycle_entre_modules() {
        slices().matching(BASE + ".(*)..").should().beFreeOfCycles().check(classes);
    }

    @Test
    void controllers_sans_model_ni_repository() {
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAnyPackage("..model..", "..repository..")
                .check(classes);
    }

    @Test
    void shared_ne_depend_d_aucun_module() {
        for (String m : MODULES) {
            noClasses().that().resideInAPackage(BASE + ".shared..")
                    .should().dependOnClassesThat().resideInAPackage(BASE + "." + m + "..")
                    .check(classes);
        }
    }
}
```

---

## 10. Frontend Vue.js

```
src/
├── modules/<module>/   # views, components, stores Pinia, api, routes
├── shared/             # composants UI, layout, auth, client HTTP
└── router/             # agrège les routes des modules (lazy loading)
```
Un module front n'importe pas les stores ni les composants internes d'un autre module.

---

## 11. Checklist de pull request

- [ ] Aucun import de `repository`/`model` d'un autre module
- [ ] Aucune entité JPA retournée par un service public ou un controller
- [ ] Aucun `@ManyToOne`/`@OneToMany` vers un autre module
- [ ] Pas de cycle entre modules (schéma §5 mis à jour si nouveau lien)
- [ ] `@Transactional` sur les services uniquement
- [ ] Pas de N+1 (méthode batch)
- [ ] Migration Flyway ajoutée si le schéma change
- [ ] Tests du service ajoutés, `ArchitectureTest` passe

---

## 12. Instructions pour les agents IA

**Toujours :**
1. Identifier le module propriétaire de la fonctionnalité et travailler dedans.
2. Placer chaque classe dans le bon dossier (§3).
3. Pour utiliser un autre module : injecter son **service** et manipuler ses **DTO**. Si la méthode manque, **l'ajouter dans le service de ce module** avec son DTO, au lieu d'accéder à son repository.
4. Respecter le sens des dépendances (§5). Pour un processus qui traverse plusieurs modules, placer l'orchestration dans le module du dessus.
5. Écrire les tests et une migration Flyway si le schéma change.

**Jamais :**
- Importer un `repository`, un `model` ou un `controller` d'un autre module.
- Retourner une entité JPA depuis un service public ou un controller.
- Créer une relation JPA vers une entité d'un autre module.
- Créer un cycle entre modules (ni `@Lazy` ni injection par setter pour le contourner).
- Mettre de la logique métier dans un controller ou dans `shared`.
- Ajouter des événements, façades, interfaces/impl, « managers » ou couches non prévues ici.
- Appeler un service dans une boucle (utiliser une méthode batch).
- Désactiver ou affaiblir `ArchitectureTest`.

**En cas de conflit entre une demande et ce document :** signaler le conflit et proposer la solution conforme.
