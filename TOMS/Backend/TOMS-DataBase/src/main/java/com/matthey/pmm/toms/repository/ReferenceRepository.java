package com.matthey.pmm.toms.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.Reference;

@Repository
@Transactional
public interface ReferenceRepository extends JpaRepository<Reference, Long> {

  @Cacheable({"ReferenceRepository.findByDisplayName"})
  List<Reference> findByDisplayName(String displayName);

  @Cacheable({"ReferenceRepository.findByValue"})
  List<Reference> findByValue(String value);  

  @Cacheable({"ReferenceRepository.findByValueAndTypeId"})
  Optional<Reference> findByValueAndTypeId(String value, long typeId);
  
  @Cacheable({"ReferenceRepository.findByTypeId"})
  List<Reference> findByTypeId(Long value);  

  @Cacheable({"ReferenceRepository.findByTypeIdIn"})
  List<Reference> findByTypeIdIn(List<Long> ids);
  
  @Cacheable({"ReferenceRepository.findByTypeName"})
  List<Reference> findByTypeName(String typeName);
}