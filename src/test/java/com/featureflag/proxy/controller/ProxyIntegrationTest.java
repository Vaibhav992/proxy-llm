package com.featureflag.proxy.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProxyIntegrationTest {

	private static final WireMockServer WIRE_MOCK_SERVER = new WireMockServer(wireMockConfig().dynamicPort());

	@Autowired
	private MockMvc mockMvc;

	@DynamicPropertySource
	static void registerPrimaryUrl(DynamicPropertyRegistry registry) {
		if (!WIRE_MOCK_SERVER.isRunning()) {
			WIRE_MOCK_SERVER.start();
			WireMock.configureFor("localhost", WIRE_MOCK_SERVER.port());
		}
		registry.add("llm.primary-url", () -> WIRE_MOCK_SERVER.baseUrl() + "/primary");
		registry.add("llm.candidate-url", () -> WIRE_MOCK_SERVER.baseUrl() + "/candidate");
	}

	@AfterAll
	static void stopWireMock() {
		WIRE_MOCK_SERVER.stop();
	}

	@Test
	void completions_callsPrimaryAndReturnsFastResponse() throws Exception {
		stubFor(WireMock.post(urlEqualTo("/primary"))
				.withRequestBody(containing("hello"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("""
								{"output":"Echo: hello","model":"primary"}
								""")));

		stubFor(WireMock.post(urlEqualTo("/candidate"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("""
								{"output":"Echo: hello","model":"candidate"}
								""")));

		long start = System.currentTimeMillis();

		mockMvc.perform(post("/api/completions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"prompt": "hello"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.output").value("Echo: hello"))
				.andExpect(jsonPath("$.model").value("primary"));

		assertThat(System.currentTimeMillis() - start).isLessThan(2_000);
		verify(postRequestedFor(urlEqualTo("/primary")));
	}

}
