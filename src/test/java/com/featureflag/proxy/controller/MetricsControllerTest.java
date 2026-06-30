package com.featureflag.proxy.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.featureflag.proxy.dto.MetricsResponse;
import com.featureflag.proxy.service.MetricsService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MetricsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MetricsService metricsService;

	@Test
	void metrics_returnsCurrentSnapshot() throws Exception {
		when(metricsService.snapshot()).thenReturn(MetricsResponse.builder()
				.totalComparisons(2)
				.matches(1)
				.matchRatePercent(50.0)
				.build());

		mockMvc.perform(get("/metrics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalComparisons").value(2))
				.andExpect(jsonPath("$.matches").value(1))
				.andExpect(jsonPath("$.matchRatePercent").value(50.0));
	}

}
