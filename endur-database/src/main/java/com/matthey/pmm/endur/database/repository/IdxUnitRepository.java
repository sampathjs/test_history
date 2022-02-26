package com.matthey.pmm.endur.database.repository;

import com.matthey.pmm.endur.database.model.Currency;
import com.matthey.pmm.endur.database.model.IdxUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface IdxUnitRepository extends JpaRepository<IdxUnit, Integer> {
}
