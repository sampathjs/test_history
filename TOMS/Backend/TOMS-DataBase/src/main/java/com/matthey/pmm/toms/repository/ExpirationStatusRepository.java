package com.matthey.pmm.toms.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.ExpirationStatus;
import com.matthey.pmm.toms.model.Reference;

@Repository
@Transactional
public interface ExpirationStatusRepository extends JpaRepository<ExpirationStatus, Long> {
  List<ExpirationStatus> findByExpirationStatusName(Reference expirationStatusName);

  List<ExpirationStatus> findByOrderType(Reference orderType);  
  
  List<ExpirationStatus> findByExpirationStatusNameAndOrderType(Reference expirationStatusName, Reference orderType);
}