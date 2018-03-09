package com.olf.jm.warehouse.autocontainerid.model;

import java.util.Date;

import com.olf.openrisk.trading.EnumDeliveryTicketFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.scheduling.FieldDescription;
import com.olf.openrisk.trading.DeliveryTicket;

public enum RelDelTicketField {
	ACTIVITY_ID (EnumDeliveryTicketFieldId.Id),
	;
	
	private final String name;
	private final EnumDeliveryTicketFieldId fieldEnum;
	
	
	private RelDelTicketField (final EnumDeliveryTicketFieldId fieldEnum) {
		this.name = null;
		this.fieldEnum = fieldEnum;
	}

	
	public double guardedGetDouble(DeliveryTicket ticket) {
		Field field = getField(ticket);
		if (field != null && field.isApplicable() && field.isReadable()) {
			return field.getValueAsDouble();
		}
		return 0.0;
	}

	public int guardedGetInt(DeliveryTicket ticket) {
		Field field = getField(ticket);
		if (field != null && field.isApplicable() && field.isReadable()) {
			return field.getValueAsInt();
		}
		return -1;
	}

	public String guardedGetString(DeliveryTicket ticket) {
		Field field = getField(ticket);
		if (field != null && field.isApplicable() && field.isReadable()) {
			return field.getValueAsString();
		}
		return "";
	}

	public Date guardedGetDate(DeliveryTicket ticket) {
		Field field = getField(ticket);
		if (field != null && field.isApplicable() && field.isReadable()) {
			return field.getValueAsDateTime();
		}
		return new Date(0);
	}
	
	public boolean matchesFieldDescription (DeliveryTicket ticket, FieldDescription fd) {
		String fieldName = (name != null)?"name":ticket.getFieldName(fieldEnum);
		if (fd.getName().equals(fieldName)) {
			return true;
		} else {
			return false;
		}
	}
	
	public void guardedSet (DeliveryTicket ticket, String newValue) {
		Field field = getField(ticket);
		if (field != null && field.isApplicable() && field.isWritable()) {
			field.setValue(newValue);
		}
	}

	public void guardedSet (DeliveryTicket ticket, int newValue) {
		Field field = getField(ticket);
		if (field != null && field.isApplicable() && field.isWritable()) {
			field.setValue(newValue);
		}
	}

	
	public String getName (DeliveryTicket ticket) {
		Field field = getField (ticket);
		if (field != null) {
			return field.getName();
		}
		return "";
	}
	
	public boolean isWritable (DeliveryTicket ticket) {
		Field field = getField (ticket);
		if (field != null) {
			return field.isWritable();			
		}
		return false;
	}
	
	public String getInfoName () {
		return name;
	}
		
	private Field getField(DeliveryTicket ticket) {
		if (name != null) {
			return ticket.getField(name);
		} else if (fieldEnum != null) {
			return ticket.getField(fieldEnum);
		}
		return null;
	}
}
