package com.matthey.pmm.toms.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.Order;
import com.matthey.pmm.toms.model.OrderVersionId;

@Repository
public interface OrderRepository extends PagingAndSortingRepository<Order, OrderVersionId>, JpaSpecificationExecutor<Order>{
   List<Order> findByOrderId(long orderId); 
   
   @Query("SELECT o FROM Order o WHERE o.orderId = :orderId AND o.version = (SELECT MAX(lo2.version) FROM LimitOrder lo2 WHERE lo2.orderId = :orderId)") 
   Optional<Order> findLatestByOrderId(@Param("orderId") Long orderId);
   

   @Query("SELECT o FROM Order o\n"
   		   + "WHERE \n"
		   + "     (COALESCE(:orderIds) IS NULL OR o.orderId IN (:orderIds))\n"
		   + " AND ((COALESCE(:versionIds) IS NULL AND (o.version = (SELECT MAX(o2.version) FROM Order o2 WHERE o2.orderId = o.orderId))) OR o.version IN (:versionIds))\n"
   		   + " AND (COALESCE(:orderTypeId) IS NULL OR o.orderTypeName.id IN (:orderTypeId))\n"  
   		   + " AND (COALESCE(:idInternalBu) IS NULL OR o.internalBu.id IN (:idInternalBu))\n"  
   		   + " AND (COALESCE(:idInternalLe) IS NULL OR o.internalLe.id IN (:idInternalLe))\n"  
		   + " AND (COALESCE(:idExternalBu) IS NULL OR o.externalBu.id IN (:idExternalBu))\n"
   		   + " AND (COALESCE(:idExternalLe) IS NULL OR o.externalLe.id IN (:idExternalLe))\n"  
		   + " AND (COALESCE(:idInternalPfolio) IS NULL OR o.intPortfolio.id IN (:idInternalPfolio))\n"
		   + " AND (COALESCE(:idExternalPfolio) IS NULL OR o.extPortfolio.id IN (:idExternalPfolio))\n"
		   + " AND (COALESCE(:idBuySell) IS NULL OR o.buySell.id IN (:idBuySell))\n"
		   + " AND (COALESCE(:idBaseCurrency) IS NULL OR o.baseCurrency.id IN (:idBaseCurrency))\n"
		   + " AND (:minBaseQuantity IS NULL OR o.baseQuantity >= :minBaseQuantity)\n" 
		   + " AND (:maxBaseQuantity IS NULL OR o.baseQuantity <= :maxBaseQuantity)\n"
		   + " AND (COALESCE(:idBaseQuantityUnit) IS NULL OR o.baseQuantityUnit.id IN (:idBaseQuantityUnit))\n"
		   + " AND (COALESCE(:idTermCurrency) IS NULL OR o.termCurrency.id IN (:idTermCurrency))\n"
		   + " AND (:reference IS NULL OR o.reference LIKE CONCAT('%',:reference,'%'))\n"
		   + " AND (COALESCE(:idMetalForm) IS NULL OR o.metalForm.id IN (:idMetalForm))\n"
		   + " AND (COALESCE(:idMetalLocation) IS NULL OR o.metalLocation.id IN (:idMetalLocation))\n"
		   + " AND (COALESCE(:idOrderStatus) IS NULL OR o.orderStatus.id IN (:idOrderStatus))\n"
		   + " AND (COALESCE(:idCreatedByUser) IS NULL OR o.createdByUser.id IN (:idCreatedByUser))\n"
		   + " AND (:minCreatedAt IS NULL OR o.createdAt >= :minCreatedAt)\n" 
		   + " AND (:maxCreatedAt IS NULL OR o.createdAt <= :maxCreatedAt)\n"
		   + " AND (COALESCE(:idUpdatedByUser) IS NULL OR o.updatedByUser.id IN (:idUpdatedByUser))\n"
		   + " AND (:minLastUpdate IS NULL OR o.lastUpdate >= :minLastUpdate)\n" 
		   + " AND (:maxLastUpdate IS NULL OR o.lastUpdate <= :maxLastUpdate)\n"
		   + " AND (:minFillPercentage IS NULL OR o.fillPercentage >= :minFillPercentage)\n"
		   + " AND (:maxFillPercentage IS NULL OR o.fillPercentage <= :maxFillPercentage)\n"
		   + " AND (COALESCE(:idContractType) IS NULL OR o.contractType.id IN (:idContractType))\n"
		   + " AND (COALESCE(:idTicker) IS NULL OR o.ticker.id IN (:idTicker))\n"
		   )
   Page<Order> findByOrderIdAndOptionalParameters(@Param("orderTypeId") List<Long> orderTypeId,
		   @Param("orderIds") List<Long> orderIds, 
		   @Param("versionIds") List<Integer> versionIds, @Param("idInternalBu") List<Long> idInternalBu,  
		   @Param("idExternalBu") List<Long> idExternalBu,  @Param("idInternalLe") List<Long> idInternalLe,  
		   @Param("idExternalLe") List<Long> idExternalLe,  @Param("idInternalPfolio") List<Long> idInternalPfolio, 
		   @Param("idExternalPfolio") List<Long> idExternalPfolio,  @Param("idBuySell") List<Long> idBuySell,  
		   @Param("idBaseCurrency") List<Long> idBaseCurrency, @Param("minBaseQuantity") Double minBaseQuantity, 
		   @Param("maxBaseQuantity") Double maxBaseQuantity, @Param("idBaseQuantityUnit") List<Long> idBaseQuantityUnit, 
		   @Param("idTermCurrency") List<Long> idTermCurrency, @Param("reference") String reference, 
		   @Param("idMetalForm") List<Long> idMetalForm, @Param("idMetalLocation") List<Long> idMetalLocation, 
		   @Param("idOrderStatus") List<Long> idOrderStatus, @Param("idCreatedByUser") List<Long> idCreatedByUser,		   
		   @Param("minCreatedAt") Date minCreatedAt, @Param("maxCreatedAt") Date maxCreatedAt,  
		   @Param("idUpdatedByUser") List<Long> idUpdatedByUser,
		   @Param("minLastUpdate") Date minLastUpdate, @Param("maxLastUpdate") Date maxLastUpdate,
		   @Param("minFillPercentage") Double minFillPercentage,
		   @Param("maxFillPercentage") Double maxFillPercentage,
		   @Param("idContractType") List<Long> idContractType,
		   @Param("idTicker") List<Long> idTicker,
		   Pageable pageable
		   );
}