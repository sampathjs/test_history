package com.matthey.pmm.endur.database.repository;

import com.matthey.pmm.endur.database.model.AbTran;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AbTranRepository extends JpaRepository<AbTran, Integer> {
}
