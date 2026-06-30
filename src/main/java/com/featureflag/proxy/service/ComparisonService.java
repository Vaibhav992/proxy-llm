package com.featureflag.proxy.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.featureflag.proxy.util.JsonExtractor;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ComparisonService {

	private final JsonExtractor jsonExtractor;

	public boolean matches(String primaryResponse, String candidateResponse) {
		try {
			if (!StringUtils.hasText(primaryResponse) || !StringUtils.hasText(candidateResponse)) {
				return false;
			}

			String primaryOutput = jsonExtractor.normalize(jsonExtractor.extractOutput(primaryResponse));
			String candidateOutput = jsonExtractor.normalize(jsonExtractor.extractOutput(candidateResponse));
			return primaryOutput.equals(candidateOutput);
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

}
