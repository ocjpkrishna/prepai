package com.ascorp.prepai.account.privacy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.common.model.PersonalDataContributor;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class PersonalDataServiceTest {

	private static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");

	@Mock
	private ObjectProvider<PersonalDataContributor> contributors;

	@Mock
	private PersonalDataContributor lessons;

	private PersonalDataService service;

	@BeforeEach
	void setUp() {
		service = new PersonalDataService(contributors);
		when(contributors.orderedStream()).thenReturn(Stream.of(lessons));
	}

	@Test
	void exportCollectsEachContributorsSection() {
		when(lessons.section()).thenReturn("lessons");
		when(lessons.export(USER_ID)).thenReturn(Map.of("count", 0));

		assertThat(service.export(USER_ID)).containsExactly(Map.entry("lessons", Map.of("count", 0)));
	}

	@Test
	void eraseAsksEachContributorToDeleteTheStudentsRows() {
		service.erase(USER_ID);

		verify(lessons).erase(USER_ID);
	}
}
