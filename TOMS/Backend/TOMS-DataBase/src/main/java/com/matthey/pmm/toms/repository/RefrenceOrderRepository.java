package com.matthey.pmm.toms.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.ReferenceOrder;

@Repository
public interface RefrenceOrderRepository extends CrudRepository<ReferenceOrder, OrderVersionId> {
   List<ReferenceOrder> findByOrderId(long orderId); 
}