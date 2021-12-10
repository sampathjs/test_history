package com.matthey.pmm.toms.model;

import java.util.Date;

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
 * Entity storing a comment for an order. The relationship is maintained within the order classes.
 * 
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "credit_check", 
    indexes = { @Index(name = "i_credit_check_id", columnList = "credit_check_id", unique = true),
    		@Index(name = "i_credit_check_party", columnList = "party_id", unique = false)})
public class CreditCheck {    
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "credit_check_id_seq")
	@SequenceGenerator(name = "credit_check_id_seq", initialValue = 1000000, allocationSize = 1,
	    sequenceName = "credit_check_id_seq", schema = DbConstants.SCHEMA_NAME)
	@Column(name = "credit_check_id", updatable = false, nullable = false)
	private Long id;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name = "party_id", nullable = false)
	private Party party;
	
	@Column(name = "credit_limit", nullable = true)
	private Double creditLimit;

	@Column(name = "current_utilization", nullable = true)
	private Double currentUtilization;
	
	@Column(name = "run_date_time", nullable = true)
	@Temporal(TemporalType.TIMESTAMP)
	private Date runDateTime;
  
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="reference_credit_check_run_status_id", nullable=false)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.CREDIT_CHECK_RUN_STATUS)
	private Reference creditCheckRunStatus;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="reference_credit_check_outcome_id", nullable=true)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.CREDIT_CHECK_OUTCOME)
	private Reference creditCheckOutcome;
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected CreditCheck() {
		
	}

	public CreditCheck(final Party party, final Double creditLimit, final Double currentUtilization,
			final Date runDateTime, final Reference creditCheckRunStatus, final Reference creditCheckOutcome) {
		this.party = party;
		this.creditLimit = creditLimit;
		this.currentUtilization = currentUtilization;
		this.runDateTime = runDateTime;
		this.creditCheckRunStatus = creditCheckRunStatus;
		this.creditCheckOutcome = creditCheckOutcome;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Party getParty() {
		return party;
	}

	public void setParty(Party party) {
		this.party = party;
	}

	public Double getCreditLimit() {
		return creditLimit;
	}

	public void setCreditLimit(Double creditLimit) {
		this.creditLimit = creditLimit;
	}

	public Double getCurrentUtilization() {
		return currentUtilization;
	}

	public void setCurrentUtilization(Double currentUtilization) {
		this.currentUtilization = currentUtilization;
	}

	public Date getRunDateTime() {
		return runDateTime;
	}

	public void setRunDateTime(Date runDateTime) {
		this.runDateTime = runDateTime;
	}

	public Reference getCreditCheckRunStatus() {
		return creditCheckRunStatus;
	}

	public void setCreditCheckRunStatus(Reference creditCheckRunStatus) {
		this.creditCheckRunStatus = creditCheckRunStatus;
	}

	public Reference getCreditCheckOutcome() {
		return creditCheckOutcome;
	}

	public void setCreditCheckOutcome(Reference creditCheckOutcome) {
		this.creditCheckOutcome = creditCheckOutcome;
	}

	@Override
	public String toString() {
		return "CreditCheck [id=" + id + ", party=" + party + ", creditLimit=" + creditLimit + ", currentUtilization="
				+ currentUtilization + ", runDateTime=" + runDateTime + ", creditCheckRunStatus=" + creditCheckRunStatus
				+ "]";
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
		CreditCheck other = (CreditCheck) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
