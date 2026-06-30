package com.featureflag.proxy.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.featureflag.proxy.config.LlmProperties;
import com.featureflag.proxy.dto.CompletionRequest;
import com.featureflag.proxy.exception.LlmClientException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import tools.jackson.databind.json.JsonMapper;

class LlmClientServiceTest {

	private WireMockServer wireMockServer;
	private LlmClientService llmClientService;

	@BeforeEach
	void setUp() {
		wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
		wireMockServer.start();
		WireMock.configureFor("localhost", wireMockServer.port());

		LlmProperties llmProperties = new LlmProperties();
		llmProperties.setPrimaryUrl(wireMockServer.baseUrl() + "/primary");
		llmProperties.setCandidateUrl(wireMockServer.baseUrl() + "/candidate");
		llmProperties.setPrimaryTimeout(Duration.ofMillis(500));
		llmProperties.setCandidateTimeout(Duration.ofMillis(500));

		RestClient primaryClient = restClientWithTimeout(llmProperties.getPrimaryTimeout());
		RestClient candidateClient = restClientWithTimeout(llmProperties.getCandidateTimeout());
		llmClientService = new LlmClientService(primaryClient, candidateClient, llmProperties,
				JsonMapper.builder().build());
	}

	@AfterEach
	void tearDown() {
		wireMockServer.stop();
	}

	@Test
	void callPrimary_returnsResponseBodyOnSuccess() {
		stubFor(WireMock.post(urlEqualTo("/primary"))
				.withHeader("Content-Type", equalTo(MediaType.APPLICATION_JSON_VALUE))
				.withRequestBody(containing("hello"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("""
								{"output":"Echo: hello","model":"primary"}
								""")));

		CompletionRequest request = new CompletionRequest();
		request.setPrompt("hello");

		assertThat(llmClientService.callPrimary(request)).contains("Echo: hello");
		verify(postRequestedFor(urlEqualTo("/primary")));
	}

	@Test
	void callPrimary_throwsWhenUpstreamFails() {
		stubFor(WireMock.post(urlEqualTo("/primary"))
				.willReturn(aResponse().withStatus(500)));

		CompletionRequest request = new CompletionRequest();
		request.setPrompt("fail");

		assertThatThrownBy(() -> llmClientService.callPrimary(request))
				.isInstanceOf(LlmClientException.class);
	}

	@Test
	void callCandidateSafely_returnsResponseOnSuccess() {
		stubFor(WireMock.post(urlEqualTo("/candidate"))
				.willReturn(aResponse()
						.withStatus(200)
						.withBody("""
								{"output":"Echo: hello","model":"candidate"}
								""")));

		CompletionRequest request = new CompletionRequest();
		request.setPrompt("hello");

		assertThat(llmClientService.callCandidateSafely(request)).isPresent();
	}

	@Test
	void callCandidateSafely_returnsEmptyOnFailure() {
		stubFor(WireMock.post(urlEqualTo("/candidate"))
				.willReturn(aResponse().withStatus(503)));

		CompletionRequest request = new CompletionRequest();
		request.setPrompt("fail");

		assertThat(llmClientService.callCandidateSafely(request)).isEmpty();
	}

	private RestClient restClientWithTimeout(Duration readTimeout) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(2));
		factory.setReadTimeout(readTimeout);
		return RestClient.builder().requestFactory(factory).build();
	}

}
