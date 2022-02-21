package com.matthey.pmm.toms.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

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
@Table(schema = DbConstants.SCHEMA_NAME, name = "user_entity", 
    indexes = { @Index(name = "i_user_id", columnList = "user_id", unique = true),
        @Index(name = "i_user_reference_lifecycle_status", columnList = "reference_lifecycle_status_id", unique = false),
        @Index(name = "i_user_role_id", columnList = "role_id", unique = false)})
public class User {	
	@Id
	@Column(name = "user_id", updatable = false, nullable = false)
	private Long id;	

	@Column(name = "email", nullable = false)
	private String email;

	@Column(name = "first_name")
	private String firstName;

	@Column(name = "last_name")
	private String lastName;

	@Column(name = "system_name", nullable=false)
	private String systemName;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="role_id", nullable = false)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.USER_ROLE)
	private Reference role;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="reference_lifecycle_status_id")
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.LIFECYCLE_STATUS)
	private Reference lifecycleStatus;
	
	@ManyToMany(cascade = CascadeType.MERGE)
	@LazyCollection(LazyCollectionOption.FALSE)
	@JoinTable(name = "user_tradeable_parties",
	            joinColumns=@JoinColumn(name = "user_id"),
	            inverseJoinColumns=@JoinColumn(name = "party_id")
				, schema = DbConstants.SCHEMA_NAME)
	private List<Party> tradeableParties;

	@ManyToMany(cascade = CascadeType.MERGE)
	@LazyCollection(LazyCollectionOption.FALSE)
	@JoinTable(name = "user_tradeable_portfolios",
	            joinColumns=@JoinColumn(name = "user_id"),
	            inverseJoinColumns=@JoinColumn(name = "reference_portfolio_id")
				, schema = DbConstants.SCHEMA_NAME)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.PORTFOLIO)
	private List<Reference> tradeablePortfolios;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="default_internal_bunit_id", nullable = true)
	private Party defaultInternalBu;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="default_internal_portfolio_reference_id", nullable = true)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.PORTFOLIO)
	private Reference defaultInternalPortfolio;
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected User() {
	}

	public User(final Long id, final String email, final String firstName, final String lastName,
			final Reference role, final Reference lifecycleStatus, 
			final List<Party> tradeableParties,
			final List<Reference> tradeablePortfolios,
			final Party defaultInternalBu,
			final Reference defaultInternalPortfolio,
			final String systemName) {
		this.id = id;
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;
		this.role = role;
		this.lifecycleStatus = lifecycleStatus;
		if (tradeableParties != null) {
			this.tradeableParties = new ArrayList<>(tradeableParties);
		} else {
			this.tradeableParties = null;
		}
		if (tradeablePortfolios != null) {
			this.tradeablePortfolios = new ArrayList<>(tradeablePortfolios);
		} else {
			this.tradeablePortfolios = null;
		}
		this.defaultInternalBu = defaultInternalBu;
		this.defaultInternalPortfolio = defaultInternalPortfolio;
		this.systemName = systemName;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Reference getRole() {
		return role;
	}

	public void setRole(Reference role) {
		this.role = role;
	}

	public Reference getLifecycleStatus() {
		return lifecycleStatus;
	}

	public void setLifecycleStatus(Reference lifecycleStatus) {
		this.lifecycleStatus = lifecycleStatus;
	}

	public List<Party> getTradeableParties() {
		return tradeableParties;
	}

	public void setTradeableParties(List<Party> tradeableParties) {
		this.tradeableParties = tradeableParties;
	}

	public List<Reference> getTradeablePortfolios() {
		return tradeablePortfolios;
	}

	public void setTradeablePortfolios(List<Reference> tradeablePortfolios) {
		this.tradeablePortfolios = tradeablePortfolios;
	}

	public Party getDefaultInternalBu() {
		return defaultInternalBu;
	}

	public void setDefaultInternalBu(Party defaultInternalBu) {
		this.defaultInternalBu = defaultInternalBu;
	}

	public Reference getDefaultInternalPortfolio() {
		return defaultInternalPortfolio;
	}

	public void setDefaultInternalPortfolio(Reference defaultInternalPortfolio) {
		this.defaultInternalPortfolio = defaultInternalPortfolio;
	}

	public String getSystemName() {
		return systemName;
	}

	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + ((firstName == null) ? 0 : firstName.hashCode());
		result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
		result = prime * result + ((role == null) ? 0 : role.hashCode());
		result = prime * result + ((systemName == null) ? 0 : systemName.hashCode());
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
		User other = (User) obj;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (firstName == null) {
			if (other.firstName != null)
				return false;
		} else if (!firstName.equals(other.firstName))
			return false;
		if (lastName == null) {
			if (other.lastName != null)
				return false;
		} else if (!lastName.equals(other.lastName))
			return false;
		if (role == null) {
			if (other.role != null)
				return false;
		} else if (!role.equals(other.role))
			return false;
		if (systemName == null) {
			if (other.systemName != null)
				return false;
		} else if (!systemName.equals(other.systemName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", email=" + email + ", firstName=" + firstName + ", lastName=" + lastName + ", role="
				+ role + ", systemName" + systemName + ", lifecycleStatus=" + lifecycleStatus + ", tradeablePortfolios=" + tradeablePortfolios + "]";
	}
}
