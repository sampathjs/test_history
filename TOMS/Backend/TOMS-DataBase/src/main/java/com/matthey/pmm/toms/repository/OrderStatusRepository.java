package com.matthey.pmm.toms.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.OrderStatus;
import com.matthey.pmm.toms.model.Reference;

@Repository
public interface OrderStatusRepository extends CrudRepository<OrderStatus, Long> {
  List<OrderStatus> findByOrderStatusName(Reference ordernStatusName);

  List<OrderStatus> findByOrderType(Reference orderType);  
  
  OrderStatus findByOrderStatusNameAndOrderType(Reference orderStatusName, Reference orderType);
}