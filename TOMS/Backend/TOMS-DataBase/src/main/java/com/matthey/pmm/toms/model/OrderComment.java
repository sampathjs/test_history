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
@Table(name = "order_comment", 
    indexes = { @Index(name = "i_order_comment_id", columnList = "order_comment_id", unique = true)})
public class OrderComment {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_comment_id_seq")
	@SequenceGenerator(name = "order_comment_id_seq", initialValue = 1000000, allocationSize = 1,
	    sequenceName = "order_comment_id_seq")
	@Column(name = "order_comment_id", updatable = false, nullable = false)
	private Long id;

	@Column(name = "comment_text", nullable = false)
	private String commentText;

	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	private Date createdAt;
 
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="created_by_user_id")
	private User createdByUser;

	@Column(name = "last_update")
	@Temporal(TemporalType.TIMESTAMP)
	private Date lastUpdate;
 
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="updated_by_user_id")
	private User updatedByUser;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="deletion_flag_reference_id", nullable = false)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.DELETION_FLAG)
	private Reference deletionFlag;
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected OrderComment() {
		
	}

	public OrderComment(final String commentText, final Date createdAt, final User createdByUser,
			final Date lastUpdate, final User updatedByUser, final Reference deletionFlag) {
		this.commentText = commentText;
		this.createdAt = createdAt;
		this.createdByUser = createdByUser;
		this.lastUpdate = lastUpdate;
		this.updatedByUser = updatedByUser;
		this.deletionFlag = deletionFlag;		
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

	public Reference getDeletionFlag() {
		return deletionFlag;
	}

	public void setDeletionFlag(Reference deletionFlag) {
		this.deletionFlag = deletionFlag;
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
		OrderComment other = (OrderComment) obj;
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
