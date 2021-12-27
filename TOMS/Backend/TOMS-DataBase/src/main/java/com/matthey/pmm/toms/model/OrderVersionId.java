package com.matthey.pmm.toms.model;

import java.io.Serializable;

import com.matthey.pmm.toms.transport.OrderTo;

/**
 * Composite ID for orders consisting of uniquely incremented id
 * and version
 * @author jwaechter
 */

public class OrderVersionId implements Serializable {
    private static final long serialVersionUID = 538917918027512L;

    private long orderId;

    private int version;
    
    /**
     * For JPA purposes only
     */
    protected OrderVersionId() {
    	
    }

    public OrderVersionId (long orderId, int version) {
    	this.orderId = orderId;
    	this.version = version;
    }
    
    public OrderVersionId (OrderTo order) {
    	this(order.id(), order.version());
    }
    
	public long getOrderId() {
		return orderId;
	}

	public void setOrderId(long orderId) {
		this.orderId = orderId;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (orderId ^ (orderId >>> 32));
		result = prime * result + (int) (version ^ (version >>> 32));
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
		OrderVersionId other = (OrderVersionId) obj;
		if (orderId != other.orderId)
			return false;
		if (version != other.version)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OrderVersionId [orderId=" + orderId + ", version=" + version + "]";
	}  
}