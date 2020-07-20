package com.olf.jm.interfaces.lims.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.olf.embedded.application.Context;
import com.olf.jm.interfaces.lims.model.ConfigurationItem;
import com.olf.jm.interfaces.lims.model.LIMSServer;
import com.olf.jm.interfaces.lims.model.MeasureDetails;
import com.olf.jm.interfaces.lims.model.MeasureSource;
import com.olf.jm.interfaces.lims.model.MeasuresWithSource;
import com.olf.jm.interfaces.lims.model.MetalProductTestTableCols;
import com.olf.jm.interfaces.lims.model.OverridableException;
import com.olf.jm.interfaces.lims.model.Pair;
import com.olf.jm.interfaces.lims.model.RelNomField;
import com.olf.jm.interfaces.lims.model.RelevantUserTables;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumPlannedMeasureFieldId;
import com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;


/**
 * The Class JmBrandProcessor. Used to set the measures for JM brand batches
 */
public class JmBrandProcessor extends ProcessorBase {

	/**
	 * Instantiates a new jm brand processor.
	 *
	 * @param nom the nomination to process
	 * @param context the script context
	 */
	public JmBrandProcessor(Nomination nom, Context context) {
		super(nom, context);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.interfaces.lims.persistence.LIMSProcessor#setMeasures()
	 */
	@Override
	public MeasuresWithSource setMeasures() {
		MeasuresWithSource measures = loadDataFromLIMS();

		if (measures.size() > 0) {
			measures.removeMeasureTypes(ConfigurationItem.MANUAL_MEASURES_JM_CASE
					.getValue());

			
			MeasuresWithSource nonManagedMeasures = getHelper()
					.retrieveBatchPlannedMeasures(getBatch());
			nonManagedMeasures
					.removeMeasureTypesExcept(ConfigurationItem.MANUAL_MEASURES_JM_CASE
							.getValue());
			measures.addAllMeasures(nonManagedMeasures);

			MeasuresWithSource resolvedMeasures = resolveInconsistencies(measures);

			addPlannedMeasures(resolvedMeasures);
			RelNomField.SPEC_COMPLETE.guardedSet(getBatch(), "Yes");
			RelNomField.SAMPLE_ID.guardedSet(getNomination(),
					resolvedMeasures.getUniqueSampleId());
			RelNomField.CAPTURE_MEASURES.guardedSet(getNomination(), "Yes");

		} else {
			// Check if a batch exists with the same batch number and spec
			// complete, if so load from there
			
			LIMSProcessor nonLimsMeasuers = new NonJmBrandProcessor(getNomination(), getContext());
			
			measures = nonLimsMeasuers.setMeasures();
			
			if(measures == null || measures.size() < 1){

				String metal = idxSubgroupToMeasurementTypeName(getCategoryId());
				showNoSampleFoundMessage(metal);
			}
		}
		
		return measures;

	}





	/**
	 * Load data from lims.
	 *
	 * @return the measures with source
	 */
	private MeasuresWithSource loadDataFromLIMS() {
		// LIMSLocalInterface local = new LIMSLocalInterface(context);

		String metal = idxSubgroupToMeasurementTypeName(getCategoryId());

		MeasuresWithSource batchAndMeasure = processServer(LIMSServer.LIMS_UK,
				metal, "United Kingdom");

		if (batchAndMeasure.size() == 0) {
			return batchAndMeasure;
		}
		RelNomField.BATCH_NUMBER.guardedSet(getNomination(),
				batchAndMeasure.getBatchNum());
		getHelper().condensePlanMeasures(batchAndMeasure);
		return batchAndMeasure;
	}

	/**
	 * Show no sample found message.
	 *
	 * @param metal the metal
	 */
	private void showNoSampleFoundMessage(String metal) {
		String message = "Cound not find sample for metal '"
				+ metal
				+ "'"
				+ " in the LIMS database."
				+ " Please enter batch measures manually or enter the correct batch num";
		throw new OverridableException(message);
	}

	/**
	 * Process server.
	 *
	 * @param server the server
	 * @param metal the metal
	 * @param country the country
	 * @return the measures with source
	 */
	private MeasuresWithSource processServer(final LIMSServer server,
			final String metal, final String country) {
		Table productsForTable = null;
		Table samples = null;
		LIMSRemoteInterface remote = new LIMSRemoteInterface(getContext(),
				server);

		String batchNumber = getBatchNumber();
		String purity = getPurity();

		try {
			productsForTable = loadMetalProductTestsForMetal(metal, country);
			if (productsForTable.getRowCount() == 0) {
				return new MeasuresWithSource(batchNumber, purity,
						RelNomField.BRAND.guardedGetString(getBatch()));
			}
			Map<String, String> productsToTests = new HashMap<>();
			for (TableRow row : productsForTable.getRows()) {
				productsToTests.put(row
						.getString(MetalProductTestTableCols.PRODUCT
								.getColName()), row
						.getString(MetalProductTestTableCols.RESULT
								.getColName()));
			}
			samples = remote
					.loadSampleIDsFromLims(batchNumber, productsToTests);
			if (samples.getRowCount() == 0) {
				return new MeasuresWithSource(batchNumber, purity,
						RelNomField.BRAND.guardedGetString(getBatch()));
			}
			Pair<String, Pair<String, String>> sampleBatchAnalysis = new Pair<>(
					Integer.toString(samples.getInt("SAMPLE_NUMBER", 0)),
					new Pair<>(samples.getString("JM_BATCH_ID", 0),
							samples.getString("ANALYSIS", 0)));

			if (samples.getRowCount() > 1) {
				String savedSampleId = (RelNomField.SAMPLE_ID
						.guardedGetString(getBatch()) != null && !RelNomField.SAMPLE_ID
						.guardedGetString(getBatch()).trim().equals("")) ? RelNomField.SAMPLE_ID
						.guardedGetString(getBatch()) : "0";
				int rowId = samples.find(samples.getColumnId("SAMPLE_NUMBER"),
						Integer.parseInt(savedSampleId), 0);
				if (rowId == -1) {
					sampleBatchAnalysis = userSampleSelect(samples, batchNumber);
				} else {
					String selectedBatch = samples.getString("JM_BATCH_ID",
							rowId);
					String selectedAnalysis = samples.getString("ANALYSIS",
							rowId);
					sampleBatchAnalysis = new Pair<>(savedSampleId, new Pair<>(
							selectedBatch, selectedAnalysis));
				}
			}
			RelNomField.SAMPLE_ID.guardedSet(getBatch(),
					sampleBatchAnalysis.getLeft());
			MeasuresWithSource ret = remote.loadPlannedMeasureDetailsFromLims(
					sampleBatchAnalysis.getLeft(), sampleBatchAnalysis
							.getRight().getLeft(), sampleBatchAnalysis
							.getRight().getRight(), purity, RelNomField.BRAND
							.guardedGetString(getBatch()));
			return ret;
		} finally {
			if (productsForTable != null) {
				productsForTable.dispose();
				productsForTable = null;
			}
			if (samples != null) {
				samples.dispose();
				samples = null;
			}
		}
	}

	/**
	 * User sample select.
	 *
	 * @param samples the samples
	 * @param batchNum the batch num
	 * @return the pair
	 */
	private Pair<String, Pair<String, String>> userSampleSelect(Table samples,
			String batchNum) {
		com.olf.openjvs.Table askTable = null;
		try {
			askTable = com.olf.openjvs.Table.tableNew("ask table");
			com.olf.openjvs.Table dataTable = getContext().getTableFactory()
					.toOpenJvs(samples);

			int ret = Ask.setAvsTable(askTable, dataTable, "Sample", 1,
					ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1, null,
					"More than one sample has been found for batch number "
							+ batchNum + ". Please select a single one", 1);

			if (Ask.viewTable(askTable, "LIMs Query", ""
					+ "Please Complete the Following Fields") == 0) {
				throw new RuntimeException(
						"Selectio of Samples retrieved by LIMS interface cancelled by User");
			}
			com.olf.openjvs.Table temp = askTable.getTable("return_value", 1);
			String selectedSample = temp.getString("ted_str_value", 1);
			String selectedBatch = samples.getString("JM_BATCH_ID",
					samples.find(0, Integer.parseInt(selectedSample), 0));
			String selectedAnalysis = samples.getString("ANALYSIS",
					samples.find(0, Integer.parseInt(selectedSample), 0));
			return new Pair<>(selectedSample, new Pair<>(selectedBatch,
					selectedAnalysis));
		} catch (OException e) {
			throw new RuntimeException("Error asking user to select sample: "
					+ e.toString());
		} finally {
			askTable = TableUtilities.destroy(askTable);
		}
	}










	/**
	 * Retrieves the name of a metal given an Index Subgroup Id.
	 *
	 * @param idxSubGroupId the idx sub group id
	 * @return the string
	 */
	public String idxSubgroupToMeasurementTypeName(int idxSubGroupId) {
		StringBuilder sql = new StringBuilder("\nSELECT mt.name \nFROM ");
		sql.append("idx_subgroup idx");
		sql.append("\nINNER JOIN measurement_type mt");
		sql.append("\n  ON mt.description = idx.name");
		sql.append("\nWHERE idx.id_number = " + idxSubGroupId);
		Table measurement = null;
		try {
			Logging.debug("About to run sql: " + sql.toString());
			measurement = getContext().getIOFactory().runSQL(sql.toString());
			if (measurement.getRowCount() == 0) {
				throw new IllegalArgumentException(
						"Could not retrieve the measurement type name for idx_subgroup with ID "
								+ idxSubGroupId);
			}

			if (measurement.getRowCount() > 1) {
				throw new IllegalArgumentException(
						"Retrieved more than one measurement type name for idx_subgroup with ID "
								+ idxSubGroupId
								+ ". This needs to be a 1:1 relationship.");
			}
			return measurement.getString(0, 0);
		} finally {
			if (measurement != null) {
				measurement.dispose();
				measurement = null;
			}
		}
	}

	/**
	 * Load metal product tests used in LIM for a given metal.
	 *
	 * @param metal the metal to file the rests for
	 * @param country the country the tests are held for
	 * @return the table containing the LIM tests
	 */
	public Table loadMetalProductTestsForMetal(String metal, String country) {
		String sql = getSQLToLoadProductTests(metal, country);
		
		Logging.debug("About to run SQL: " + sql);
		return getContext().getIOFactory().runSQL(sql);
	}

	/**
	 * Gets the SQL string to load product tests used in LIMS.
	 *
	 * @param metal the metal
	 * @param country the country
	 * @return the SQL to load product tests
	 */
	private String getSQLToLoadProductTests(String metal, String country) {
		StringBuilder sb = new StringBuilder("\nSELECT * FROM ");
		sb.append(RelevantUserTables.JM_METAL_PRODUCT_TEST.getName());
		sb.append("\nWHERE ");
		sb.append("\n	").append(MetalProductTestTableCols.METAL.getColName())
				.append("= '").append(metal).append("'");
		sb.append("\n	AND ")
				.append(MetalProductTestTableCols.COUNTRY.getColName())
				.append("= '").append(country).append("'");
		
		return sb.toString();
	}
}
