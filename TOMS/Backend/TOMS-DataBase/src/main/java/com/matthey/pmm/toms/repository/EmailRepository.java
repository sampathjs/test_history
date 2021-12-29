package com.matthey.pmm.toms.repository;

import java.util.Collection;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.Email;

@Repository
@Transactional
public interface EmailRepository extends JpaRepository<Email, Long> {
    @Query("SELECT e FROM Email e WHERE :orderId member e.associatedOrders") 
	Collection<Email> findEmailsBelongingToOrderId (@Param("orderId") long orderId);
}