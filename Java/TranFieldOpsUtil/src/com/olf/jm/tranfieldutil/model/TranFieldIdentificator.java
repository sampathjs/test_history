package com.olf.jm.tranfieldutil.model;

import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.TRANF_FIELD;
import  com.olf.jm.logging.Logging;

/*
 * History:
 * 2015-MM-DD	V1.0	jwaechter	- Initial Version
 * 2016-02-18	V1.1	jwaechter	- Added logging to setValue
 */


public class TranFieldIdentificator {
	private final TRANF_FIELD field;
	private final int side;
	private final int seqNum2;
	private final int seqNum3;
	private final int seqNum4;
	private final int seqNum5;
	private final String name;

	public TranFieldIdentificator (final TRANF_FIELD field, final String name, final int side, final int seqNum2,
			final int seqNum3, final int seqNum4, final int seqNum5) {
		this.field = field;
		this.side = side;
		this.seqNum2 = seqNum2;
		this.seqNum3 = seqNum3;
		this.seqNum4 = seqNum4;
		this.seqNum5 = seqNum5;
		this.name = name;
	}
	
	public String getValue (final Transaction tran) throws OException {
		return tran.getField(field.toInt(), side, name, seqNum2, seqNum3, seqNum4, seqNum5);
	}
	
	public void setValue(Transaction tran, String value) throws OException {
		Logging.info ("Tran#" + tran.getTranNum() + ": Setting field " + toString() + " to " + value);
		tran.setField(field.toInt(), side, name, value, seqNum2, seqNum3, seqNum4, seqNum5);
	}
	
	public boolean equals (final TRANF_FIELD field, final String name, final int side, final int seqNum2,
			final int seqNum3, final int seqNum4, final int seqNum5) {
		if (name != null && !name.equals("")) {
			return this.field == field && this.name.equals(name) && this.seqNum2 == seqNum2
					&& this.seqNum3 == seqNum3 && this.seqNum4 == seqNum4 && this.seqNum5 == seqNum5;			
		} 
				
		return this.field == field && this.name.equals(name) && this.side == side && this.seqNum2 == seqNum2
				&& this.seqNum3 == seqNum3 && this.seqNum4 == seqNum4 && this.seqNum5 == seqNum5;
	}
	
	public boolean equals (final TRANF_FIELD field, final String name, final int side, final int seqNum2,
			final int seqNum3, final int seqNum4) {
		return equals (field, name, side, seqNum2, seqNum3, seqNum4, 0);
	}
	
	public boolean equals (final TRANF_FIELD field, final String name, final int side, final int seqNum2,
			final int seqNum3) {
		return equals (field, name, side, seqNum2, seqNum3, 0);
	}
	
	public boolean equals (final TRANF_FIELD field, final String name, final int side, final int seqNum2) {
		return equals (field, name, side, seqNum2, 0);
	}
	
	public boolean equals (final TRANF_FIELD field, final String name, final int side) {
		return equals (field, name, side, 0);
	}
	
	public boolean equals (final TRANF_FIELD field, final String name) {
		return equals (field, name, 0);
	}
	

	public TranFieldIdentificator (final TRANF_FIELD field, final String name, final int side, final int seqNum2,
			final int seqNum3, final int seqNum4) {
		this(field, name, side, seqNum2, seqNum3, seqNum4, 0);
	}

	public TranFieldIdentificator (final TRANF_FIELD field, final String name, final int side, final int seqNum2,
			final int seqNum3) {
		this(field, name, side, seqNum2, seqNum3, 0);
	}

	public TranFieldIdentificator (final TRANF_FIELD field, final String name, final int side, final int seqNum2) {
		this(field, name, side, seqNum2, 0);
	}

	public TranFieldIdentificator (final TRANF_FIELD field, final String name, final int side) {
		this(field, name, side, 0);
	}
	
	public TranFieldIdentificator (final TRANF_FIELD field, final String name) {
		this(field, name, 0);
	}

	public TRANF_FIELD getField() {
		return field;
	}

	public int getSide() {
		return side;
	}

	public int getSeqNum2() {
		return seqNum2;
	}

	public int getSeqNum3() {
		return seqNum3;
	}

	public int getSeqNum4() {
		return seqNum4;
	}

	public int getSeqNum5() {
		return seqNum5;
	}

	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + seqNum2;
		result = prime * result + seqNum3;
		result = prime * result + seqNum4;
		result = prime * result + seqNum5;
		result = prime * result + side;
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
		TranFieldIdentificator other = (TranFieldIdentificator) obj;
		if (field != other.field)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (seqNum2 != other.seqNum2)
			return false;
		if (seqNum3 != other.seqNum3)
			return false;
		if (seqNum4 != other.seqNum4)
			return false;
		if (seqNum5 != other.seqNum5)
			return false;
		if (side != other.side)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "field=" + field + ", side=" + side + ", seqNum2=" + seqNum2
				+ ", seqNum3=" + seqNum3 + ", seqNum4=" + seqNum4
				+ ", seqNum5=" + seqNum5 + ", name=" + name;
	}
}
