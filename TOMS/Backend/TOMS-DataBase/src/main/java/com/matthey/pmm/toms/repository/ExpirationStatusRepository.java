package com.matthey.pmm.toms.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.ExpirationStatus;
import com.matthey.pmm.toms.model.Reference;

@Repository
public interface ExpirationStatusRepository extends CrudRepository<ExpirationStatus, Long> {
  List<ExpirationStatus> findByExpirationStatusName(Reference expirationStatusName);

  List<ExpirationStatus> findByOrderType(Reference orderType);  
  
  ExpirationStatus findByExpirationStatusNameAndOrderType(Reference expirationStatusName, Reference orderType);
}