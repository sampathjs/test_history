package com.jm.shanghai.accounting.udsr.model.retrieval;

import java.util.ArrayList;
import java.util.List;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.DatabaseTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;

public class RetrievalConfigurationColDescriptionLoader {
	private final Session session;
	private List<RetrievalConfigurationColDescription> colDescriptions;
	private RetrievalConfigurationColDescription priority;
	private RetrievalConfigurationColDescription runtimeDataTable;
	private RetrievalConfigurationColDescription reportOutput;
	private RetrievalConfigurationColDescription retrievalLogic;	
	private RetrievalConfigurationColDescription mappingTable;	
	private RetrievalConfigurationColDescription taxTable;	
	private RetrievalConfigurationColDescription materialNumberTable;
	private RetrievalConfigurationColDescription custCompTable;
	
	public RetrievalConfigurationColDescriptionLoader (final Session session) {
		this.session = session;
		colDescriptions = new ArrayList<>();
		loadColDecriptionsFromDatabase();
	}

	private void loadColDecriptionsFromDatabase() {
		DatabaseTable colDescDBTable = session.getIOFactory().getDatabaseTable("USER_jm_acc_retrieval_desc");
		Table colDescTable = colDescDBTable.retrieveTable();
		for (int rowId=colDescTable.getRowCount()-1; rowId>= 0; rowId--) {
			String name = colDescTable.getString("name", rowId);
			String title = colDescTable.getString("title", rowId);
			String type = colDescTable.getString("type", rowId);
			String colTypeName = colDescTable.getString("col_type_name", rowId);
			String usageType = colDescTable.getString("usage_type", rowId);
			int mappingEvaluationOrder = colDescTable.getInt("mapping_evaluation_order", rowId);
			String mappingTableName = colDescTable.getString("mapping_table_name", rowId);
			RetrievalConfigurationTableCols tableCol = 
					new RetrievalConfigurationTableCols(name, title, EnumColType.valueOf(type), colTypeName, ColumnSemantics.valueOf(usageType), mappingEvaluationOrder, mappingTableName);
			colDescriptions.add(tableCol);
			switch (name) {
			case "priority":
				priority = tableCol;
				break;
			case "col_name_runtime_table":
				runtimeDataTable = tableCol;
				break;
			case "col_name_report_output":
				reportOutput = tableCol;
				break;
			case "retrieval_logic":
				retrievalLogic = tableCol;
				break;
			case "col_name_mapping_table":
				mappingTable = tableCol;
				break;
			case "col_name_tax_table":
				taxTable = tableCol;
				break;
			case "col_name_material_number_table":
				materialNumberTable = tableCol;
				break;
			case "col_name_cust_comp_table":
				custCompTable = tableCol;
				break;
			default:
			}
		}
	}

	public List<RetrievalConfigurationColDescription> getColDescriptions() {
		return colDescriptions;
	}

	public void setColDescriptions(
			List<RetrievalConfigurationColDescription> colDescriptions) {
		this.colDescriptions = colDescriptions;
	}

	public RetrievalConfigurationColDescription getPriority() {
		return priority;
	}

	public void setPriority(RetrievalConfigurationColDescription priority) {
		this.priority = priority;
	}

	public RetrievalConfigurationColDescription getRuntimeDataTable() {
		return runtimeDataTable;
	}

	public void setRuntimeDataTable(
			RetrievalConfigurationColDescription runtimeDataTable) {
		this.runtimeDataTable = runtimeDataTable;
	}

	public RetrievalConfigurationColDescription getReportOutput() {
		return reportOutput;
	}

	public void setReportOutput(RetrievalConfigurationColDescription reportOutput) {
		this.reportOutput = reportOutput;
	}

	public RetrievalConfigurationColDescription getRetrievalLogic() {
		return retrievalLogic;
	}

	public void setRetrievalLogic(
			RetrievalConfigurationColDescription retrievalLogic) {
		this.retrievalLogic = retrievalLogic;
	}

	public Session getSession() {
		return session;
	}

	public RetrievalConfigurationColDescription getMappingTable() {
		return mappingTable;
	}

	public RetrievalConfigurationColDescription getTaxTable() {
		return taxTable;
	}

	public RetrievalConfigurationColDescription getMaterialNumberTable() {
		return materialNumberTable;
	}

	public RetrievalConfigurationColDescription getCustCompTable() {
		return custCompTable;
	}	
}
