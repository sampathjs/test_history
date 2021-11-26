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
 * Generic entity containing reference objects of different types that had been previously
 * saved as enums or simple string values.
 * It is assumed that all authenticated users are allowed to access all reference data from
 * all different types.
 * 
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(name = "party", 
    indexes = { @Index(name = "i_party_id", columnList = "party_id", unique = true),
        @Index(name = "i_party_type", columnList = "reference_party_type_id", unique = false),
        @Index(name = "i_party_sort_column", columnList = "sort_column", unique = false)},
    		uniqueConstraints = { @UniqueConstraint(columnNames = { "reference_party_type_id", "name"}) })
public class Party {	
	@Id
	@Column(name = "party_id", updatable = false, nullable = false)
	private Long id;

	@Column(name = "name", nullable = true)
	private String name;
 
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="reference_party_type_id")
	@ReferenceTypeDesignator(referenceTypes=DefaultReferenceType.PARTY_TYPE)
	private Reference type;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="legal_entity_id", nullable = true)
	private Party legalEntity;
	
	@Column(name = "sort_column", nullable = true)
	private Long sortColumn;
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected Party() {
	}

	public Party(final Long id, final String name, final Reference type, Party legalEntity, Long sortColumn) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.legalEntity = legalEntity;
		this.sortColumn = sortColumn;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Reference getType() {
		return type;
	}

	public void setType(Reference type) {
		this.type = type;
	}

	public Party getLegalEntity() {
		return legalEntity;
	}

	public void setLegalEntity(Party legalEntity) {
		this.legalEntity = legalEntity;
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
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		Party other = (Party) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Party [id=" + id + ", name=" + name + ", type=" + type + ", legalEntity=" + legalEntity + "]";
	}
}
