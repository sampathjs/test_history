package com.matthey.pmm.toms.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.ReferenceOrder;

@Repository
public interface ReferenceOrderRepository extends PagingAndSortingRepository<ReferenceOrder, OrderVersionId> {
   List<ReferenceOrder> findByOrderId(long orderId);

   @Query("SELECT o FROM ReferenceOrder o WHERE o.orderId = :orderId AND o.version = (SELECT MAX(ro2.version) FROM ReferenceOrder ro2 WHERE ro2.orderId = :orderId)") 
   Optional<ReferenceOrder> findLatestByOrderId(@Param("orderId")long orderId);
      
   @Query("SELECT DISTINCT o FROM ReferenceOrder o \n"
		   + "  LEFT JOIN o.legs legs\n"
   		   + "WHERE \n"
		   + "     (COALESCE(:orderIds) IS NULL OR o.orderId IN (:orderIds))\n"
		   + " AND ((COALESCE(:versionIds) IS NULL AND (o.version = (SELECT MAX(ro2.version) FROM ReferenceOrder ro2 WHERE ro2.orderId = o.orderId))) OR o.version IN (:versionIds))\n"
   		   + " AND (COALESCE(:idInternalBu) IS NULL OR o.internalBu.id IN (:idInternalBu))\n"  
		   + " AND (COALESCE(:idExternalBu) IS NULL OR o.externalBu.id IN (:idExternalBu))\n"
   		   + " AND (COALESCE(:idExternalLe) IS NULL OR o.internalLe.id IN (:idInternalLe))\n"  
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
		   + " AND (:minCreatedAt IS NULL OR o.createdAt >= :minCreatedAt)\n" 
		   + " AND (:maxCreatedAt IS NULL OR o.createdAt <= :maxCreatedAt)\n"
		   + " AND (:minLastUpdate IS NULL OR o.lastUpdate >= :minLastUpdate)\n" 
		   + " AND (:maxLastUpdate IS NULL OR o.lastUpdate <= :maxLastUpdate)\n"
		   // all above: order fields, all below: reference order fields
		   + " AND (:minMetalPriceSpread IS NULL OR o.metalPriceSpread >= :minMetalPriceSpread)\n" 
		   + " AND (:maxMetalPriceSpread IS NULL OR o.metalPriceSpread <= :maxMetalPriceSpread)\n"
		   + " AND (:minFxPriceSpread IS NULL OR o.fxRateSpread >= :minFxPriceSpread)\n" 
		   + " AND (:maxFxPriceSpread IS NULL OR o.fxRateSpread <= :maxFxPriceSpread)\n"
		   + " AND (:minContangoBackwardation IS NULL OR o.contangoBackwardation >= :minContangoBackwardation)\n" 
		   + " AND (:maxContangoBackwardation IS NULL OR o.contangoBackwardation <= :maxContangoBackwardation)\n"
		   + " AND (COALESCE(:idContractType) IS NULL OR o.contractType.id IN (:idContractType))\n"
		   + " AND (COALESCE(:legIds) IS NULL OR legs.id IN (:legIds))"
		   + " AND (:minLegNotional IS NULL OR legs.notional >= :minLegNotional)\n" 
		   + " AND (:maxLegNotional IS NULL OR legs.notional >= :maxLegNotional)\n" 
		   + " AND (:minLegFixingStartDate IS NULL OR legs.fixingStartDate >= :minLegFixingStartDate)\n" 
		   + " AND (:maxLegFixingStartDate IS NULL OR legs.fixingStartDate >= :maxLegFixingStartDate)\n" 
		   + " AND (:minLegFixingEndDate IS NULL OR legs.fixingStartDate >= :minLegFixingEndDate)\n" 
		   + " AND (:maxLegFixingEndDate IS NULL OR legs.fixingEndDate >= :maxLegFixingEndDate)\n" 
		   + " AND (COALESCE(:idLegPaymentOffset) IS NULL OR legs.paymentOffset.id IN (:idLegPaymentOffset))"
		   + " AND (COALESCE(:idLegSettlementCurrencies) IS NULL OR legs.settleCurrency.id IN (:idLegSettlementCurrencies))"
		   + " AND (COALESCE(:idRefSources) IS NULL OR legs.refSource.id IN (:idRefSources))"
		   + " AND (COALESCE(:idLegFxIndexRefSource) IS NULL OR legs.fxIndexRefSource.id IN (:idLegFxIndexRefSource))"
		   )
   Page<ReferenceOrder> findByOrderIdAndOptionalParameters (@Param("orderIds") List<Long> orderIds, 
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
		   // all above: order fields, all below: reference order fields;
		   @Param("minMetalPriceSpread") Double minMetalPriceSpread, 
		   @Param("maxMetalPriceSpread") Double maxMetalPriceSpread,
		   @Param("minFxPriceSpread") Double minFxPriceSpread, 
		   @Param("maxFxPriceSpread") Double maxFxPriceSpread,
		   @Param("minContangoBackwardation") Double minContangoBackwardation, 
		   @Param("maxContangoBackwardation") Double maxContangoBackwardation,
		   @Param("idContractType") List<Long> idContractType,
		   @Param("legIds") List<Long> legIds,
		   @Param("minLegNotional") Double minLegNotional, 
		   @Param("maxLegNotional") Double maxLegNotional,
		   @Param("minLegFixingStartDate") Date minLegFixingStartDate, 
		   @Param("maxLegFixingStartDate") Date maxLegFixingStartDate,
		   @Param("minLegFixingEndDate") Date minLegFixingEndDate, 
		   @Param("maxLegFixingEndDate") Date maxLegFixingEndDate,
		   @Param("idLegPaymentOffset") List<Long> idLegPaymentOffset,
		   @Param("idLegSettlementCurrencies") List<Long> idLegSettlementCurrencies,
		   @Param("idRefSources") List<Long> idRefSources,	
		   @Param("idLegFxIndexRefSource") List<Long> idLegFxIndexRefSource,
		   Pageable pageable);
   // https://docs.spring.io/spring-data/rest/docs/2.0.0.M1/reference/html/paging-chapter.html
}