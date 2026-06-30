package com.featureflag.proxy.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.featureflag.proxy.dto.CompletionRequest;
import com.featureflag.proxy.dto.CompletionResponse;
import com.featureflag.proxy.exception.LlmClientException;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class ProxyService {

	private final LlmClientService llmClientService;
	private final ShadowService shadowService;
	private final JsonMapper jsonMapper;

	public CompletionResponse process(CompletionRequest request) {
		String primaryBody = llmClientService.callPrimary(request);
		CompletionResponse response = parsePrimaryResponse(primaryBody);
		shadowService.shadowAndCompare(UUID.randomUUID(), request, primaryBody);
		return response;
	}

	private CompletionResponse parsePrimaryResponse(String body) {
		try {
			return jsonMapper.readValue(body, CompletionResponse.class);
		}
		catch (JacksonException ex) {
			throw new LlmClientException("Primary LLM returned invalid JSON", ex);
		}
	}

}
