package com.matthey.pmm.toms.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.OrderVersionId;

@Repository
public interface LimitOrderRepository extends CrudRepository<LimitOrder, OrderVersionId> {
   List<LimitOrder> findByOrderId(long orderId); 
}