package com.matthey.pmm.toms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.OrderVersionId;

@Repository
public interface LimitOrderRepository extends PagingAndSortingRepository<LimitOrder, OrderVersionId>, JpaSpecificationExecutor<LimitOrder> {
   List<LimitOrder> findByOrderId(long orderId); 
   
   @Query("SELECT o FROM LimitOrder o WHERE o.orderId = :orderId AND o.version = (SELECT MAX(lo2.version) FROM LimitOrder lo2 WHERE lo2.orderId = :orderId)") 
   Optional<LimitOrder> findLatestByOrderId(@Param("orderId") Long orderId);
}