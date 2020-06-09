package com.jm.shanghai.accounting.udsr.gui;

import java.util.Date;

/*
 * History:
 * 2020-02-12	V1.0	jwaechter		- Initial Version
 */

/**
 * This class stored generic history entries from user input of the Accounting Operators GUI.
 * @author jwaechter
 * @version 1.0
 */
public class GuiHistoryEntry {
	private final String type;
	private final int entryNo;
	private final String value;
	private final String personnelName;
	private final Date lastUpdate;
	
	public GuiHistoryEntry (final String type, final int entryNo, final String value, 
			final String personnelName, final Date lastUpdate) {
		this.type = type;
		this.entryNo = entryNo;
		this.value = value;
		this.personnelName = personnelName;
		this.lastUpdate = lastUpdate;
	}

	public String getType() {
		return type;
	}

	public int getEntryNo() {
		return entryNo;
	}

	public String getValue() {
		return value;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public String getPersonnelName() {
		return personnelName;
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
		GuiHistoryEntry other = (GuiHistoryEntry) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}
