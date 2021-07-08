package com.matthey.pmm.toms.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.CreditCheck;

@Repository
public interface CreditCheckRepository extends CrudRepository<CreditCheck, Long> {
}