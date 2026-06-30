package com.featureflag.proxy.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JsonExtractor {

	private final JsonMapper jsonMapper;

	public String extractOutput(String jsonBody) {
		if (!StringUtils.hasText(jsonBody)) {
			throw new IllegalArgumentException("Response body must not be blank");
		}

		try {
			JsonNode root = jsonMapper.readTree(jsonBody);
			JsonNode outputNode = root.get("output");
			if (outputNode == null || outputNode.isNull() || !outputNode.isTextual()) {
				throw new IllegalArgumentException("Response JSON must contain a text 'output' field");
			}
			return outputNode.asText();
		}
		catch (IllegalArgumentException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Response body is not valid JSON", ex);
		}
	}

	public String normalize(String value) {
		if (value == null) {
			return "";
		}
		return value.trim().replaceAll("\\s+", " ");
	}

}
