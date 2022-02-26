package com.matthey.pmm.metal.transfers.repository;

import com.matthey.pmm.endur.database.model.AbTran;
import com.matthey.pmm.endur.database.model.IdxUnit;
import com.matthey.pmm.endur.database.repository.AbTranRepository;
import com.matthey.pmm.endur.database.repository.IdxUnitRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MtIdxUnitRepository extends IdxUnitRepository {

    @Query("select iu from IdxUnit iu where lower(iu.unitLabel) in ('kgs', 'toz', 'gms', 'mgs')")
    List<IdxUnit> findPreciousMetalsIdxUnits();
}
