package com.matthey.pmm.toms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.ReferenceOrder;

@Repository
public interface RefrenceOrderRepository extends JpaRepository<ReferenceOrder, OrderVersionId> {
   List<ReferenceOrder> findByOrderId(long orderId); 
}