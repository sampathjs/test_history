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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Entity containing metadata about the defaulting and calculation of dependent attribute values.
 * It is defined for a certain concrete TO immutable class (e.g. ImmutableReferenceOrderTo), 
 * an attribute method of the immutable that is supposed to be calculated, the  
 * <a href="https://docs.spring.io/spring-framework/docs/4.3.10.RELEASE/spring-framework-reference/html/expressions.html"> Spring SPEL </>
 * expression term used to calculate the value and an explicit list of attributed that are used to calculate the value.
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(schema = DbConstants.SCHEMA_NAME,name = "attribute_calc", 
    indexes = { @Index(name = "i_attribute_calc_id", columnList = "attribute_calc_id", unique = true),
        @Index(name = "i_attribute_calc_class_attribute", columnList = "class_name, attribute_name", unique = true) },
    		uniqueConstraints = { @UniqueConstraint(columnNames = { "class_name", "attribute_name" }) })
public class AttributeCalculation {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attribute_calc_id_seq")
	@SequenceGenerator(name = "attribute_calc_id_seq", initialValue = 100000, allocationSize = 1,
	    sequenceName = "attribute_calc_id_seq", schema = DbConstants.SCHEMA_NAME)
	@Column(name = "attribute_calc_id", updatable = false, nullable = false)
	private Long id;
		
	
	@Column(name = "class_name", nullable = false)
	private String className;
	
	@ElementCollection (fetch = FetchType.EAGER)
    @CollectionTable(schema = DbConstants.SCHEMA_NAME, 
    		name = "attribute_calc_attributes", joinColumns = @JoinColumn(name = "attribute_calc_id"),
    		indexes = { @Index(name = "i_attribute_calc_attributes", columnList = "attribute_calc_id", unique = false) } )
    @Column(name = "dependent_attribute_name")
	private List<String> dependentAttributes;

	@Column(name = "attribute_name", nullable = false)
	private String attributeName;
 
	@Column(name = "spel_expression")
	private String spelExpression;
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected AttributeCalculation() {
	}

	public AttributeCalculation(String className, List<String> dependentAttributes, 
			String attributeName, String spelExpression) {
		this.className = className;
		this.dependentAttributes = new ArrayList<>(dependentAttributes);
		this.attributeName = attributeName;
		this.spelExpression = spelExpression;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public List<String> getDependentAttributes() {
		return dependentAttributes;
	}

	public void setDependentAttributes(List<String> dependentAttributes) {
		this.dependentAttributes = dependentAttributes;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public String getSpelExpression() {
		return spelExpression;
	}

	public void setSpelExpression(String spelExpression) {
		this.spelExpression = spelExpression;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributeName == null) ? 0 : attributeName.hashCode());
		result = prime * result + ((className == null) ? 0 : className.hashCode());
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
		AttributeCalculation other = (AttributeCalculation) obj;
		if (attributeName == null) {
			if (other.attributeName != null)
				return false;
		} else if (!attributeName.equals(other.attributeName))
			return false;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AttributeCalculation [id=" + id + ", className=" + className + ", attributeName=" + attributeName + "]";
	}
}
