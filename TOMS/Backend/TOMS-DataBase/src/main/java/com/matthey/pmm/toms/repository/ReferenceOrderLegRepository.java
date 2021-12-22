package com.matthey.pmm.toms.repository;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.ReferenceOrderLeg;

@Repository
@Transactional
public interface ReferenceOrderLegRepository extends CrudRepository<ReferenceOrderLeg, Long> {
	
}