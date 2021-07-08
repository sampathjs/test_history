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

/**
 * Entity storing a comment for an order. The relationship is maintained within the order classes.
 * 
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(name = "credit_check", 
    indexes = { @Index(name = "i_order_comment_id", columnList = "order_comment_id", unique = true)})
public class CreditCheck {    
    @Auxiliary
    public abstract long idCreditCheckRunStatus();

    @Auxiliary
    @Nullable
    public abstract Long idCreditCheckOutcome();
	
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
	@JoinColumn(name="credit_check_run_status")
	private Reference creditCheckRunStatus;
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected CreditCheck() {
		
	}

	public CreditCheck(final String commentText, final Date createdAt, final User createdByUser,
			final Date lastUpdate, final User updatedByUser) {
		this.commentText = commentText;
		this.createdAt = createdAt;
		this.createdByUser = createdByUser;
		this.lastUpdate = lastUpdate;
		this.updatedByUser = updatedByUser;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCommentText() {
		return commentText;
	}

	public void setCommentText(String commentText) {
		this.commentText = commentText;
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

	@Override
	public String toString() {
		return "OrderComment [id=" + id + ", commentText=" + commentText + "]";
	}
}
