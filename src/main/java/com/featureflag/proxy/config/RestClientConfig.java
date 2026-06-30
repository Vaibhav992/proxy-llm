package com.featureflag.proxy.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	public static final String PRIMARY_REST_CLIENT = "primaryRestClient";
	public static final String CANDIDATE_REST_CLIENT = "candidateRestClient";

	@Bean(PRIMARY_REST_CLIENT)
	RestClient primaryRestClient(LlmProperties llmProperties) {
		return restClientWithTimeout(llmProperties.getPrimaryTimeout());
	}

	@Bean(CANDIDATE_REST_CLIENT)
	RestClient candidateRestClient(LlmProperties llmProperties) {
		return restClientWithTimeout(llmProperties.getCandidateTimeout());
	}

	private RestClient restClientWithTimeout(Duration readTimeout) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(5));
		factory.setReadTimeout(readTimeout);
		return RestClient.builder().requestFactory(factory).build();
	}

}
