package com.featureflag.proxy.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.featureflag.proxy.config.LlmProperties;
import com.featureflag.proxy.config.RestClientConfig;
import com.featureflag.proxy.dto.CompletionRequest;
import com.featureflag.proxy.exception.LlmClientException;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
public class LlmClientService {

	private final RestClient primaryRestClient;
	private final RestClient candidateRestClient;
	private final LlmProperties llmProperties;
	private final JsonMapper jsonMapper;

	public LlmClientService(
			@Qualifier(RestClientConfig.PRIMARY_REST_CLIENT) RestClient primaryRestClient,
			@Qualifier(RestClientConfig.CANDIDATE_REST_CLIENT) RestClient candidateRestClient,
			LlmProperties llmProperties,
			JsonMapper jsonMapper) {
		this.primaryRestClient = primaryRestClient;
		this.candidateRestClient = candidateRestClient;
		this.llmProperties = llmProperties;
		this.jsonMapper = jsonMapper;
	}

	public String callPrimary(CompletionRequest request) {
		return callCompletion(primaryRestClient, llmProperties.getPrimaryUrl(), request);
	}

	public Optional<String> callCandidateSafely(CompletionRequest request) {
		try {
			return Optional.of(callCompletion(candidateRestClient, llmProperties.getCandidateUrl(), request));
		}
		catch (LlmClientException ex) {
			log.warn("Candidate LLM call failed for prompt='{}': {}", request.getPrompt(), ex.getMessage());
			return Optional.empty();
		}
	}

	private String callCompletion(RestClient restClient, String url, CompletionRequest request) {
		try {
			return restClient.post()
					.uri(url)
					.contentType(MediaType.APPLICATION_JSON)
					.body(toJson(request))
					.retrieve()
					.onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
							(req, response) -> {
								throw new LlmClientException(
										"Upstream LLM returned HTTP " + response.getStatusCode().value());
							})
					.body(String.class);
		}
		catch (LlmClientException ex) {
			throw ex;
		}
		catch (RestClientResponseException ex) {
			throw new LlmClientException("Upstream LLM returned HTTP " + ex.getStatusCode().value(), ex);
		}
		catch (Exception ex) {
			throw new LlmClientException("Failed to call upstream LLM at " + url, ex);
		}
	}

	private String toJson(CompletionRequest request) {
		try {
			return jsonMapper.writeValueAsString(request);
		}
		catch (JacksonException ex) {
			throw new LlmClientException("Failed to serialize completion request", ex);
		}
	}

}
