package com.olf.jm.sr142111.app;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openjvs.OException;
import com.olf.openjvs.StlDoc;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.Queries;
import com.olf.openrisk.io.Query;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/*
 * History: 
 * 2016-12-23	V1.0	jwaechter	/ Initial Version
 */

@ScriptCategory({ EnumScriptCategory.Generic })
public class AssignInvoiceDocNumber extends AbstractGenericScript {
	private static final String DOC_QUERY_NAME = "SR142111";

	@Override
	public Table execute(final Session session, final ConstTable table) {
		QueryResult qr=null;
		try (Queries queries = session.getIOFactory().getQueries()) {
			for (Query q : queries) {
				if (q.getName().equals(DOC_QUERY_NAME)) {
					qr = q.execute(true);
				}
			}
		}
		String sql = 
				"\nSELECT DISTINCT d.document_num, d.deal_tracking_num"
						+	"\nFROM " + qr.getDatabaseTableName() + " qr"
						+	"\n  INNER JOIN stldoc_details d"
						+	"\n     ON d.event_num = qr.query_result"
						+   "\n  INNER JOIN stldoc_header h"
						+	"\n  	ON h.document_num = d.document_num"
						+	"\n	    AND h.doc_type = 1" // invoice 
						+   "\n  INNER JOIN deal_document_link ddl"
						+	"\n  	ON ddl.deal_tracking_num = d.deal_tracking_num"
						+	"\nWHERE qr.unique_id = " + qr.getId()
			;
		try (Table sqlResult = session.getIOFactory().runSQL(sql);
			Table report = session.getTableFactory().createTable("Report about deals having no invoice document linked")) {
			report.addColumn("deal_num", EnumColType.Int);
			report.addColumn("doc_num", EnumColType.Int);
			for (int row=sqlResult.getRowCount()-1; row >= 0; row--) {
				int docNum = sqlResult.getInt("document_num", row);
				int dealNum = sqlResult.getInt("deal_tracking_num", row);
				try (Transaction tran =session.getTradingFactory().retrieveTransactionByDeal(dealNum)) {
					Field field = tran.getField(EnumTransactionFieldId.DealDocumentTable);
					ConstTable dealTable = field.getValueAsTable();
					int linkRow = -1;
					for (int r2 = dealTable.getRowCount()-1; r2 >= 0; r2--) {
						String ref = dealTable.getString("reference", r2);
						if (ref.equalsIgnoreCase("Invoice")) {
							linkRow = r2;
							break;
						}
					}
					if (linkRow != -1) {
						String savedLink = dealTable.getString("saved_link", linkRow);
						String fileName = savedLink.substring(savedLink.lastIndexOf("\\")+1);
						String invoiceDocNum = fileName.substring(0, fileName.indexOf("_"));
						try {
							StlDoc.saveInfoValue(docNum, "Our Doc Num", invoiceDocNum);
						} catch (OException e) {
							throw new RuntimeException (e.toString());
						}
					} else {
						TableRow r = report.addRow();
						report.setInt("deal_num", r.getNumber(), dealNum);
						report.setInt("doc_num", r.getNumber(), docNum);
					}
				}
			}
			session.getDebug().viewTable(report);
			return null;
		} finally {
			qr.clear();
			qr.close();		
		}	
	}



}
