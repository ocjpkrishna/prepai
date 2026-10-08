package com.ascorp.prepai.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Every module explains itself: a module folder without a MODULE.md fails the build (spec 9.1.1, Rule 8). */
class ModuleDocumentationTest {

	private static final Path MODULES_ROOT = Path.of("src/main/java/com/ascorp/prepai");
	private static final String DOCUMENT = "MODULE.md";

	@Test
	void everyModuleHasAModuleDocument() throws IOException {
		assertThat(modulesWithoutDocument()).as("modules without " + DOCUMENT).isEmpty();
	}

	@Test
	void everyBusinessModuleOfTheArchitectureTestExists() {
		List<String> missing = ArchitectureTest.BUSINESS_MODULES.stream()
				.filter(module -> Files.notExists(MODULES_ROOT.resolve(module)))
				.toList();

		assertThat(missing).as("business modules listed in ArchitectureTest but missing on disk").isEmpty();
	}

	private List<String> modulesWithoutDocument() throws IOException {
		try (Stream<Path> children = Files.list(MODULES_ROOT)) {
			return children.filter(Files::isDirectory)
					.filter(module -> Files.notExists(module.resolve(DOCUMENT)))
					.map(module -> module.getFileName().toString())
					.sorted()
					.toList();
		}
	}
}
