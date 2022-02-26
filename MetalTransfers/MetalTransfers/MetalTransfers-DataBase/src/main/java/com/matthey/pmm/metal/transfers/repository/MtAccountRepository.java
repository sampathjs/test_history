package com.matthey.pmm.metal.transfers.repository;

import com.matthey.pmm.endur.database.model.AbTran;
import com.matthey.pmm.endur.database.model.Account;
import com.matthey.pmm.endur.database.repository.AbTranRepository;
import com.matthey.pmm.endur.database.repository.AccountRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MtAccountRepository extends AccountRepository {

    List<Account> findByUserId(Integer userId);

    List<Account> findByHolderId(Integer holderId);

}
