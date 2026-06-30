package com.featureflag.proxy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.featureflag.proxy.dto.MetricsResponse;
import com.featureflag.proxy.service.MetricsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MetricsController {

	private final MetricsService metricsService;

	@GetMapping("/metrics")
	public MetricsResponse metrics() {
		return metricsService.snapshot();
	}

}
