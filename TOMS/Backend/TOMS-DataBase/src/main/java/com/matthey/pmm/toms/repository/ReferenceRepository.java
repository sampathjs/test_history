package com.matthey.pmm.toms.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.Reference;

@Repository
public interface ReferenceRepository extends CrudRepository<Reference, Long> {

  List<Reference> findByDisplayName(String displayName);

  List<Reference> findByValue(String value);  
  
  List<Reference> findByTypeId(Long value);  

  List<Reference> findByTypeName(String typeName);
}