package com.olf.jm.metalTransfer.confirmations.dataSource;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openjvs.OException;
import com.olf.openjvs.Tpm;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;



/**
 * The Class TpmRunVariables. Returns the TPM variables used in the metal transfer.
 * The workflows to process are defined in the query result passed to the plugin via
 * the argt
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class TpmRunVariables extends ReportBuilderDataSourceBase {

	/* (non-Javadoc)
	 * @see com.olf.jm.metalTransfer.confirmations.dataSource.ReportBuilderDataSourceBase#buildReturnTable(com.olf.openrisk.table.Table)
	 */
	@Override
	final
	void buildReturnTable(final Table returnt)  {
		returnt.addColumn("workflow_id", EnumColType.Int);
		returnt.addColumn("from_account_number", EnumColType.String);
		returnt.addColumn("from_account_id", EnumColType.Int);
		returnt.addColumn("from_account_name", EnumColType.String);
		returnt.addColumn("tran_num", EnumColType.Int);
		returnt.addColumn("deal_tracking_num", EnumColType.Int);
		returnt.addColumn("to_account_number", EnumColType.String);
		returnt.addColumn("to_account_id", EnumColType.Int);
		returnt.addColumn("to_account_name", EnumColType.String);
		returnt.addColumn("reference", EnumColType.String);
		returnt.addColumn("metal", EnumColType.String);
		returnt.addColumn("metal_id", EnumColType.Int);
		returnt.addColumn("transfer_unit", EnumColType.String);
		
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.metalTransfer.confirmations.dataSource.ReportBuilderDataSourceBase#setReturnTable(com.olf.openrisk.table.Table)
	 */
	@Override
	final
	void setReturnTable(final Table returnt)  {
		Table queryResults = getQueryResults();
		
		for (int  row = 0;  row < queryResults.getRowCount(); row++) {
			long workflowId = queryResults.getLong("query_result", row);
			
			try {
				com.olf.openjvs.Table tpmVariables = Tpm.getVariables(workflowId).getTable(1, 1);
				
				
				int newRow = returnt.addRows(1);
				returnt.setInt("workflow_id", newRow, (int) workflowId);
				
				for (int variablesRow = 1; variablesRow <= tpmVariables.getNumRows(); variablesRow++) {
					switch (tpmVariables.getString("name", variablesRow)) {

						case "FromAccountNumber":
							returnt.setString("from_account_number", newRow, tpmVariables.getString("value", variablesRow));
							break;
							
						case "FromAccountId":
							returnt.setInt("from_account_id", newRow, 
									new Integer(tpmVariables.getString("value", variablesRow)).intValue());
							break;
														
						case "FromAccountName":
							returnt.setString("from_account_name", newRow, tpmVariables.getString("value", variablesRow));
							break;

						case "TranNum":
							returnt.setInt("tran_num", newRow, new Integer(tpmVariables.getString("value", variablesRow)).intValue());
							break;	
							
						case "DealNum":
							returnt.setInt("deal_tracking_num", newRow, 
									new Integer(tpmVariables.getString("value", variablesRow)).intValue());
							break;	

						case "ToAccountNumber":
							returnt.setString("to_account_number", newRow, tpmVariables.getString("value", variablesRow));
							break;
							
						case "ToAccountId":
							returnt.setInt("to_account_id", newRow, 
									new Integer(tpmVariables.getString("value", variablesRow)).intValue());
							break;
														
						case "ToAccountName":
							returnt.setString("to_account_name", newRow, tpmVariables.getString("value", variablesRow));
							break;

						case "Reference":
							returnt.setString("reference", newRow, tpmVariables.getString("value", variablesRow));
							break;

						case "Metal":
							returnt.setString("metal", newRow, tpmVariables.getString("value", variablesRow));
							break;

						case "MetalId":
							returnt.setInt("metal_id", newRow, 
									new Integer(tpmVariables.getString("value", variablesRow)).intValue());
							
						case "TransferUnit":	
							returnt.setString("transfer_unit", newRow, tpmVariables.getString("value", variablesRow));
							break;	
						default:
							// Skip variable not relevant to this plugin
							break;
					}
				}
			} catch (OException e) {
	            String errorMessage = "Error extracting TPM variables for workflow id " + workflowId + ". " + e.getMessage();
	            PluginLog.error(errorMessage);
	            throw new RuntimeException(errorMessage);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.metalTransfer.confirmations.dataSource.ReportBuilderDataSourceBase#getConstRepContext()
	 */
	@Override
	final
	String getConstRepContext() {
		
		return "MetalTransferConfirmation";
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.metalTransfer.confirmations.dataSource.ReportBuilderDataSourceBase#getConstRepSubContext()
	 */
	@Override
	final
	String getConstRepSubContext() {

		return "TpmVariables";
	}   

	
}
