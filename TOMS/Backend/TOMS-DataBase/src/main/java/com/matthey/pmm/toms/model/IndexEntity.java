package com.matthey.pmm.toms.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;

/**
 * Entity representing an Index. Name has been set to IndexEntity to avoid confusion with other Index classes.
 * Endur maintained ID. 
 * 
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(name = "index", 
    indexes = { @Index(name = "i_index_id", columnList = "index_id", unique = true),
    		@Index(name = "i_index_name", columnList = "reference_index_name_id", unique = true),
    		@Index(name = "i_index_sort_column", columnList = "sort_column", unique = false)})
public class IndexEntity {	
	@Id
	@Column(name = "index_id", updatable = false, nullable = false)
	private Long id;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="reference_index_name_id")
	private Reference indexName;
 
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="reference_currency_one_id")
	@ReferenceTypeDesignator(referenceTypes = { DefaultReferenceType.CCY_CURRENCY, DefaultReferenceType.CCY_METAL })
	private Reference currencyOneName;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="reference_currency_two_id")
	@ReferenceTypeDesignator(referenceTypes = { DefaultReferenceType.CCY_CURRENCY, DefaultReferenceType.CCY_METAL })
	private Reference currencyTwoName;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="reference_lifecycle_status_id")
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.LIFECYCLE_STATUS)
	private Reference lifecycleStatus;
	
	@Column(name = "sort_column", nullable = true)
	private Long sortColumn;	
		
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected IndexEntity() {
	}

	public IndexEntity(final Long id, final Reference indexName, 
			final Reference currencyOneName, final Reference currencyTwoName,
			final Reference lifecycleStatus,
			Long sortColumn) {
		this.id = id;
		this.indexName = indexName;
		this.currencyOneName = currencyOneName;
		this.currencyTwoName = currencyTwoName;
		this.lifecycleStatus = lifecycleStatus;
		this.sortColumn = sortColumn;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Reference getIndexName() {
		return indexName;
	}

	public void setIndexName(Reference indexName) {
		this.indexName = indexName;
	}

	public Reference getCurrencyOneName() {
		return currencyOneName;
	}

	public void setCurrencyOneName(Reference currencyOneName) {
		this.currencyOneName = currencyOneName;
	}

	public Reference getCurrencyTwoName() {
		return currencyTwoName;
	}

	public void setCurrencyTwoName(Reference currencyTwoName) {
		this.currencyTwoName = currencyTwoName;
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
		result = prime * result + ((indexName == null) ? 0 : indexName.hashCode());
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
		IndexEntity other = (IndexEntity) obj;
		if (indexName == null) {
			if (other.indexName != null)
				return false;
		} else if (!indexName.equals(other.indexName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "IndexEntity [id=" + id + ", indexName=" + indexName + "]";
	}
}
