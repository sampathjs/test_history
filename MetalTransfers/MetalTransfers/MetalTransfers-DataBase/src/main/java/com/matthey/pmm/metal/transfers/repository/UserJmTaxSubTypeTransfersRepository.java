package com.matthey.pmm.metal.transfers.repository;

import com.matthey.pmm.metal.transfers.model.UserJmNegativeThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJmNegativeThresholdRepository extends JpaRepository<UserJmNegativeThreshold, Float> {
}
