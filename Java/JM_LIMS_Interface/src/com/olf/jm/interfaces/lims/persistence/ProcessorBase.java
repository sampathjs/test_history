package com.olf.jm.interfaces.lims.persistence;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.olf.embedded.application.Context;
import com.olf.jm.interfaces.lims.model.MeasureDetails;
import com.olf.jm.interfaces.lims.model.MeasureSource;
import com.olf.jm.interfaces.lims.model.MeasuresWithSource;
import com.olf.jm.interfaces.lims.model.MeasuresWithSourceHelper;
import com.olf.jm.interfaces.lims.model.Pair;
import com.olf.jm.interfaces.lims.model.RelNomField;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumPlannedMeasureFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.PlannedMeasure;
import com.olf.openrisk.trading.PlannedMeasures;
import com.openlink.util.logging.PluginLog;

public abstract class ProcessorBase implements LIMSProcessor {

	private Nomination nom;

	private Batch batch;

	private Context context;

	private MeasuresWithSourceHelper helper;

	protected ProcessorBase(Nomination nomToProcess, Context currentContext) {
		nom = nomToProcess;

		batch = (Batch) nomToProcess;

		context = currentContext;

		helper = new MeasuresWithSourceHelper(currentContext);
	}

	@Override
	public boolean coaBypass() {
		String bypass = RelNomField.BYPASS_MEASURES_SYNC.guardedGetString(nom);
		return "Yes".equalsIgnoreCase(bypass);
	}

	@Override
	public boolean specComplete() {
		String specComplete = RelNomField.SPEC_COMPLETE.guardedGetString(nom);
		return "Yes".equalsIgnoreCase(specComplete);
	}

	protected String getBatchNumber() {
		return RelNomField.BATCH_NUMBER.guardedGetString(nom);
	}

	protected String getBatchId() {
		return RelNomField.BATCH_ID.guardedGetString(nom);
	}
	
	protected String getBrand() {
		return RelNomField.BRAND.guardedGetString(nom);
	}

	protected String getCategory() {
		return RelNomField.CATEGORY.guardedGetString(nom);
	}

	protected String getPurity() {
		return RelNomField.PURITY.guardedGetString(nom);
	}

	protected int getCategoryId() {
		return context.getStaticDataFactory().getId(
				EnumReferenceTable.IdxSubgroup, getCategory());
	}

	protected Batch getBatch() {
		return batch;
	}

	protected Context getContext() {
		return context;
	}

	protected Nomination getNomination() {
		return nom;
	}

	protected MeasuresWithSourceHelper getHelper() {
		return helper;
	}

	protected void addPlannedMeasures(MeasuresWithSource resolvedMeasures) {
		PlannedMeasures ms = clearPlannedMeasures(getBatch());

		String purity = RelNomField.PURITY.guardedGetString(getBatch());
		String measurementTypeForPurity = getMeasureTypeForPurity(purity);
		for (String measureType : resolvedMeasures.getUsedMeasureTypes()) {
			List<Pair<MeasureDetails, MeasureSource>> measure = resolvedMeasures
					.getMeasures(measureType);
			measure.get(0).getLeft()
					.updatePlannedMeasure(ms, measurementTypeForPurity);
		}
	}
	
	/**
	 * Resolve inconsistencies.
	 *
	 * @param mws the mws
	 * @return the measures with source
	 */
	protected MeasuresWithSource resolveInconsistencies(MeasuresWithSource mws) {
		try {
			if (!mws.hasCompetingMeasures()) {
				return mws;
			}
			if (mws.hasCompetingSampleIds()) {
				return sampleSelectLogic(mws);
			}
			return measureSelectLogic(mws);

		} catch (OException ex) {
			throw new RuntimeException(
					"OException while asking user to resolve competing measures");
		}
	}
	
	/**
	 * Measure select logic.
	 *
	 * @param mws the mws
	 * @return the measures with source
	 * @throws OException the o exception
	 */
	private MeasuresWithSource measureSelectLogic(MeasuresWithSource mws)
			throws OException {
		com.olf.openjvs.Table askTable = com.olf.openjvs.Table.tableNew("Ask");
		Map<String, Integer> rowsToMeasureTypes = new TreeMap<>();
		int counter = 1;
		for (String competingMT : mws.getMeasureTypesHavingCompetingDetails()) {
			rowsToMeasureTypes.put(competingMT, counter++);
			com.olf.openjvs.Table listTable = generateCompetingMeasuresList(
					competingMT, mws);
			com.olf.openjvs.Table defaultTable = getContext().getTableFactory().toOpenJvs(generateDefaults(competingMT,
					mws));
			int ret;
			ret = Ask.setAvsTable(askTable, listTable, competingMT,
					listTable.getColNum("data"),
					ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(),
					listTable.getColNum("id"), defaultTable,
					"Please select the details for " + competingMT, 1);
		}

		if (Ask.viewTable(askTable, "Competing measure details found for "
				+ mws.getBatchNum() + "/" + mws.getPurity(),
				"Please select the measure details for all of the following measure types") <= 0) {
			throw new RuntimeException(
					"User has cancelled selection of competing measures for "
							+ mws.getBatchNum() + "/" + mws.getPurity());
		}

		for (String competingMT : mws.getMeasureTypesHavingCompetingDetails()) {
			int rowId = rowsToMeasureTypes.get(competingMT);
			com.olf.openjvs.Table returnValueTable = askTable.getTable(
					"return_value", rowId);
			int returnValue = returnValueTable.getInt("return_value", 1);
			selectCompetingMeasure(competingMT, mws, returnValue);
		}
		askTable.destroy();
		return mws;
	}	
	
	/**
	 * Select competing measure.
	 *
	 * @param competingMT the competing mt
	 * @param mws the mws
	 * @param returnValue the return value
	 */
	private void selectCompetingMeasure(String competingMT,
			MeasuresWithSource mws, int returnValue) {
		List<Pair<MeasureDetails, MeasureSource>> compMeasures = mws
				.getMeasures(competingMT);
		Pair<MeasureDetails, MeasureSource> selectedMeasure = compMeasures
				.get(returnValue);
		mws.clear(competingMT);
		mws.addMeasure(selectedMeasure.getLeft(), selectedMeasure.getRight());
	}
	
	/**
	 * Generate competing measures list.
	 *
	 * @param competingMT the competing mt
	 * @param mws the mws
	 * @return the com.olf.openjvs. table
	 * @throws OException the o exception
	 */
	private com.olf.openjvs.Table generateCompetingMeasuresList(
			String competingMT, MeasuresWithSource mws) throws OException {
		com.olf.openjvs.Table listTable = com.olf.openjvs.Table
				.tableNew(competingMT);
		listTable.addCol("id", COL_TYPE_ENUM.COL_INT, "id");
		listTable.addCol("data", COL_TYPE_ENUM.COL_STRING, "Source");
		for (int i = mws.getMeasures(competingMT).size() - 1; i >= 0; i--) {
		//for (int i = 0; i < mws.getMeasures(competingMT).size() ;  i++) {	
			
			Pair<MeasureDetails, MeasureSource> details = mws.getMeasures(
					competingMT).get(i);
			int rowNum = listTable.addRow();
			listTable.setInt("id", rowNum, i);
			String sourceMeasureTypeUnitValue = generateDisplay(details);
			listTable.setString("data", rowNum, sourceMeasureTypeUnitValue);
		}
		return listTable;
	}
	
	/**
	 * Generate display.
	 *
	 * @param details the details
	 * @return the string
	 */
	private String generateDisplay(Pair<MeasureDetails, MeasureSource> details) {
		String lowerValue = details.getLeft().getDetails()
				.get(EnumPlannedMeasureFieldId.LowerValue);
		String lowerValueToShow;
		if (!lowerValue.contains("<") && !lowerValue.contains(">")) {
			lowerValueToShow = ((!lowerValue.equals("ND") && Double
					.parseDouble(lowerValue) == -1.0) ? "" : lowerValue + "/"
					+ details.getLeft().getSampleId());
		} else {
			lowerValueToShow = lowerValue + "/"
					+ details.getLeft().getSampleId();
		}

		String sourceMeasureTypeUnitValue = details.getRight().toString()
				+ " / "
				+ details.getLeft().getMeasureType()
				+ " / "
				+ details.getLeft().getDetails()
						.get(EnumPlannedMeasureFieldId.Unit) + " / "
				+ lowerValueToShow;
		return sourceMeasureTypeUnitValue;
	}	

	/**
	 * Generate defaults.
	 *
	 * @param competingMT the competing mt
	 * @param mws the mws
	 * @return the com.olf.openjvs. table
	 * @throws OException the o exception
	 */
	private Table generateDefaults(String competingMT,
			MeasuresWithSource mws) throws OException {
		Table table = getContext().getTableFactory().createTable("default table for " + competingMT);
		table.addColumn("data", EnumColType.String);
		List<Pair<MeasureDetails, MeasureSource>> measures = mws
				.getMeasures(competingMT);
		int numberInMemoryMeasures = 0;
		for (Pair<MeasureDetails, MeasureSource> measure : measures) {
//			if (measure.getRight() == MeasureSource.IN_MEMORY) {Set the database as the default
			if (measure.getRight() == MeasureSource.DATABASE) {

				numberInMemoryMeasures++;
			}
		}
		if (numberInMemoryMeasures == 1) {
			int rowNum = table.addRows(1);
			for (Pair<MeasureDetails, MeasureSource> measure : measures) {
				// if (measure.getRight() == MeasureSource.IN_MEMORY) { Set the database as the default
				if (measure.getRight() == MeasureSource.DATABASE) {
					String sourceMeasureTypeUnitValue = generateDisplay(measure);
					table.setString("data", rowNum, sourceMeasureTypeUnitValue);
				}
			}
		}
		return table;

	}	
	/**
	 * Sample select logic.
	 *
	 * @param mws the mws
	 * @return the measures with source
	 * @throws OException the o exception
	 */
	private MeasuresWithSource sampleSelectLogic(MeasuresWithSource mws)
			throws OException {
		com.olf.openjvs.Table askTable = com.olf.openjvs.Table.tableNew("Ask");
		com.olf.openjvs.Table sampleIdTable = com.olf.openjvs.Table
				.tableNew("SampleIds");
		sampleIdTable
				.addCol("sample_id", COL_TYPE_ENUM.COL_STRING, "Sample ID");
		for (String sampleId : mws.getCompetingSampleIds()) {
			int row = sampleIdTable.addRow();
			sampleIdTable.setString("sample_id", row, sampleId);
		}

		int ret;
		ret = Ask.setAvsTable(askTable, sampleIdTable, "Select the sample ID",
				sampleIdTable.getColNum("sample_id"),
				ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(),
				sampleIdTable.getColNum("sample_id"), null,
				"Please select the sample ID to used", 1);

		if (Ask.viewTable(askTable,
				"Competing sample IDs found for " + mws.getBatchNum() + "/"
						+ mws.getPurity(),
				"Please select the sample ID for batch " + mws.getBatchNum()
						+ " having purity " + mws.getPurity()) <= 0) {
			throw new RuntimeException(
					"User has cancelled selection of competing sample IDs for "
							+ mws.getBatchNum() + "/" + mws.getPurity());
		}
		com.olf.openjvs.Table returnValueTable = askTable.getTable(
				"return_value", 1);
		String returnValue = returnValueTable.getString("return_value", 1);
		selectCompetingSampleId(mws, returnValue);
		askTable.destroy();
		return mws;
	}
	
	/**
	 * Select competing sample id.
	 *
	 * @param mws the mws
	 * @param returnValue the return value
	 */
	private void selectCompetingSampleId(MeasuresWithSource mws,
			String returnValue) {
		for (String mt : mws.getAllMeasures()) {
			List<Pair<MeasureDetails, MeasureSource>> compMeasures = mws
					.getMeasures(mt);

			Pair<MeasureDetails, MeasureSource> selectedMeasure = null;
			for (Pair<MeasureDetails, MeasureSource> measure : compMeasures) {
				if (measure.getLeft().getSampleId().equals(returnValue)) {
					selectedMeasure = measure;
					break;
				}
			}
			if (selectedMeasure != null) {
				mws.clear(mt);
				mws.addMeasure(selectedMeasure.getLeft(),
						selectedMeasure.getRight());
			} else {
				mws.clear(mt);
			}
		}
	}

	private PlannedMeasures clearPlannedMeasures(Batch nom) {
		PlannedMeasures ms = nom.getBatchPlannedMeasures();
		String purity = RelNomField.PURITY.guardedGetString(nom);
		String measureTypeForIndexSubgroupAndPurity = getMeasureTypeForPurity(purity);
		for (int i = ms.size() - 1; i >= 0; i--) {
			PlannedMeasure measure = ms.getItem(i);
			String measureType = measure
					.getValueAsString(EnumPlannedMeasureFieldId.MeasurementType);
			if (!measureType.equals(measureTypeForIndexSubgroupAndPurity)) {
				ms.remove(i);
			}
		}
		loadDefaultMeasures(ms, purity);
		return ms;
	}

	private String getMeasureTypeForPurity(String purity) {
		String sql = "\nSELECT mt.name FROM idx_subgroup sg"
				+ "\n  INNER JOIN measure_group mg ON mg.idx_subgroup = sg.id_number"
				+ "\n  INNER JOIN measure_group_item mgi ON mgi.measure_group_id = mg.id_number"
				+ "\n  INNER JOIN measurement_type mt ON mt.id_number = mgi.measurement_type"
				+ "\nWHERE mg.name = '" + purity + "'"
				+ "\n  AND mgi.lower_value != -1.0";
		Table sqlResult = null;
		try {
			PluginLog.debug("About to run SQL: " + sql);
			sqlResult = getContext().getIOFactory().runSQL(sql);
			if (sqlResult.getRowCount() == 0) {
				return "";
			}
			return sqlResult.getString("name", 0);
		} finally {
			if (sqlResult != null) {
				sqlResult.dispose();
			}
		}

	}

	private void loadDefaultMeasures(PlannedMeasures ms, String purity) {
		String sql = "\nSELECT mgi.*"
				+ "\nFROM idx_subgroup sg"
				+ "\n  INNER JOIN measure_group mg ON mg.idx_subgroup = sg.id_number"
				+ "\n  INNER JOIN measure_group_item mgi ON mgi.measure_group_id = mg.id_number"
				+ "\nWHERE mg.name = '" + purity + "'"
				+ "\n  AND mgi.lower_value = -1.0";
		Table sqlResult = null;
		try {
			PluginLog.debug("About to run SQL: " + sql);
			sqlResult = getContext().getIOFactory().runSQL(sql);
			for (TableRow row : sqlResult.getRows()) {
				int measurementTypeId = row.getInt("measurement_type");
				int unit = row.getInt("unit");
				double lowerValue = row.getDouble("lower_value");
				double upperValue = row.getDouble("upper_value");
				double minTolerance = row.getDouble("min_tolerance");
				double maxTolerance = row.getDouble("max_tolerance");
				String measureGroupComment = row
						.getString("measure_group_comment");

				PlannedMeasure defMeasure = null;
				boolean created;
				for (PlannedMeasure pm : ms) {
					int mtId = pm
							.getValueAsInt(EnumPlannedMeasureFieldId.MeasurementType);
					if (mtId == measurementTypeId) {
						defMeasure = pm;
						break;
					}
				}
				if (defMeasure == null) {
					defMeasure = ms.addItem();
				}

				Field mtField = defMeasure
						.getField(EnumPlannedMeasureFieldId.MeasurementType);
				Field unitField = defMeasure
						.getField(EnumPlannedMeasureFieldId.Unit);
				Field lowerValueField = defMeasure
						.getField(EnumPlannedMeasureFieldId.LowerValue);
				Field upperValueField = defMeasure
						.getField(EnumPlannedMeasureFieldId.UpperValue);
				Field minToleranceField = defMeasure
						.getField(EnumPlannedMeasureFieldId.MinTolerance);
				Field maxToleranceField = defMeasure
						.getField(EnumPlannedMeasureFieldId.MaxTolerance);
				Field commentField = defMeasure
						.getField(EnumPlannedMeasureFieldId.Comments);

				if (fieldCheck(mtField)) {
					mtField.setValue(measurementTypeId);
				}
				if (fieldCheck(unitField)) {
					unitField.setValue(unit);
				}
				if (fieldCheck(lowerValueField)) {
					lowerValueField.setValue(lowerValue);
				}
				if (fieldCheck(upperValueField)) {
					upperValueField.setValue(upperValue);
				}
				if (fieldCheck(minToleranceField)) {
					minToleranceField.setValue(minTolerance);
				}
				if (fieldCheck(maxToleranceField)) {
					maxToleranceField.setValue(maxTolerance);
				}
				if (fieldCheck(commentField)) {
					commentField.setValue(measureGroupComment);
				}
			}
		} finally {
			if (sqlResult != null) {
				sqlResult.dispose();
			}
		}
	}

	private boolean fieldCheck(Field field) {
		return field != null && field.isApplicable() && field.isWritable()
				&& field.isReadable();
	}
}
