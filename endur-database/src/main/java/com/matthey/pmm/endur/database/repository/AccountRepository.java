package com.matthey.pmm.endur.database.repository;

import com.matthey.pmm.endur.database.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
}
