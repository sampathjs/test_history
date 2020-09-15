package com.olf.jm.interfaces.lims.model;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.olf.embedded.application.Context;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumPlannedMeasureFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.PlannedMeasure;
import com.olf.jm.logging.Logging;

public class MeasuresWithSourceHelper {
	
	private Context context;
	
	public MeasuresWithSourceHelper(Context currentrContext) {
		context = currentrContext;
	}

	public MeasuresWithSource retrieveBatchPlannedMeasures(
			Batch batch) {
		final MeasuresWithSource measures = new MeasuresWithSource(
				RelNomField.BATCH_NUMBER.guardedGetString(batch),
				RelNomField.PURITY.guardedGetString(batch),
				RelNomField.BRAND.guardedGetString(batch));
		for (PlannedMeasure pm : batch.getBatchPlannedMeasures()) {
			String measureType = pm.getValueAsString(EnumPlannedMeasureFieldId.MeasurementType);
			Map<EnumPlannedMeasureFieldId, String> measure = new TreeMap<>();
			for (EnumPlannedMeasureFieldId fid : EnumPlannedMeasureFieldId.values()) {
				Field field = pm.getField(fid);
				if (field.isApplicable() && field.isReadable()) {
					measure.put(fid, field.getValueAsString());
				}
			}
			MeasureDetails md = new MeasureDetails(measureType, measure, RelNomField.SAMPLE_ID.guardedGetString(batch));
			measures.addMeasure(md, MeasureSource.IN_MEMORY);
		}
		return measures;
	}	
	
	public void condensePlanMeasures(
			final MeasuresWithSource measures) {
		Set<String> knownMeasurementTypes = getKnownMeasurementTypes();
		Set<String> usedMeasurementTypes = measures.getUsedMeasureTypes();
		for (String mt : usedMeasurementTypes) {
			if (!knownMeasurementTypes.contains(mt)) {
				measures.removeMeasures(mt);
				Logging.info("Removed measurement type '" + mt  + "' as it is not set up in Endur");
				continue;
			}
			measures.distinct(mt);
		}
	}	
	
	
	private Set<String> getKnownMeasurementTypes() {
		String sql = "SELECT name FROM measurement_type";
		Table mts = null;
		Set<String> knownMeasureGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		try {
			mts = context.getIOFactory().runSQL(sql);			
			for (TableRow row : mts.getRows()) {
				String mt = row.getString("name");
				knownMeasureGroups.add(mt);
			}			
			return knownMeasureGroups;
		} finally {
			if (mts != null) {
				mts.dispose();
			}
		}
	}	
}
