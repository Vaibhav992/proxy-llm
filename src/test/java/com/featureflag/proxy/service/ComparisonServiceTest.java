package com.featureflag.proxy.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.featureflag.proxy.util.JsonExtractor;

import tools.jackson.databind.json.JsonMapper;

class ComparisonServiceTest {

	private ComparisonService comparisonService;

	@BeforeEach
	void setUp() {
		comparisonService = new ComparisonService(new JsonExtractor(JsonMapper.builder().build()));
	}

	@Test
	void matches_whenOutputsAreIdentical() {
		String primary = """
				{"output": "same answer", "model": "primary"}
				""";
		String candidate = """
				{"output": "same answer", "model": "candidate"}
				""";

		assertThat(comparisonService.matches(primary, candidate)).isTrue();
	}

	@Test
	void matches_whenOutputsDifferOnlyByWhitespace() {
		String primary = """
				{"output": "hello   world", "model": "primary"}
				""";
		String candidate = """
				{"output": "hello world", "model": "candidate"}
				""";

		assertThat(comparisonService.matches(primary, candidate)).isTrue();
	}

	@Test
	void doesNotMatch_whenOutputsDiffer() {
		String primary = """
				{"output": "answer A", "model": "primary"}
				""";
		String candidate = """
				{"output": "answer B", "model": "candidate"}
				""";

		assertThat(comparisonService.matches(primary, candidate)).isFalse();
	}

	@Test
	void doesNotMatch_whenResponseIsInvalid() {
		assertThat(comparisonService.matches(null, "{\"output\": \"x\"}")).isFalse();
		assertThat(comparisonService.matches("not-json", "{\"output\": \"x\"}")).isFalse();
	}

}
