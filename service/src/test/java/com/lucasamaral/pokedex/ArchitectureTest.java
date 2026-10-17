package com.lucasamaral.pokedex;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Enforces the Clean Architecture dependency rule: every dependency points inward, towards the domain.
 */
@AnalyzeClasses(packages = "com.lucasamaral.pokedex", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "com.lucasamaral.pokedex.domain..";
    private static final String APPLICATION = "com.lucasamaral.pokedex.application..";
    private static final String INFRASTRUCTURE = "com.lucasamaral.pokedex.infrastructure..";
    private static final String WEB = "com.lucasamaral.pokedex.web..";

    @ArchTest
    static final ArchRule layersDependOnlyInward = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy(DOMAIN)
            .layer("Application").definedBy(APPLICATION)
            .layer("Infrastructure").definedBy(INFRASTRUCTURE)
            .layer("Web").definedBy(WEB)
            .whereLayer("Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure", "Web")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Web");

    @ArchTest
    static final ArchRule coreIsFrameworkFree = noClasses()
            .that().resideInAnyPackage(DOMAIN, APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.servlet..",
                    "tools.jackson..",
                    "com.fasterxml.jackson..")
            .because("domain and use cases must not know about frameworks, HTTP or persistence");
}
