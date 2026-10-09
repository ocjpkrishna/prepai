package com.ascorp.prepai.account.privacy.service;

import com.ascorp.prepai.common.model.PersonalDataContributor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** Gathers the export and erases the purge across every module that holds personal data (spec 2.7). */
@Service
@RequiredArgsConstructor
public class PersonalDataService {

	private final ObjectProvider<PersonalDataContributor> contributors;

	public Map<String, Object> export(UUID userId) {
		Map<String, Object> sections = new LinkedHashMap<>();
		contributors.orderedStream()
				.forEach(contributor -> sections.put(contributor.section(), contributor.export(userId)));
		return sections;
	}

	public void erase(UUID userId) {
		contributors.orderedStream().forEach(contributor -> contributor.erase(userId));
	}
}
