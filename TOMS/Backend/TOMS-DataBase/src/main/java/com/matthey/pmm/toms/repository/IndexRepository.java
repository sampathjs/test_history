package com.matthey.pmm.toms.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.IndexEntity;
import com.matthey.pmm.toms.model.Reference;

@Repository
public interface IndexRepository extends CrudRepository<IndexEntity, Long> {
  Optional<IndexEntity> findByIndexNameValue(String value);

  Set<IndexEntity> findByCurrencyOneNameValue(String value);  
  
  Set<IndexEntity> findByCurrencyTwoNameValue(String value); 
  
  Set<IndexEntity> findByCurrencyOneNameOrCurrencyTwoName(Reference currencyOne, Reference currencyTwo);  
}