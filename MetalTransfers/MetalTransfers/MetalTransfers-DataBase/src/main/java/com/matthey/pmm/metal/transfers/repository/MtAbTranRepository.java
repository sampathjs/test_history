package com.matthey.pmm.metal.transfers.repository;

import com.matthey.pmm.endur.database.model.AbTran;
import com.matthey.pmm.endur.database.repository.AbTranRepository;
import com.matthey.pmm.endur.database.repository.CurrencyRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MtAbTranRepository extends AbTranRepository {

    @Query("select t from AbTran t where t.tranType = 39 and t.insType = 66000 and t.toolset = 8 and t.inputDate >= " +
            ":dateTime")
    List<AbTran> findMtDealsSinceDateTime( @Param("dateTime") LocalDateTime dateTime);
}
