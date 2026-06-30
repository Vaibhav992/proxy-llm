package com.featureflag.proxy.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.featureflag.proxy.dto.CompletionRequest;
import com.featureflag.proxy.mapper.MismatchLogMapper;
import com.featureflag.proxy.repository.MismatchLogRepository;
import com.featureflag.proxy.util.JsonExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShadowService {

	private final LlmClientService llmClientService;
	private final ComparisonService comparisonService;
	private final JsonExtractor jsonExtractor;
	private final MismatchLogRepository mismatchLogRepository;
	private final MetricsService metricsService;

	@Async("shadowExecutor")
	@Transactional
	public void shadowAndCompare(UUID requestId, CompletionRequest request, String primaryBody) {
		Optional<String> candidateBody = llmClientService.callCandidateSafely(request);
		if (candidateBody.isEmpty()) {
			log.warn("Candidate unavailable for request {}", requestId);
			return;
		}

		if (comparisonService.matches(primaryBody, candidateBody.get())) {
			metricsService.recordComparison(true);
			return;
		}

		metricsService.recordComparison(false);

		try {
			String primaryOutput = jsonExtractor.extractOutput(primaryBody);
			String candidateOutput = jsonExtractor.extractOutput(candidateBody.get());
			mismatchLogRepository.save(MismatchLogMapper.toEntity(
					requestId, request.getPrompt(), primaryOutput, candidateOutput));
			log.info("Mismatch logged for request {}", requestId);
		}
		catch (IllegalArgumentException ex) {
			log.warn("Could not persist mismatch for request {}: {}", requestId, ex.getMessage());
		}
	}

}
