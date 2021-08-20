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
 * Reference Order class.
 * 
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(name = "reference_order", 
    indexes = { @Index(name = "i_reference_order_order_id", columnList = "order_id", unique = true)})
@PrimaryKeyJoinColumns(value = {
        @PrimaryKeyJoinColumn( name = "order_id", referencedColumnName = "order_id" ),
        @PrimaryKeyJoinColumn( name = "version", referencedColumnName = "version" )
    })
public class ReferenceOrder extends Order {
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
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="metal_reference_index_id", nullable = false)
	private IndexEntity metalReferenceIndex;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="currency_reference_index_id", nullable = false)
	private IndexEntity currencyReferenceIndex;
	
	@Column(name = "fixing_start_date", nullable = false)
	@Temporal(TemporalType.DATE)
	private Date fixingStartDate;

	@Column(name = "fixing_end_date", nullable = false)
	@Temporal(TemporalType.DATE)
	private Date fixingEndDate;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="averaging_rule_reference_id", nullable = false)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.AVERAGING_RULE)
	private Reference averagingRule;
		
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected ReferenceOrder() {
	}

	public ReferenceOrder(final int version, final Party internalBu, final Party externalBu, 
			final Party internalLe, final Party externalLe, final Reference intPortfolio,
			final Reference extPortfolio, final Reference buySell, final Reference baseCurrency,
			final Double baseQuantity, final Reference baseQuantityUnit, 
			final Reference termCurrency, final Reference physicalDeliveryRequired,
			final OrderStatus orderStatus, final Date createdAt, 
			final User createdByUser, final Date lastUpdate,
			final User updatedByUser, final List<OrderComment> orderComments,
			final List<Fill> fills, final List<CreditCheck> creditChecks, // << order fields
			final IndexEntity metalReferenceIndex, final IndexEntity currencyReferenceIndex, 
			final Date fixingStartDate, final Date fixingEndDate,
			final Reference averagingRule) {
		super(internalBu, externalBu, internalLe, externalLe, intPortfolio,
				extPortfolio, buySell, baseCurrency, baseQuantity, baseQuantityUnit,
				termCurrency, physicalDeliveryRequired, orderStatus, createdAt,
				createdByUser, lastUpdate, updatedByUser, orderComments, 
				fills, creditChecks);
		this.metalReferenceIndex = metalReferenceIndex;
		this.currencyReferenceIndex = currencyReferenceIndex;
		this.fixingStartDate = fixingStartDate;
		this.fixingEndDate = fixingEndDate;
		this.averagingRule = averagingRule;
	}

	public IndexEntity getMetalReferenceIndex() {
		return metalReferenceIndex;
	}

	public void setMetalReferenceIndex(IndexEntity metalReferenceIndex) {
		this.metalReferenceIndex = metalReferenceIndex;
	}

	public IndexEntity getCurrencyReferenceIndex() {
		return currencyReferenceIndex;
	}

	public void setCurrencyReferenceIndex(IndexEntity currencyReferenceIndex) {
		this.currencyReferenceIndex = currencyReferenceIndex;
	}

	public Date getFixingStartDate() {
		return fixingStartDate;
	}

	public void setFixingStartDate(Date fixingStartDate) {
		this.fixingStartDate = fixingStartDate;
	}

	public Date getFixingEndDate() {
		return fixingEndDate;
	}

	public void setFixingEndDate(Date fixingEndDate) {
		this.fixingEndDate = fixingEndDate;
	}

	public Reference getAveragingRule() {
		return averagingRule;
	}

	public void setAveragingRule(Reference averagingRule) {
		this.averagingRule = averagingRule;
	}

	@Override
	public String toString() {
		return "ReferenceOrder [metalReferenceIndex=" + metalReferenceIndex + ", currencyReferenceIndex="
				+ currencyReferenceIndex + ", fixingStartDate=" + fixingStartDate + ", fixingEndDate=" + fixingEndDate
				+ ", averagingRule=" + averagingRule + ", getOrderId()=" + getOrderId() + ", getVersion()="
				+ getVersion() + ", getInternalBu()=" + getInternalBu() + ", getExternalBu()=" + getExternalBu()
				+ ", getInternalLe()=" + getInternalLe() + ", getExternalLe()=" + getExternalLe()
				+ ", getIntPortfolio()=" + getIntPortfolio() + ", getExtPortfolio()=" + getExtPortfolio()
				+ ", getBuySell()=" + getBuySell() + ", getBaseCurrency()=" + getBaseCurrency() + ", getBaseQuantity()="
				+ getBaseQuantity() + ", getBaseQuantityUnit()=" + getBaseQuantityUnit() + ", getTermCurrency()="
				+ getTermCurrency() + ", getPhysicalDeliveryRequired()=" + getPhysicalDeliveryRequired()
				+ ", getOrderStatus()=" + getOrderStatus() + ", getCreatedAt()=" + getCreatedAt()
				+ ", getCreatedByUser()=" + getCreatedByUser() + ", getLastUpdate()=" + getLastUpdate()
				+ ", getUpdatedByUser()=" + getUpdatedByUser() + ", getOrderComments()=" + getOrderComments()
				+ ", getFills()=" + getFills() + ", getCreditChecks()=" + getCreditChecks() + "]";
	}
	
	//  inherit hashCode and equals and to String from Order base class
}
