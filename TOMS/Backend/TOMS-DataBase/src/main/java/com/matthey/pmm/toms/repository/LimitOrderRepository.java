package com.matthey.pmm.toms.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;

@Repository
public interface LimitOrderRepository extends JpaRepository<LimitOrder, OrderVersionId> {
   List<LimitOrder> findByOrderId(long orderId); 
   
   @Query("SELECT lo FROM LimitOrder lo WHERE lo.orderId = :orderId AND lo.version = (SELECT MAX(lo2.version) FROM LimitOrder lo2 WHERE lo2.orderId = :orderId)") 
   Optional<LimitOrder> findLatestByOrderId(@Param("orderId") Long orderId);
   
   @Query("SELECT lo FROM LimitOrder lo WHERE (:internalBu IS NULL OR lo.internalBu = :internalBu) "  
		   + " AND (:externalBu IS NULL OR lo.externalBu = :externalBu) AND (:minCreatedAt IS NULL OR lo.createdAt >= :minCreatedAt)" 
		   + " AND (:buySell IS NULL OR lo.buySell = :buySell)"
		   + " AND (:maxCreatedAt IS NULL OR lo.createdAt <= :maxCreatedAt) " 
		   + " AND ((:version IS NULL AND (lo.version = (SELECT MAX(lo2.version) FROM LimitOrder lo2 WHERE lo2.orderId = lo.orderId))) OR lo.version = :version)")
   List<LimitOrder> findByOrderIdAndOptionalParameters (@Param("internalBu") Party internalBu, @Param("externalBu") Party externalBu, 
		   @Param("buySell") Reference buySell,
		   @Param("minCreatedAt") Date minCreatedAt, @Param("maxCreatedAt") Date maxCreatedAt, 
		   @Param("version")Integer version);
   
}