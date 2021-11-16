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
@Table(name = "expiration_status", 
    indexes = { @Index(name = "i_expiration_status_id", columnList = "expiration_status_id", unique = true),
        @Index(name = "i_expiration_status_name_reference", columnList = "name_reference_id", unique = false),
        @Index(name = "i_expiration_status_sort_column", columnList = "sort_column", unique = false)},
	uniqueConstraints = { @UniqueConstraint(columnNames = { "name_reference_id", "order_type_reference_id" }) })
public class ExpirationStatus {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "expiration_status_id_seq")
	@SequenceGenerator(name = "expiration_status_id_seq", initialValue = 10000, allocationSize = 1,
	    sequenceName = "expiration_status_id_seq")
	@Column(name = "expiration_status_id", updatable = false, nullable = false)
	private Long id;
 
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="name_reference_id")
	@ReferenceTypeDesignator(referenceTypes=DefaultReferenceType.EXPIRATION_STATUS)
	private Reference expirationStatusName;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="order_type_reference_id")
	@ReferenceTypeDesignator(referenceTypes=DefaultReferenceType.ORDER_TYPE_NAME)
	private Reference orderType;

	@Column(name = "sort_column", nullable = true)
	private Long sortColumn;

	/**
	 * For JPA purposes only. Do not use.
	 */
	protected ExpirationStatus() {
	}

	public ExpirationStatus(final Reference expirationStatusName, final Reference orderType) {
		this.expirationStatusName = expirationStatusName;
		this.orderType = orderType;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Reference getExpirationStatusName() {
		return expirationStatusName;
	}

	public void setExpirationStatusName(Reference expirationStatusName) {
		this.expirationStatusName = expirationStatusName;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((expirationStatusName == null) ? 0 : expirationStatusName.hashCode());
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
		ExpirationStatus other = (ExpirationStatus) obj;
		if (expirationStatusName == null) {
			if (other.expirationStatusName != null)
				return false;
		} else if (!expirationStatusName.equals(other.expirationStatusName))
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
		return "ExpirationStatus [id=" + id + ", expirationStatusName=" + expirationStatusName + ", orderType="
				+ orderType + "]";
	}
}
