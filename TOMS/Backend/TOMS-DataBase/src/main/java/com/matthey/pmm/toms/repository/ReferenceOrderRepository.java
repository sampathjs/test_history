package com.matthey.pmm.toms.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceOrder;

@Repository
public interface ReferenceOrderRepository extends JpaRepository<ReferenceOrder, OrderVersionId> {
   List<ReferenceOrder> findByOrderId(long orderId);

   @Query("SELECT ro FROM ReferenceOrder ro WHERE ro.orderId = :orderId AND ro.version = (SELECT MAX(ro2.version) FROM ReferenceOrder ro2 WHERE ro2.orderId = :orderId)") 
   Optional<ReferenceOrder> findLatestByOrderId(@Param("orderId")long orderId);
   
   @Query("SELECT ro FROM ReferenceOrder ro WHERE (:internalBu IS NULL OR ro.internalBu = :internalBu) "  
		   + " AND (:externalBu IS NULL OR ro.externalBu = :externalBu) AND (:minCreatedAt IS NULL OR ro.createdAt >= :minCreatedAt)" 
		   + " AND (:buySell IS NULL OR ro.buySell = :buySell)"
		   + " AND (:maxCreatedAt IS NULL OR ro.createdAt <= :maxCreatedAt) " 
		   + " AND ((:version IS NULL AND (ro.version = (SELECT MAX(ro2.version) FROM ReferenceOrder ro2 WHERE ro2.orderId = ro.orderId))) OR ro.version = :version)")
   List<ReferenceOrder> findByOrderIdAndOptionalParameters (@Param("internalBu") Party internalBu, @Param("externalBu") Party externalBu, 
		   @Param("buySell") Reference buySell,
		   @Param("minCreatedAt") Date minCreatedAt, @Param("maxCreatedAt") Date maxCreatedAt, 
		   @Param("version")Integer version);
}