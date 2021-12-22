package com.matthey.pmm.toms.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.AttributeCalculation;

@Repository
@Transactional
public interface AttributeCalculationRepository extends JpaRepository<AttributeCalculation, Long> {

  List<AttributeCalculation> findByClassName(String className);

  List<AttributeCalculation> findByAttributeName(String attributeName);  
  
  AttributeCalculation findByClassNameAndAttributeName(String className, String attributeName);
}