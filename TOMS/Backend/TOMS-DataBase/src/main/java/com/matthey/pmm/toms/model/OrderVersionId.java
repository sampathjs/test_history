package com.matthey.pmm.toms.model;

import java.io.Serializable;

/**
 * Composite ID for orders consisting of uniquely incremented id
 * and version
 * @author jwaechter
 */

public class OrderVersionId implements Serializable {
    private static final long serialVersionUID = 538917918027512L;

    private long id;

    private long version;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
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
		if (id != other.id)
			return false;
		if (version != other.version)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OrderVersionId [id=" + id + ", version=" + version + "]";
	}  
}