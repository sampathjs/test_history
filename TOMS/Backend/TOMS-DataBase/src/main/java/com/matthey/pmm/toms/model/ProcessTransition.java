package com.matthey.pmm.toms.model;

import java.util.ArrayList;
import java.util.List;

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
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Entity containing metadata about process transitions. The IDs to denote the from 
 * and to status can denote different entities.
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "process_transition", 
    indexes = { @Index(name = "i_process_transition_id", columnList = "process_transition_id", unique = true),
        @Index(name = "i_process_transition_category", columnList = "reference_category_id", unique = false),
        @Index(name = "i_process_transition_sort_column", columnList = "sort_column", unique = false)},
    		uniqueConstraints = { @UniqueConstraint(columnNames = { "reference_category_id", "from_status_id", "to_status_id" }) })
public class ProcessTransition {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "process_transition_id_seq")
	@SequenceGenerator(name = "process_transition_id_seq", initialValue = 100000, allocationSize = 1,
	    sequenceName = "process_transition_id_seq", schema = DbConstants.SCHEMA_NAME)
	@Column(name = "process_transition_id", updatable = false, nullable = false)
	private Long id;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="reference_category_id", nullable = false)
	private Reference referenceCategory;

	@Column(name = "from_status_id", nullable = false)
	private long fromStatusId;

	@Column(name = "to_status_id", nullable = false)
	private long toStatusId;
	
	@ElementCollection (fetch = FetchType.EAGER)
    @CollectionTable(name = "process_transition_attributes", joinColumns = @JoinColumn(name = "process_transition_id"),
    		indexes = { @Index(name = "i_process_transition_attributes", columnList = "process_transition_id") }
    	    , schema = DbConstants.SCHEMA_NAME)
    @Column(name = "unchangeable_attribute")
	private List<String> unchangeableAttributes;
	
	@Column(name = "sort_column", nullable = true)
	private Long sortColumn;
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected ProcessTransition() {
	}

	public ProcessTransition(Reference referenceCategory, long fromStatusId, long toStatusId, Long sortColumn, List<String> unchangeableAttributes) {
		this.referenceCategory = referenceCategory;
		this.fromStatusId = fromStatusId;
		this.toStatusId = toStatusId;
		this.unchangeableAttributes = new ArrayList<>(unchangeableAttributes);
		this.sortColumn = sortColumn;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Reference getReferenceCategory() {
		return referenceCategory;
	}

	public void setReferenceCategory(Reference referenceCategory) {
		this.referenceCategory = referenceCategory;
	}

	public long getFromStatusId() {
		return fromStatusId;
	}

	public void setFromStatusId(long fromStatusId) {
		this.fromStatusId = fromStatusId;
	}

	public long getToStatusId() {
		return toStatusId;
	}

	public void setToStatusId(long toStatusId) {
		this.toStatusId = toStatusId;
	}

	public List<String> getUnchangeableAttributes() {
		return unchangeableAttributes;
	}

	public void setUnchangeableAttributes(List<String> unchangeableAttributes) {
		this.unchangeableAttributes = unchangeableAttributes;
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
		result = prime * result + (int) (fromStatusId ^ (fromStatusId >>> 32));
		result = prime * result + ((referenceCategory == null) ? 0 : referenceCategory.hashCode());
		result = prime * result + (int) (toStatusId ^ (toStatusId >>> 32));
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
		ProcessTransition other = (ProcessTransition) obj;
		if (fromStatusId != other.fromStatusId)
			return false;
		if (referenceCategory == null) {
			if (other.referenceCategory != null)
				return false;
		} else if (!referenceCategory.equals(other.referenceCategory))
			return false;
		if (toStatusId != other.toStatusId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ProcessTransition [id=" + id + ", referenceCategory=" + referenceCategory + ", fromStatusId="
				+ fromStatusId + ", toStatusId=" + toStatusId + "]";
	}
}
