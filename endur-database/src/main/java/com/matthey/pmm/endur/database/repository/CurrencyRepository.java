package com.matthey.pmm.endur.database.repository;

import com.matthey.pmm.endur.database.model.Currency;
import com.matthey.pmm.endur.database.model.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Repository;

@NoRepositoryBean
public interface CurrencyRepository extends JpaRepository<Currency, Integer> {
}
