package com.matthey.pmm.endur.database.repository;

import com.matthey.pmm.endur.database.model.Currency;
import com.matthey.pmm.endur.database.model.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Integer> {
}
