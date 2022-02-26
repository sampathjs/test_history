package com.matthey.pmm.metal.transfers.repository;

import com.matthey.pmm.endur.database.model.Currency;
import com.matthey.pmm.endur.database.repository.CurrencyRepository;
import com.matthey.pmm.endur.database.repository.PartyRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MtCurrencyRepository extends CurrencyRepository {

    @Query("select c from Currency c where c.preciousMetal = 1")
    List<Currency> findAllPreciousMetals();
}
