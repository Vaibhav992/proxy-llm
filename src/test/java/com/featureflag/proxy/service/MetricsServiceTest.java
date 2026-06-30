package com.featureflag.proxy.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetricsServiceTest {

	private MetricsService metricsService;

	@BeforeEach
	void setUp() {
		metricsService = new MetricsService();
	}

	@Test
	void snapshot_returnsZeroWhenNoComparisons() {
		assertThat(metricsService.snapshot())
				.satisfies(metrics -> {
					assertThat(metrics.getTotalComparisons()).isZero();
					assertThat(metrics.getMatches()).isZero();
					assertThat(metrics.getMatchRatePercent()).isZero();
				});
	}

	@Test
	void snapshot_calculatesMatchRate() {
		metricsService.recordComparison(true);
		metricsService.recordComparison(true);
		metricsService.recordComparison(false);

		assertThat(metricsService.snapshot())
				.satisfies(metrics -> {
					assertThat(metrics.getTotalComparisons()).isEqualTo(3);
					assertThat(metrics.getMatches()).isEqualTo(2);
					assertThat(metrics.getMatchRatePercent()).isEqualTo(66.7);
				});
	}

}
