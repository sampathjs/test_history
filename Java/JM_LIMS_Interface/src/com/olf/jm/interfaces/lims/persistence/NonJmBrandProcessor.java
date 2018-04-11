package com.olf.jm.interfaces.lims.persistence;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.olf.embedded.application.Context;
import com.olf.jm.interfaces.lims.model.ConfigurationItem;
import com.olf.jm.interfaces.lims.model.MeasureDetails;
import com.olf.jm.interfaces.lims.model.MeasureSource;
import com.olf.jm.interfaces.lims.model.MeasuresWithSource;
import com.olf.jm.interfaces.lims.model.RelNomField;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumPlannedMeasureFieldId;
import com.openlink.util.logging.PluginLog;

public class NonJmBrandProcessor extends ProcessorBase {


	protected NonJmBrandProcessor(Nomination nomToProcess,	Context currentContext) {
		super(nomToProcess, currentContext);
	}


	@Override
	public MeasuresWithSource setMeasures() {
		MeasuresWithSource measures = null;
		
		if(isThereAnyOtherExistingBatchWithSameBatchNum()) {
			
			if (didBatchNumOrPurityOrBrandChange()) {
				// creation case
				measures = applyLocalLogic();
			} else {
				// merge case
				measures = getHelper().retrieveBatchPlannedMeasures(getBatch());
				measures.addAllMeasures(applyLocalLogic());
			}	
			
			if (measures.size() > 0) {
				getHelper().condensePlanMeasures(measures);
				MeasuresWithSource resolvedMeasures = resolveInconsistencies(measures);

				addPlannedMeasures(resolvedMeasures);
				RelNomField.SPEC_COMPLETE.guardedSet(getBatch(), "Yes");
				RelNomField.SAMPLE_ID.guardedSet(getNomination(),
						resolvedMeasures.getUniqueSampleId());
				RelNomField.CAPTURE_MEASURES.guardedSet(getNomination(), "Yes");
			}
		}
		
		return measures;
	}
	
	private boolean didBatchNumOrPurityOrBrandChange() {
		int batchId = getBatch().getBatchId();
		if (batchId <= 0) {
			return true; // new batch
		}
		int deliveryId = getBatch().getId();
		Batch oldBatch = getContext().getSchedulingFactory().retrieveBatchByDeliveryId(deliveryId);
		String batchNum = RelNomField.BATCH_NUMBER.guardedGetString(getBatch());
		String oldBatchNum = RelNomField.BATCH_NUMBER.guardedGetString(oldBatch);
		String purity = RelNomField.PURITY.guardedGetString(getBatch());
		String oldPurity = RelNomField.PURITY.guardedGetString(oldBatch);
		String brand = RelNomField.BRAND.guardedGetString(getBatch());
		String oldBrand = RelNomField.BRAND.guardedGetString(oldBatch);

		if (batchNum.equals(oldBatchNum) && purity.equals(oldPurity) && brand.equals(oldBrand)) {
			return false;
		}
		return true;		
	}
	
	private MeasuresWithSource applyLocalLogic() {
		

		MeasuresWithSource measurements = null;
		measurements = retrieveAllRelevantPlannedMeasures();

		return measurements;
	}

	public boolean isThereAnyOtherExistingBatchWithSameBatchNum() {
		String sql = getSQLTOLoadBatchesHavingSameBatchNum();
		Table sqlResult = null;
		try {
			sqlResult = getContext().getIOFactory().runSQL(sql);
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
	
	private String getSQLTOLoadBatchesHavingSameBatchNum() {
		StringBuilder sb = new StringBuilder ();
		sb.append("\nSELECT DISTINCT cb.batch_id, csdc.delivery_id, cbn.brand_name");
		sb.append("\nFROM ab_tran ab");
		sb.append("\n  INNER JOIN comm_batch cb");
		sb.append("\n  ON cb.ins_num = ab.ins_num");
		sb.append("\n  AND cb.batch_num = '" + getBatchNumber() + "'");
		sb.append("\n  INNER JOIN comm_sched_delivery_cmotion csdc");
		sb.append("\n  ON csdc.batch_id = cb.batch_id");
		sb.append("\n  INNER JOIN measure_group mg");
		sb.append("\n  ON mg.id_number = csdc.measure_group_id");
		sb.append("\n  AND mg.name = '" + getPurity() + "'");
		sb.append("\n  INNER JOIN comm_brand_name cbn");
		sb.append("\n  ON cbn.brand_id = cb.brand_id");	
		sb.append("\n  INNER JOIN nom_info ni");
		sb.append("\n  ON ni.delivery_id = csdc.delivery_id and type_id = 20001 and info_value = 'Yes'");

		sb.append("\nWHERE ab.current_flag = 1");
		sb.append("\n AND cb.batch_id != ");
		sb.append(getBatch().getBatchId());
		
		
		
		return sb.toString();
	}	
	
	public MeasuresWithSource retrieveAllRelevantPlannedMeasures () {		

		StringBuilder sql = generateRetrieveMeasureSQL();
		Table measuresTable = null;
		String rightPadding = ConfigurationItem.RIGHT_PADDING.getValue();
		try {
			measuresTable = getContext().getIOFactory().runSQL(sql.toString());
			PluginLog.debug("About to run SQL: " + sql.toString());
			MeasuresWithSource measures = new MeasuresWithSource(getBatchNumber(), getPurity(), getBrand());
			for (TableRow row : measuresTable.getRows()) {
				Map<EnumPlannedMeasureFieldId, String> measure = new TreeMap<>(Collections.reverseOrder());
				int type = row.getInt("measurement_type");
				String typeName = getContext().getStaticDataFactory().getName(EnumReferenceTable.MeasurementType, type);
				int unit = row.getInt("unit");
				String unitName = getContext().getStaticDataFactory().getName(EnumReferenceTable.IdxUnit, unit);
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
			getHelper().condensePlanMeasures (measures);
			
			PluginLog.debug("Loaded measures: " + measures.toString());
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
		
	// TODO update to only bring back one seet of measures
		private StringBuilder generateRetrieveMeasureSQL() {
			StringBuilder sql = new StringBuilder();
			
			
			sql.append("\nSELECT tsd_measure_type");
			sql.append("\n		,schedule_id");
			sql.append("\n		,tsd_delivery_ticket_id");
			sql.append("\n		,measurement_type");
			sql.append("\n		,unit");
			sql.append("\n		,lower_value");
			sql.append("\n		,upper_value");
			sql.append("\n		,batch_id");
			sql.append("\n		,measure_comment");
			sql.append("\n		,min_tolerance");
			sql.append("\n		,max_tolerance");
			sql.append("\n		,measure_num");
			sql.append("\n		,test_method_id");
			sql.append("\n		,value_modifier");
			sql.append("\n		,sample_id");
			sql.append("\nFROM (");
			sql.append("\n			SELECT DISTINCT m.tsd_measure_type");
			sql.append("\n				,m.schedule_id");
			sql.append("\n				,m.tsd_delivery_ticket_id");
			sql.append("\n				,m.measurement_type");
			sql.append("\n				,m.unit");
			sql.append("\n				,m.lower_value");
			sql.append("\n				,m.upper_value");
			sql.append("\n				,cb.batch_id");
			sql.append("\n				,m.measure_comment");
			sql.append("\n				,m.min_tolerance");
			sql.append("\n				,m.max_tolerance");
			sql.append("\n				,m.measure_num");
			sql.append("\n				,m.test_method_id");
			sql.append("\n				,m.value_modifier");
			sql.append("\n				,ISNULL(ni_sample_id.info_value, sti.default_value) AS sample_id");
			sql.append("\n				,m.last_update");
			sql.append("\n				,RANK() OVER (");
			sql.append("\n					PARTITION BY m.measurement_type ORDER BY m.last_update DESC");
			sql.append("\n					) AS desc_rank");
			sql.append("\n			FROM ab_tran ab");
			sql.append("\n			INNER JOIN comm_batch cb ON cb.ins_num = ab.ins_num");
			sql.append("\n				AND cb.idx_subgroup_id = " + getCategoryId());
			sql.append("\n				AND cb.batch_num = '" + getBatchNumber() + "'");
			sql.append("\n				AND cb.batch_id != " + getBatchId());
			sql.append("\n			INNER JOIN comm_brand_name cbrand ON cbrand.brand_id = cb.brand_id");
			sql.append("\n				AND cbrand.brand_name = '").append(getBrand()).append("'");
			sql.append("\n			INNER JOIN csd_cmotion_view ccv ON ccv.batch_id = cb.batch_id");
			//sql.append("\n				AND ccv.activity_id = 20003"); // Warehouse Inventory noms only
			sql.append("\n			INNER JOIN comm_schedule_header h ON h.delivery_id = ccv.delivery_id");
			sql.append("\n			INNER JOIN tsd_measure m ON m.schedule_id = h.schedule_id");
			sql.append("\n				AND m.tsd_delivery_ticket_id = 0");
			sql.append("\n			INNER JOIN comm_sched_deliv_deal  csdd ON csdd.delivery_id = h.delivery_id ");
			sql.append("\n				and receipt_delivery = 0 and deal_num > 6 "); // only take measures where there is a linked receipt deal
			sql.append("\n			INNER JOIN nom_info_types sti ON sti.type_name = 'Internal: LIMS Sample ID'");
			sql.append("\n			LEFT OUTER JOIN nom_info ni_sample_id ON ni_sample_id.delivery_id = h.delivery_id");
			sql.append("\n				AND ni_sample_id.type_id = sti.type_id");
			sql.append("\n			INNER JOIN nom_info ni_spec_complete ON ni_spec_complete.delivery_id = h.delivery_id");
			sql.append("\n				AND ni_spec_complete.type_id = 20001"); // Spec complete flag
			sql.append("\n				AND ni_spec_complete.info_value = 'Yes'");
			sql.append("\n			WHERE ab.current_flag = 1");
			sql.append("\n			) AS result");
			sql.append("\n		WHERE result.desc_rank = 1");

			return sql;
		}		
	
}
