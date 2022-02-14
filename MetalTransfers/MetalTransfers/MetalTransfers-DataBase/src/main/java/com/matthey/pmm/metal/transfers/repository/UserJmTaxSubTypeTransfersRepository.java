package com.matthey.pmm.metal.transfers.repository;

import com.matthey.pmm.metal.transfers.model.UserJmTaxSubTypeTransfers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJmTaxSubTypeTransfersRepository extends JpaRepository<UserJmTaxSubTypeTransfers, String> {
}
