package com.matthey.pmm.toms.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;

/**
 * Base class for Limit Order and Reference Order
 * 
 * @author jwaechter
 * @version 1.0
 */
@IdClass (OrderVersionId.class)
@Entity
@Table(name = "abstract_order", 
    indexes = { @Index(name = "i_order_order_id_version", columnList = "order_id,version", unique = true),
        @Index(name = "i_order_internal_bunit", columnList = "internal_bunit_id", unique = false) })
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Order {
	@Id
    @Column(name = "order_id", nullable = false)
	@GeneratedValue(generator = "order-id-generator")
    @GenericGenerator(name = "order-id-generator", 
      strategy = "com.matthey.pmm.toms.model.OrderIdGenerator")
    private long orderId;

	@Id
    @Column(name = "version", insertable = false, updatable = false)
	@GeneratedValue(generator = "order-version-generator")
    @GenericGenerator(name = "order-version-generator", 
      strategy = "com.matthey.pmm.toms.model.OrderVersionGenerator")	
    private int version;
		 
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="internal_bunit_id", nullable = false)
	private Party internalBu;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="external_bunit_id", nullable = false)
	private Party externalBu;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="internal_legal_entity_id", nullable = false)
	private Party internalLe;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="external_legal_entity_id", nullable = false)
	private Party externalLe;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="internal_portfolio_id", nullable = false)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.PORTFOLIO)
	private Reference intPortfolio;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="external_portfolio_id", nullable = true)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.PORTFOLIO)
	private Reference extPortfolio;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="buy_sell_reference_id", nullable = false)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.BUY_SELL)
	private Reference buySell;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="base_currency_reference_id", nullable = false)
	@ReferenceTypeDesignator(referenceTypes = { DefaultReferenceType.CCY_CURRENCY, DefaultReferenceType.CCY_METAL})
	private Reference baseCurrency;
	
	@Column(name="base_quantity", nullable = false)
	private Double baseQuantity;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="base_quantity_unit_reference_id", nullable = false)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.QUANTITY_UNIT)
	private Reference baseQuantityUnit;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="term_currency_reference_id", nullable = false)
	@ReferenceTypeDesignator(referenceTypes = { DefaultReferenceType.CCY_CURRENCY, DefaultReferenceType.CCY_METAL})
	private Reference termCurrency;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="order_status_id", nullable = false)
	private OrderStatus orderStatus;

	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	private Date createdAt;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="created_by", nullable = false)
	private User createdByUser;
	
	@Column(name = "last_update")
	@Temporal(TemporalType.TIMESTAMP)
	private Date lastUpdate;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="updated_by", nullable = false)
	private User updatedByUser;
	
	@Column(name = "reference")
	private String reference;
		
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="metal_form_reference_id", nullable = true)
	@ReferenceTypeDesignator(referenceTypes = { DefaultReferenceType.METAL_FORM})
	private Reference metalForm;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="metal_location_reference_id", nullable = true)
	@ReferenceTypeDesignator(referenceTypes = { DefaultReferenceType.METAL_LOCATION})
	private Reference metalLocation;
	
	@ManyToMany(cascade = CascadeType.MERGE)
	@LazyCollection(LazyCollectionOption.FALSE)
	@JoinTable(name = "order_comment_map",
	            joinColumns= { @JoinColumn(name = "order_id"), @JoinColumn(name = "version") },
	            inverseJoinColumns=@JoinColumn(name = "order_comment_id"))
	private List<OrderComment> orderComments;
	
	@ManyToMany(cascade = CascadeType.MERGE)
	@LazyCollection(LazyCollectionOption.FALSE)
	@JoinTable(name = "order_fills_map", 
            joinColumns= { @JoinColumn(name = "order_id"), @JoinColumn(name = "version") },
	            inverseJoinColumns=@JoinColumn(name = "fill_id"))
	private List<Fill> fills;
	
	@ManyToMany(cascade = CascadeType.MERGE)
	@LazyCollection(LazyCollectionOption.FALSE)
	@JoinTable(name = "order_credit_check_map",
            joinColumns= { @JoinColumn(name = "order_id"), @JoinColumn(name = "version") },
	            inverseJoinColumns=@JoinColumn(name = "credit_check_id"))
	private List<CreditCheck> creditChecks;	
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected Order() {
	}

	public Order(final Party internalBu, final Party externalBu, 
			final Party internalLe, final Party externalLe, final Reference intPortfolio,
			final Reference extPortfolio, final Reference buySell, final Reference baseCurrency,
			final Double baseQuantity, final Reference baseQuantityUnit, 
			final Reference termCurrency, 
			final String reference, final Reference metalForm, final Reference metalLocation,
			final OrderStatus orderStatus, final Date createdAt, 
			final User createdByUser, final Date lastUpdate,
			final User updatedByUser, final List<OrderComment> orderComments,
			final List<Fill> fills, final List<CreditCheck> creditChecks) {
		this.internalBu = internalBu;
		this.externalBu = externalBu;
		this.internalLe = internalLe;
		this.externalLe = externalLe;		
		this.intPortfolio = intPortfolio;
		this.extPortfolio = extPortfolio;
		this.buySell = buySell;
		this.baseCurrency = baseCurrency;
		this.baseQuantity = baseQuantity;
		this.baseQuantityUnit = baseQuantityUnit;
		this.termCurrency = termCurrency;
		this.reference = reference;
		this.metalForm = metalForm;
		this.metalLocation = metalLocation;
		this.orderStatus = orderStatus;
		this.createdAt = createdAt;
		this.createdByUser = createdByUser;
		this.lastUpdate = lastUpdate;
		this.updatedByUser = updatedByUser;
		this.orderComments = new ArrayList<>(orderComments);
		this.fills = new ArrayList<>(fills);
		this.creditChecks = new ArrayList<>(creditChecks);
	}	
	
	public Order(Order toClone) {
		this.orderId = toClone.orderId;
		this.version = toClone.version;
		this.internalBu = toClone.internalBu;
		this.externalBu = toClone.externalBu;
		this.internalLe = toClone.internalLe;
		this.externalLe = toClone.externalLe;		
		this.intPortfolio = toClone.intPortfolio;
		this.extPortfolio = toClone.extPortfolio;
		this.buySell = toClone.buySell;
		this.baseCurrency = toClone.baseCurrency;
		this.baseQuantity = toClone.baseQuantity;
		this.baseQuantityUnit = toClone.baseQuantityUnit;
		this.termCurrency = toClone.termCurrency;
		this.reference = toClone.reference;
		this.metalForm = toClone.metalForm;
		this.metalLocation = toClone.metalLocation;
		this.orderStatus = toClone.orderStatus;
		this.createdAt = toClone.createdAt;
		this.createdByUser = toClone.createdByUser;
		this.lastUpdate = toClone.lastUpdate;
		this.updatedByUser = toClone.updatedByUser;
		this.orderComments = new ArrayList<>(toClone.orderComments);
		this.fills = new ArrayList<>(toClone.fills);
		this.creditChecks = new ArrayList<>(toClone.creditChecks);
	}	

	public long getOrderId() {
		return orderId;
	}

	public void setOrderId(long orderId) {
		this.orderId = orderId;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public Party getInternalBu() {
		return internalBu;
	}

	public void setInternalBu(Party internalBu) {
		this.internalBu = internalBu;
	}

	public Party getExternalBu() {
		return externalBu;
	}

	public void setExternalBu(Party externalBu) {
		this.externalBu = externalBu;
	}

	public Party getInternalLe() {
		return internalLe;
	}

	public void setInternalLe(Party internalLe) {
		this.internalLe = internalLe;
	}

	public Party getExternalLe() {
		return externalLe;
	}

	public void setExternalLe(Party externalLe) {
		this.externalLe = externalLe;
	}

	public Reference getIntPortfolio() {
		return intPortfolio;
	}

	public void setIntPortfolio(Reference intPortfolio) {
		this.intPortfolio = intPortfolio;
	}

	public Reference getExtPortfolio() {
		return extPortfolio;
	}

	public void setExtPortfolio(Reference extPortfolio) {
		this.extPortfolio = extPortfolio;
	}

	public Reference getBuySell() {
		return buySell;
	}

	public void setBuySell(Reference buySell) {
		this.buySell = buySell;
	}

	public Reference getBaseCurrency() {
		return baseCurrency;
	}

	public void setBaseCurrency(Reference baseCurrency) {
		this.baseCurrency = baseCurrency;
	}

	public Double getBaseQuantity() {
		return baseQuantity;
	}

	public void setBaseQuantity(Double baseQuantity) {
		this.baseQuantity = baseQuantity;
	}

	public Reference getBaseQuantityUnit() {
		return baseQuantityUnit;
	}

	public void setBaseQuantityUnit(Reference baseQuantityUnit) {
		this.baseQuantityUnit = baseQuantityUnit;
	}

	public Reference getTermCurrency() {
		return termCurrency;
	}

	public void setTermCurrency(Reference termCurrency) {
		this.termCurrency = termCurrency;
	}

	public OrderStatus getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(OrderStatus orderStatus) {
		this.orderStatus = orderStatus;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public User getCreatedByUser() {
		return createdByUser;
	}

	public void setCreatedByUser(User createdByUser) {
		this.createdByUser = createdByUser;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public User getUpdatedByUser() {
		return updatedByUser;
	}

	public void setUpdatedByUser(User updatedByUser) {
		this.updatedByUser = updatedByUser;
	}

	public List<OrderComment> getOrderComments() {
		return orderComments;
	}

	public void setOrderComments(List<OrderComment> orderComments) {
		this.orderComments = orderComments;
	}

	public List<Fill> getFills() {
		return fills;
	}

	public void setFills(List<Fill> fills) {
		this.fills = fills;
	}

	public List<CreditCheck> getCreditChecks() {
		return creditChecks;
	}

	public void setCreditChecks(List<CreditCheck> creditChecks) {
		this.creditChecks = creditChecks;
	}
	
	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public Reference getMetalForm() {
		return metalForm;
	}

	public void setMetalForm(Reference metalForm) {
		this.metalForm = metalForm;
	}

	public Reference getMetalLocation() {
		return metalLocation;
	}

	public void setMetalLocation(Reference metalLocation) {
		this.metalLocation = metalLocation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (orderId ^ (orderId >>> 32));
		result = prime * result + (int) (version ^ (version >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Order other = (Order) obj;
		if (orderId != other.orderId)
			return false;
		if (version != other.version)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Order [orderId=" + orderId + ", version=" + version + ", internalBu=" + internalBu + ", externalBu=" + externalBu
				+ ", internalLe=" + internalLe + ", externalLe=" + externalLe + ", intPortfolio=" + intPortfolio
				+ ", buySell=" + buySell + ", baseCurrency=" + baseCurrency + ", baseQuantity=" + baseQuantity
				+ ", reference=" + reference + ", metalForm=" + metalForm + ", metalLocation=" + metalLocation
				+ ", baseQuantityUnit=" + baseQuantityUnit + ", termCurrency=" + termCurrency + ", orderStatus="
				+ orderStatus + "]";
	}
}