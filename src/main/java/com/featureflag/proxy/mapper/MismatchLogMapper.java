package com.featureflag.proxy.mapper;

import java.time.Instant;
import java.util.UUID;

import com.featureflag.proxy.entity.MismatchLog;

public final class MismatchLogMapper {

	private MismatchLogMapper() {
	}

	public static MismatchLog toEntity(UUID requestId, String prompt, String primaryOutput, String candidateOutput) {
		return MismatchLog.builder()
				.requestId(requestId)
				.prompt(prompt)
				.primaryOutput(primaryOutput)
				.candidateOutput(candidateOutput)
				.createdAt(Instant.now())
				.build();
	}

}
