package com.featureflag.proxy.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

	private String primaryUrl;
	private String candidateUrl;
	private Duration primaryTimeout = Duration.ofSeconds(10);
	private Duration candidateTimeout = Duration.ofSeconds(30);
	private Mock mock = new Mock();

	@Data
	public static class Mock {

		private boolean enabled = false;
		private Duration primaryDelay = Duration.ZERO;
		private Duration candidateDelay = Duration.ZERO;
		private boolean candidateMismatch = false;

	}

}
