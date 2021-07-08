package com.matthey.pmm.toms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.IndexEntity;

@Repository
public interface IndexRepository extends CrudRepository<IndexEntity, Long> {
  Optional<IndexEntity> findByIndexNameValue(String value);

  List<IndexEntity> findByCurrencyOneNameValue(String value);  
  
  List<IndexEntity> findByCurrencyTwoNameValue(String value);  
}