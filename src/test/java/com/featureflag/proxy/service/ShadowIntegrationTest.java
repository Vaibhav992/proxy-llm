package com.featureflag.proxy.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.featureflag.proxy.repository.MismatchLogRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShadowIntegrationTest {

	private static final WireMockServer WIRE_MOCK_SERVER = new WireMockServer(wireMockConfig().dynamicPort());

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MismatchLogRepository mismatchLogRepository;

	@DynamicPropertySource
	static void registerUrls(DynamicPropertyRegistry registry) {
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

	@BeforeEach
	void cleanDatabase() {
		mismatchLogRepository.deleteAll();
		WIRE_MOCK_SERVER.resetAll();
	}

	@Test
	void primaryReturnsFastEvenWhenCandidateIsSlow() throws Exception {
		stubPrimary("hello", "Echo: hello");
		stubFor(WireMock.post(urlEqualTo("/candidate"))
				.willReturn(aResponse()
						.withStatus(200)
						.withFixedDelay(3_000)
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
				.andExpect(jsonPath("$.output").value("Echo: hello"));

		assertThat(System.currentTimeMillis() - start).isLessThan(1_000);
		verify(postRequestedFor(urlEqualTo("/primary")));
	}

	@Test
	void mismatchIsPersistedWhenOutputsDiffer() throws Exception {
		stubPrimary("hello", "Echo: hello");
		stubCandidate("hello", "Echo (candidate): hello");

		mockMvc.perform(post("/api/completions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"prompt": "hello"}
						"""))
				.andExpect(status().isOk());

		waitUntilMismatchCount(1);

		assertThat(mismatchLogRepository.count()).isEqualTo(1);
		assertThat(mismatchLogRepository.findAll().getFirst().getPrimaryOutput()).isEqualTo("Echo: hello");
		assertThat(mismatchLogRepository.findAll().getFirst().getCandidateOutput())
				.isEqualTo("Echo (candidate): hello");
	}

	@Test
	void matchIsNotPersistedWhenOutputsAreSame() throws Exception {
		stubPrimary("hello", "Echo: hello");
		stubCandidate("hello", "Echo: hello");

		mockMvc.perform(post("/api/completions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"prompt": "hello"}
						"""))
				.andExpect(status().isOk());

		waitForShadowCompletion();

		assertThat(mismatchLogRepository.count()).isZero();
	}

	private void stubPrimary(String prompt, String output) {
		stubFor(WireMock.post(urlEqualTo("/primary"))
				.withRequestBody(containing(prompt))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("""
								{"output":"%s","model":"primary"}
								""".formatted(output))));
	}

	private void stubCandidate(String prompt, String output) {
		stubFor(WireMock.post(urlEqualTo("/candidate"))
				.withRequestBody(containing(prompt))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody("""
								{"output":"%s","model":"candidate"}
								""".formatted(output))));
	}

	private void waitUntilMismatchCount(long expected) throws InterruptedException {
		for (int attempt = 0; attempt < 50; attempt++) {
			if (mismatchLogRepository.count() == expected) {
				return;
			}
			Thread.sleep(100);
		}
		throw new AssertionError("Expected " + expected + " mismatch logs but found " + mismatchLogRepository.count());
	}

	private void waitForShadowCompletion() throws InterruptedException {
		for (int attempt = 0; attempt < 50; attempt++) {
			try {
				verify(postRequestedFor(urlEqualTo("/candidate")));
				Thread.sleep(200);
				return;
			}
			catch (AssertionError ex) {
				Thread.sleep(100);
			}
		}
		throw new AssertionError("Shadow call to candidate did not complete");
	}

}
