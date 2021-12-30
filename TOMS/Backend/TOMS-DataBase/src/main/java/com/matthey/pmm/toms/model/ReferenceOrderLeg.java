package com.matthey.pmm.toms.model;

import java.util.Date;
import java.util.List;

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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;

/**
 * Reference Order leg class.
 * 
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "reference_order_leg", 
    indexes = { @Index(name = "i_reference_order_leg_leg_id", columnList = "leg_id", unique = true)})

public class ReferenceOrderLeg {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "leg_id_seq")
	@SequenceGenerator(name = "leg_id_seq", initialValue = 1000000, allocationSize = 1,
	    sequenceName = "leg_id_seq", schema = DbConstants.SCHEMA_NAME)
	@Column(name = "leg_id", updatable = false, nullable = false)
	private Long id;
	
	@Column(name = "fixing_start_date", nullable = false)
	@Temporal(TemporalType.DATE)
	private Date fixingStartDate;  
	
	@Column(name = "fixing_end_date", nullable = true)
	@Temporal(TemporalType.DATE)
	private Date fixingEndDate;
	
	@Column(name = "payment_date", nullable = false)
	@Temporal(TemporalType.DATE)
	private Date paymentDate;

	@Column(name = "notional", nullable=false)
	private Double notional;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="settle_currency_reference_id", nullable = true)
	@ReferenceTypeDesignator(referenceTypes = { DefaultReferenceType.CCY_CURRENCY, DefaultReferenceType.CCY_METAL })
	private Reference settleCurrency;  	
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="ref_source_reference_id", nullable = false)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.REF_SOURCE)
	private Reference refSource;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="fx_index_ref_source_reference_id", nullable = true)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.REF_SOURCE)
	private Reference fxIndexRefSource;

		
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected ReferenceOrderLeg() {
	}

	public ReferenceOrderLeg(final Date fixingStartDate, final Date fixingEndDate, final Date paymentDate,
			final Double notional, final Reference settleCurrency, final Reference refSource, 
			final Reference fxIndexRefSource) {
		this.fixingStartDate = fixingStartDate;
		this.fixingEndDate = fixingEndDate;
		this.paymentDate = paymentDate;
		this.notional = notional;
		this.settleCurrency = settleCurrency;
		this.refSource = refSource;
		this.fxIndexRefSource = fxIndexRefSource;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Date getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(Date paymentDate) {
		this.paymentDate = paymentDate;
	}

	public Double getNotional() {
		return notional;
	}

	public void setNotional(Double notional) {
		this.notional = notional;
	}

	public Reference getSettleCurrency() {
		return settleCurrency;
	}

	public void setSettleCurrency(Reference settleCurrency) {
		this.settleCurrency = settleCurrency;
	}

	public Reference getRefSource() {
		return refSource;
	}

	public void setRefSource(Reference refSource) {
		this.refSource = refSource;
	}

	public Reference getFxIndexRefSource() {
		return fxIndexRefSource;
	}

	public void setFxIndexRefSource(Reference fxIndexRefSource) {
		this.fxIndexRefSource = fxIndexRefSource;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		ReferenceOrderLeg other = (ReferenceOrderLeg) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ReferenceOrderLeg [id=" + id + ", fixingStartDate=" + fixingStartDate + ", fixingEndDate="
				+ fixingEndDate + ", paymentDate=" + paymentDate + ", notional=" + notional + ", settleCurrency="
				+ settleCurrency + ", refSource=" + refSource + ", fxIndexRefSource=" + fxIndexRefSource + "]";
	}
}
