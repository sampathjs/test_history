package com.matthey.pmm.toms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;

@Repository
public interface PartyRepository extends JpaRepository<Party, Long> {
  List<Party> findByName(String name);

  List<Party> findByType(Reference type);  
  
  List<Party> findByLegalEntity(Party legalEntity);  

  List<Party> findByTypeAndLegalEntity(Reference type, Party legalEntity);  
}