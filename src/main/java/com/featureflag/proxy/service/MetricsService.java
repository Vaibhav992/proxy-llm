package com.featureflag.proxy.service;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.featureflag.proxy.dto.MetricsResponse;

@Service
public class MetricsService {

	private final AtomicLong totalComparisons = new AtomicLong();
	private final AtomicLong matches = new AtomicLong();

	public void recordComparison(boolean matched) {
		totalComparisons.incrementAndGet();
		if (matched) {
			matches.incrementAndGet();
		}
	}

	public MetricsResponse snapshot() {
		long total = totalComparisons.get();
		long matchCount = matches.get();
		double matchRate = total == 0 ? 0.0 : (matchCount * 100.0) / total;

		return MetricsResponse.builder()
				.totalComparisons(total)
				.matches(matchCount)
				.matchRatePercent(Math.round(matchRate * 10.0) / 10.0)
				.build();
	}

}
