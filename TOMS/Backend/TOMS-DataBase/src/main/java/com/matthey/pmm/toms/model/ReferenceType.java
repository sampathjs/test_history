package com.matthey.pmm.toms.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
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
@Table(name = "reference", 
    indexes = { @Index(name = "i_reference_type_id", columnList = "reference_type_id", unique = true),
        @Index(name = "i_reference_type_name", columnList = "name", unique = true) })
public class ReferenceType {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reference_type_id_seq")
	@SequenceGenerator(name = "reference_type_id_seq", initialValue = 10000, allocationSize = 1,
	    sequenceName = "reference_type_id_seq")
	@Column(name = "reference_type_id", updatable = false, nullable = false)
	private Long id;

	@Column(name = "name")
	private String name;
 	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected ReferenceType() {
	}

	public ReferenceType(final String name) {
		this.name = name;
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
