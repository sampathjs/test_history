package com.olf.jm.interfaces.lims.model;

import java.util.Date;

import com.olf.openrisk.scheduling.EnumNomfField;
import com.olf.openrisk.scheduling.EnumNominationFieldId;
import com.olf.openrisk.scheduling.Field;
import com.olf.openrisk.scheduling.FieldDescription;
import com.olf.openrisk.scheduling.Nomination;

public enum RelNomField {
	ACTIVITY_ID (EnumNomfField.NomCmotionCsdActivityId, 0),
	BATCH_NUMBER (EnumNominationFieldId.CommodityBatchNumber),
	BRAND (EnumNomfField.NominationCsdBrand, 0),
	CATEGORY (EnumNominationFieldId.CategoryId),
	PURITY("Quality"),
	SPEC_COMPLETE ("Spec Complete"),
	LIMS_LAST_ASK("Internal: LIMS Last Ask"),
	SAMPLE_ID ("Internal: LIMS Sample ID"),
	CAPTURE_MEASURES ("Internal: Capture Measures"),
	NET_QTY(EnumNominationFieldId.BestAvailableVolumeNetQuantity),
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
		String fieldName = (name != null)?"name":nom.getFieldName(fieldEnum);
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

	public void guardedSet (Nomination nom, int newValue) {
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
	
	public boolean isWritable (Nomination nom) {
		Field field = getField (nom);
		if (field != null) {
			return field.isWritable();			
		}
		return false;
	}
	
	public String getInfoName () {
		return name;
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
