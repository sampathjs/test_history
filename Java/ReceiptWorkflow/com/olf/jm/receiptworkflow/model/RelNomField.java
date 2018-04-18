package com.olf.jm.receiptworkflow.model;

import java.util.Date;

import com.olf.openrisk.scheduling.EnumNomfField;
import com.olf.openrisk.scheduling.EnumNominationFieldId;
import com.olf.openrisk.scheduling.Field;
import com.olf.openrisk.scheduling.FieldDescription;
import com.olf.openrisk.scheduling.Nomination;

/*
 * History:
 * 2015-MM-DD	V1.0	jwaechter	- Initial Version
 * 2016-02-18	V1.1	jwaechter	- added WAREHOUSE_LOCATION
 * 2016-03-01	V1.2	jwaechter	- added CONTAINER_COUNTER 
 *                                  - added BATCH_ID
 */

/**
 * Data found on nominations and batches.
 * @author jwaechter
 * @version 1.1
 */
public enum RelNomField {
	WAREHOUSE_DEAL(EnumNominationFieldId.ServiceProviderDealNumber),
	RECEIPT_DATE(EnumNominationFieldId.MovementDate),
	RECEIPT_DEAL (EnumNomfField.NominationReceiptDealDealNum, 0),
	PURITY("Quality"),
	FORM(EnumNominationFieldId.CommodityForm),
	BRAND("Brand"),
	VOLUME_FIELD(EnumNominationFieldId.BestAvailableVolumeNetQuantity),
	UNIT (EnumNominationFieldId.Unit),
	COUNTERPARTY ("Counterparty"),
	PRODUCT (EnumNominationFieldId.CategoryId),
	BATCH_RECEIPT_DATE (EnumNominationFieldId.StartDate),
	ACTIVITY_ID (EnumNomfField.NomCmotionCsdActivityId, 0),
	BATCH_NUMBER (EnumNominationFieldId.CommodityBatchNumber),
	BATCH_ID (EnumNominationFieldId.BatchId),
	WAREHOUSE_LOCATION (EnumNomfField.NominationDeliveryLocationId, 0),
	CONTAINER_COUNTER (EnumNominationFieldId.NumberContainers),
	DELIVERY_ID(EnumNominationFieldId.Id)
	;
	
	private final String name;
	private final EnumNominationFieldId fieldEnum;
	private final EnumNomfField nomfFieldEnum;
	private final int seqNum2;
	
	private RelNomField (final EnumNominationFieldId fieldEnum) {
		this.name = null;
		this.fieldEnum = fieldEnum;
		this.nomfFieldEnum = null; 
		this.seqNum2 = -1;
	}

	private RelNomField (final String name) {
		this.name = name;
		this.fieldEnum = null;
		this.nomfFieldEnum = null; 
		this.seqNum2 = -1;
	}

	private RelNomField (final EnumNomfField nomfField, int seqNum) {
		this.name = null;
		this.fieldEnum = null;
		this.nomfFieldEnum = nomfField; 
		this.seqNum2 = seqNum;
		
	}
	
	public double guardedGetDouble(Nomination nom) {
		Field field = getField(nom);
		if (field != null && field.isApplicable() && field.isReadable()) {
			return field.getValueAsDouble();
		}
		return 0.0;
	}

	public int guardedGetInt(Nomination nom) {
		Field field = getField(nom);
		if (field != null && field.isApplicable() && field.isReadable()) {
			return field.getValueAsInt();
		}
		return -1;
	}

	public String guardedGetString(Nomination nom) {
		Field field = getField(nom);
		if (field != null && field.isApplicable() && field.isReadable()) {
			return field.getValueAsString();
		}
		return "";
	}

	public Date guardedGetDate(Nomination nom) {
		Field field = getField(nom);
		if (field != null && field.isApplicable() && field.isReadable()) {
			return field.getValueAsDateTime();
		}
		return new Date(0);
	}
	
	public boolean matchesFieldDescription (Nomination nom, FieldDescription fd) {
		String fieldName = (name != null)?name:nom.getFieldName(fieldEnum);
		if (fd.getName().equals(fieldName)) {
			return true;
		} else {
			return false;
		}
	}
	
	public void guardedSet (Nomination nom, String newValue) {
		Field field = getField(nom);
		if (field != null && field.isApplicable() && field.isWritable()) {
			field.setValue(newValue);
		}
	}
	
	public String getName (Nomination nom) {
		Field field = getField (nom);
		if (field != null) {
			return field.getName();
		}
		return "";
	}
		
	private Field getField(Nomination nom) {
		if (name != null) {
			return nom.getField(name);
		} else if (fieldEnum != null) {
			return nom.getField(fieldEnum);
		} else if (nomfFieldEnum != null) {
			return nom.retrieveField(nomfFieldEnum, seqNum2);
		}
		return null;
	}
}
