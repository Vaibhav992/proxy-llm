package com.featureflag.proxy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.featureflag.proxy.dto.CompletionRequest;
import com.featureflag.proxy.exception.LlmClientException;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ProxyServiceTest {

	@Mock
	private LlmClientService llmClientService;

	@Mock
	private ShadowService shadowService;

	private ProxyService proxyService;

	@BeforeEach
	void setUp() {
		proxyService = new ProxyService(llmClientService, shadowService, JsonMapper.builder().build());
	}

	@Test
	void process_returnsParsedPrimaryResponseAndTriggersShadow() {
		CompletionRequest request = new CompletionRequest();
		request.setPrompt("hello");
		String primaryBody = """
				{"output":"Echo: hello","model":"primary"}
				""";

		when(llmClientService.callPrimary(request)).thenReturn(primaryBody);

		assertThat(proxyService.process(request).getOutput()).isEqualTo("Echo: hello");
		verify(shadowService).shadowAndCompare(any(), eq(request), eq(primaryBody));
	}

	@Test
	void process_propagatesLlmClientException() {
		CompletionRequest request = new CompletionRequest();
		request.setPrompt("fail");

		when(llmClientService.callPrimary(request))
				.thenThrow(new LlmClientException("Upstream LLM returned HTTP 500"));

		assertThatThrownBy(() -> proxyService.process(request))
				.isInstanceOf(LlmClientException.class);
	}

}
