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
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceOrder;

@Repository
public interface ReferenceOrderRepository extends PagingAndSortingRepository<ReferenceOrder, OrderVersionId> {
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
   
   @Query("SELECT ro FROM ReferenceOrder ro \n"
		   + "  LEFT JOIN ro.legs legs\n"
   		   + "WHERE \n"
		   + "     (COALESCE(:orderIds) IS NULL OR ro.orderId IN (:orderIds))\n"
		   + " AND ((COALESCE(:versionIds) IS NULL AND (ro.version = (SELECT MAX(ro2.version) FROM ReferenceOrder ro2 WHERE ro2.orderId = ro.orderId))) OR ro.version IN (:versionIds))\n"
   		   + " AND (COALESCE(:idInternalBu) IS NULL OR ro.internalBu.id IN (:idInternalBu))\n"  
		   + " AND (COALESCE(:idExternalBu) IS NULL OR ro.externalBu.id IN (:idExternalBu))\n"
   		   + " AND (COALESCE(:idExternalLe) IS NULL OR ro.internalLe.id IN (:idInternalLe))\n"  
		   + " AND (COALESCE(:idInternalPfolio) IS NULL OR ro.intPortfolio.id IN (:idInternalPfolio))\n"
		   + " AND (COALESCE(:idExternalPfolio) IS NULL OR ro.extPortfolio.id IN (:idExternalPfolio))\n"
		   + " AND (COALESCE(:idBuySell) IS NULL OR ro.buySell.id IN (:idBuySell))\n"
		   + " AND (COALESCE(:idBaseCurrency) IS NULL OR ro.baseCurrency.id IN (:idBaseCurrency))\n"
		   + " AND (:minBaseQuantity IS NULL OR ro.baseQuantity >= :minBaseQuantity)\n" 
		   + " AND (:maxBaseQuantity IS NULL OR ro.baseQuantity <= :maxBaseQuantity)\n"
		   + " AND (COALESCE(:idBaseQuantityUnit) IS NULL OR ro.baseQuantityUnit.id IN (:idBaseQuantityUnit))\n"
		   + " AND (COALESCE(:idTermCurrency) IS NULL OR ro.termCurrency.id IN (:idTermCurrency))\n"
		   + " AND (:reference IS NULL OR ro.reference LIKE CONCAT('%',:reference,'%'))\n"
		   + " AND (COALESCE(:idMetalForm) IS NULL OR ro.metalForm.id IN (:idMetalForm))\n"
		   + " AND (COALESCE(:idMetalLocation) IS NULL OR ro.metalLocation.id IN (:idMetalLocation))\n"
		   + " AND (COALESCE(:idOrderStatus) IS NULL OR ro.orderStatus.id IN (:idOrderStatus))\n"		   
		   + " AND (:minCreatedAt IS NULL OR ro.createdAt >= :minCreatedAt)\n" 
		   + " AND (:maxCreatedAt IS NULL OR ro.createdAt <= :maxCreatedAt)\n"
		   + " AND (:minLastUpdate IS NULL OR ro.lastUpdate >= :minLastUpdate)\n" 
		   + " AND (:maxLastUpdate IS NULL OR ro.lastUpdate <= :maxLastUpdate)\n"
		   // all above: order fields, all below: reference order fields
		   + " AND (:minMetalPriceSpread IS NULL OR ro.metalPriceSpread >= :minMetalPriceSpread)\n" 
		   + " AND (:maxMetalPriceSpread IS NULL OR ro.metalPriceSpread <= :maxMetalPriceSpread)\n"
		   + " AND (:minFxPriceSpread IS NULL OR ro.fxRateSpread >= :minFxPriceSpread)\n" 
		   + " AND (:maxFxPriceSpread IS NULL OR ro.fxRateSpread <= :maxFxPriceSpread)\n"
		   + " AND (:minContangoBackwardation IS NULL OR ro.contangoBackwardation >= :minContangoBackwardation)\n" 
		   + " AND (:maxContangoBackwardation IS NULL OR ro.contangoBackwardation <= :maxContangoBackwardation)\n"
		   + " AND (COALESCE(:idContractType) IS NULL OR ro.contractType.id IN (:idContractType))\n"
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