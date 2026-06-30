package com.featureflag.proxy.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mismatch_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MismatchLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "request_id", nullable = false)
	private UUID requestId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String prompt;

	@Column(name = "primary_output", nullable = false, columnDefinition = "TEXT")
	private String primaryOutput;

	@Column(name = "candidate_output", nullable = false, columnDefinition = "TEXT")
	private String candidateOutput;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

}
