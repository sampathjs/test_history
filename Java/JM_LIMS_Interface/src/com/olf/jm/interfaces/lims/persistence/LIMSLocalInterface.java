package com.olf.jm.interfaces.lims.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.olf.jm.interfaces.lims.model.ConfigurationItem;
import com.olf.jm.interfaces.lims.model.MeasureDetails;
import com.olf.jm.interfaces.lims.model.MeasureSource;
import com.olf.jm.interfaces.lims.model.MeasuresWithSource;
import com.olf.jm.interfaces.lims.model.MetalProductTestTableCols;
import com.olf.jm.interfaces.lims.model.Pair;
import com.olf.jm.interfaces.lims.model.RelNomField;
import com.olf.jm.interfaces.lims.model.RelevantUserTables;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableColumn;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.DeliveryTicket;
import com.olf.openrisk.trading.DeliveryTickets;
import com.olf.openrisk.trading.EnumObservedMeasureFieldId;
import com.olf.openrisk.trading.EnumPlannedMeasureFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.ObservedMeasure;
import com.olf.openrisk.trading.PlannedMeasure;
import com.olf.openrisk.trading.PlannedMeasures;
import com.openlink.util.logging.PluginLog;


/*
 * History:
 * 
 * 2015-09-02	V1.0	jwaechter	-	Initial Version
 * 2016-02-09	V1.1	jwaechter	-	Added check to generateDisplay to cover < and >
 * 									- 	Added check to generateRetrieveMeasureSQL to limit measures to 
 *                                      unlinked receipts OR linked inventories
 * 2016-04-12	V1.2	jwaechter	- 	Added changes for CR about making LOI and LOR 
 *                                    	managed manually in case of LIMS based measures.
                                     
 */

/**
 * Class containing all methods used to retrieve and change data from/in Endur used in LIMS interface. 
 * @author jwaechter
 * @version 1.2
 */
public class LIMSLocalInterface {
	private enum EnumUnionStates { IN_NEW_NOT_IN_OLD, IN_OLD_NOT_IN_NEW ,IN_BOTH }

	private final Session session;

	public LIMSLocalInterface (final Session session) {
		this.session = session;
	}

	/**
	 * @param nom 
	 * @param idxSubGroup the commodity sub group.
	 * @param brand 
	 * @param batchNumber 
	 * @return
	 */
	public List<Map<EnumObservedMeasureFieldId, String>> retrieveAllRelevantObservedMeasures (final Session session, Nomination nom, final int idxSubGroup, String batchNumber,
			String purity) {		
		String brand = RelNomField.BRAND.guardedGetString(nom);
		StringBuilder sql = generateRetrieveMeasureSQL(idxSubGroup,
				batchNumber, purity, brand);
		Table measuresTable = null;
		try {
			measuresTable = session.getIOFactory().runSQL(sql.toString());
			List<Map<EnumObservedMeasureFieldId, String>> measures = new ArrayList<> ();
			for (TableRow row : measuresTable.getRows()) {
				Map<EnumObservedMeasureFieldId, String> measure = new TreeMap<>(Collections.reverseOrder());
				int type = row.getInt("measurement_type");
				String typeName = session.getStaticDataFactory().getName(EnumReferenceTable.MeasurementType, type);
				int unit = row.getInt("unit");
				String unitName = session.getStaticDataFactory().getName(EnumReferenceTable.IdxUnit, unit);
				String comment = row.getString("measure_comment");
				double value = row.getDouble("lower_value");
				int measureNum = row.getInt("measure_num");
				measure.put(EnumObservedMeasureFieldId.Comments, comment);
				measure.put(EnumObservedMeasureFieldId.Unit, unitName);
				measure.put(EnumObservedMeasureFieldId.MeasurementType, typeName);
				//				measure.put(EnumObservedMeasureFieldId.Number, Integer.toString(measureNum));				
				measure.put(EnumObservedMeasureFieldId.Value, Double.toString(value));				
				measures.add(measure);
			}
			condenseObsMeasuresAndCheckForIncons (measures) ;
			return measures;
		} catch (RuntimeException t) {	
			PluginLog.info ("Error executing SQL " + sql.toString());
			throw t;
		} finally {
			if (measuresTable != null) {
				measuresTable.dispose();
			}
		}
	}

	public void condenseObsMeasuresAndCheckForIncons(
			final List<Map<EnumObservedMeasureFieldId, String>> measures) {
		for (int i = measures.size()-1; i >= 0; i--) {
			Map<EnumObservedMeasureFieldId, String> measure = measures.get(i);
			String unit = measure.get(EnumObservedMeasureFieldId.Unit);
			String type = measure.get(EnumObservedMeasureFieldId.MeasurementType);
			String value = measure.get(EnumObservedMeasureFieldId.Value);

			for (int k=i-1; k>0; k--) {
				Map<EnumObservedMeasureFieldId, String> toCompare = measures.get(k);
				String unitTC = toCompare.get(EnumObservedMeasureFieldId.Unit);
				String typeTC = toCompare.get(EnumObservedMeasureFieldId.MeasurementType);
				String valueTC = toCompare.get(EnumObservedMeasureFieldId.Value);
				if (typeTC.equals(type)) {
					if (!unitTC.equals(unit) || !valueTC.equals(value)) {
						throw new RuntimeException (String.format("There are duplicate measues for '%s': Units: %s vs %s; values: %s vs %s", 
								type, unit, unitTC, value, valueTC));
					} else {
						measures.remove(i);
						break;
					}
				}
			}
		}
	}

	/**
	 * @param nom 
	 * @param idxSubGroup the commodity sub group.
	 * @param brand 
	 * @param batchNumber 
	 * @param purity 
	 * @param brand2 
	 * @return
	 */
	public MeasuresWithSource retrieveAllRelevantPlannedMeasures (final Session session, Nomination nom, final int idxSubGroup, 
			final String batchNumber, String purity) {		
		String brand = RelNomField.BRAND.guardedGetString(nom);
		StringBuilder sql = generateRetrieveMeasureSQL(idxSubGroup,
				batchNumber, purity, brand);
		Table measuresTable = null;
		String rightPadding = ConfigurationItem.RIGHT_PADDING.getValue();
		try {
			measuresTable = session.getIOFactory().runSQL(sql.toString());
			MeasuresWithSource measures = new MeasuresWithSource(batchNumber, purity, brand);
			for (TableRow row : measuresTable.getRows()) {
				Map<EnumPlannedMeasureFieldId, String> measure = new TreeMap<>(Collections.reverseOrder());
				int type = row.getInt("measurement_type");
				String typeName = session.getStaticDataFactory().getName(EnumReferenceTable.MeasurementType, type);
				int unit = row.getInt("unit");
				String unitName = session.getStaticDataFactory().getName(EnumReferenceTable.IdxUnit, unit);
				String comment = row.getString("measure_comment");
				double lowerValue = row.getDouble("lower_value");
				double upperValue = row.getDouble("upper_value");
				String lowerValueAsString=null;
				String upperValueAsString=null;
				int measureNum = row.getInt("measure_num");	
				int valueModifier = row.getInt("value_modifier");

				switch (valueModifier) {
				case 0:
					lowerValueAsString = String.format("%1$." + rightPadding + "f", lowerValue);
					upperValueAsString = String.format("%1$." + rightPadding + "f", upperValue);
					break;
				case 3:
					lowerValueAsString = "ND";
					upperValueAsString = "ND";
					break;
				case 2:
					lowerValueAsString = "< " + String.format("%1$." + rightPadding + "f", lowerValue);
					upperValueAsString = "< " + String.format("%1$." + rightPadding + "f", upperValue);	
					break;
				case 1:
					lowerValueAsString = "> " + String.format("%1$." + rightPadding + "f", lowerValue);
					upperValueAsString = "> " + String.format("%1$." + rightPadding + "f", upperValue);	
					break;
				}
				String sampleId = row.getString ("sample_id");
				measure.put(EnumPlannedMeasureFieldId.Comments, comment);
				measure.put(EnumPlannedMeasureFieldId.Unit, unitName);
				measure.put(EnumPlannedMeasureFieldId.MeasurementType, typeName);
				//				measure.put(EnumPlannedMeasureFieldId.Number, Integer.toString(measureNum));
				measure.put(EnumPlannedMeasureFieldId.LowerValue, lowerValueAsString);
				measure.put(EnumPlannedMeasureFieldId.UpperValue, upperValueAsString);
				MeasureDetails md = new MeasureDetails (typeName, measure, sampleId);
				measures.addMeasure(md, MeasureSource.DATABASE);
			}
			condensePlanMeasures (measures);
			return measures;
		} catch (RuntimeException t) {	
			PluginLog.info ("Error executing SQL " + sql.toString());
			throw t;
		} finally {
			if (measuresTable != null) {
				measuresTable.dispose();
			}
		}
	}

	public void condensePlanMeasures(
			final MeasuresWithSource measures) {
		Set<String> knownMeasurementTypes = getKnownMeasurementTypes();
		Set<String> usedMeasurementTypes = measures.getUsedMeasureTypes();
		for (String mt : usedMeasurementTypes) {
			if (!knownMeasurementTypes.contains(mt)) {
				measures.removeMeasures(mt);
				PluginLog.info("Removed measurement type '" + mt  + "' as it is not set up in Endur");
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
			mts = session.getIOFactory().runSQL(sql);			
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

	private StringBuilder generateRetrieveMeasureSQL(final int idxSubGroup,
			final String batchNumber, String purity, String brand) {
		StringBuilder sql = new StringBuilder();
		sql.append("\nSELECT DISTINCT m.tsd_measure_type, m.schedule_id, m.tsd_delivery_ticket_id, m.measurement_type");
		sql.append("\n  , m.unit, m.lower_value, m.upper_value, cb.batch_id");
		sql.append("\n	, m.measure_comment, m.min_tolerance, m.max_tolerance, m.measure_num, m.test_method_id, m.value_modifier");
		sql.append("\n , ISNULL(ni_sample_id.info_value, sti.default_value) AS sample_id");
		sql.append("\nFROM ab_tran ab");
		sql.append("\n  INNER JOIN comm_batch cb");
		sql.append("\n  ON cb.ins_num = ab.ins_num");
		sql.append("\n  AND cb.idx_subgroup_id = " + idxSubGroup);
		sql.append("\n  AND cb.batch_num = '" + batchNumber + "'");
		sql.append("\n  INNER JOIN comm_brand_name cbrand");
		sql.append("\n  ON cbrand.brand_id = cb.brand_id");
		sql.append("\n  AND cbrand.brand_name = '").append(brand).append("'");
		sql.append("\n  INNER JOIN comm_sched_delivery_cmotion csdc");
		sql.append("\n  ON csdc.batch_id = cb.batch_id");
//		sql.append("\n  AND csdc.delivery_id = (SELECT MAX(csdc2.delivery_id) FROM comm_sched_delivery_cmotion csdc2 WHERE csdc2.batch_id = cb.batch_id)");
		sql.append("\n  INNER JOIN comm_sched_deliv_deal csdd");
		sql.append("\n  ON csdd.delivery_id = csdc.delivery_id");
		sql.append("\n  AND (csdd.deal_num = 6 OR (csdd.deal_num != 6 AND csdd.receipt_delivery = 1))");
		sql.append("\n  INNER JOIN measure_group mg");
		sql.append("\n  ON mg.id_number = csdc.measure_group_id");
		sql.append("\n  AND mg.name = '" + purity + "'");
		sql.append("\n  INNER JOIN comm_schedule_header h");
		//		sql.append("\n  ON h.ins_num = ab.ins_num");
		sql.append("\n  ON h.delivery_id = csdc.delivery_id");
		sql.append("\n  INNER JOIN ab_tran deals");
		sql.append("\n  ON deals.ins_num = h.ins_num");
		sql.append("\n  AND deals.current_flag = 1");
		sql.append("\n  INNER JOIN tsd_measure m");
		sql.append("\n  ON m.schedule_id = h.schedule_id");
		sql.append("\n  INNER JOIN nom_info_types sti ");
		sql.append("\n  ON sti.type_name = '").append(RelNomField.SAMPLE_ID.getInfoName()).append("'");
		sql.append("\n  LEFT OUTER JOIN nom_info ni_sample_id");
		sql.append("\n  ON ni_sample_id.delivery_id = h.delivery_id");
		sql.append("\n  AND ni_sample_id.type_id = sti.type_id");
		sql.append("\nWHERE ab.current_flag = 1");
		//		sql.append("\n  AND ab.ins_type = ").append(EnumInsType.CommStorage.getValue());
		return sql;
	}

	/**
	 * Retrieves the name of a metal given an Index Subgroup Id
	 * @param idxSubGroupId
	 * @return
	 */
	public String idxSubgroupToMeasurementTypeName (int idxSubGroupId) {
		StringBuilder sql = new StringBuilder ("\nSELECT mt.name \nFROM ");sql.append("idx_subgroup idx");
		sql.append("\nINNER JOIN measurement_type mt");
		sql.append("\n  ON mt.description = idx.name");
		sql.append("\nWHERE idx.id_number = " + idxSubGroupId);
		Table measurement = null;
		try {
			measurement = session.getIOFactory().runSQL(sql.toString());
			if (measurement.getRowCount() == 0) {
				throw new IllegalArgumentException ("Could not retrieve the measurement type name for idx_subgroup with ID " 
						+	idxSubGroupId );
			}

			if (measurement.getRowCount() > 1) {
				throw new IllegalArgumentException ("Retrieved more than one measurement type name for idx_subgroup with ID " 
						+	idxSubGroupId + ". This needs to be a 1:1 relationship." );
			}
			return measurement.getString(0, 0);
		} finally {
			if (measurement != null) {
				measurement.dispose();
				measurement = null;
			}
		}
	}

	public Table loadMetalProductTestsForMetal (String metal, String country) {
		String sql = getSQLToLoadProductTests(metal, country);
		return session.getIOFactory().runSQL(sql);
	}

	private String getSQLToLoadProductTests(String metal, String country) {
		StringBuilder sb = new StringBuilder("\nSELECT * FROM ");
		sb.append (RelevantUserTables.JM_METAL_PRODUCT_TEST.getName());
		sb.append("\nWHERE ");
		sb.append("\n	").append(MetalProductTestTableCols.METAL.getColName()).append("= '").append(metal).append("'");
		sb.append("\n	AND ").append(MetalProductTestTableCols.COUNTRY.getColName()).append("= '").append(country).append("'");
		return sb.toString();
	}

	public void clearObservedMeasures (Nomination nom) {
		Batch batch = (Batch) nom;
		DeliveryTickets tickets = batch.getBatchContainers();

		for (DeliveryTicket ticket : tickets) {
			ticket.getObservedMeasures().clear();
		}
		//		nom.saveIncremental();
	}

	public void addObservedMeasures (Nomination nom, List<Map<EnumObservedMeasureFieldId, String>> values) {	
		for (Map<EnumObservedMeasureFieldId, String> value : values) {
			addObservedMeasure(nom, value);
		}
	}

	public void addObservedMeasure (Nomination nom, Map<EnumObservedMeasureFieldId, String> values) {
		Batch batch = (Batch)nom;
		DeliveryTickets tickets = batch.getBatchContainers();

		for (DeliveryTicket ticket : tickets) {
			addObservedMeasure(batch, ticket, values);
		}
		//		Delivery del = nom.getDelivery();		
		//		for (Deal deal : del.getDeals()) {
		//			Transaction tran = deal.getTransaction();
		//			for (Leg leg : tran.getLegs()) {
		//				for (ScheduleDetail sd :  leg.getScheduleDetails()) {
		//					for (DeliveryTicket ticket : sd.getDeliveryTickets()) {
		//						addObservedMeasure(nom, ticket, values);
		//					}				
		//				}
		//			}					
		//		}
	}

	public void addPlannedMeasures (Batch batch, MeasuresWithSource resolvedMeasures ){
		PlannedMeasures ms = clearPlannedMeasures(batch);
//		if (ms.size() > 1) {
//			PluginLog.info("Could not remove existing Planned Measures. Skipping update.");
//			return;
//		}
		String purity = RelNomField.PURITY.guardedGetString(batch);
		String measurementTypeForPurity = getMeasureTypeForPurity(purity);
		for (String measureType : resolvedMeasures.getUsedMeasureTypes() ) {
			List<Pair<MeasureDetails, MeasureSource>> measure =  resolvedMeasures.getMeasures(measureType);
			measure.get(0).getLeft().updatePlannedMeasure(ms, measurementTypeForPurity);
		}
	}

//	private void addPlannedMeasure (Map<EnumPlannedMeasureFieldId, String> map, PlannedMeasures ms, 
//			String measurementTypeForPurity ){
//		if (map.get(EnumPlannedMeasureFieldId.MeasurementType).equals(measurementTypeForPurity)) {
//			return; // don't changed purity bases measure
//		}
//		PlannedMeasure pm = ms.addItem();
//		for (EnumPlannedMeasureFieldId field : map.keySet()) {
//		 	Field pmField = pm.getField(field);
//			if (pmField != null && pmField.isApplicable() && pmField.isWritable() 
//				&& map.get(field) != null && !map.get(field).equals("")) {
//				pm.setValue(field, map.get(field));
//			}
//		}
//	}

	public PlannedMeasures clearPlannedMeasures (Batch nom ){
		PlannedMeasures ms = nom.getBatchPlannedMeasures();		
		String purity = RelNomField.PURITY.guardedGetString(nom);
		String measureTypeForIndexSubgroupAndPurity = 
				getMeasureTypeForPurity(purity);
		for (int i = ms.size()-1; i >=0; i--) {
			PlannedMeasure measure = ms.getItem(i);
			String measureType = measure.getValueAsString(EnumPlannedMeasureFieldId.MeasurementType);
			if (!measureType.equals(measureTypeForIndexSubgroupAndPurity)) {
				ms.remove(i);
			}
		}
		loadDefaultMeasures(ms, purity);
		return ms;
	}

	private void loadDefaultMeasures(PlannedMeasures ms, String purity) {
		String sql = "\nSELECT mgi.*"
				+ "\nFROM idx_subgroup sg"
				+ "\n  INNER JOIN measure_group mg ON mg.idx_subgroup = sg.id_number"
				+ "\n  INNER JOIN measure_group_item mgi ON mgi.measure_group_id = mg.id_number"
				+ "\nWHERE mg.name = '" + purity + "'"
				+ "\n  AND mgi.lower_value = -1.0"
				;
		Table sqlResult = null;
		try {
			sqlResult = session.getIOFactory().runSQL(sql);
			for (TableRow row : sqlResult.getRows()) {	
				int measurementTypeId = row.getInt("measurement_type");
				int unit = row.getInt("unit");
				double lowerValue = row.getDouble("lower_value");
				double upperValue = row.getDouble("upper_value");
				double minTolerance = row.getDouble("min_tolerance");
				double maxTolerance = row.getDouble("max_tolerance");
				String measureGroupComment = row.getString("measure_group_comment");

				PlannedMeasure defMeasure = null;
				boolean created;
				for (PlannedMeasure pm : ms) {
					int mtId = pm.getValueAsInt(EnumPlannedMeasureFieldId.MeasurementType);
					if (mtId == measurementTypeId) {
						defMeasure = pm;
						break;
					}
				}
				if (defMeasure == null) {
					defMeasure = ms.addItem();
				}			
				
				Field mtField = defMeasure.getField(EnumPlannedMeasureFieldId.MeasurementType);
				Field unitField = defMeasure.getField(EnumPlannedMeasureFieldId.Unit);
				Field lowerValueField = defMeasure.getField(EnumPlannedMeasureFieldId.LowerValue);
				Field upperValueField = defMeasure.getField(EnumPlannedMeasureFieldId.UpperValue);
				Field minToleranceField = defMeasure.getField(EnumPlannedMeasureFieldId.MinTolerance);
				Field maxToleranceField = defMeasure.getField(EnumPlannedMeasureFieldId.MaxTolerance);
				Field commentField = defMeasure.getField(EnumPlannedMeasureFieldId.Comments);
				
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
		return field != null && field.isApplicable() && field.isWritable() && field.isReadable();
	}

	public String getMeasureTypeForPurity(String purity) {
		String sql = "\nSELECT mt.name FROM idx_subgroup sg"
				+ "\n  INNER JOIN measure_group mg ON mg.idx_subgroup = sg.id_number"
				+ "\n  INNER JOIN measure_group_item mgi ON mgi.measure_group_id = mg.id_number"
				+ "\n  INNER JOIN measurement_type mt ON mt.id_number = mgi.measurement_type"
				+ "\nWHERE mg.name = '" + purity + "'"
				+ "\n  AND mgi.lower_value != -1.0"
				;
		Table sqlResult = null;
		try {
			sqlResult = session.getIOFactory().runSQL(sql);
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

	private void clearObservedMeasures (Nomination nom, DeliveryTicket ticket) {
		ticket.getObservedMeasures().removeAll(ticket.getObservedMeasures());
	}

	private void addObservedMeasure (Nomination nom, DeliveryTicket ticket, Map<EnumObservedMeasureFieldId, String> values) {
		Batch batch = (Batch) nom;
		for (ObservedMeasure existingMeasure : ticket.getObservedMeasures()) {
			String measurementTypeExisting = existingMeasure.getValueAsString(EnumObservedMeasureFieldId.MeasurementType);
			String measurementTypeToAdd = values.get(EnumObservedMeasureFieldId.MeasurementType);
			if (measurementTypeExisting.equals(measurementTypeToAdd	) ) {
				PluginLog.info ("Skipping addition of measurement type " + measurementTypeToAdd + 
						" as it's already present on delivery ticket " + ticket.getDeliveryTicketNumber());
				return;
			}
		}

		ObservedMeasure om = batch.addObservedMeasure(ticket);  //ticket.getObservedMeasures().addItem();
		for (EnumObservedMeasureFieldId fid : values.keySet()) {
			om.setValue(fid, values.get(fid));
		}
	}

	public boolean isThereAnyOtherExistingBatchWithSameBatchNum(String batchNum, String purity, String brand, Nominations noms) {
		String sql = getSQLTOLoadBatchesHavingSameBatchNum(batchNum, purity, brand, noms);
		Table sqlResult = null;
		try {
			sqlResult = session.getIOFactory().runSQL(sql);
			if (sqlResult.getRowCount() == 0) {
				return false;
			}
			return true;
		} finally {
			if (sqlResult != null) {
				sqlResult.dispose();
			}
		}
	}

	public boolean updatePlannedMeasuresOnBatch(PlannedMeasures measures, Table measuresTable, 
			String measureTypeForPurity, String comSepListExcludedMeasures) {
		boolean modified = false;
		// following map will contain an overview about which measure types (e.g. PD or PT) 
		// are in the old measures, in the new measures and which are in both.
		Set<String> measuresToSkip = new TreeSet<>();
		for (String token : comSepListExcludedMeasures.split(",")) {
			measuresToSkip.add(token.trim());
		}
		Map<String, EnumUnionStates> unionOldAndNew = new HashMap<>();
		for (PlannedMeasure pm : measures) {
			String measureType = pm.getValueAsString(EnumPlannedMeasureFieldId.MeasurementType);
			if (measureType.equals(measureTypeForPurity)) {
				continue;
			}
			if (measuresToSkip.contains(measureType)) {
				continue;
			}
			unionOldAndNew.put(measureType, EnumUnionStates.IN_OLD_NOT_IN_NEW);
		}
		for (TableRow pm : measuresTable.getRows()) {
			String measureType = pm.getString(EnumPlannedMeasureFieldId.MeasurementType.toString());
			if (measureType.equals(measureTypeForPurity)) {
				continue;
			}
			if (measuresToSkip.contains(measureType)) {
				continue;
			}
			if (unionOldAndNew.containsKey(measureType)) {
				unionOldAndNew.put(measureType, EnumUnionStates.IN_BOTH);
			} else {
				unionOldAndNew.put(measureType, EnumUnionStates.IN_NEW_NOT_IN_OLD);				
			}
		}
		for (String measureType : unionOldAndNew.keySet()) {
			EnumUnionStates state = unionOldAndNew.get(measureType);
			switch (state) {
			case IN_NEW_NOT_IN_OLD: // just in the memory table, not on the nom:
				// add new measure to old nom
				addPlannedMeasureToBatch ( measures, measureType, measuresTable);
				modified = true;
				break;
			case IN_OLD_NOT_IN_NEW:
				removePlannedMeasure (measures, measureType);
				modified = true;
				break;
			case IN_BOTH:
				modified |= revisePlannedMeasure (measures, measureType, measuresTable);
				break;
			}
		}
		return modified;
	}

	private boolean revisePlannedMeasure(PlannedMeasures measures,
			String measureType, Table measuresTable) {
		boolean modified = false;
		int row = measuresTable.findRowId(EnumPlannedMeasureFieldId.MeasurementType.toString() + " == '" + measureType + "'" , 0);
		PlannedMeasure toBeModified = null;
		for (PlannedMeasure pm : measures) {
			String mt = pm.getValueAsString(EnumPlannedMeasureFieldId.MeasurementType);
			if (mt.equals(measureType)) {
				toBeModified = pm;
				break;
			}
		}

		modified = setupMeasure(measuresTable, row, toBeModified);
		return modified;
	}

	private void removePlannedMeasure(PlannedMeasures measures,
			String measureType) {
		for (PlannedMeasure measure : measures) {
			String mtype = measure.getValueAsString(EnumPlannedMeasureFieldId.MeasurementType);
			if (mtype.equals(measureType)) {
				measures.remove(measure);
				break;
			}
		}
	}

	private void addPlannedMeasureToBatch(PlannedMeasures measures,
			String measureType, Table measuresTable) {
		int row = measuresTable.findRowId(EnumPlannedMeasureFieldId.MeasurementType.toString() + " == '" + measureType + "'" , 0);
		PlannedMeasure newMeasure = measures.addItem();

		setupMeasure(measuresTable, row, newMeasure);
	}

	private boolean setupMeasure(Table measuresTable, int row,
			PlannedMeasure newMeasure) {
		boolean modified = false;
		TableColumn col = measuresTable.getColumn(EnumPlannedMeasureFieldId.MeasurementType.toString());
		modified |= updateMeasureField(measuresTable, row, newMeasure, col);
		col = measuresTable.getColumn(EnumPlannedMeasureFieldId.Unit.toString());
		modified |= updateMeasureField(measuresTable, row, newMeasure, col);
		col = measuresTable.getColumn(EnumPlannedMeasureFieldId.MaxTolerance.toString());
		modified |= updateMeasureField(measuresTable, row, newMeasure, col);
		col = measuresTable.getColumn(EnumPlannedMeasureFieldId.LowerValue.toString());
		modified |= updateMeasureField(measuresTable, row, newMeasure, col);
		col = measuresTable.getColumn(EnumPlannedMeasureFieldId.UpperValue.toString());
		modified |= updateMeasureField(measuresTable, row, newMeasure, col);
		col = measuresTable.getColumn(EnumPlannedMeasureFieldId.Comments.toString());
		modified |= updateMeasureField(measuresTable, row, newMeasure, col);
		col = measuresTable.getColumn(EnumPlannedMeasureFieldId.TestMethod.toString());
		modified |= updateMeasureField(measuresTable, row, newMeasure, col);
		return modified;
	}

	private boolean updateMeasureField(Table measuresTable, int row,
			PlannedMeasure newMeasure, TableColumn col) {
		EnumPlannedMeasureFieldId fid = EnumPlannedMeasureFieldId.valueOf(col.getName());
		Field field = newMeasure.getField(fid);
		if (field.isApplicable() && field.isWritable()) {
			String newValue = measuresTable.getString(fid.toString(), row);
			if (newValue == null || newValue.equals("")) {
				return false;
			}
			field.setValue(newValue);
			return true;
		}
		return false;
	}


	private String getSQLTOLoadBatchesHavingSameBatchNum(String batchNum, String purity, String brand, Nominations noms) {
		StringBuilder sb = new StringBuilder ();
		sb.append("\nSELECT DISTINCT cb.batch_id, csdc.delivery_id, cbn.brand_name");
		sb.append("\nFROM ab_tran ab");
		sb.append("\n  INNER JOIN comm_batch cb");
		sb.append("\n  ON cb.ins_num = ab.ins_num");
		sb.append("\n  AND cb.batch_num = '" + batchNum + "'");
		sb.append("\n  INNER JOIN comm_sched_delivery_cmotion csdc");
		sb.append("\n  ON csdc.batch_id = cb.batch_id");
		sb.append("\n  INNER JOIN measure_group mg");
		sb.append("\n  ON mg.id_number = csdc.measure_group_id");
		sb.append("\n  AND mg.name = '" + purity + "'");
		sb.append("\n  INNER JOIN comm_brand_name cbn");
		sb.append("\n  ON cbn.brand_id = cb.brand_id");		
		sb.append("\nWHERE ab.current_flag = 1");
		for (Nomination nom : noms) {
			if (!(nom instanceof Batch)) {
				continue;
			}
			sb.append("\n AND cb.batch_id != ");
			Batch batch = (Batch) nom;
			sb.append(batch.getBatchId());
		}
		;
		return sb.toString();
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

	public Set<Integer> retrieveAllOtherBatchesHavingSameBatchNum(String batchNum, String purity, String brand, 
			Nominations noms) {
		final Set<Integer> deliveryIds = new TreeSet<>();
		String sql = getSQLTOLoadBatchesHavingSameBatchNum(batchNum, purity, brand, noms);
		Table sqlResult = null;
		try {
			sqlResult = session.getIOFactory().runSQL(sql);
			for (TableRow row : sqlResult.getRows()) {
				deliveryIds.add(row.getInt("delivery_id"));
			}
		} finally {
			if (sqlResult != null) {
				sqlResult.dispose();
			}
		}
		return deliveryIds;
	}

	public MeasuresWithSource resolveInconsistencies(MeasuresWithSource mws) {
		try {
			if (!mws.hasCompetingMeasures()) {
				return mws;
			}
			if (mws.hasCompetingSampleIds()) {
				return sampleSelectLogic (mws);
			}
			return measureSelectLogic (mws);
			
		} catch (OException ex) {
			throw new RuntimeException ("OException while asking user to resolve competing measures");
		}
	}

	private MeasuresWithSource sampleSelectLogic(MeasuresWithSource mws) throws OException {
		com.olf.openjvs.Table askTable = com.olf.openjvs.Table.tableNew("Ask");
		com.olf.openjvs.Table sampleIdTable = com.olf.openjvs.Table.tableNew("SampleIds");
		sampleIdTable.addCol("sample_id", COL_TYPE_ENUM.COL_STRING, "Sample ID");
		for (String sampleId : mws.getCompetingSampleIds()) {
			int row = sampleIdTable.addRow();
			sampleIdTable.setString("sample_id", row, sampleId);
		}
		
		int ret;
		ret  = Ask.setAvsTable(askTable,
				sampleIdTable,
				"Select the sample ID",
				sampleIdTable.getColNum("sample_id"),
				ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(),
				sampleIdTable.getColNum("sample_id"),
				null,
				"Please select the sample ID to used",
				1);

		if(Ask.viewTable (askTable,"Competing sample IDs found for " + mws.getBatchNum() + 
				"/" + mws.getPurity(), 
				"Please select the sample ID for batch " + mws.getBatchNum() + " having purity " + mws.getPurity()) <= 0) {
			throw new RuntimeException ("User has cancelled selection of competing sample IDs for " + mws.getBatchNum() + 
					"/" + mws.getPurity());
		}
		com.olf.openjvs.Table returnValueTable = askTable.getTable("return_value", 1);
		String returnValue = returnValueTable.getString("return_value", 1);
		selectCompetingSampleId (mws, returnValue);
		askTable.destroy();
		return mws;
	}

	private void selectCompetingSampleId(MeasuresWithSource mws,
			String returnValue) {
		for (String mt : mws.getAllMeasures()) {
			List<Pair<MeasureDetails, MeasureSource>> compMeasures = mws.getMeasures(mt);
			
			Pair<MeasureDetails, MeasureSource> selectedMeasure = null;			
			for (Pair<MeasureDetails, MeasureSource> measure : compMeasures) {
				if (measure.getLeft().getSampleId().equals(returnValue)) {
					selectedMeasure = measure;
					break;
				}
			}
			if (selectedMeasure != null) {
				mws.clear(mt);
				mws.addMeasure(selectedMeasure.getLeft(), selectedMeasure.getRight());					
			} else {
				mws.clear(mt);
			}
		}
	}

	private MeasuresWithSource measureSelectLogic(MeasuresWithSource mws) throws OException {
		com.olf.openjvs.Table askTable = com.olf.openjvs.Table.tableNew("Ask");
		Map<String, Integer> rowsToMeasureTypes = new TreeMap<>();			
		int counter=1;
		for (String competingMT : mws.getMeasureTypesHavingCompetingDetails()) {
			rowsToMeasureTypes.put(competingMT, counter++);
			com.olf.openjvs.Table listTable = generateCompetingMeasuresList(competingMT, mws);
			com.olf.openjvs.Table defaultTable = generateDefaults (competingMT, mws);
			int ret;
			ret  = Ask.setAvsTable(askTable,
					listTable,
					competingMT,
					listTable.getColNum("data"),
					ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(),
					listTable.getColNum("id"),
					defaultTable,
					"Please select the details for " + competingMT,
					1);
		}

		if(Ask.viewTable (askTable,"Competing measure details found for " + mws.getBatchNum() + 
				"/" + mws.getPurity(), 
				"Please select the measure details for all of the following measure types") <= 0) {
			throw new RuntimeException ("User has cancelled selection of competing measures for " + mws.getBatchNum() + 
					"/" + mws.getPurity());
		}

		for (String competingMT : mws.getMeasureTypesHavingCompetingDetails()) {
			int rowId = rowsToMeasureTypes.get(competingMT);
			com.olf.openjvs.Table returnValueTable = askTable.getTable("return_value", rowId);
			int returnValue = returnValueTable.getInt("return_value", 1);
			selectCompetingMeasure (competingMT, mws, returnValue);
		}
		askTable.destroy();
		return mws;
	}

	private com.olf.openjvs.Table generateDefaults(String competingMT,
			MeasuresWithSource mws) throws OException {
		com.olf.openjvs.Table table =  com.olf.openjvs.Table.tableNew("default table for " + competingMT );
		table.addCol("data", COL_TYPE_ENUM.COL_STRING);
		List<Pair<MeasureDetails, MeasureSource>> measures = mws.getMeasures(competingMT);
		int numberInMemoryMeasures = 0;
		for (Pair<MeasureDetails, MeasureSource> measure : measures) {
			if (measure.getRight() ==  MeasureSource.IN_MEMORY) {
				numberInMemoryMeasures++;
			}
		}
		if (numberInMemoryMeasures == 1) {
			int rowNum = table.addRow();
			for (Pair<MeasureDetails, MeasureSource> measure : measures) {
				if (measure.getRight() ==  MeasureSource.IN_MEMORY) {
					String sourceMeasureTypeUnitValue = generateDisplay(measure);					
					table.setString("data", rowNum, sourceMeasureTypeUnitValue);
				}
			}
		}
		return table;
		
	}

	private void selectCompetingMeasure(String competingMT,
			MeasuresWithSource mws, int returnValue) {
		List<Pair<MeasureDetails, MeasureSource>> compMeasures = mws.getMeasures(competingMT);
		Pair<MeasureDetails, MeasureSource> selectedMeasure = compMeasures.get(returnValue);
		mws.clear(competingMT);
		mws.addMeasure(selectedMeasure.getLeft(), selectedMeasure.getRight());
	}

	private com.olf.openjvs.Table generateCompetingMeasuresList(
			String competingMT, MeasuresWithSource mws) throws OException {
		com.olf.openjvs.Table listTable = com.olf.openjvs.Table.tableNew(competingMT);
		listTable.addCol("id", COL_TYPE_ENUM.COL_INT, "id");
		listTable.addCol("data", COL_TYPE_ENUM.COL_STRING, "Source");
		for (int i = mws.getMeasures(competingMT).size()-1; i >= 0; i--) {
			Pair<MeasureDetails, MeasureSource> details = mws.getMeasures(competingMT).get(i);
			int rowNum = listTable.addRow();
			listTable.setInt("id", rowNum, i);
			String sourceMeasureTypeUnitValue = generateDisplay(details);
			listTable.setString("data", rowNum,  sourceMeasureTypeUnitValue );
		}
		return listTable;
	}

	private String generateDisplay(Pair<MeasureDetails, MeasureSource> details) {
		String lowerValue = details.getLeft().getDetails().get(EnumPlannedMeasureFieldId.LowerValue);
		String lowerValueToShow;
		if (!lowerValue.contains("<") && !lowerValue.contains(">")) {
			lowerValueToShow = ((!lowerValue.equals("ND") && Double.parseDouble(lowerValue) == -1.0)?"":lowerValue+ "/" +
					details.getLeft().getSampleId());
		} else {
			lowerValueToShow = lowerValue+ "/" +details.getLeft().getSampleId();
		}
		
		String sourceMeasureTypeUnitValue = details.getRight().toString() + " / " +
				details.getLeft().getMeasureType() + " / " +
				details.getLeft().getDetails().get(EnumPlannedMeasureFieldId.Unit) + " / " +
				lowerValueToShow;
		return sourceMeasureTypeUnitValue;
	}
	
	public static boolean isDispatchDeal(Session session, int dealTrackingNum) {
		String sql = 
				"\nSELECT csdd.delivery_id" 
			+	"\nFROM ab_tran ab "
			+   "\nINNER JOIN comm_sched_deliv_deal csdd"
			+   "\n  ON csdd.deal_num = ab.deal_tracking_num"
			+   "\n  AND csdd.receipt_delivery = 1"
			+   "\nWHERE ab.deal_tracking_num = " + dealTrackingNum
			+   "\n  AND ab.current_flag = 1"
			;
		Table sqlResult = null;
		try {
			sqlResult = session.getIOFactory().runSQL(sql);
			if (sqlResult.getRowCount() == 1) {
				return true;
			}
			return false;
		} finally {
			if (sqlResult != null) {
				sqlResult.dispose();
			}
		}
	}
}