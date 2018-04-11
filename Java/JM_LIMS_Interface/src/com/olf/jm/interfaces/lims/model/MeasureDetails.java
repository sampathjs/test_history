package com.olf.jm.interfaces.lims.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.trading.EnumPlannedMeasureFieldId;
import com.olf.openrisk.trading.PlannedMeasure;
import com.olf.openrisk.trading.PlannedMeasures;
/*
 * History:
 * 2015-MM-DD	V1.0	jwaechter	- Initial Version
 * 2016-02-09	V1.1	jwaechter	- modified equalsRequiredFields to work with < and >
 */

/**
 * List of properties of a measure for a certain measure type. 
 * Natural ordering and equals / hash code by measure type only.
 * @author jwaechter
 * @version 1.1
 */
public class MeasureDetails implements Comparable<MeasureDetails>{
	private static final EnumPlannedMeasureFieldId[] REQUIRED_FIELDS = 
		{ EnumPlannedMeasureFieldId.MeasurementType, EnumPlannedMeasureFieldId.Unit, EnumPlannedMeasureFieldId.LowerValue };
	
	private static final Set<EnumPlannedMeasureFieldId> REQUIRED_FIELDS_SET = new TreeSet<>(Arrays.asList(REQUIRED_FIELDS));
	
	private final Map<EnumPlannedMeasureFieldId, String> details; // fields, including measure type
	private final String measureType; // primary key
	private final String sampleId;
	
	public MeasureDetails (String measureType, String sampleId) {
		details = new TreeMap<>();
		this.measureType = measureType;
		this.sampleId = sampleId;
		details.put(EnumPlannedMeasureFieldId.MeasurementType, measureType);
	}
	
	public MeasureDetails (String measureType, Map<EnumPlannedMeasureFieldId, String> details, String sampleId) {
		this(measureType, sampleId);
		addDetails (details);
	}
	
	public void updatePlannedMeasure (PlannedMeasures measures, String measurementTypeForPurity) {
		if (details.get(EnumPlannedMeasureFieldId.MeasurementType).equals(measurementTypeForPurity)) {
			return; // don't change purity bases measure
		}
		PlannedMeasure pm = findPlannedMeasure(measures);
		for (int i=0; i < REQUIRED_FIELDS.length; i++) {
			EnumPlannedMeasureFieldId fid = REQUIRED_FIELDS[i];
			Field field = pm.getField(fid);
			String value = details.get(fid);
			if (field != null && field.isApplicable() && field.isWritable() && field.isReadable()
				&& value != null && !value.equals("")) {
				
				pm.setValue(fid, value);		
				
			} else {
//				throw new RuntimeException ("Required field can't be set to value " + value + " : " + field.toString());
			}
		}
		for (EnumPlannedMeasureFieldId fid : details.keySet()) {
			if (!REQUIRED_FIELDS_SET.contains(fid)) {
				Field field = pm.getField(fid);
				String value = details.get(fid);
				if (field != null && field.isApplicable() && field.isWritable() &&
						value != null && !value.equals("")) {
					pm.setValue(fid, value);				
				}
			}
		}		
	}
	
	public PlannedMeasure findPlannedMeasure(PlannedMeasures measures) {
		for (PlannedMeasure pm : measures) {
			if (pm.getValueAsString(EnumPlannedMeasureFieldId.MeasurementType).equals(measureType)) {
				return pm;
			}
		}
		return measures.addItem();
	}

	public String getSampleId() {
		return sampleId;
	}

	public void addDetails (Map<EnumPlannedMeasureFieldId, String> details) {
		this.details.putAll(details);
	}

	public void addDetail (EnumPlannedMeasureFieldId field, String value) {
		this.details.put(field, value);
	}
	
	public Map<EnumPlannedMeasureFieldId, String> getDetails() {
		return details;
	}
	
	public String getMeasureType () {
		return measureType;
	}
	
	/**
	 * Checks if all details are matching
	 * @param right
	 * @return
	 */
	public boolean equalsAll (MeasureDetails right) {
		return measureType.equals(right.measureType) && details.equals(right.details);
	}

	public boolean equalsRequiredFields (MeasureDetails right) {
		boolean equals = true;
		
		for (EnumPlannedMeasureFieldId pmfid : REQUIRED_FIELDS) {
			if (pmfid != EnumPlannedMeasureFieldId.LowerValue) {
				equals &= details.get(pmfid).equals(right.details.get(pmfid));
				if (!equals) {
					break;
				}
			} else {
				String value1 = details.get(pmfid);
				String value2 = right.details.get(pmfid);
				if ("ND".equals(value1)) {
					value1 = "0.0";
				}
				if ("ND".equals(value2)) {
					value2 = "0.0";
				}
				if (value1.contains(">") && value2.contains(">")) {
					value1 = value1.replaceAll(">", "").trim();
					value2 = value2.replaceAll(">", "").trim();
				} else if (value1.contains("<") && value2.contains("<")) {
					value1 = value1.replaceAll("<", "").trim();
					value2 = value2.replaceAll("<", "").trim();
				} else if (value1.contains("<") && !value2.contains("<"))  {
					equals = false;
					break;
				} else if (value1.contains(">") && !value2.contains(">"))  {
					equals = false;
					break;
				} else if (value2.contains("<") && !value1.contains("<"))  {
					equals = false;
					break;
				} else if (value2.contains(">") && !value1.contains(">"))  {
					equals = false;
					break;
				}
				
				double v1 = Double.parseDouble(value1);
				double v2 = Double.parseDouble(value2);
				equals &= v1 == v2;
			}
		}
		return equals;
	}
	
	public boolean hasRequiredFields () {
		for (EnumPlannedMeasureFieldId pmfid : REQUIRED_FIELDS) {
			if (!details.containsKey(pmfid) || details.get(pmfid) == null || details.get(pmfid).equals("")) {
				return false;
			}
		}
		return true;
	}
	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((measureType == null) ? 0 : measureType.hashCode());
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
		MeasureDetails other = (MeasureDetails) obj;
		if (measureType == null) {
			if (other.measureType != null)
				return false;
		} else if (!measureType.equals(other.measureType))
			return false;
		return true;
	}
	
	public boolean isMeasureType (String measureType) {
		return this.measureType.equals(measureType);
	}

	@Override
	public int compareTo(MeasureDetails right) {
		return measureType.compareTo(right.measureType);
	}

	@Override
	public String toString() {
		return " measureType="
				+ measureType + " sample ID= '" + sampleId  + "' " + "details=" + details;
	}
}