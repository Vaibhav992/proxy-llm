package com.featureflag.proxy.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.featureflag.proxy.dto.CompletionRequest;
import com.featureflag.proxy.dto.CompletionResponse;
import com.featureflag.proxy.service.ProxyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@Validated
@RequiredArgsConstructor
public class ProxyController {

	private final ProxyService proxyService;

	@PostMapping("/completions")
	public CompletionResponse completions(@Valid @RequestBody CompletionRequest request) {
		return proxyService.process(request);
	}

}
