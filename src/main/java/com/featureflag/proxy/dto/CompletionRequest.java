package com.featureflag.proxy.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class CompletionRequest {

	@NotBlank(message = "prompt must not be blank")
	private String prompt;

}
