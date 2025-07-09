package com.fiap.video.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    @Test
    void onlyServiceCanAccessConfig() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("com.fiap.video");

        ArchRuleDefinition.noClasses()
                .that()
                .resideOutsideOfPackage("com.fiap.video.service..")
                .should()
                .accessClassesThat()
                .resideInAnyPackage("com.fiap.video.config..")
                .because("apenas o pacote service pode acessar o pacote config")
                .check(importedClasses);
    }

    @Test
    void controllerCannotAccessConfig() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("com.fiap.video");

        ArchRuleDefinition.noClasses()
                .that()
                .resideInAnyPackage("com.fiap.video.controller..")
                .should()
                .accessClassesThat()
                .resideInAnyPackage("com.fiap.video.config..")
                .because("controller não pode acessar config diretamente")
                .check(importedClasses);
    }
}
