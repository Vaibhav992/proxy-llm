package com.featureflag.proxy.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {

	private int status;
	private String message;
	private String path;
	@Builder.Default
	private Instant timestamp = Instant.now();

}
