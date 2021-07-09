package com.matthey.pmm.toms.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;

/**
 * Base class for Limit Order and Reference Order
 * 
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(name = "order", 
    indexes = { @Index(name = "i_order_id_version", columnList = "order_id,version", unique = true),
        @Index(name = "i_internal_bunit", columnList = "internal_bunit_id", unique = false) })
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Order {	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_id_seq")
	@SequenceGenerator(name = "order_id_seq", initialValue = 1000000, allocationSize = 1,
	    sequenceName = "order_id_seq")	
	@Column(name = "order_id", updatable = false, nullable = false)
	private Long id;

	@Column(name = "version", nullable = false)
	private Integer version;
 
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
	@JoinColumn(name="physical_delivery_required_reference_id", nullable = false)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.YES_NO)
	private Reference physicalDeliveryRequired;

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
	
	@ManyToMany(cascade = CascadeType.ALL)
	@LazyCollection(LazyCollectionOption.FALSE)
	@JoinTable(name = "order_comment_map",
	            joinColumns=@JoinColumn(name = "order_id"),
	            inverseJoinColumns=@JoinColumn(name = "order_comment_id"))
	private List<OrderComment> orderComments;
	
	@ManyToMany(cascade = CascadeType.ALL)
	@LazyCollection(LazyCollectionOption.FALSE)
	@JoinTable(name = "order_fills_map",
	            joinColumns=@JoinColumn(name = "order_id"),
	            inverseJoinColumns=@JoinColumn(name = "fill_id"))
	private List<Fill> fills;
	
	@ManyToMany(cascade = CascadeType.ALL)
	@LazyCollection(LazyCollectionOption.FALSE)
	@JoinTable(name = "order_credit_check_map",
	            joinColumns=@JoinColumn(name = "order_id"),
	            inverseJoinColumns=@JoinColumn(name = "credit_check_id"))
	private List<CreditCheck> creditChecks;	
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected Order() {
	}

	public Order(final int version, final Party internalBu, final Party externalBu, 
			final Party internalLe, final Party externalLe, final Reference intPortfolio,
			final Reference extPortfolio, final Reference buySell, final Reference baseCurrency,
			final Double baseQuantity, final Reference baseQuantityUnit, 
			final Reference termCurrency, final Reference physicalDeliveryRequired,
			final OrderStatus orderStatus, final Date createdAt, 
			final User createdByUser, final Date lastUpdate,
			final User updatedByUser, final List<OrderComment> orderComments,
			final List<Fill> fills, final List<CreditCheck> creditChecks) {
		this.version = version;
		this.internalBu = internalBu;
		this.externalBu = externalBu;
		this.internalLe = internalLe;
		this.intPortfolio = intPortfolio;
		this.baseQuantity = baseQuantity;
		this.baseQuantityUnit = baseQuantityUnit;
		this.termCurrency = termCurrency;
		this.physicalDeliveryRequired = physicalDeliveryRequired;
		this.orderStatus = orderStatus;
		this.createdAt = createdAt;
		this.createdByUser = createdByUser;
		this.lastUpdate = lastUpdate;
		this.updatedByUser = updatedByUser;
		this.orderComments = new ArrayList<>(orderComments);
		this.fills = new ArrayList<>(fills);
		this.creditChecks = new ArrayList<>(creditChecks);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
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

	public Reference getPhysicalDeliveryRequired() {
		return physicalDeliveryRequired;
	}

	public void setPhysicalDeliveryRequired(Reference physicalDeliveryRequired) {
		this.physicalDeliveryRequired = physicalDeliveryRequired;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Order [id=" + id + ", version=" + version + ", internalBu=" + internalBu + ", externalBu=" + externalBu
				+ ", internalLe=" + internalLe + ", externalLe=" + externalLe + ", intPortfolio=" + intPortfolio
				+ ", buySell=" + buySell + ", baseCurrency=" + baseCurrency + ", baseQuantity=" + baseQuantity
				+ ", baseQuantityUnit=" + baseQuantityUnit + ", termCurrency=" + termCurrency + ", orderStatus="
				+ orderStatus + "]";
	}
}
