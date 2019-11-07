package com.olf.jm.interfaces.lims.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.olf.jm.interfaces.lims.model.ConfigurationItem;
import com.olf.jm.interfaces.lims.model.LIMSServer;
import com.olf.jm.interfaces.lims.model.MeasureDetails;
import com.olf.jm.interfaces.lims.model.MeasureSource;
import com.olf.jm.interfaces.lims.model.MeasuresWithSource;
import com.olf.jm.interfaces.lims.model.MetalProductTestTableCols;
import com.olf.jm.interfaces.lims.model.OverridableException;
import com.olf.jm.interfaces.lims.model.Pair;
import com.olf.jm.interfaces.lims.model.RelNomField;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.staticdata.Address;
import com.olf.openrisk.staticdata.Country;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.staticdata.SecurityGroup;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumPlannedMeasureFieldId;
import com.olf.openrisk.trading.PlannedMeasures;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2015-MM-DD	V1.0	jwaechter	- Initial Versions
 * 2016-04-12	V1.1	jwaechter	- Added changes for CR about making LOI and LOR 
 *                                    managed manually in case of LIMS based measures.
 */

/**
 * Class containing helper methods that are used in the OPS definitions.
 * @author jwaechter
 * @version 1.1
 */
public class LIMSOpsInterface {
	public static final String CLIENT_DATA_COL_NAME_LIMS = "LIMS";
	private final Session session;
	
	public LIMSOpsInterface (final Session session) {
		this.session = session;
	}

	public void processInMemory(Nominations nominations,
			Transactions transactions, Table userData) {
		final LIMSLocalInterface local = new LIMSLocalInterface(session);
//		ensureUserTableDataStructure(userData);

		List<MeasuresWithSource> batchesAndMeasures = new ArrayList<>(); 
		for (Nomination nom : nominations) {			
			try {
				if (!isRelevant (nom)) {
					continue;
				}
				Batch batch = (Batch) nom;

				final String batchNumber = RelNomField.BATCH_NUMBER.guardedGetString(nom);
				final String brand = RelNomField.BRAND.guardedGetString(nom);
				final String category = RelNomField.CATEGORY.guardedGetString(nom);
				final String purity = RelNomField.PURITY.guardedGetString(nom);
				MeasuresWithSource measures;

				if (isJohnsonMattheyBrand(nom)) {
					// JM Case
					measures = applyRemoteLogic( batch, batchNumber, category);
					measures.removeMeasureTypes(ConfigurationItem.MANUAL_MEASURES_JM_CASE.getValue());

					RelNomField.SPEC_COMPLETE.guardedSet(batch, "Yes");
					MeasuresWithSource nonManagedMeasures = local.retrieveBatchPlannedMeasures(batch);
					nonManagedMeasures.removeMeasureTypesExcept(ConfigurationItem.MANUAL_MEASURES_JM_CASE.getValue());
					measures.addAllMeasures(nonManagedMeasures);
				} else {
					if (!local.isThereAnyOtherExistingBatchWithSameBatchNum(batchNumber, purity, brand, nominations)) {
						// Non JM Case - and no other existing batches, no post process needed
						// nothing to do, measures are already in place as inputed by user
						measures = local.retrieveBatchPlannedMeasures(batch);
//						measures.clear();
					} else {
						if (didBatchNumOrPurityOrBrandChange (batch)) {
							// creation case
							measures = applyLocalLogic(batch, batchNumber, purity, category);
//							measures.clear();
						} else {
							// merge case
							measures = local.retrieveBatchPlannedMeasures (batch);
							measures.addAllMeasures(applyLocalLogic(batch, batchNumber, purity, category));
						}
					}
				}

				if (measures.size() > 0) {
					if (!batchesAndMeasures.contains(measures)) {
						batchesAndMeasures.add(measures);
					} else {
						int index = batchesAndMeasures.indexOf(measures);
						batchesAndMeasures.get(index).addAllMeasures(measures);
					}
				}
			} finally {
				int counter = RelNomField.LIMS_LAST_ASK.guardedGetInt(nom);
				counter--;
				RelNomField.LIMS_LAST_ASK.guardedSet(nom, counter);				
			}
		}
		if (batchesAndMeasures.size() > 0) {
			for (MeasuresWithSource mws : batchesAndMeasures) {
				local.condensePlanMeasures(mws);
				MeasuresWithSource resolvedMeasures = local.resolveInconsistencies(mws);
				setResolvedMeasuresOnInMemoryNoms ( local, nominations, resolvedMeasures);
//				addMeasuresForPaddMeasuresForPostProcessingostProcessing ( userData, resolvedMeasures, nominations);
			}
		}
	}

	public void processDatabaseNoms(Table clientData) {
		Collection<Integer> manuallyLoadedBatches = loadBatches(session, clientData);
		applyPostProcessLogicOnNomsInMemory(manuallyLoadedBatches, clientData);		
	}
	
	
	private boolean didBatchNumOrPurityOrBrandChange(Batch batch) {
		int batchId = batch.getBatchId();
		if (batchId <= 0) {
			return true; // new batch
		}
		int deliveryId = batch.getId();
		Batch oldBatch = session.getSchedulingFactory().retrieveBatchByDeliveryId(deliveryId);
		String batchNum = RelNomField.BATCH_NUMBER.guardedGetString(batch);
		String oldBatchNum = RelNomField.BATCH_NUMBER.guardedGetString(oldBatch);
		String purity = RelNomField.PURITY.guardedGetString(batch);
		String oldPurity = RelNomField.PURITY.guardedGetString(oldBatch);
		String brand = RelNomField.BRAND.guardedGetString(batch);
		String oldBrand = RelNomField.BRAND.guardedGetString(oldBatch);

		if (batchNum.equals(oldBatchNum) && purity.equals(oldPurity) && brand.equals(oldBrand)) {
			return false;
		}
		return true;		
	}

	private void setResolvedMeasuresOnInMemoryNoms(
			LIMSLocalInterface local, Nominations nominations, MeasuresWithSource resolvedMeasures) {
		for (Nomination nom : nominations) {
			if (isRelevant(nom)) {
				String batchNum = RelNomField.BATCH_NUMBER.guardedGetString(nom);
				String purity = RelNomField.PURITY.guardedGetString(nom);
				String brand = RelNomField.BRAND.guardedGetString(nom);
				if (resolvedMeasures.getBatchNum().equals(batchNum) &&
					resolvedMeasures.getPurity().equals(purity) &&
					resolvedMeasures.getBrand().equals(brand)
					) {
					local.addPlannedMeasures((Batch)nom, resolvedMeasures);
					RelNomField.SAMPLE_ID.guardedSet(nom, resolvedMeasures.getUniqueSampleId());
					RelNomField.CAPTURE_MEASURES.guardedSet(nom, "Yes");
				}
			}
		}
	}

	private boolean isJohnsonMattheyBrand(Nomination nom) {
		return RelNomField.BRAND.guardedGetString(nom).contains("Johnson Matthey");
	}
	
	private Collection<Integer> loadBatches(Session session, Table clientData) {
		Collection<Integer> batchesToProcess = new ArrayList<>();
		for (TableRow row : clientData.getRows()) {
			int deliveryId = row.getInt("delivery_id");
			batchesToProcess.add(deliveryId);
		}
		return batchesToProcess;
	}

	private void applyPostProcessLogicOnNomsInMemory(Collection<Integer> manuallyLoadedBatches, Table clientData) {
		LIMSLocalInterface local = new LIMSLocalInterface(session);
		for (Integer delId : manuallyLoadedBatches) {
			Batch batch = null;
			try {
				batch = session.getSchedulingFactory().retrieveBatchByDeliveryId(delId);
				boolean modified = false;
				String purity = RelNomField.PURITY.guardedGetString(batch);
				int deliveryId = batch.getId();
				PlannedMeasures measures;
				try {
					measures = batch.getBatchPlannedMeasures();
				} catch (Exception ex) {
					PluginLog.error(ex.toString());
					continue;
				}
				for (TableRow cdRow : clientData.getRows()) {
					int deliveryIdCd = cdRow.getInt("delivery_id");
					if (deliveryIdCd == deliveryId) {
						int counter = cdRow.getInt("counter");
						PluginLog.info("DeliveryID = " + deliveryId + " counter = " + counter);
						RelNomField.LIMS_LAST_ASK.guardedSet(batch, counter);
						Table measuresTable = cdRow.getTable("measures");
						String measureTypeForPurity = local.getMeasureTypeForPurity(purity);
						String sampleId = cdRow.getString("sample_id");
						String brand = cdRow.getString("brand");
						String existingSampleId = RelNomField.SAMPLE_ID.guardedGetString(batch);
						String comSepListExcludedMeasures;
						if (!sampleId.equals(existingSampleId)) {
							RelNomField.SAMPLE_ID.guardedSet(batch, sampleId);
							modified = true;
						} 
						if (sampleId.length() > 0) {
							comSepListExcludedMeasures = ConfigurationItem.MANUAL_MEASURES_JM_CASE.getValue();
						} else {
							comSepListExcludedMeasures = ConfigurationItem.MANUAL_MEASURES_NON_JM_CASE.getValue();
						}
						modified |= local.updatePlannedMeasuresOnBatch (measures, measuresTable,
								measureTypeForPurity, comSepListExcludedMeasures);
						break;
					}
				}
				if (modified) {
					batch.save();
				}				
			} finally {
				if (batch != null) {
					batch.dispose();					
				}
			}
		}
	}



	private void ensureUserTableDataStructure(final Table userData) {
		boolean containsLIMSCol = hasLimsClientDataTable(userData);
		if (!containsLIMSCol) {
			userData.addColumn(CLIENT_DATA_COL_NAME_LIMS, EnumColType.Table);
		}
		if (userData.getRowCount() == 0) {
			userData.addRows(1);
		}
		Table limsClientData = userData.getTable(CLIENT_DATA_COL_NAME_LIMS, 0);
		if (limsClientData == null) {
			limsClientData = session.getTableFactory().createTable("LIMS client data table");
			userData.setTable(CLIENT_DATA_COL_NAME_LIMS, 0, limsClientData);
		}
		limsClientData.clear();
		limsClientData.addColumn("batch_id", EnumColType.Int);		
		limsClientData.addColumn("delivery_id", EnumColType.Int);		
		limsClientData.addColumn("batch_num", EnumColType.String);		
		limsClientData.addColumn("purity", EnumColType.String);		
		limsClientData.addColumn("brand", EnumColType.String);		
		limsClientData.addColumn("counter", EnumColType.Int);
		limsClientData.addColumn("sample_id", EnumColType.String);
		limsClientData.addColumn("measures", EnumColType.Table);
	}

	private boolean hasLimsClientDataTable(final Table userData) {
		boolean containsLIMSCol = false;
		for (String colName : userData.getColumnNames().split(",")) {
			if (colName.equals(CLIENT_DATA_COL_NAME_LIMS)) {
				containsLIMSCol = true;
			}
		}
		return containsLIMSCol;
	}

	/**
	 * Checks if the provided personnel is a safe desktop user.
	 * This is done by checking if the user is  belonging to the security group 
	 * @param p
	 * @return
	 */
	public boolean isSafeUser (final Person p) {
		String safeUserSecGroup = ConfigurationItem.SAFE_SECURITY_GROUP.getValue();
		for (SecurityGroup group : p.getSecurityGroups()) {
			if (group.getName().equals(safeUserSecGroup)) {
				return true;
			}
		}
		return false;
	}

	private void addMeasuresForPostProcessing(
			final Table clientData, MeasuresWithSource measures,
			final Nominations noms) {
		Table limsClientData = clientData.getTable(CLIENT_DATA_COL_NAME_LIMS, 0);
		LIMSLocalInterface local = new LIMSLocalInterface(session);
		Set<Integer> otherDeliveryIds = local.retrieveAllOtherBatchesHavingSameBatchNum(measures.getBatchNum(),
				measures.getPurity(), measures.getBrand(), noms);
		for (Nomination nom : noms) {
			if (!(nom instanceof Batch)) {
				continue;
			}
			String batchNum = measures.getBatchNum();
			String purity =  measures.getPurity();
			String brand = measures.getBrand();
			String batchNumNom = RelNomField.BATCH_NUMBER.guardedGetString(nom);
			String purityNom = RelNomField.PURITY.guardedGetString(nom);
			String brandNom = RelNomField.BRAND.guardedGetString(nom);
			if (batchNum.equals(batchNumNom) && purity.equals(purityNom) && brand.equals(brandNom)) {
				RelNomField.LIMS_LAST_ASK.guardedSet(nom, 0);
			}
		}
		for (int deliveryId : otherDeliveryIds) {
			TableRow udRow = limsClientData.addRow();
			String uniqueSampleId = measures.getUniqueSampleId();
			limsClientData.setInt("delivery_id", udRow.getNumber(), deliveryId);
			limsClientData.setString ("batch_num", udRow.getNumber(), measures.getBatchNum());
			limsClientData.setString ("purity", udRow.getNumber(), measures.getPurity());
			limsClientData.setString ("brand", udRow.getNumber(), measures.getBrand());
			limsClientData.setInt ("counter", udRow.getNumber(), 1);
			limsClientData.setString ("sample_id", udRow.getNumber(), uniqueSampleId);
			Table measuresForBatch = session.getTableFactory().createTable("(planned measures)");
			for (EnumPlannedMeasureFieldId fid : EnumPlannedMeasureFieldId.values()) {
				measuresForBatch.addColumn(fid.toString(), EnumColType.String);
			}
			if (uniqueSampleId.length() > 0) {
				measures.removeMeasureTypes(ConfigurationItem.MANUAL_MEASURES_JM_CASE.getValue());
			}
			for (String measureType : measures.getUsedMeasureTypes()) {
				TableRow mRow = measuresForBatch.addRow();
				List<Pair<MeasureDetails, MeasureSource>> m = measures.getMeasures(measureType);
				Map<EnumPlannedMeasureFieldId, String> ms = m.get(0).getLeft().getDetails();
				for (EnumPlannedMeasureFieldId fid : ms.keySet()) {
					measuresForBatch.setString(fid.toString(), mRow.getNumber(), ms.get(fid));
				}
			}
			limsClientData.setTable("measures", udRow.getNumber(), measuresForBatch);

		}
	}

	private boolean isRelevant(Nomination nom) {
		if (!(nom instanceof Batch)) {
			return false;
		}
		Batch batch = (Batch) nom;
		if (!RelNomField.ACTIVITY_ID.guardedGetString(batch).equals("Warehouse Receipt") 
				&& !RelNomField.ACTIVITY_ID.guardedGetString(batch).equals("Warehouse Inventory")
				) {
			return false;
		}

		final String batchNumber = RelNomField.BATCH_NUMBER.guardedGetString(nom);
		final String brand = RelNomField.BRAND.guardedGetString(nom);
		final String category = RelNomField.CATEGORY.guardedGetString(nom);
		if (brand.isEmpty() || category.isEmpty()) {
			return false;
		}
		int counter = RelNomField.LIMS_LAST_ASK.guardedGetInt(nom);
		if (counter > 0) {
			return false;
		}
		if ( RelNomField.ACTIVITY_ID.guardedGetString(batch).equals("Warehouse Inventory")
				&& batch.isOriginBatch()) {
			return false;
		}
				
		return true;
	}

	private MeasuresWithSource applyLocalLogic(Nomination nom,
			String batchNumber, String purity, String category) {
		Batch batch = (Batch) nom;
		LIMSLocalInterface local = new LIMSLocalInterface(session);
		int categoryId = session.getStaticDataFactory().getId(EnumReferenceTable.IdxSubgroup, category);
		MeasuresWithSource measurements = null;
		measurements = local.retrieveAllRelevantPlannedMeasures(session, batch, categoryId, batchNumber, purity);
		if (measurements.size() == 0) {
			return measurements;
		}
		return measurements;
	}

	private MeasuresWithSource applyRemoteLogic ( Nomination nom, String batchNumber, String category) {
		LIMSLocalInterface local = new LIMSLocalInterface(session);		
		int categoryId = session.getStaticDataFactory().getId(EnumReferenceTable.IdxSubgroup, category);

		Person currentUser = session.getUser();
		Address fullAddress = currentUser.getAddress();
		if (fullAddress == null) {
			showNoCountryMessage (currentUser);
			return new MeasuresWithSource(batchNumber, RelNomField.PURITY.guardedGetString(nom), RelNomField.BRAND.guardedGetString(nom));
		}
		Country country = currentUser.getAddress().getCountry();
		if (country == null) {
			showNoCountryMessage (currentUser);
			return new MeasuresWithSource(batchNumber, RelNomField.PURITY.guardedGetString(nom), RelNomField.BRAND.guardedGetString(nom));
		}
		String metal = local.idxSubgroupToMeasurementTypeName(categoryId);
		MeasuresWithSource batchAndMeasure;

		switch (country.getName()) {
		case "United Kingdom":
		case "United States":
			batchAndMeasure = processServer(LIMSServer.LIMS_UK, metal, country.getName(), batchNumber, local,
					RelNomField.PURITY.guardedGetString(nom), (Batch)nom);
			// LIMS_US and LIMS_UK point to the same database so only query once
			//if (batchAndMeasure.size() == 0) {
			//	batchAndMeasure = processServer(LIMSServer.LIMS_US, metal, country.getName(), batchNumber, local, 
			//			RelNomField.PURITY.guardedGetString(nom), (Batch)nom);
			//}
			if (batchAndMeasure.size() == 0) {
				showNoSampleFoundMessage (metal);
				return batchAndMeasure;
			}
			RelNomField.BATCH_NUMBER.guardedSet(nom, batchAndMeasure.getBatchNum());
			local.condensePlanMeasures(batchAndMeasure);
			return batchAndMeasure;

		default:
			showNoCountryMessage(currentUser);
			return new MeasuresWithSource(batchNumber, RelNomField.PURITY.guardedGetString(nom), RelNomField.BRAND.guardedGetString(nom));
		}
	}

	private MeasuresWithSource processServer(
			final LIMSServer server, final String metal, final String country,
			final String batchNumber, final LIMSLocalInterface local, String purity,
			Batch batch) {
		Table productsForTable = null;
		Table samples = null;
		LIMSRemoteInterface remote = new LIMSRemoteInterface(session, server);

		try  {
			productsForTable = local.loadMetalProductTestsForMetal (metal, country);
			if (productsForTable.getRowCount() == 0) {
				return new MeasuresWithSource (batchNumber, purity, RelNomField.BRAND.guardedGetString(batch));
			}
			Map<String, String> productsToTests = new HashMap<> ();
			for (TableRow row : productsForTable.getRows()) {
				productsToTests.put(row.getString(MetalProductTestTableCols.PRODUCT.getColName()),
						row.getString(MetalProductTestTableCols.RESULT.getColName()));
			}
			samples = remote.loadSampleIDsFromLims(batchNumber, productsToTests);
			if (samples.getRowCount() == 0) {
				return new MeasuresWithSource(batchNumber, purity, RelNomField.BRAND.guardedGetString(batch));
			}
			Pair<String, Pair<String, String>> sampleBatchAnalysis = new Pair<> (
					Integer.toString(samples.getInt("SAMPLE_NUMBER", 0)), 
					new Pair<>(samples.getString("JM_BATCH_ID",0),
							samples.getString("ANALYSIS",0)));

			if (samples.getRowCount() > 1) {
				String savedSampleId = (RelNomField.SAMPLE_ID.guardedGetString(batch) != null && !RelNomField.SAMPLE_ID.guardedGetString(batch).trim().equals(""))?
						RelNomField.SAMPLE_ID.guardedGetString(batch):"0";
				int rowId = samples.find(samples.getColumnId("SAMPLE_NUMBER"), Integer.parseInt(savedSampleId), 0);
				if (rowId == -1) {
					sampleBatchAnalysis = userSampleSelect (samples, batchNumber);
				} else {
					String selectedBatch = samples.getString("JM_BATCH_ID", rowId);
					String selectedAnalysis = samples.getString("ANALYSIS", rowId);
					sampleBatchAnalysis = new Pair<> (savedSampleId, new Pair<>(selectedBatch, selectedAnalysis));
				}
			}
			RelNomField.SAMPLE_ID.guardedSet(batch, sampleBatchAnalysis.getLeft());
			MeasuresWithSource ret =  remote.loadPlannedMeasureDetailsFromLims(sampleBatchAnalysis.getLeft(), 
					sampleBatchAnalysis.getRight().getLeft(), sampleBatchAnalysis.getRight().getRight(), purity,
					RelNomField.BRAND.guardedGetString(batch));
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

	private Pair<String, Pair<String, String>> userSampleSelect(Table samples, String batchNum) {
		com.olf.openjvs.Table askTable=null;
		try {
			askTable = com.olf.openjvs.Table.tableNew("ask table");
			com.olf.openjvs.Table dataTable = session.getTableFactory().toOpenJvs(samples);


			int ret = Ask.setAvsTable(askTable,
					dataTable,
					"Sample",
					1,
					ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(),
					1,
					null,
					"More than one sample has been found for batch number " + batchNum + ". Please select a single one",
					1);

			if(Ask.viewTable (askTable,"LIMs Query","" +
					"Please Complete the Following Fields") == 0) {
				throw new RuntimeException ("Selectio of Samples retrieved by LIMS interface cancelled by User");
			}
			com.olf.openjvs.Table temp=askTable.getTable("return_value", 1);
			String selectedSample = temp.getString("ted_str_value", 1);
			String selectedBatch = samples.getString("JM_BATCH_ID", samples.find(0, Integer.parseInt(selectedSample), 0));
			String selectedAnalysis = samples.getString("ANALYSIS", samples.find(0, Integer.parseInt(selectedSample), 0));
			return new Pair<> (selectedSample, new Pair<>(selectedBatch, selectedAnalysis));
		} catch (OException e) {
			throw new RuntimeException ("Error asking user to select sample: " + e.toString());
		} finally {
			askTable = TableUtilities.destroy(askTable);
		}
	}

	private void showNoSampleFoundMessage(String metal) {
		String message = "Cound not find sample for metal '" + metal + "'"
				+ " in either UK or US LIMS database."
				+ " Please enter batch measure	s manually or enter the correct batch num";
		throw new OverridableException(message);
	}

	private void showNoCountryMessage(Person currentUser) {
		String message = "The country of the user " + currentUser.getAliasName() + "(" + currentUser.getName() + ")"
				+ " is neither located in United Kingdom nor in United States. Batch measures cannot be retrieved from LIMS."
				+ " Please enter batch measures manually or setup user address.";
		throw new RuntimeException (message);
	}

	public boolean isForPostProcess(Table clientData) {
		if (clientData.getRowCount() == 0 || !hasLimsClientDataTable(clientData)) {
			throw new RuntimeException("Client data table not found. Ensure all LIMS related OPS are in configured and running.");
		}
		Table limsClientData = clientData.getTable(CLIENT_DATA_COL_NAME_LIMS, 0);
		return limsClientData.getRowCount() > 0;
	}
	
	public void init(Session session) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR") + "\\error_logs";
		String logLevel = ConfigurationItem.LOG_LEVEL.getValue();
		String logFile = ConfigurationItem.LOG_FILE.getValue();
		String logDir = abOutdir; //ConfigurationItem.LOG_DIRECTORY.getValue();
		if (logDir.trim().equals("")) {
			logDir = abOutdir;
		}
		try {
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}

	public void retrieveMeasuresForPostProcess(Nominations nominations,
			Table clientData) {	
		ensureUserTableDataStructure(clientData);
		LIMSLocalInterface local = new LIMSLocalInterface(session);
		List<MeasuresWithSource> batchesAndMeasures = new ArrayList<>(); 
		for (Nomination nom : nominations) {
			if (!isRelevant (nom)) {
				continue;
			}
			Batch batch = (Batch) nom;
			if (RelNomField.CAPTURE_MEASURES.guardedGetString(batch).equalsIgnoreCase("Yes")) {
				MeasuresWithSource measures = local.retrieveBatchPlannedMeasures(batch);
				batchesAndMeasures.add(measures);
				RelNomField.CAPTURE_MEASURES.guardedSet(batch, "No");
			}			
		}
		for (MeasuresWithSource mws : batchesAndMeasures) {
			addMeasuresForPostProcessing ( clientData, mws, nominations);
		}
	}
}
