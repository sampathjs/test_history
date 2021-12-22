package com.matthey.pmm.toms.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.ReferenceType;

@Repository
@Transactional
public interface ReferenceTypeRepository extends JpaRepository<ReferenceType, Long> {

  List<ReferenceType> findByName(String name);
}