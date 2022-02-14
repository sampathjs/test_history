package com.matthey.pmm.metal.transfers.repository;

import com.matthey.pmm.metal.transfers.model.UserJmForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJmFormRepository extends JpaRepository<UserJmForm, String> {
}
