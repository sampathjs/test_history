package com.matthey.pmm.toms.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;

@Repository
public interface LimitOrderRepository extends PagingAndSortingRepository<LimitOrder, OrderVersionId> {
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

   @Query("SELECT lo FROM LimitOrder lo \n"
   		   + "WHERE \n"
		   + "     (COALESCE(:orderIds) IS NULL OR lo.orderId IN (:orderIds))\n"
		   + " AND ((COALESCE(:versionIds) IS NULL AND (lo.version = (SELECT MAX(lo2.version) FROM LimitOrder lo2 WHERE lo2.orderId = lo.orderId))) OR lo.version IN (:versionIds))\n"
   		   + " AND (COALESCE(:idInternalBu) IS NULL OR lo.internalBu.id IN (:idInternalBu))\n"  
		   + " AND (COALESCE(:idExternalBu) IS NULL OR lo.externalBu.id IN (:idExternalBu))\n"
   		   + " AND (COALESCE(:idExternalLe) IS NULL OR lo.internalLe.id IN (:idInternalLe))\n"  
		   + " AND (COALESCE(:idInternalPfolio) IS NULL OR lo.intPortfolio.id IN (:idInternalPfolio))\n"
		   + " AND (COALESCE(:idExternalPfolio) IS NULL OR lo.extPortfolio.id IN (:idExternalPfolio))\n"
		   + " AND (COALESCE(:idBuySell) IS NULL OR lo.buySell.id IN (:idBuySell))\n"
		   + " AND (COALESCE(:idBaseCurrency) IS NULL OR lo.baseCurrency.id IN (:idBaseCurrency))\n"
		   + " AND (:minBaseQuantity IS NULL OR lo.baseQuantity >= :minBaseQuantity)\n" 
		   + " AND (:maxBaseQuantity IS NULL OR lo.baseQuantity <= :maxBaseQuantity)\n"
		   + " AND (COALESCE(:idBaseQuantityUnit) IS NULL OR lo.baseQuantityUnit.id IN (:idBaseQuantityUnit))\n"
		   + " AND (COALESCE(:idTermCurrency) IS NULL OR lo.termCurrency.id IN (:idTermCurrency))\n"
		   + " AND (:reference IS NULL OR lo.reference LIKE CONCAT('%',:reference,'%'))\n"
		   + " AND (COALESCE(:idMetalForm) IS NULL OR lo.metalForm.id IN (:idMetalForm))\n"
		   + " AND (COALESCE(:idMetalLocation) IS NULL OR lo.metalLocation.id IN (:idMetalLocation))\n"
		   + " AND (COALESCE(:idOrderStatus) IS NULL OR lo.orderStatus.id IN (:idOrderStatus))\n"		   
		   + " AND (:minCreatedAt IS NULL OR lo.createdAt >= :minCreatedAt)\n" 
		   + " AND (:maxCreatedAt IS NULL OR lo.createdAt <= :maxCreatedAt)\n"
		   + " AND (:minLastUpdate IS NULL OR lo.lastUpdate >= :minLastUpdate)\n" 
		   + " AND (:maxLastUpdate IS NULL OR lo.lastUpdate <= :maxLastUpdate)\n"
		   // all above: order fields, all below: limit order fields
		   + " AND (:minSettle IS NULL OR lo.settleDate >= :minSettle)\n" 
		   + " AND (:maxSettle IS NULL OR lo.settleDate <= :maxSettle)\n"
		   + " AND (:minStartConcrete IS NULL OR lo.startDateConcrete >= :minStartConcrete)\n" 
		   + " AND (:maxStartConcrete IS NULL OR lo.startDateConcrete <= :maxStartConcrete)\n"
		   + " AND (COALESCE(:idStartDateSymbolic) IS NULL OR lo.startDateSymbolic.id IN (:idStartDateSymbolic))\n"
		   + " AND (COALESCE(:idPriceType) IS NULL OR lo.priceType.id IN (:idPriceType))\n"
		   + " AND (COALESCE(:idYesNoPartFillable) IS NULL OR lo.yesNoPartFillable.id IN (:idYesNoPartFillable))\n"
		   + " AND (COALESCE(:idStopTriggerType) IS NULL OR lo.stopTriggerType.id IN (:idStopTriggerType))\n"
		   + " AND (COALESCE(:idCurrencyCrossMetal) IS NULL OR lo.currencyCrossMetal.id IN (:idCurrencyCrossMetal))\n" 
		   + " AND (COALESCE(:idValidationType) IS NULL OR lo.validationType.id IN (:idValidationType))\n"
		   + " AND (:minExpiry IS NULL OR lo.expiryDate >= :minExpiry)\n" 
		   + " AND (:maxExpiry IS NULL OR lo.expiryDate <= :maxExpiry)\n"
		   + " AND (:minExecutionLikelihood IS NULL OR lo.executionLikelihood >= :minExecutionLikelihood)\n" 
		   + " AND (:maxExecutionLikelihood IS NULL OR lo.executionLikelihood <= :maxExecutionLikelihood)\n"
		   + " AND (:minLimitPrice IS NULL OR lo.limitPrice >= :minLimitPrice)\n" 
		   + " AND (:maxLimitPrice IS NULL OR lo.limitPrice <= :maxLimitPrice)\n" 
		   )
   List<LimitOrder> findByOrderIdAndOptionalParameters(@Param("orderIds") List<Long> orderIds, 
		   @Param("versionIds") List<Integer> versionIds, @Param("idInternalBu") List<Long> idInternalBu,  
		   @Param("idExternalBu") List<Long> idExternalBu,  @Param("idInternalLe") List<Long> idInternalLe,  
		   @Param("idExternalLe") List<Long> idExternalLe,  @Param("idInternalPfolio") List<Long> idInternalPfolio, 
		   @Param("idExternalPfolio") List<Long> idExternalPfolio,  @Param("idBuySell") List<Long> idBuySell,  
		   @Param("idBaseCurrency") List<Long> idBaseCurrency, @Param("minBaseQuantity") Double minBaseQuantity, 
		   @Param("maxBaseQuantity") Double maxBaseQuantity, @Param("idBaseQuantityUnit") List<Long> idBaseQuantityUnit, 
		   @Param("idTermCurrency") List<Long> idTermCurrency, @Param("reference") String reference, 
		   @Param("idMetalForm") List<Long> idMetalForm, @Param("idMetalLocation") List<Long> idMetalLocation, 
		   @Param("idOrderStatus") List<Long> idOrderStatus,  @Param("minCreatedAt") Date minCreatedAt, 
		   @Param("maxCreatedAt") Date maxCreatedAt,  @Param("minLastUpdate") Date minLastUpdate, 
		   @Param("maxLastUpdate") Date maxLastUpdate,
		   // all above: order fields, all below: limit order fields
		   @Param("minSettle") Date minSettle, @Param("maxSettle") Date maxSettle,  
		   @Param("minStartConcrete") Date minStartConcrete, @Param("maxStartConcrete") Date maxStartConcrete,  
		   @Param("idStartDateSymbolic") List<Long> idStartDateSymbolic, @Param("idPriceType") List<Long> idPriceType, 
		   @Param("idYesNoPartFillable") List<Long> idYesNoPartFillable, @Param("idStopTriggerType")  List<Long> idStopTriggerType,  
		   @Param("idCurrencyCrossMetal") List<Long> idCurrencyCrossMetal, @Param("idValidationType") List<Long> idValidationType,  
		   @Param("minExpiry") Date minExpiry, @Param("maxExpiry") Date maxExpiry, 
		   @Param("minExecutionLikelihood") Double minExecutionLikelihood,
		   @Param("maxExecutionLikelihood") Double maxExecutionLikelihood,
		   @Param("minLimitPrice") Double minLimitPrice,
		   @Param("maxLimitPrice") Double maxLimitPrice,
		   Pageable pageable
		   );
}