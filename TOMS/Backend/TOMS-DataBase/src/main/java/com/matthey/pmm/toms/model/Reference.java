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
    indexes = { @Index(name = "i_reference_id", columnList = "reference_id", unique = true),
        @Index(name = "i_reference_type_value", columnList = "type,value", unique = true) },
    uniqueConstraints = { @UniqueConstraint(columnNames = { "type", "value" }) })
public class Reference {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reference_id_seq")
	@SequenceGenerator(name = "reference_id_seq", initialValue = 10000, allocationSize = 1,
	    sequenceName = "reference_id_seq")
	@Column(name = "reference_id", updatable = false, nullable = false)
	private Long id;

	@Column(name = "value", nullable = false)
	private String value;

	@Column(name = "display_name")
	private String displayName;
 
	@Enumerated(EnumType.ORDINAL)
	@Column(name = "type", nullable = false)
	private DefaultReferenceType type;

	@Column(name = "endur_id")
	private int endurId;

	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected Reference() {
	}

	public Reference(final DefaultReferenceType type, final String value, final String displayName, 
			int endurId) {
		this.value = value;
		this.type = type;
		this.displayName = displayName;
		this.endurId = endurId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public DefaultReferenceType getType() {
		return type;
	}

	public void setType(DefaultReferenceType type) {
		this.type = type;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public int getEndurId() {
		return endurId;
	}

	public void setEndurId(int endurId) {
		this.endurId = endurId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		if (type != other.type)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return value;
	}
}
