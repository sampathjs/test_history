package com.matthey.pmm.toms.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.AttributeCalculation;

@Repository
public interface AttributeCalculationRepository extends CrudRepository<AttributeCalculation, Long> {

  List<AttributeCalculation> findByClassName(String className);

  List<AttributeCalculation> findByAttributeName(String attributeName);  
  
  AttributeCalculation findByClassNameAndAttributeName(String className, String attributeName);
}