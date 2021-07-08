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

import org.immutables.value.Value.Auxiliary;
import org.jetbrains.annotations.Nullable;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;

/**
 * Entity storing a comment for an order. The relationship is maintained within the order classes.
 * 
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(name = "credit_check", 
    indexes = { @Index(name = "i_credit_check_id", columnList = "credit_check_id", unique = true),
    		@Index(name = "i_credit_check_party", columnList = "party_id", unique = false)})
public class CreditCheck {    
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "credit_check_id_seq")
	@SequenceGenerator(name = "credit_check_id_seq", initialValue = 1000000, allocationSize = 1,
	    sequenceName = "credit_check_id_seq")
	@Column(name = "credit_check_id", updatable = false, nullable = false)
	private Long id;

	@Column(name = "party_id", nullable = false)
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

	public CreditCheck(final String commentText, final Date createdAt, final User createdByUser,
			final Date lastUpdate, final User updatedByUser) {

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
