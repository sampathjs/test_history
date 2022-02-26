package com.matthey.pmm.metal.transfers.repository;

import com.matthey.pmm.endur.database.model.Personnel;
import com.matthey.pmm.metal.transfers.model.UserJmForm;
import com.matthey.pmm.metal.transfers.model.UserJmMtProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserJmMtProcessRepository extends JpaRepository<UserJmMtProcess, Integer> {

    @Query("select p from UserJmMtProcess p where p.personnel = :personnel and p.lastUpdatedTime >= :dateTime")
    List<UserJmMtProcess> findTransfersByUserAndSinceDateTime(@Param("personnel") Personnel personnel,
                                                           @Param("dateTime") LocalDateTime dateTime);
}
