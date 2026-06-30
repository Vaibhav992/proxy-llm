package com.featureflag.proxy.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestErrorController {

	@GetMapping("/test/error")
	void throwApiException() {
		throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "bad request");
	}

}
