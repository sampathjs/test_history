package com.matthey.openlink.accounting.ops;

import java.io.Serializable;

public class TranFieldKey implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private String fieldName;
	private String altFieldName;
	private int fieldId;
	private String side;
	private String seq2;
	private String seq3;
	private String seq4;
	private String seq5;
	
	public TranFieldKey() {
	}
	
	public TranFieldKey(String fieldName, String altFieldName, int fieldId, String side, String seq2, String seq3, String seq4, String seq5) {
		this.fieldName = fieldName;
		this.altFieldName = altFieldName;
		this.fieldId = fieldId;
		this.side = side;
		this.seq2 = seq2;
		this.seq3 = seq3;
		this.seq4 = seq4;
		this.seq5 = seq5;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getAltFieldName() {
		return altFieldName;
	}

	public void setAltFieldName(String altFieldName) {
		this.altFieldName = altFieldName;
	}

	public int getFieldId() {
		return fieldId;
	}

	public void setFieldId(int fieldId) {
		this.fieldId = fieldId;
	}

	public String getSide() {
		return side;
	}

	public void setSide(String side) {
		this.side = side;
	}

	public String getSeq2() {
		return seq2;
	}

	public void setSeq2(String seq2) {
		this.seq2 = seq2;
	}

	public String getSeq3() {
		return seq3;
	}

	public void setSeq3(String seq3) {
		this.seq3 = seq3;
	}

	public String getSeq4() {
		return seq4;
	}

	public void setSeq4(String seq4) {
		this.seq4 = seq4;
	}

	public String getSeq5() {
		return seq5;
	}

	public void setSeq5(String seq5) {
		this.seq5 = seq5;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((altFieldName == null) ? 0 : altFieldName.hashCode());
		result = prime * result + fieldId;
		result = prime * result
				+ ((fieldName == null) ? 0 : fieldName.hashCode());
		result = prime * result + ((seq2 == null) ? 0 : seq2.hashCode());
		result = prime * result + ((seq3 == null) ? 0 : seq3.hashCode());
		result = prime * result + ((seq4 == null) ? 0 : seq4.hashCode());
		result = prime * result + ((seq5 == null) ? 0 : seq5.hashCode());
		result = prime * result + ((side == null) ? 0 : side.hashCode());
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
		TranFieldKey other = (TranFieldKey) obj;
		if (altFieldName == null) {
			if (other.altFieldName != null)
				return false;
		} else if (!altFieldName.equals(other.altFieldName))
			return false;
		if (fieldId != other.fieldId)
			return false;
		if (fieldName == null) {
			if (other.fieldName != null)
				return false;
		} else if (!fieldName.equals(other.fieldName))
			return false;
		if (seq2 == null) {
			if (other.seq2 != null)
				return false;
		} else if (!seq2.equals(other.seq2))
			return false;
		if (seq3 == null) {
			if (other.seq3 != null)
				return false;
		} else if (!seq3.equals(other.seq3))
			return false;
		if (seq4 == null) {
			if (other.seq4 != null)
				return false;
		} else if (!seq4.equals(other.seq4))
			return false;
		if (seq5 == null) {
			if (other.seq5 != null)
				return false;
		} else if (!seq5.equals(other.seq5))
			return false;
		if (side == null) {
			if (other.side != null)
				return false;
		} else if (!side.equals(other.side))
			return false;
		return true;
	}

}
