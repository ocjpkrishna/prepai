package com.ascorp.prepai.generation.rag.service;

import com.ascorp.prepai.generation.embedding.service.EmbeddingService;
import com.ascorp.prepai.generation.rag.model.ProblemProbe;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Steps 1 to 3 of spec 6.3: normalise the problem, read its numbers and embed it. */
@Component
@RequiredArgsConstructor
public class ProblemProbeService {

	private final TextNormalizer normalizer;
	private final NumericSignatureService signatures;
	private final EmbeddingService embedder;

	/** Empty when the signature is too long to store, so the problem is never cached. */
	public Optional<ProblemProbe> probe(String problemText) {
		String canonical = normalizer.withCanonicalUnits(problemText);
		String text = canonical.toLowerCase(Locale.ROOT);
		return signatures.signatureOf(canonical)
				.map(signature -> new ProblemProbe(text, signature, embedder.embed(text)));
	}
}
