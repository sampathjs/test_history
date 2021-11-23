package com.matthey.pmm.toms.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;

/**
 * Entity containing the expiration status having a Reference containing the name and a
 * designated order type.
 * 
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(name = "order_status", 
    indexes = { @Index(name = "i_order_status_id", columnList = "order_status_id", unique = true),
        @Index(name = "i_order_status_name_reference", columnList = "name_reference_id", unique = false),
        @Index(name = "i_order_status_sort_column", columnList = "sort_column", unique = false)},
	uniqueConstraints = { @UniqueConstraint(columnNames = { "name_reference_id", "order_type_reference_id" }) })
public class OrderStatus {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_status_id_seq")
	@SequenceGenerator(name = "order_status_id_seq", initialValue = 10000, allocationSize = 1,
	    sequenceName = "order_status_id_seq")
	@Column(name = "order_status_id", updatable = false, nullable = false)
	private Long id; 
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="name_reference_id")
	@ReferenceTypeDesignator(referenceTypes=DefaultReferenceType.ORDER_STATUS_NAME)
	private Reference orderStatusName;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="order_type_reference_id")
	@ReferenceTypeDesignator(referenceTypes=DefaultReferenceType.ORDER_TYPE_NAME)
	private Reference orderType;	

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="order_type_category_reference_id")
	@ReferenceTypeDesignator(referenceTypes=DefaultReferenceType.ORDER_TYPE_CATEGORY)
	private Reference orderTypeCategory;
	
	@Column(name = "sort_column", nullable = true)
	private Long sortColumn;
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected OrderStatus() {
	}

	public OrderStatus(final Reference orderStatusName, final Reference orderType, final Reference orderTypeCategory, Long sortColumn) {
		this.orderStatusName = orderStatusName;
		this.orderType = orderType;
		this.orderTypeCategory = orderTypeCategory;
		this.sortColumn = sortColumn;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}	

	public Reference getOrderStatusName() {
		return orderStatusName;
	}

	public void setOrderStatusName(Reference orderStatusName) {
		this.orderStatusName = orderStatusName;
	}

	public Reference getOrderType() {
		return orderType;
	}

	public void setOrderType(Reference orderType) {
		this.orderType = orderType;
	}

	public Long getSortColumn() {
		return sortColumn;
	}

	public void setSortColumn(Long sortColumn) {
		this.sortColumn = sortColumn;
	}
	
	

	public Reference getOrderTypeCategory() {
		return orderTypeCategory;
	}

	public void setOrderTypeCategory(Reference orderTypeCategory) {
		this.orderTypeCategory = orderTypeCategory;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((orderStatusName == null) ? 0 : orderStatusName.hashCode());
		result = prime * result + ((orderType == null) ? 0 : orderType.hashCode());
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
		OrderStatus other = (OrderStatus) obj;
		if (orderStatusName == null) {
			if (other.orderStatusName != null)
				return false;
		} else if (!orderStatusName.equals(other.orderStatusName))
			return false;
		if (orderType == null) {
			if (other.orderType != null)
				return false;
		} else if (!orderType.equals(other.orderType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OrderStatus [id=" + id + ", orderStatusName=" + orderStatusName + ", orderType="
				+ orderType  + ", orderTypeCategory=" + orderTypeCategory + "]";
	}
}
