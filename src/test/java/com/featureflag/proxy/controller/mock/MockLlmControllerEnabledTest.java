package com.featureflag.proxy.controller.mock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "llm.mock.enabled=true")
class MockLlmControllerEnabledTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void primaryMock_returnsExpectedResponse() throws Exception {
		mockMvc.perform(post("/mock/primary")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"prompt": "hello"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.output").value("Echo: hello"))
				.andExpect(jsonPath("$.model").value("primary"));
	}

	@Test
	void mock_returnsBadRequestWhenPromptMissing() throws Exception {
		mockMvc.perform(post("/mock/primary")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

}
