package com.jm.shanghai.accounting.udsr.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jm.shanghai.accounting.udsr.AbstractShanghaiAccountingUdsr;
import com.jm.shanghai.accounting.udsr.model.fixed.ConfigurationItem;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.simulation.EnumResultClass;
import com.olf.openrisk.simulation.ResultType;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;

/*
 * History:
 * 2018-11-17	V1.0 	jwaechter	- Initial version
 * 2019-02-11	V1.1	jwaechter	- Removed company ID (now in ReportBuilder reports)
 */


/**
 * Class containing the logic to apply the retrieval logic for the UDSR. 
 * @author jwaechter
 * @version 1.1
 */
public class RuntimeTableRetrievalApplicator {
	private final RetrievalConfiguration rc;
	private final AbstractShanghaiAccountingUdsr baseUdsr;
	
	// the attributes on the following block are all null / 0  in case of retrieval from computation table. 
	private EnumResultClass resultClass=null; 
	private ResultType resultType = null;
	private int resultTypeId = -1;
	private String resultClassName = null;
	private String resultTypeName = null;
	private String colName = null;
	private ConstTable srcTableEndur = null;
	private String parameterName = null;
	private String tranInfoFieldName = null;
	private String udsrDefField = null;
//	private JavaTable srcTableJava = null;
	
	
	public RuntimeTableRetrievalApplicator (AbstractShanghaiAccountingUdsr baseUdsr, RetrievalConfiguration rc) {
		this.rc = rc;
		this.baseUdsr = baseUdsr;
	}
		
	public void apply(Table runtimeTable, Session session,
			Scenario scenario, RevalResults prerequisites,
			Transactions transactions, Map<String, String> parameters) {
		
		if (isRetrievalFromUdsrDefinition()) {
			applyRetrievalFromUdsrDefinition(runtimeTable);
		} else if (isRetrievalFromParameterList(parameters)) {
			applyRetrievalFromParameterList(runtimeTable, parameters);
		} else if (isRetrievalFromTransactionInfoField(runtimeTable, transactions)) {
			applyRetrievalFromTransactionInfoField (runtimeTable, transactions);
		} else if (isRetrievalFromRuntimeTable(runtimeTable)) {
			applyRetrievalFromComputationTable();
		}  else if (isRetrievalFromResultType(session, prerequisites, runtimeTable)) {
			switch (resultClass) {
			case Tran:
				applyTranResultRetrieval(runtimeTable);
				break;
			case TranCum:
				applyTranResultRetrieval(runtimeTable);
				break;
			case TranLeg:
				applyTranResultRetrieval(runtimeTable);
				break;
			case Gen:
				applyGenResultRetrieval(runtimeTable);
				break;
			}
		}
	}

	private void applyRetrievalFromUdsrDefinition(Table runtimeTable) {
		int colId = -1;
		if (!runtimeTable.isValidColumn(rc.getColNameRuntimeTable())) {
			colId = runtimeTable.addColumn(rc.getColNameRuntimeTable(), EnumColType.String).getNumber();
		} else {
			throw new RuntimeException ("The column '" + rc.getColNameRuntimeTable() 
					+ "' does already exist in the runtimeTable while applying \n" + rc.toString());
		}
		String value = "";
		switch (udsrDefField.toLowerCase()) {
		case "typeprefix":
			value = baseUdsr.getTypePrefix().getValue();
			break;
		default:
			throw new RuntimeException("The field '" + udsrDefField + "' does not exist. "
					+ "Please adapt table '" + ConfigurationItem.RETRIEVAL_CONFIG_TABLE_NAME.getValue()
					+ "' containing the row + '" + rc + "'");
		}
		runtimeTable.setColumnValues(colId, value);
	}

	private boolean isRetrievalFromUdsrDefinition() {
		if (rc.getRetrievalLogic().trim().toLowerCase().startsWith("udsrdefinition")) {
			int bracketOpen = rc.getRetrievalLogic().indexOf("(");
			int bracketClosed = rc.getRetrievalLogic().indexOf(")");
			if (bracketOpen == -1 || bracketClosed == -1) {
				return false;
			}
			udsrDefField = rc.getRetrievalLogic().substring(bracketOpen+1, bracketClosed).trim();
			return true;
		}
		return false;
	}

	private void applyRetrievalFromTransactionInfoField(Table runtimeTable, Transactions transactions) {
		int tranFieldColId = -1;
		if (!runtimeTable.isValidColumn(rc.getColNameRuntimeTable())) {
			tranFieldColId = runtimeTable.addColumn(rc.getColNameRuntimeTable(), EnumColType.String).getNumber();
		} else {
			throw new RuntimeException ("The column '" + rc.getColNameRuntimeTable() 
					+ "' does already exist in the runtimeTable while applying \n" + rc.toString());
		}
		int colIdTranNum = runtimeTable.getColumnId("tran_num");
		for (int rowId = runtimeTable.getRowCount()-1; rowId >= 0; rowId--) {
			int tranNum = runtimeTable.getInt(colIdTranNum, rowId);
			Transaction tran = transactions.getTransactionById(tranNum);
			Field field = tran.getField(tranInfoFieldName);
			if (field != null) {
				try {
					String displayString = field.getDisplayString();
					runtimeTable.setString(tranFieldColId, rowId, displayString);					
				} catch (OpenRiskException ex) {
					runtimeTable.setString(tranFieldColId, rowId, "");
				}
			} else {
			}
		}
	}

	private boolean isRetrievalFromTransactionInfoField(Table runtimeTable,
			Transactions transactions) {
		if (rc.getRetrievalLogic().trim().toLowerCase().startsWith("tranfield")) {
			int bracketOpen = rc.getRetrievalLogic().indexOf("(");
			int bracketClosed = rc.getRetrievalLogic().indexOf(")");
			if (bracketOpen == -1 || bracketClosed == -1) {
				return false;
			}
			tranInfoFieldName = rc.getRetrievalLogic().substring(bracketOpen+1, bracketClosed).trim();
			return true;
		}
		return false;
	}

	private void applyRetrievalFromParameterList(Table runtimeTable, Map<String, String> parameters) {
		String paramValue = parameters.get(parameterName);
		if (!runtimeTable.isValidColumn(rc.getColNameRuntimeTable())) {
			runtimeTable.addColumn(rc.getColNameRuntimeTable(), EnumColType.String);
		} else {
			throw new RuntimeException ("The column '" + rc.getColNameRuntimeTable() 
					+ "' does already exist in the runtimeTable while applying \n" + rc.toString());
		}
		runtimeTable.setColumnValues(rc.getColNameRuntimeTable(), paramValue);
	}

	private boolean isRetrievalFromParameterList(Map<String, String> parameters) {
		if (rc.getRetrievalLogic().trim().toLowerCase().startsWith("parameter")) {
			int bracketOpen = rc.getRetrievalLogic().indexOf("(");
			int bracketClosed = rc.getRetrievalLogic().indexOf(")");
			if (bracketOpen == -1 || bracketClosed == -1) {
				return false;
			}
			parameterName = rc.getRetrievalLogic().substring(bracketOpen+1, bracketClosed).trim();
			String[] tokens = parameterName.split("\\\\");
			if (tokens.length != 3) {
				throw new RuntimeException (getInvalidParameterExceptionText());
			}
			if (!parameters.containsKey(parameterName)) {
				throw new RuntimeException ("The parameter defined in '" + rc.getRetrievalLogic() 
						+ "' is not known. Known parameters: " + parameters.toString());
			}
			return true;
		} else {
			return false;
		}
	}

	private String getInvalidParameterExceptionText() {
		return "The parameter retrieval definition '" + rc.getRetrievalLogic() 
				+ "' is invalid. It should follow the following syntax: "
				+ "\n  Parameter(<Configuration Type>\\<Selection>\\<Field Name> "
				+ "\n  eg. Parameter (Result\\Accounting\\Mode)";
	}

	private void applyGenResultRetrieval(Table runtimeTable) {
		// if necessary apply special joins for individual sim results before the
		// heuristic join
		if (resultTypeName.equals("JM Sales Ledger Data")) {
			Table reducedCopy = srcTableEndur.cloneData();
			Map<Integer, Integer> dealNumToMaxDocNum = new HashMap<>();
			srcTableEndur = reducedCopy;				
			for (int row = reducedCopy.getRowCount()-1; row >= 0; row--) {
				int dealNum = reducedCopy.getInt("deal_num", row);
				int endurDocNum = reducedCopy.getInt("endur_doc_num", row);
				if (dealNumToMaxDocNum.containsKey(dealNum)) {
					int existingDocNum = dealNumToMaxDocNum.get(dealNum);
					if (endurDocNum > existingDocNum) {
						dealNumToMaxDocNum.put(dealNum, endurDocNum);
					}
				} else {
					dealNumToMaxDocNum.put(dealNum, endurDocNum);					
				}
			}
			for (int row = reducedCopy.getRowCount()-1; row >= 0; row--) {
				int dealNum = reducedCopy.getInt("deal_num", row);
				int endurDocNum = reducedCopy.getInt("endur_doc_num", row);
				int maxDocNumForDeal = dealNumToMaxDocNum.get(dealNum);
				String slDocumentStatus = reducedCopy.getString ("sl_status", row);
				if (maxDocNumForDeal > endurDocNum) {
					if (slDocumentStatus.equals("Pending Cancelled")) {
						
					}
					reducedCopy.removeRow(row);
				}
			}
		}
		applyHeuristicJoin(runtimeTable);
	}

	private void applyHeuristicJoin(Table runtimeTable) {
		List<StringBuilder> heuristicJoinConditions = new ArrayList<>();
		for (HeuristicJoinColPair hjp : HeuristicJoinColPair.values()) {
			if (hjp.canApply(srcTableEndur, runtimeTable)) {
				heuristicJoinConditions.add(hjp.getJoinCondition());
				break;
			}
		}
		if (heuristicJoinConditions.size() > 0) {
			StringBuilder joinConditions = new StringBuilder();
			boolean first=true;
			for (StringBuilder hjc : heuristicJoinConditions) {
				if (!first) {
					joinConditions.append(" AND ");
				}
				joinConditions.append(hjc);
				first = false;
			}
			runtimeTable.select(srcTableEndur, colName + "->" + rc.getColNameRuntimeTable(), joinConditions.toString());
		} else {
			throw new RuntimeException ("Could not apply heuristic join between prerequisite sim result '" + resultTypeName + "'"
					+ " and the runtime computation table.");
		}
	}

	private void applyTranResultRetrieval(Table runtimeTable) {	
		runtimeTable.select(srcTableEndur, colName + "->" + rc.getColNameRuntimeTable(), "[In.deal_num] == [Out.deal_tracking_num] AND [In.deal_leg] == [Out.ins_para_seq_num]");
	}

	private boolean isRetrievalFromResultType(Session session, RevalResults prerequisites, Table runtimeTable) {
		int posFirstDot = rc.getRetrievalLogic().indexOf(".");
		if (posFirstDot == -1) {
			showGeneralRetrievalSyntaxError ("");
		}
		int posSecondDot = rc.getRetrievalLogic().indexOf(".", posFirstDot+1);
		if (posSecondDot == -1) {
			showGeneralRetrievalSyntaxError ("");
		}
		
		resultClassName = rc.getRetrievalLogic().substring(0, posFirstDot).trim();
		resultTypeName = rc.getRetrievalLogic().substring(posFirstDot+1, posSecondDot).trim();
		colName = rc.getRetrievalLogic().substring(posSecondDot+1).trim();
		
		retrieveResultClass(resultClassName);
		retrieveResultType(session, resultTypeName);

		srcTableEndur = prerequisites.getResultTable(resultType);
		if (colName.equals(resultTypeName) && this.resultClass != EnumResultClass.Gen) {
			colName = Integer.toString(resultTypeId);
		}
		if (srcTableEndur.isValidColumn(colName)) {
			if (!runtimeTable.isValidColumn(rc.getColNameRuntimeTable())) {
				return true;
			} else {
				showGeneralRetrievalSyntaxError ("The column name '" + rc.getColNameRuntimeTable() + "' for result type '" + resultTypeName + "' is already used in the runtime data table\n\n" );
			}
		} else {
			showGeneralRetrievalSyntaxError ("Could not find the provided column '" + colName + "' for result type '" + resultTypeName + "'\n\n" );
		}
		// TODO: additional checks for join heuristics from 
		return true;
	}

	private void retrieveResultClass(String resultClass) {
		switch (resultClass) {
		case "Tran Results":
			this.resultClass = EnumResultClass.Tran;
			break;
		case "Cum Results":
			this.resultClass = EnumResultClass.TranCum;
			break;
		case "Leg Results":
			this.resultClass = EnumResultClass.TranLeg;
			break;
		case "Gen Results":
			this.resultClass = EnumResultClass.Gen;
			break;
		default:
			showGeneralRetrievalSyntaxError ("Error: illegal result class '" + resultClass + "' in retrieval configuration table row " + this.toString() +"\n\n");
		}
	}

	private void retrieveResultType(Session session, String resultType) {
		this.resultType = (ResultType)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.ResultType, resultType);
		this.resultTypeId = this.resultType.getId();
	}
	
	private void showGeneralRetrievalSyntaxError(String firstMessagePart) {
		String errorMessage = firstMessagePart + "Syntax of the retrieval logic does has to follow one of the two allowed options: "
				+ "\n1. <column_name> for data retrieved from an existing field in the runtime computation table (e.g. the deal event fields)"
				+ "\n2. <result class>.<result type>.<column name>. "
				+ "\n Allowed values for \"result class\"\n- Tran Results\n- Cum Results \n- Leg Results \n Gen Results\n\nFor Gen Results an optional selection part in brackets can follow.\n\nUse \"Simulation Results\" -> \"Raw Viewer\" for details.";
		throw new RuntimeException (errorMessage);
	}

	private boolean isRetrievalFromRuntimeTable(Table runtimeTable) {
		if (!rc.getRetrievalLogic().contains(".")) {
//			srcTableJava = runtimeTable;
			srcTableEndur = runtimeTable;
			if (!srcTableEndur.isValidColumn(rc.getRetrievalLogic())) {
				showGeneralRetrievalSyntaxError("The provided column name '" + rc.getRetrievalLogic() + "' does not exist in the computation data table.\n\n");
			}
			return true;
		} else {
			return false;
		}
	}

	private void applyRetrievalFromComputationTable() {
		if (rc.getColNameRuntimeTable().equals(rc.getRetrievalLogic())) {
			return;
		}
//		srcTableJava.renameColumn(rc.getRetrievalLogic(), rc.getColNameRuntimeTable()); 
		((Table)srcTableEndur).setColumnName(srcTableEndur.getColumnId(rc.getRetrievalLogic()),
				rc.getColNameRuntimeTable()); 
	}
}
