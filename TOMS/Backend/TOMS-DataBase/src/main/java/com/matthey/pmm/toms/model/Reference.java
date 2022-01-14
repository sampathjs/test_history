package com.matthey.pmm.toms.model;

import javax.persistence.CascadeType;
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
 * Generic entity containing reference objects of different types that had been previously
 * saved as enums or simple string values.
 * It is assumed that all authenticated users are allowed to access all reference data from
 * all different types.
 * 
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "reference", 
    indexes = { @Index(name = "i_reference_id", columnList = "reference_id", unique = true),
        @Index(name = "i_reference_type_value", columnList = "reference_type_id,value", unique = true),
        @Index(name = "i_reference_sort_column", columnList = "sort_column",  unique=false)},
    		uniqueConstraints = { @UniqueConstraint(columnNames = { "reference_type_id", "value" }) })
public class Reference {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reference_id_seq")
	@SequenceGenerator(name = "reference_id_seq", initialValue = 10000, allocationSize = 1,
	    sequenceName = "reference_id_seq", schema = DbConstants.SCHEMA_NAME)
	@Column(name = "reference_id", updatable = false, nullable = false)
	private long id;

	@Column(name = "value", nullable = false)
	private String value;

	@Column(name = "display_name")
	private String displayName;
 
	@OneToOne(fetch=FetchType.EAGER, cascade = CascadeType.MERGE)
	@JoinColumn(name="reference_type_id")
	private ReferenceType type;

	@Column(name = "endur_id",  nullable = true)
	private Long endurId;
	
	@OneToOne(fetch=FetchType.LAZY, optional = true, cascade = CascadeType.MERGE)
	@JoinColumn(name="reference_lifecycle_status_id")
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.LIFECYCLE_STATUS)
	private Reference lifecycleStatus;
	
	@Column(name = "sort_column", nullable = true)
	private Long sortColumn;
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected Reference() {
	}

	public Reference(final ReferenceType type, final String value, final String displayName, 
			final long endurId, final Reference lifecycleStatus, 
			final Long sortColumn) {
		this.value = value;
		this.type = type;
		this.displayName = displayName;
		this.endurId = endurId;
		this.lifecycleStatus = lifecycleStatus;
		this.sortColumn = sortColumn;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public ReferenceType getType() {
		return type;
	}

	public void setType(ReferenceType type) {
		this.type = type;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Long getEndurId() {
		return endurId;
	}

	public void setEndurId(Long endurId) {
		this.endurId = endurId;
	}
	
	public Reference getLifecycleStatus() {
		return lifecycleStatus;
	}

	public void setLifecycleStatus(Reference lifecycleStatus) {
		this.lifecycleStatus = lifecycleStatus;
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
		result = prime * result + (int) (id ^ (id >>> 32));
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
		Reference other = (Reference) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return value;
	}
}
