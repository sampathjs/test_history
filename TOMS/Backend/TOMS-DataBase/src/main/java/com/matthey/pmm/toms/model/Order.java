package com.matthey.pmm.toms.model;

import java.util.List;

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

import org.immutables.value.Value.Auxiliary;
import org.jetbrains.annotations.Nullable;

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
@Table(name = "order", 
    indexes = { @Index(name = "i_order_id", columnList = "order_id", unique = true),
        @Index(name = "i_party_type", columnList = "reference_party_type_id", unique = false) },
    	uniqueConstraints = { @UniqueConstraint(columnNames = { "order_id,", "version" }) })
public abstract class Order {		

    @Auxiliary
    public abstract long idBuySell();
    
    @Auxiliary
    public abstract long idBaseCurrency();
    
    @Nullable
    @Auxiliary
    public abstract Double baseQuantity();

    @Auxiliary
    @Nullable
    public abstract Long idBaseQuantityUnit();

    @Auxiliary
    public abstract Long idTermCurrency();
   
    @Auxiliary
    public abstract Long idYesNoPhysicalDeliveryRequired();

    @Auxiliary
    public abstract Long idOrderStatus();
        
    @Auxiliary
    @Nullable
    public abstract List<Long> creditChecksIds();

    @Auxiliary
    public abstract String createdAt();
    
    @Auxiliary
    public abstract long idCreatedByUser();
    
    @Auxiliary
    public abstract String lastUpdate();

    @Auxiliary
    public abstract long idUpdatedByUser();
    
    @Auxiliary
    @Nullable
    public abstract List<Long> orderCommentIds();
    
    @Auxiliary
    @Nullable
    public abstract List<Long> fillIds();	
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_id_seq")
	@SequenceGenerator(name = "order_id_seq", initialValue = 1000000, allocationSize = 1,
	    sequenceName = "order_id_seq")	
	@Column(name = "order_id", updatable = false, nullable = false)
	private Long id;

	@Column(name = "version", nullable = false)
	private Integer version;
 
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="internal_bunit_id", nullable = false)
	private Party internalBu;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="external_bunit_id", nullable = false)
	private Party externalBu;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="internal_legal_entity_id", nullable = false)
	private Party internalLe;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="external_legal_entity_id", nullable = false)
	private Party externalLe;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="internal_portfolio_id", nullable = false)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.PORTFOLIO)
	private Reference intPortfolio;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="external_portfolio_id", nullable = true)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.PORTFOLIO)
	private Reference extPortfolio;

	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="buy_sell_reference_id", nullable = true)
	@ReferenceTypeDesignator(referenceTypes = DefaultReferenceType.BUY_SELL)
	private Reference buySell;

	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected Order() {
	}

//	public Order(final String name, final Reference type, Order legalEntity) {
//
//	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

}
