package com.featureflag.proxy.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.featureflag.proxy.entity.MismatchLog;

public interface MismatchLogRepository extends JpaRepository<MismatchLog, Long> {

}
