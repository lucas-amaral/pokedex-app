package com.example.tasks;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Conventional package names, Clean Architecture rules, checked on every build. Services and entities are the core:
 * they depend on nothing but each other and the repository interfaces. Controllers, the JPA implementation and the
 * Spring configuration sit outside and point inward.
 */
@AnalyzeClasses(packages = "com.example.tasks", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String CONTROLLER = "com.example.tasks.controller..";
    private static final String SERVICE = "com.example.tasks.service..";
    private static final String ENTITY = "com.example.tasks.entity..";
    private static final String REPOSITORY = "com.example.tasks.repository";
    private static final String REPOSITORY_JPA = "com.example.tasks.repository.jpa..";
    private static final String CONFIG = "com.example.tasks.config..";

    @ArchTest
    static final ArchRule dependenciesPointInward = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controller").definedBy(CONTROLLER)
            .layer("Service").definedBy(SERVICE)
            .layer("Entity").definedBy(ENTITY)
            .layer("Repository").definedBy(REPOSITORY)
            .layer("RepositoryJpa").definedBy(REPOSITORY_JPA)
            .layer("Config").definedBy(CONFIG)
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("RepositoryJpa").mayNotBeAccessedByAnyLayer()
            .whereLayer("Config").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Config")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Controller", "Service", "RepositoryJpa", "Config")
            .whereLayer("Entity").mayOnlyBeAccessedByLayers("Service", "Repository", "RepositoryJpa", "Controller", "Config");

    /** Controllers may build query values (filter, sort, page) for a use case, but never reach storage themselves. */
    @ArchTest
    static final ArchRule controllersGoThroughServices = noClasses()
            .that().resideInAPackage(CONTROLLER)
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
            .because("controllers call use cases; only services talk to repositories");

    @ArchTest
    static final ArchRule coreIsFrameworkFree = noClasses()
            .that().resideInAnyPackage(SERVICE, ENTITY, REPOSITORY)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "jakarta.servlet..", "tools.jackson..")
            .because("services, entities and repository interfaces must not depend on frameworks, HTTP or persistence");
}
