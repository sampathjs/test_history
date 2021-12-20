package com.matthey.pmm.toms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.Reference;

@Repository
public interface ReferenceRepository extends JpaRepository<Reference, Long> {

  List<Reference> findByDisplayName(String displayName);

  List<Reference> findByValue(String value);  

  Optional<Reference> findByValueAndTypeId(String value, long typeId);
  
  List<Reference> findByTypeId(Long value);  

  List<Reference> findByTypeIdIn(List<Long> ids);
  
  List<Reference> findByTypeName(String typeName);
}