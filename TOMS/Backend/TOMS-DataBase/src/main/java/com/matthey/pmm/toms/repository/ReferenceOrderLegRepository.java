package com.matthey.pmm.toms.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.ReferenceOrderLeg;

@Repository
public interface ReferenceOrderLegRepository extends CrudRepository<ReferenceOrderLeg, Long> {

}