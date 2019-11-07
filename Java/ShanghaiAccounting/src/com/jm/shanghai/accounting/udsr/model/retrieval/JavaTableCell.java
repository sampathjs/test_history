package com.jm.shanghai.accounting.udsr.model.retrieval;


/*
 * History:
 * 2018-11-27		V1.0		jwaechter	- Initial Version
 */

/**
 * Contains the data of a simple cell within the java table.
 * @author jwaechter
 * @version 1.0
 */
public class JavaTableCell {
	private String displayString;
	private Object value;
	private final JavaTableColumn colDef;
	
	public JavaTableCell (final String displayString,
			final Object value, 
			final JavaTableColumn colDef) {
		this.displayString = displayString;
		this.value = value;
		this.colDef = colDef;
	}

	public String getDisplayString() {
		return displayString;
	}

	public Object getValue() {
		return value;
	}

	public JavaTableColumn getColDef() {
		return colDef;
	}
	
	public void setDisplayString(String displayString) {
		this.displayString = displayString;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((colDef == null) ? 0 : colDef.hashCode());
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
		JavaTableCell other = (JavaTableCell) obj;
		if (colDef == null) {
			if (other.colDef != null)
				return false;
		} else if (!colDef.equals(other.colDef))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "JavaTableCell [displayString=" + displayString + ", value="
				+ value + ", colDef=" + colDef + "]";
	}
}
