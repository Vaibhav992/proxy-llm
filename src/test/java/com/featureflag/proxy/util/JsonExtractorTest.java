package com.featureflag.proxy.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class JsonExtractorTest {

	private JsonExtractor jsonExtractor;

	@BeforeEach
	void setUp() {
		jsonExtractor = new JsonExtractor(JsonMapper.builder().build());
	}

	@Test
	void extractOutput_returnsOutputField() {
		String json = """
				{"output": "hello world", "model": "primary"}
				""";

		assertThat(jsonExtractor.extractOutput(json)).isEqualTo("hello world");
	}

	@Test
	void extractOutput_throwsForInvalidInput() {
		assertThatThrownBy(() -> jsonExtractor.extractOutput("not-json"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> jsonExtractor.extractOutput("{\"no_output\":\"x\"}"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void normalize_collapsesWhitespace() {
		assertThat(jsonExtractor.normalize("  hello   world  ")).isEqualTo("hello world");
		assertThat(jsonExtractor.normalize("hello\n\tworld")).isEqualTo("hello world");
	}

}
