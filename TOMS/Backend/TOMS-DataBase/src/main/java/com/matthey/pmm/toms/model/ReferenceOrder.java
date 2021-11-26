package com.matthey.pmm.toms.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

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
		
	// new columns below:
	@Column(name="metal_price_spread", nullable=true)
	private Double metalPriceSpread;

	@Column(name="fx_rate_spread", nullable=true)
	private Double fxRateSpread;

	@Column(name="contango_backwardation", nullable=true)
	private Double contangoBackwardation;
	
	@ManyToMany(cascade = CascadeType.MERGE)
	@LazyCollection(LazyCollectionOption.FALSE)
	@JoinTable(name = "reference_order_leg_map",
            joinColumns= { @JoinColumn(name = "order_id"), @JoinColumn(name = "version") },
	        inverseJoinColumns=@JoinColumn(name = "leg_id"))
	private List<ReferenceOrderLeg> legs;
	
	// new columns end
	// TODO: DB schema init scripts 
		
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected ReferenceOrder() {
	}

	public ReferenceOrder(final Reference orderTypeName,
			final int version, final Party internalBu, final Party externalBu, 
			final Party internalLe, final Party externalLe, final Reference intPortfolio,
			final Reference extPortfolio, final Reference buySell, final Reference baseCurrency,
			final Double baseQuantity, final Reference baseQuantityUnit, 
			final Reference termCurrency,
			final String reference, final Reference metalForm, final Reference metalLocation,			
			final OrderStatus orderStatus,
			final Date createdAt, 
			final User createdByUser, final Date lastUpdate,
			final User updatedByUser, final double fillPercentage,
			final Reference contractType, final Reference ticker,
			final List<OrderComment> orderComments,
			final List<Fill> fills, final List<CreditCheck> creditChecks, 
			// << order fields
			final Double metalPriceSpread,  final Double fxRateSpread, final Double contangoBackwardation, 
			final List<ReferenceOrderLeg> legs) {
		super(orderTypeName, internalBu, externalBu, internalLe, externalLe, intPortfolio,
				extPortfolio, buySell, baseCurrency, baseQuantity, baseQuantityUnit,
				termCurrency, reference, metalForm, metalLocation, orderStatus, createdAt,
				createdByUser, lastUpdate, updatedByUser, fillPercentage, contractType, ticker, orderComments, 
				fills, creditChecks);
		this.metalPriceSpread = metalPriceSpread;
		this.fxRateSpread = fxRateSpread;
		this.contangoBackwardation = contangoBackwardation;
		this.legs = new ArrayList<>(legs);
	}
	
	public ReferenceOrder(final ReferenceOrder toClone) {
		super(toClone);
		this.metalPriceSpread = toClone.metalPriceSpread;
		this.fxRateSpread = toClone.fxRateSpread;
		this.contangoBackwardation = toClone.contangoBackwardation;
		this.legs = new ArrayList<>(toClone.legs);
	}

	public Double getMetalPriceSpread() {
		return metalPriceSpread;
	}

	public void setMetalPriceSpread(Double metalPriceSpread) {
		this.metalPriceSpread = metalPriceSpread;
	}

	public Double getFxRateSpread() {
		return fxRateSpread;
	}

	public void setFxRateSpread(Double fxRateSpread) {
		this.fxRateSpread = fxRateSpread;
	}

	public Double getContangoBackwardation() {
		return contangoBackwardation;
	}

	public void setContangoBackwardation(Double contangoBackwardation) {
		this.contangoBackwardation = contangoBackwardation;
	}

	public List<ReferenceOrderLeg> getLegs() {
		return legs;
	}

	public void setLegs(List<ReferenceOrderLeg> legs) {
		this.legs = legs;
	}

	@Override
	public String toString() {
		return "ReferenceOrder [orderId=" + getOrderId() + " version()" + getVersion() + ", legs="
				+ legs + ", metalPriceSpread=" + metalPriceSpread + ", fxRateSpread=" + fxRateSpread
				+ ", contangoBackwardation=" + contangoBackwardation + ", getInternalBu()=" + getInternalBu() + ", getExternalBu()=" + getExternalBu()
				+ ", getInternalLe()=" + getInternalLe() + ", getExternalLe()=" + getExternalLe()
				+ ", getIntPortfolio()=" + getIntPortfolio() + ", getExtPortfolio()=" + getExtPortfolio()
				+ ", getBuySell()=" + getBuySell() + ", getBaseCurrency()=" + getBaseCurrency() + ", getBaseQuantity()="
				+ getBaseQuantity() + ", getBaseQuantityUnit()=" + getBaseQuantityUnit() + ", getTermCurrency()="
				+ getTermCurrency() + ", getContractType()=" + getContractType() + ", ticker=" + getTicker()
				+ ", getOrderStatus()=" + getOrderStatus() + ", getCreatedAt()=" + getCreatedAt()
				+ ", getCreatedByUser()=" + getCreatedByUser() + ", getLastUpdate()=" + getLastUpdate()
				+ ", getUpdatedByUser()=" + getUpdatedByUser() + ", getOrderComments()=" + getOrderComments()
				+ ", getFills()=" + getFills() + ", getCreditChecks()=" + getCreditChecks() + "]";
	}
	
	//  inherit hashCode and equals from Order base class
}
