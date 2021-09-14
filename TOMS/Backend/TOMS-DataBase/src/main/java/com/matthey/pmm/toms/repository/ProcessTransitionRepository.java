package com.matthey.pmm.toms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.ProcessTransition;
import com.matthey.pmm.toms.model.Reference;

@Repository
public interface ProcessTransitionRepository extends CrudRepository<ProcessTransition, Long> {
  List<ProcessTransition> findByReferenceCategory(Reference referenceCategory);

  List<ProcessTransition> findByReferenceCategoryIdAndFromStatusId(long referenceCategoryId, long fromStatusId);   
  
  Optional<ProcessTransition> findByReferenceCategoryIdAndFromStatusIdAndToStatusId(long referenceCategoryId, long fromStatusId, long toStatusId);    

}