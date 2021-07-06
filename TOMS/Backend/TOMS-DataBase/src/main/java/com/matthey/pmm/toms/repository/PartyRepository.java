package com.matthey.pmm.toms.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;

@Repository
public interface PartyRepository extends CrudRepository<Party, Long> {
  List<Party> findByName(String name);

  List<Party> findByType(Reference type);  
  
  Party findByLegalEntity(Party legalEntity);
}