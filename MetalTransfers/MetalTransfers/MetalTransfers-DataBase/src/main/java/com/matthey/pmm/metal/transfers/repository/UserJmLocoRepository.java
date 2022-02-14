package com.matthey.pmm.metal.transfers.repository;

import com.matthey.pmm.metal.transfers.model.UserJmLoco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJmLocoRepository extends JpaRepository<UserJmLoco, Integer> {
}
