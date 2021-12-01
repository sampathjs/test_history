package com.matthey.pmm.toms.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
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
 * Entity containing information about sending out emails. This table is used
 * to submit requests to send out email, to track the email sending process and to provide meta information
 * for auditing purposes.
 * 
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(name = "email", 
    indexes = { @Index(name = "i_email_id", columnList = "email_id", unique = true),
        @Index(name = "i_email_email_status_reference_id", columnList = "email_status_reference_id", unique = false)})
public class Email {	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "email_id_seq")
	@SequenceGenerator(name = "email_id_seq", initialValue = 1000000, allocationSize = 1,
	    sequenceName = "email_id_seq")
	@Column(name = "email_id", updatable = false, nullable = false)
	private Long id;

	@Column(name = "subject", nullable = false)
	private String subject;

	@Column(name = "body", nullable = true)
	private String body;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="send_as_user_id", nullable = false)
	private User sendAs;
	 
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "email_to", joinColumns = @JoinColumn(name = "email_id"))
	@Column (name="to")
	private Set<String> toSet;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "email_cc", joinColumns = @JoinColumn(name = "email_id"))
	@Column (name="cc")
	private Set<String> ccSet;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "email_bcc", joinColumns = @JoinColumn(name = "email_id"))
	@Column (name="bcc")
	private Set<String> bccSet;
		
	@ManyToMany(cascade = CascadeType.MERGE)
	@LazyCollection(LazyCollectionOption.TRUE)
	@JoinTable(name = "email_attachments",
	            joinColumns=@JoinColumn(name = "email_id"),
	            inverseJoinColumns=@JoinColumn(name = "database_file_id"))
	private Set<DatabaseFile> attachments;
	
	// metadata
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="email_status_reference_id", nullable = false)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.EMAIL_STATUS)
	private Reference emailStatus;

	@Column(name = "error_message", nullable = true)
	private String errorMessage;

	@Column(name = "retry_count", nullable = false)
	private int retryCount;
	
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
	
	
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "email_order_map", joinColumns = @JoinColumn(name = "email_id"))
	@Column (name="order_id")
	private Set<Long> associatedOrders;
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected Email() {
	}

	public Email(final Long id, final User sendAs, final String subject, final String body,
			final Set<String> toSet, final Set<String> ccSet, final Set<String> bccSet,
			final Set<DatabaseFile> attachments, final Reference emailStatus,
			final String errorMessage, final int retryCount, 
			final Date createdAt, final User createdByUser,
			final Date lastUpdate, final User updatedByUser,
			final Set<Order> associatedOrders) {
		this.id = id;
		this.sendAs = sendAs;
		this.subject = subject;
		this.body = body;
		this.emailStatus = emailStatus;
		this.errorMessage = errorMessage;
		this.retryCount = retryCount;
		this.createdAt = createdAt;
		this.createdByUser = createdByUser;
		this.lastUpdate = lastUpdate;
		this.updatedByUser = updatedByUser;
		
		if (attachments != null) {
			this.attachments = new HashSet<>(attachments);
		} else {
			this.attachments = null;
		}
		
		if (toSet != null) {
			this.toSet = new HashSet<>(toSet);
		} else {
			this.toSet = null;
		}
		if (ccSet != null) {
			this.ccSet = new HashSet<>(ccSet);
		} else {
			this.ccSet = null;
		}		
		if (bccSet != null) {
			this.bccSet = new HashSet<>(bccSet);
		} else {
			this.bccSet = null;
		}
		
		if (associatedOrders != null) {
			this.associatedOrders = new HashSet<>(associatedOrders.stream().map(x -> x.getOrderId()).collect(Collectors.toSet()));
		} else {
			this.associatedOrders = null;
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public User getSendAs() {
		return sendAs;
	}

	public void setSendAs(User sendAs) {
		this.sendAs = sendAs;
	}

	public Set<String> getToSet() {
		return toSet;
	}

	public void setToSet(Set<String> toSet) {
		this.toSet = toSet;
	}

	public Set<String> getCcSet() {
		return ccSet;
	}

	public void setCcSet(Set<String> ccSet) {
		this.ccSet = ccSet;
	}

	public Set<String> getBccSet() {
		return bccSet;
	}

	public void setBccSet(Set<String> bccSet) {
		this.bccSet = bccSet;
	}

	public Set<DatabaseFile> getAttachments() {
		return attachments;
	}

	public void setAttachments(Set<DatabaseFile> attachments) {
		this.attachments = attachments;
	}

	public Reference getEmailStatus() {
		return emailStatus;
	}

	public void setEmailStatus(Reference emailStatus) {
		this.emailStatus = emailStatus;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
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

	public Set<Long> getAssociatedOrders() {
		return associatedOrders;
	}

	public void setAssociatedOrders(Set<Order> associatedOrders) {
		this.associatedOrders = new HashSet<>(associatedOrders.stream().map(x -> x.getOrderId()).collect(Collectors.toSet()));;
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
		Email other = (Email) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Email [id=" + id + ", subject=" + subject + ", body=" + body + ", sendAs=" + sendAs + ", toSet="
				+ toSet + ", ccSet=" + ccSet + ", bccSet=" + bccSet + ", attachments=" + attachments
				+ ", emailStatus=" + emailStatus + ", errorMessage=" + errorMessage + ", retryCount=" + retryCount
				+ ", createdAt=" + createdAt + ", createdByUser=" + createdByUser + ", lastUpdate=" + lastUpdate
				+ ", updatedByUser=" + updatedByUser + ", associatedOrders=" + associatedOrders + "]";
	}
}
