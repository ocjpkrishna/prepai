package com.ascorp.prepai.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enforces the code structure rules of spec section 9.1.1 on every build: strict
 * controller, service, repository layering; entities and repositories private to their module;
 * no dependency from the shared module on a business module; constructor injection only;
 * thin controllers.
 */
@AnalyzeClasses(packages = "com.ascorp.prepai", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	static final List<String> BUSINESS_MODULES =
			List.of("account", "quota", "generation", "lesson", "billing", "speech");

	private static final String ROOT = "com.ascorp.prepai";
	private static final String CONTROLLERS = "..controller..";
	private static final String SERVICES = "..service..";
	private static final String REPOSITORIES = "..repository..";
	private static final String ENTITIES = "..model.entity..";

	@ArchTest
	static final ArchRule LAYERS_ARE_RESPECTED = layeredArchitecture()
			.consideringOnlyDependenciesInLayers()
			.withOptionalLayers(true)
			.layer("Controller").definedBy(CONTROLLERS)
			.layer("Service").definedBy(SERVICES)
			.layer("Repository").definedBy(REPOSITORIES)
			.whereLayer("Controller").mayNotBeAccessedByAnyLayer()
			.whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service")
			.whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

	@ArchTest
	static final ArchRule CONTROLLERS_NEVER_SEE_ENTITIES = noClasses()
			.that().resideInAPackage(CONTROLLERS)
			.should().dependOnClassesThat().resideInAPackage(ENTITIES)
			.because("controllers return DTOs, never entities");

	@ArchTest
	static final ArchRule SERVICES_KNOW_NOTHING_ABOUT_HTTP = noClasses()
			.that().resideInAPackage(SERVICES)
			.should().dependOnClassesThat().resideInAnyPackage(
					"jakarta.servlet..", "org.springframework.web.bind..", "org.springframework.web.servlet..")
			.because("business logic must not depend on the web layer");

	@ArchTest
	static final ArchRule CONTROLLERS_ARE_REST_CONTROLLERS = classes()
			.that().resideInAPackage(CONTROLLERS).and().areTopLevelClasses()
			.should().haveSimpleNameEndingWith("Controller")
			.andShould().beAnnotatedWith(RestController.class);

	@ArchTest
	static final ArchRule REPOSITORIES_ARE_NAMED_AFTER_THEIR_ROLE = classes()
			.that().resideInAPackage(REPOSITORIES).and().areTopLevelClasses()
			.should().haveSimpleNameEndingWith("Repository");

	@ArchTest
	static final ArchRule TRANSACTIONS_BELONG_TO_SERVICES = noClasses()
			.that().resideInAnyPackage(CONTROLLERS, REPOSITORIES)
			.should().beAnnotatedWith(Transactional.class)
			.because("transaction boundaries are decided by business logic");

	@ArchTest
	static final ArchRule CONTROLLER_METHODS_ARE_NOT_TRANSACTIONAL = noMethods()
			.that().areDeclaredInClassesThat().resideInAPackage(CONTROLLERS)
			.should().beAnnotatedWith(Transactional.class);

	@ArchTest
	static final ArchRule NO_FIELD_INJECTION = noFields()
			.should().beAnnotatedWith(Autowired.class)
			.because("dependencies come through the constructor so classes stay testable");

	@ArchTest
	static final ArchRule SETTINGS_USE_CONFIGURATION_PROPERTIES = noFields()
			.should().beAnnotatedWith(Value.class)
			.because("settings are bound with @ConfigurationProperties, not scattered @Value fields");

	@ArchTest
	static final ArchRule MODULES_ARE_FREE_OF_CYCLES = slices()
			.matching(ROOT + ".(*)..")
			.should().beFreeOfCycles();

	@ArchTest
	static final ArchRule COMMON_DEPENDS_ON_NO_BUSINESS_MODULE = noClasses()
			.that().resideInAPackage(ROOT + ".common..")
			.should().dependOnClassesThat().resideInAnyPackage(businessModulePackages())
			.because("common is the foundation; business modules depend on it, never the other way round");

	@ArchTest
	static void modulesKeepTheirEntitiesAndRepositoriesPrivate(JavaClasses classes) {
		BUSINESS_MODULES.forEach(module -> {
			privateToModule(module, "model.entity").check(classes);
			privateToModule(module, "repository").check(classes);
		});
	}

	private static String[] businessModulePackages() {
		return BUSINESS_MODULES.stream().map(module -> ROOT + "." + module + "..").toArray(String[]::new);
	}

	private static ArchRule privateToModule(String module, String layerPackage) {
		String modulePackage = ROOT + "." + module + "..";
		String ownedPackage = ROOT + "." + module + ".." + layerPackage + "..";
		return noClasses()
				.that().resideOutsideOfPackage(modulePackage)
				.should().dependOnClassesThat().resideInAPackage(ownedPackage)
				.because(module + " owns its " + layerPackage + " classes; others go through its services");
	}
}
