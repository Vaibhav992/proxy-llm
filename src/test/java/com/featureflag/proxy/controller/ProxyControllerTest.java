package com.featureflag.proxy.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.featureflag.proxy.dto.CompletionResponse;
import com.featureflag.proxy.exception.LlmClientException;
import com.featureflag.proxy.service.ProxyService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProxyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProxyService proxyService;

	@Test
	void completions_returnsPrimaryResponse() throws Exception {
		when(proxyService.process(any())).thenReturn(CompletionResponse.builder()
				.output("Echo: hello")
				.model("primary")
				.build());

		mockMvc.perform(post("/api/completions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"prompt": "hello"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.output").value("Echo: hello"))
				.andExpect(jsonPath("$.model").value("primary"));
	}

	@Test
	void completions_returnsBadGatewayWhenPrimaryFails() throws Exception {
		when(proxyService.process(any()))
				.thenThrow(new LlmClientException("Upstream LLM returned HTTP 503"));

		mockMvc.perform(post("/api/completions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"prompt": "hello"}
						"""))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.status").value(502));
	}

}
