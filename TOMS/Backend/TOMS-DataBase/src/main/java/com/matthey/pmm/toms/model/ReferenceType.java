package com.matthey.pmm.toms.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

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
@Table(name = "reference_type", 
    indexes = { @Index(name = "i_reference_type_id", columnList = "reference_type_id", unique = true),
        @Index(name = "i_reference_type_name", columnList = "name", unique = true),
    	@Index(name = "i_reference_type_sort_column", columnList = "sort_column", unique = false)})
public class ReferenceType {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reference_type_id_seq")
	@SequenceGenerator(name = "reference_type_id_seq", initialValue = 10000, allocationSize = 1,
	    sequenceName = "reference_type_id_seq")
	@Column(name = "reference_type_id", updatable = false, nullable = false)
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;
	
	@Column(name = "sort_column", nullable = true)
	private Long sortColumn;
 	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected ReferenceType() {
	}

	public ReferenceType(final String name, Long sortColumn) {
		this.name = name;
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
		ReferenceType other = (ReferenceType) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
