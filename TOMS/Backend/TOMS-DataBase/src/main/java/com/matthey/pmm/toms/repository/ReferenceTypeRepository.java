package com.matthey.pmm.toms.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.ReferenceType;

@Repository
public interface ReferenceTypeRepository extends CrudRepository<ReferenceType, Long> {

  List<ReferenceType> findByName(String name);
}