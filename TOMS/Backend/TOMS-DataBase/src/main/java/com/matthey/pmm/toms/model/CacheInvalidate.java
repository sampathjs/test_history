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
 * Entity containing a cache invalidate for a certain cache category and a certain cutoff date/time..
 * 
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(name = "cache_invalidate", 
    indexes = { @Index(name = "i_cache_invalidate_id", columnList = "cache_invalidate_id", unique = true)})
public class CacheInvalidate {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cache_invalidate_id_seq")
	@SequenceGenerator(name = "cache_invalidate_id_seq", initialValue = 10000, allocationSize = 1,
	    sequenceName = "cache_invalidate_id_seq")
	@Column(name = "cache_invalidate_id", updatable = false, nullable = false)
	private Long id;
 
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="cache_category_reference_id", nullable = false)
	@ReferenceTypeDesignator(referenceTypes=DefaultReferenceType.CACHE_TYPE)
	private Reference cacheCategory;
	
	@Column(name = "created_at", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date cutOffDateTime;
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected CacheInvalidate() {
		
	}

	public CacheInvalidate(final Reference cacheCategory, final Date cutOffDateTime) {
		this.cacheCategory = cacheCategory;
		this.cutOffDateTime = cutOffDateTime;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Reference getCacheCategory() {
		return cacheCategory;
	}

	public void setCacheCategory(Reference cacheCategory) {
		this.cacheCategory = cacheCategory;
	}

	public Date getCutOffDateTime() {
		return cutOffDateTime;
	}

	public void setCutOffDateTime(Date cutOffDateTime) {
		this.cutOffDateTime = cutOffDateTime;
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
		CacheInvalidate other = (CacheInvalidate) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CacheInvalidate [id=" + id + ", cacheCategory=" + cacheCategory + ", cutOffDateTime=" + cutOffDateTime
				+ "]";
	}
}
