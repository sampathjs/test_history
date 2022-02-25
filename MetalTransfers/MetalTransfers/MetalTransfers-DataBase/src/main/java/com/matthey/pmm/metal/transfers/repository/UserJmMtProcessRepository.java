package com.matthey.pmm.metal.transfers.repository;

import com.matthey.pmm.metal.transfers.model.UserJmForm;
import com.matthey.pmm.metal.transfers.model.UserJmMtProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJmMtProcessRepository extends JpaRepository<UserJmMtProcess, Integer> {
}
