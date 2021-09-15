package com.matthey.pmm.toms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.ReferenceOrder;

@Repository
public interface ReferenceOrderRepository extends CrudRepository<ReferenceOrder, OrderVersionId> {
   List<ReferenceOrder> findByOrderId(long orderId);

   @Query("SELECT ro FROM ReferenceOrder ro WHERE ro.orderId = :orderId AND ro.version = (SELECT MAX(ro2.version) FROM ReferenceOrder ro2 WHERE ro2.orderId = :orderId)") 
   Optional<ReferenceOrder> findLatestByOrderId(@Param("orderId")long orderId);
}