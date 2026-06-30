package com.featureflag.proxy.controller.mock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.featureflag.proxy.config.LlmProperties;
import com.featureflag.proxy.dto.CompletionRequest;
import com.featureflag.proxy.dto.CompletionResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mock")
@ConditionalOnProperty(prefix = "llm.mock", name = "enabled", havingValue = "true")
@Validated
@RequiredArgsConstructor
public class MockLlmController {

	private final LlmProperties llmProperties;

	@PostMapping("/primary")
	public CompletionResponse primary(@Valid @RequestBody CompletionRequest request) throws InterruptedException {
		applyDelay(llmProperties.getMock().getPrimaryDelay());
		return CompletionResponse.builder()
				.output("Echo: " + request.getPrompt())
				.model("primary")
				.build();
	}

	@PostMapping("/candidate")
	public CompletionResponse candidate(@Valid @RequestBody CompletionRequest request) throws InterruptedException {
		applyDelay(llmProperties.getMock().getCandidateDelay());
		String output = llmProperties.getMock().isCandidateMismatch()
				? "Echo (candidate): " + request.getPrompt()
				: "Echo: " + request.getPrompt();
		return CompletionResponse.builder()
				.output(output)
				.model("candidate")
				.build();
	}

	private void applyDelay(java.time.Duration delay) throws InterruptedException {
		if (delay != null && !delay.isZero() && !delay.isNegative()) {
			Thread.sleep(delay.toMillis());
		}
	}

}
