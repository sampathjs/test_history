package com.matthey.pmm.toms.model;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;

/**
 * Limit Order class.
 * 
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(name = "limit_order", 
    indexes = { @Index(name = "i_limit_order_order_id", columnList = "order_id,version", unique = true)})
@PrimaryKeyJoinColumns(value = {
        @PrimaryKeyJoinColumn( name = "order_id", referencedColumnName = "order_id" ),
        @PrimaryKeyJoinColumn( name = "version", referencedColumnName = "version" )
    })
public class LimitOrder extends Order{
	public void setOrderId (long id) {
		super.setOrderId(id);
	}
	
	@Column(name="order_id", nullable = false)
	public long getOrderId () {
		return super.getOrderId();
	}
	
	@Override
	@Column(name="version", nullable = false)
	public int getVersion () {
		return super.getVersion();
	}
	
	@Override
	public void setVersion (int version) {
		super.setVersion(version);
	}
		
	@Column(name = "settle_date", nullable = true)
	@Temporal(TemporalType.DATE)
	private Date settleDate;
	
	// Add Start Date (fixed one with concrete date and symbolic one pick list)
	@Column(name = "start_date_concrete", nullable=true)
	@Temporal(TemporalType.DATE)
	private Date startDateConcrete;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="start_date_symbolic_reference_id", nullable = true)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.SYMBOLIC_DATE)
	private Reference startDateSymbolic; 	
		
	@Column(name="limit_price", nullable = true)
	private Double limitPrice;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="price_type_reference_id", nullable = true)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.PRICE_TYPE)
	private Reference priceType;	
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="part_fillable_reference_id", nullable = true)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.YES_NO)
	private Reference yesNoPartFillable;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="stop_trigger_type_reference_id", nullable = true)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.STOP_TRIGGER_TYPE)
	private Reference stopTriggerType; 
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="currency_cross_metal_reference_id", nullable = true)
	@ReferenceTypeDesignator(referenceTypes = { DefaultReferenceType.CCY_CURRENCY, DefaultReferenceType.CCY_METAL})
	private Reference currencyCrossMetal;
		
	@Column(name="execution_likelihood", nullable = true)
	private Double executionLikelihood;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="validation_type_reference_id", nullable = false)
	@ReferenceTypeDesignator(referenceTypes = { DefaultReferenceType.VALIDATION_TYPE})
	private Reference validationType;

	@Column(name = "expiry_date", nullable=true)
	@Temporal(TemporalType.DATE)
	private Date expiryDate;
	
	
	// Add Validation Type, pick list ("Good Til Cancelled", "Expiry Date")
	// Add expiry date, optional
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected LimitOrder() {
	}
	
	public LimitOrder(final int version, final Party internalBu, final Party externalBu, 
			final Party internalLe, final Party externalLe, final Reference intPortfolio,
			final Reference extPortfolio, final Reference buySell, final Reference baseCurrency,
			final Double baseQuantity, final Reference baseQuantityUnit, 
			final Reference termCurrency, 
			final String reference, final Reference metalForm, final Reference metalLocation,			
			final OrderStatus orderStatus, final Date createdAt, 
			final User createdByUser, final Date lastUpdate,
			final User updatedByUser, final double fillPercentage, final List<OrderComment> orderComments,
			final List<Fill> fills, final List<CreditCheck> creditChecks, // << order fields
			final Date settleDate, final Date startDateConcrete, final Reference startDateSymbolic,
			final Double limitPrice, final Reference priceType,
			final Reference stopTriggerType, final Reference currencyCrossMetal,
			final Reference yesNoPartFillable, final Reference validationType,
			final Date expiryDate, final Double executionLikelihood) {
		super(internalBu, externalBu, internalLe, externalLe, intPortfolio,
				extPortfolio, buySell, baseCurrency, baseQuantity, baseQuantityUnit,
				termCurrency, reference, metalForm, metalLocation, orderStatus, createdAt,
				createdByUser, lastUpdate, updatedByUser, fillPercentage, orderComments, 
				fills, creditChecks);
		this.settleDate = settleDate;
		this.startDateConcrete = startDateConcrete;
		this.startDateSymbolic = startDateSymbolic;
		this.limitPrice = limitPrice;
		this.priceType = priceType;
		this.stopTriggerType = stopTriggerType;
		this.currencyCrossMetal = currencyCrossMetal;
		this.executionLikelihood = executionLikelihood;
		this.yesNoPartFillable = yesNoPartFillable;
		this.validationType = validationType;
		this.expiryDate = expiryDate;
	}
	
	public LimitOrder(LimitOrder toClone) {
		super(toClone);
		this.settleDate = toClone.settleDate;
		this.startDateConcrete = toClone.startDateConcrete;
		this.startDateSymbolic = toClone.startDateSymbolic;
		this.limitPrice = toClone.limitPrice;
		this.priceType = toClone.priceType;
		this.stopTriggerType = toClone.stopTriggerType;
		this.currencyCrossMetal = toClone.currencyCrossMetal;
		this.executionLikelihood = toClone.executionLikelihood;
		this.yesNoPartFillable = toClone.yesNoPartFillable;
		this.validationType = toClone.validationType;
		this.expiryDate = toClone.expiryDate;
	}

	public Date getSettleDate() {
		return settleDate;
	}

	public void setSettleDate(Date settleDate) {
		this.settleDate = settleDate;
	}

	public Double getLimitPrice() {
		return limitPrice;
	}

	public void setLimitPrice(Double limitPrice) {
		this.limitPrice = limitPrice;
	}

	public Reference getPriceType() {
		return priceType;
	}

	public void setPriceType(Reference priceType) {
		this.priceType = priceType;
	}

	public Reference getYesNoPartFillable() {
		return yesNoPartFillable;
	}

	public void setYesNoPartFillable(Reference yesNoPartFillable) {
		this.yesNoPartFillable = yesNoPartFillable;
	}

	public Reference getStopTriggerType() {
		return stopTriggerType;
	}

	public void setStopTriggerType(Reference stopTriggerType) {
		this.stopTriggerType = stopTriggerType;
	}

	public Reference getCurrencyCrossMetal() {
		return currencyCrossMetal;
	}

	public void setCurrencyCrossMetal(Reference currencyCrossMetal) {
		this.currencyCrossMetal = currencyCrossMetal;
	}

	public Double getExecutionLikelihood() {
		return executionLikelihood;
	}

	public void setExecutionLikelihood(Double executionLikelihood) {
		this.executionLikelihood = executionLikelihood;
	}

	public Date getStartDateConcrete() {
		return startDateConcrete;
	}

	public void setStartDateConcrete(Date startDateConcrete) {
		this.startDateConcrete = startDateConcrete;
	}

	public Reference getStartDateSymbolic() {
		return startDateSymbolic;
	}

	public void setStartDateSymbolic(Reference startDateSymbolic) {
		this.startDateSymbolic = startDateSymbolic;
	}

	public Reference getValidationType() {
		return validationType;
	}

	public void setValidationType(Reference validationType) {
		this.validationType = validationType;
	}

	public Date getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(Date expiryDate) {
		this.expiryDate = expiryDate;
	}

	@Override
	public String toString() {
		return "LimitOrder [orderId=" + getOrderId() + ", version=" + getVersion() + ", settleDate=" + settleDate
				+ ", expiryDate=" + expiryDate + ", startDateConcrete=" + startDateConcrete
				+ ", startDateSymbolic=" + startDateSymbolic
				+ ", limitPrice=" + limitPrice + ", priceType=" + priceType 
				+ ", yesNoPartFillable=" + yesNoPartFillable
				+ ", validationType=" + validationType
				+ ", stopTriggerType=" + stopTriggerType + ", currencyCrossMetal=" + currencyCrossMetal
				+ ", executionLikelihood=" + executionLikelihood + ", getInternalBu()=" + getInternalBu() + ", getExternalBu()=" + getExternalBu()
				+ ", getInternalLe()=" + getInternalLe() + ", getExternalLe()=" + getExternalLe()
				+ ", getIntPortfolio()=" + getIntPortfolio() + ", getExtPortfolio()=" + getExtPortfolio()
				+ ", getBuySell()=" + getBuySell() + ", getBaseCurrency()=" + getBaseCurrency() + ", getBaseQuantity()="
				+ getBaseQuantity() + ", getBaseQuantityUnit()=" + getBaseQuantityUnit() + ", getTermCurrency()="
				+ getTermCurrency() 
				+ ", getOrderStatus()=" + getOrderStatus() + ", getCreatedAt()=" + getCreatedAt()
				+ ", getCreatedByUser()=" + getCreatedByUser() + ", getLastUpdate()=" + getLastUpdate()
				+ ", getUpdatedByUser()=" + getUpdatedByUser() + "]";
	}
	//  inherit hashCode and equals and to String from Order base class	
}
