package com.matthey.pmm.toms.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.Order;
import com.matthey.pmm.toms.model.OrderVersionId;

@Repository
@Transactional
public interface OrderRepository extends PagingAndSortingRepository<Order, OrderVersionId>, JpaSpecificationExecutor<Order>{
   List<Order> findByOrderId(long orderId); 
   
   @Query("SELECT o FROM Order o WHERE o.orderId = :orderId AND o.version = (SELECT MAX(o2.version) FROM Order o2 WHERE o2.orderId = :orderId)") 
   Optional<Order> findLatestByOrderId(@Param("orderId") Long orderId);

   @Query("SELECT o FROM Order o WHERE o.version = (SELECT MAX(o2.version) FROM Order o2 WHERE o2.orderId = o.orderId)") 
   List<Order> findLatest();

}