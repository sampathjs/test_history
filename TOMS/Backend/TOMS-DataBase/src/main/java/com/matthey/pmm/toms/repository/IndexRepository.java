package com.matthey.pmm.toms.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.IndexEntity;
import com.matthey.pmm.toms.model.Reference;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<IndexEntity, Long> {
  Optional<IndexEntity> findByIndexNameValue(String value);

  Set<IndexEntity> findByCurrencyOneNameValue(String value);  
  
  Set<IndexEntity> findByCurrencyTwoNameValue(String value); 
  
  Set<IndexEntity> findByCurrencyOneNameOrCurrencyTwoName(Reference currencyOne, Reference currencyTwo);  
}