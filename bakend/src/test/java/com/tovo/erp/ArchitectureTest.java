package com.tovo.erp;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Verifie les regles de ARCHITECTURE_BAKEND.md (§9) et de OUR_IA.md (E1/E2).
 *
 * Le module {@code paie} est present dans le code mais hors perimetre OUR_IA.
 * Il est maintenu comme extension isolee : il doit respecter les memes regles
 * d'isolation et de cycle que les autres modules.
 */
@AnalyzeClasses(packages = "com.tovo.erp", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String BASE = "com.tovo.erp";
    private static final List<String> MODULES = List.of(
            "iam", "employe", "besoin", "recrutement", "embauche",
            "contrat", "temps", "conges", "paie");

    @ArchTest
    static final ArchRule pas_de_cycle_entre_modules = SlicesRuleDefinition
            .slices().matching(BASE + ".(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule controllers_sans_model_ni_repository = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAnyPackage("..model..", "..repository..");

    @ArchTest
    static final ArchRule shared_ne_depend_d_aucun_module = noClasses()
            .that().resideInAPackage(BASE + ".shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    MODULES.stream().map(m -> BASE + "." + m + "..").toArray(String[]::new));

    /**
     * R1 : un module ne touche ni repository, ni model, ni controller d'un autre module.
     * Les services et DTO restent les seuls points d'entree inter-modules.
     */
    @ArchTest
    static final ArchRule un_module_ne_touche_que_les_services_et_dto_des_autres =
            isolationDesModules();

    private static ArchRule isolationDesModules() {
        CompositeArchRule composite = null;
        for (String module : MODULES) {
            String[] interdits = MODULES.stream()
                    .filter(autre -> !autre.equals(module))
                    .flatMap(autre -> List.of(
                            BASE + "." + autre + ".repository..",
                            BASE + "." + autre + ".model..",
                            BASE + "." + autre + ".controller..").stream())
                    .toArray(String[]::new);
            ArchRule regle = noClasses()
                    .that().resideInAPackage(BASE + "." + module + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(interdits)
                    .because("un module ne doit utiliser que les services et DTO des autres modules");
            composite = composite == null
                    ? CompositeArchRule.of(regle)
                    : composite.and(regle);
        }
        return composite;
    }
}
