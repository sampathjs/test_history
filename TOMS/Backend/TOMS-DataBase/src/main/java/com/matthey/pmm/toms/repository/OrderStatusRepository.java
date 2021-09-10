package com.matthey.pmm.toms.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.OrderStatus;
import com.matthey.pmm.toms.model.Reference;

@Repository
public interface OrderStatusRepository extends CrudRepository<OrderStatus, Long> {
  List<OrderStatus> findByOrderStatusName(Reference ordernStatusName);

  List<OrderStatus> findByOrderType(Reference orderType);  
  
  List<OrderStatus> findByOrderStatusNameAndOrderType(Reference orderStatusName, Reference orderType);
  
  Set<OrderStatus> findByIdAndOrderStatusName(Long id, Reference orderTypeName);
  
}