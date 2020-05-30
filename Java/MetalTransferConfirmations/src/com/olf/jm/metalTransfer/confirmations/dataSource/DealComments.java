package com.olf.jm.metalTransfer.confirmations.dataSource;

import java.util.ArrayList;
import java.util.Arrays;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;

import com.olf.openjvs.Tpm;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;




/**
 * The Class DealComments. Load the deal comments associated with a metal transfer strategy deal. 
 * The deals to load are ifentifed via the TPM workflow ids passed in the query results table to the 
 * data source.
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class DealComments extends ReportBuilderDataSourceBase {

	/* (non-Javadoc)
	 * @see com.olf.jm.metalTransfer.confirmations.dataSource.ReportBuilderDataSourceBase#buildReturnTable(com.olf.openrisk.table.Table)
	 */
	@Override
	final
	void buildReturnTable(final Table returnt)  {
		returnt.addColumn("tran_num", EnumColType.Int);
		returnt.addColumn("sap_transfer_3rd_party", EnumColType.String);
		returnt.addColumn("sap_transfer_3rd_party_ref", EnumColType.String);
		returnt.addColumn("sap_transfer", EnumColType.String);
		
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.metalTransfer.confirmations.dataSource.ReportBuilderDataSourceBase#setReturnTable(com.olf.openrisk.table.Table)
	 */
	@Override
	final
	void setReturnTable(final Table returnt)  {
		Table queryResults = getQueryResults();
		
		ArrayList<Integer> tranNumbers = new ArrayList<Integer>();
		
		for (int  row = 0;  row < queryResults.getRowCount(); row++) {
			long workflowId = queryResults.getLong("query_result", row);
			
			try {
				com.olf.openjvs.Table tpmVariables = Tpm.getVariables(workflowId).getTable(1, 1);
				
				tpmVariables.sortCol("name");
				int tranNumRow = tpmVariables.findString("name", "TranNum", SEARCH_ENUM.FIRST_IN_GROUP);
				
				if (tranNumRow > 0) { 
					tranNumbers.add(new Integer(tpmVariables.getString("value", tranNumRow)));
				}		
			} catch (Exception e) {
	            String errorMessage = "Error extracting TPM variables for workflow id " + workflowId + ". " + e.getMessage();
	            Logging.error(errorMessage);
	            throw new RuntimeException(errorMessage);
			}
		}
		
		Table dealComments = loadDealComments(tranNumbers);
		addDealCommentsToReturnT(returnt, dealComments);
		
		
	}
	
	/**
	 * Adds the deal comments to return table. Deal comments can be split 
	 * over multiple rows in the table, if this is the case the rows are recombined before 
	 * setting in the return table.
	 *
	 * @param returnt the table to add the comments to.
	 * @param dealComments the deal comments loaded from the db.
	 */
	private void addDealCommentsToReturnT(final Table returnt, final Table dealComments)  {
		Table dealNumbers = dealComments.getDistinctValues("tran_num");
		
		for (int dealRow = 0; dealRow < dealNumbers.getRowCount(); dealRow++) {
			int newRow = returnt.addRows(1);
			returnt.setInt("tran_num", newRow, dealNumbers.getInt("tran_num", dealRow));
			int[] noteTypes = {20006, 20007, 20008};
					
			for (int noteType : noteTypes) {
				// create a view showing entries for the current note type and deal number
				ConstTable notes = dealComments.createConstView("*", 
						"[tran_num] == " + dealNumbers.getInt(0, dealRow)
						+ " and [note_type] == " + noteType);
				
				if (notes.getRowCount() > 0) {
					String noteText = Arrays.toString(notes.getColumnValues("line_text"))
							.replace("[", "").replace("]", "").replace(", ", "");


					String columnName;
					switch (noteType) {
					case 20006:
						columnName = "sap_transfer_3rd_party";
						break;
					case 20007:
						columnName = "sap_transfer_3rd_party_ref";
						break;
					case 20008:
						columnName = "sap_transfer";
						break;
					default:
						String errorMessage = "Error, unknown note type " + noteType;
						Logging.error(errorMessage);
						throw new RuntimeException(errorMessage);
					}
					returnt.setString(columnName, newRow, noteText);
					
				}
			}
		}
	}

	/**
	 * Load deal comments.
	 *
	 * @param tranNumbers the tran numbers
	 * @return the table
	 */
	private Table loadDealComments(final ArrayList<Integer> tranNumbers) {
		String sql =  " select tran_num, note_type, type_name, line_num, line_text "
					+ " from tran_notepad "
					+ " join deal_comments_type on type_id = note_type "
					+ " where tran_num in (" + tranNumbers.toString().replace("[", "").replace("]", "") + ") "
					+ " order by tran_num, note_type, line_num";
		
        IOFactory iof = getScriptContext().getIOFactory();
        
        Logging.debug("About to run SQL. \n" + sql);
        
        
        Table dealComments = null;
        try {
        	dealComments = iof.runSQL(sql);
        } catch (Exception e) {
            String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
            Logging.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        
        
        return dealComments;		
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
		return "DealComments";
	}
}

