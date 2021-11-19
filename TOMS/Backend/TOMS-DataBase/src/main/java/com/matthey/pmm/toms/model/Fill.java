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
import javax.persistence.UniqueConstraint;

/**
 * Entity storing a comment for an order. The relationship is maintained within the order classes.
 * 
 * @author jwaechter
 * @version 1.0
 */
@Entity
@Table(name = "fill", 
    indexes = { @Index(name = "i_fill_id", columnList = "fill_id", unique = true),
    		@Index(name = "i_fill_trade_id", columnList = "trade_id", unique = false)},
    		uniqueConstraints = { @UniqueConstraint(columnNames = { "trade_id"})})
public class Fill {	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fill_id_seq")
	@SequenceGenerator(name = "fill_id_seq", initialValue = 1000000, allocationSize = 1,
	    sequenceName = "fill_id_seq")
	@Column(name = "fill_id", updatable = false, nullable = false)
	private Long id;
	
	@Column(name = "fill_quantity", nullable = false)
	private Double fillQuantity;

	@Column(name = "fill_price", nullable = false)
	private Double fillPrice;
	
	@Column(name = "trade_id", nullable = false, unique = true)
	private long tradeId;
  
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="trader_user_id", nullable=false)
	private User trader;
	
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="updated_by_user_id", nullable=false)
	private User updatedBy;
	
	@Column(name = "last_update", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date lastUpdateDateTime;
	
	/**
	 * For JPA purposes only. Do not use.
	 */
	protected Fill() {
		
	}

	public Fill(final Double fillQuantity, final Double fillPrice, final long tradeId,
			final User trader, final User updatedBy, final Date lastUpdateDateTime) {
		this.fillQuantity = fillQuantity;
		this.fillPrice = fillPrice;
		this.tradeId = tradeId;
		this.trader = trader;
		this.updatedBy = updatedBy;
		this.lastUpdateDateTime = lastUpdateDateTime;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Double getFillQuantity() {
		return fillQuantity;
	}

	public void setFillQuantity(Double fillQuantity) {
		this.fillQuantity = fillQuantity;
	}

	public Double getFillPrice() {
		return fillPrice;
	}

	public void setFillPrice(Double fillPrice) {
		this.fillPrice = fillPrice;
	}

	public long getTradeId() {
		return tradeId;
	}

	public void setTradeId(long tradeId) {
		this.tradeId = tradeId;
	}

	public User getTrader() {
		return trader;
	}

	public void setTrader(User trader) {
		this.trader = trader;
	}

	public User getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(User updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Date getLastUpdateDateTime() {
		return lastUpdateDateTime;
	}

	public void setLastUpdateDateTime(Date lastUpdateDateTime) {
		this.lastUpdateDateTime = lastUpdateDateTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (tradeId ^ (tradeId >>> 32));
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
		Fill other = (Fill) obj;
		if (tradeId != other.tradeId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Fill [id=" + id + ", fillQuantity=" + fillQuantity + ", fillPrice=" + fillPrice + ", tradeId=" + tradeId
				+ "]";
	}
}