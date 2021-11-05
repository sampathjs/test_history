package com.matthey.pmm.toms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.CreditCheck;

@Repository
public interface CreditCheckRepository extends JpaRepository<CreditCheck, Long> {
}