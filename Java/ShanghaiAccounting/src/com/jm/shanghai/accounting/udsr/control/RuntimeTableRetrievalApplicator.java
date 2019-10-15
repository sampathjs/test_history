package com.jm.shanghai.accounting.udsr.control;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jm.shanghai.accounting.udsr.AbstractShanghaiAccountingUdsr;
import com.jm.shanghai.accounting.udsr.model.fixed.ConfigurationItem;
import com.jm.shanghai.accounting.udsr.model.retrieval.JavaTable;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescriptionLoader;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.calendar.SymbolicDate;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.simulation.EnumResultClass;
import com.olf.openrisk.simulation.ResultType;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.ColumnFormatter;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumFormatDateTime;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;

/*
 * History:
 * 2018-11-17	V1.0 	jwaechter	- Initial version
 * 2019-02-11	V1.1	jwaechter	- Removed company ID (now in ReportBuilder reports)
 * 2019-09-15	V1.2	jwaechter	- Separated column creation from filling the column
 * 2019-10-04	V1.3	jwaechter	- Added retrieval of party info fields
 * 2019-10-07	V1.4	jwaechter	- Added symbolic date retrieval.
 */

/**
 * Class containing the logic to apply the retrieval logic for the UDSR. 
 * @author jwaechter
 * @version 1.4
 */
public class RuntimeTableRetrievalApplicator {
	private final RetrievalConfiguration rc;
	private final String retrievalLogic;
	private final String colNameRuntimeTable;
	private final AbstractShanghaiAccountingUdsr baseUdsr;
	
	// the attributes on the following block are all null / 0  in case of retrieval from computation table. 
	// they are being set depending on the type of retrieval operation and not all retrieval operations
	// use the complete set of attributes
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
	private String partyInfoName = null;
	private String partyInfoJoinColumn = null;
	private String baseSymbolicDateColumn = null;
	private String symbolicDate = null;
	
	
	public RuntimeTableRetrievalApplicator (AbstractShanghaiAccountingUdsr baseUdsr, RetrievalConfiguration rc,
			RetrievalConfigurationColDescriptionLoader colLoader) {
		this.rc = rc;
		this.baseUdsr = baseUdsr;
		this.colNameRuntimeTable = rc.getColumnValue(colLoader.getRuntimeDataTable());
		this.retrievalLogic = rc.getColumnValue(colLoader.getRetrievalLogic());
	}
	
	public void apply(RuntimeTableRetrievalApplicatorInput input) {
		if (isRetrievalFromUdsrDefinition()) {
			applyRetrievalFromUdsrDefinition(input.getRuntimeTable());
		} else if (isRetrievalFromSymbolicDateExpression()) {
			applyRetrievalOfSymbolicDate(input.getRuntimeTable(), input.getSession().getCalendarFactory());
		} else if (isRetrievalFromPartyInfoField()) {
			applyRetrievalFromPartyInfoField(input.getRuntimeTable(), input.getPartyInfoTable());
		} else if (isRetrievalFromParameterList(input.getParameters())) {
			applyRetrievalFromParameterList(input.getRuntimeTable(), input.getParameters());
		} else if (isRetrievalFromTransactionField(input.getTransactions())) {
			applyRetrievalFromTransactionField (input.getRuntimeTable(), input.getTransactions());
		} else if (isRetrievalFromRuntimeTable(input.getRuntimeTable())) {
			applyRetrievalFromComputationTable(input.getRuntimeTable());
		}  else if (isRetrievalFromResultType(input.getSession(), input.getPrerequisites(), input.getRuntimeTable())) {
			switch (resultClass) {
			case Tran:
				applyTranResultRetrieval(input.getRuntimeTable());
				break;
			case TranCum:
				applyTranResultRetrieval(input.getRuntimeTable());
				break;
			case TranLeg:
				applyTranResultRetrieval(input.getRuntimeTable());
				break;
			case Gen:
				applyGenResultRetrieval(input.getRuntimeTable());
				break;
			}
		}
	}

	public EnumColType getColType(JavaTable eventTable, Session session,
			Scenario scenario, RevalResults prerequisites,
			Transactions transactions, Map<String, String> parameters) {
		if (isRetrievalFromUdsrDefinition()) {
			return getColTypeForRetrievalFromUdsrDefinition();
		} else if (isRetrievalFromSymbolicDateExpression()) {
			return EnumColType.Int;
		} if (isRetrievalFromPartyInfoField()) {
			return EnumColType.String;
		} else if (isRetrievalFromParameterList(parameters)) {
			return getColTypeForRetrievalFromParameterList(parameters);
		} else if (isRetrievalFromTransactionField(transactions)) {
			return getColTypeForRetrievalFromTransactionField (transactions);
		} else if (isRetrievalFromRuntimeTable(eventTable)) {
			return getColTypeForRetrievalFromComputationTable(eventTable);
		}  else if (isRetrievalFromResultType(session, prerequisites, eventTable)) {
			switch (resultClass) {
			case Tran:
				return getResultTypeForTranResultRetrieval();
			case TranCum:
				return getResultTypeForTranResultRetrieval();
			case TranLeg:
				return getResultTypeForTranResultRetrieval();
			case Gen:
				return getResultTypeForGenResultRetrieval();
			}
		}
		throw new RuntimeException("Undefined column type for " + this.toString());
	}
	
	public void applyDefaultFormatting (RuntimeTableRetrievalApplicatorInput input) {
		if (isRetrievalFromUdsrDefinition()) {
			// do nothing
		} else if (isRetrievalFromSymbolicDateExpression()) {
			ColumnFormatter cf = input.getRuntimeTable().getFormatter().createColumnFormatterAsDateTime(EnumFormatDateTime.Date);
			input.getRuntimeTable().getFormatter().setColumnFormatter(colNameRuntimeTable, cf);
		} else if (isRetrievalFromPartyInfoField()) {
			// do nothing
		} else if (isRetrievalFromParameterList(input.getParameters())) {
			// do nothing
		} else if (isRetrievalFromTransactionField(input.getTransactions())) {
			// do nothing
		} else if (isRetrievalFromRuntimeTable(input.getRuntimeTable())) {
			// do nothing
		}  else if (isRetrievalFromResultType(input.getSession(), input.getPrerequisites(), input.getRuntimeTable())) {
			switch (resultClass) {
			case Tran:
				// do nothing
				break;
			case TranCum:
				// do nothing
				break;
			case TranLeg:
				// do nothing
				break;
			case Gen:
				// do nothing
				break;
			}
		}
	}
		
	private boolean isRetrievalFromSymbolicDateExpression() {
		if (retrievalLogic.trim().toLowerCase().startsWith("symbolicdate")) {
			int bracketOpen = retrievalLogic.indexOf("(");
			int bracketClosed = retrievalLogic.indexOf(")");
			int comma = retrievalLogic.indexOf(",");
			if (bracketOpen == -1 || bracketClosed == -1 || comma == -1 || 
					comma < bracketOpen || comma > bracketClosed || bracketOpen >= bracketClosed) {
				return false;
			}
			symbolicDate = retrievalLogic.substring(bracketOpen+1, comma).trim();
			baseSymbolicDateColumn = retrievalLogic.substring(comma+1, bracketClosed).trim();
			return true;
		} else {
			return false;
		}
	}
	
	private void applyRetrievalOfSymbolicDate(Table runtimeTable, CalendarFactory calendarFactory) {
		if (!runtimeTable.isValidColumn(baseSymbolicDateColumn)) {
			throw new RuntimeException("The column '" + baseSymbolicDateColumn + "' does not exist"
					+ " in the runtime table for symbolic date retrieval logic:\n" 
					+ retrievalLogic);
		}
		int baseDateColumnId = runtimeTable.getColumnId(baseSymbolicDateColumn);
		if (runtimeTable.getColumnType(baseDateColumnId) != EnumColType.Int) {
			throw new RuntimeException("The column '" + baseSymbolicDateColumn + "' is not of type Julian Date (int)"
					+ " in the runtime table for symbolic date retrieval logic:\n" 
					+ retrievalLogic);
		}
		String tokens[] = symbolicDate.split("\\|");
		SymbolicDate sd[] = new SymbolicDate[tokens.length]; 
		for (int i=0; i < tokens.length; i++) {
			sd[i] = calendarFactory.createSymbolicDate(tokens[i].trim());
		}
		
		for (int row = runtimeTable.getRowCount()-1; row >= 0; row--) {
			int baseDate = runtimeTable.getInt(baseDateColumnId, row);
			Date baseDateAsDate = calendarFactory.getDate(baseDate);
			Date evaluatedDate = baseDateAsDate;
			for (int i=0; i < sd.length; i++) {
				evaluatedDate = sd[i].evaluate(evaluatedDate);
			}
			int evaluatedDateJd = calendarFactory.getJulianDate(evaluatedDate);
			runtimeTable.setInt(colNameRuntimeTable, row, evaluatedDateJd);
		}
	}
	
	private boolean isRetrievalFromResultType(Session session,
			RevalResults prerequisites, JavaTable eventTable) {
		int posFirstDot = retrievalLogic.indexOf(".");
		if (posFirstDot == -1) {
			showGeneralRetrievalSyntaxError ("");
		}
		int posSecondDot = retrievalLogic.indexOf(".", posFirstDot+1);
		if (posSecondDot == -1) {
			showGeneralRetrievalSyntaxError ("");
		}
		
		resultClassName = retrievalLogic.substring(0, posFirstDot).trim();
		resultTypeName = retrievalLogic.substring(posFirstDot+1, posSecondDot).trim();
		colName = retrievalLogic.substring(posSecondDot+1).trim();
		
		retrieveResultClass(resultClassName);
		retrieveResultType(session, resultTypeName);

		srcTableEndur = prerequisites.getResultTable(resultType);
		if (colName.equals(resultTypeName) && this.resultClass != EnumResultClass.Gen) {
			colName = Integer.toString(resultTypeId);
		}
		if (srcTableEndur.isValidColumn(colName)) {
			if (!eventTable.isValidColumn(colNameRuntimeTable)) {
				return true;
			} else {
				showGeneralRetrievalSyntaxError ("The column name '" + colNameRuntimeTable + "' for result type '" + resultTypeName + "' is already used in the runtime data table\n\n" );
			}
		} else {
			showGeneralRetrievalSyntaxError ("Could not find the provided column '" + colName + "' for result type '" + resultTypeName + "'\n\n" );
		}
		// TODO: additional checks for join heuristics from 
		return true;
	}

	private boolean isRetrievalFromRuntimeTable(JavaTable eventTable) {
		if (!retrievalLogic.contains(".")) {
//			srcTableJava = runtimeTable;
			if (!eventTable.isValidColumn(retrievalLogic)) {
				showGeneralRetrievalSyntaxError("The provided column name '" + retrievalLogic + "' does not exist in the computation data table.\n\n");
			}
			colName = retrievalLogic;
			return true;
		} else {
			return false;
		}
	}

	private EnumColType getResultTypeForGenResultRetrieval() {
		return srcTableEndur.getColumnType(srcTableEndur.getColumnId(colName));
	}

	private EnumColType getResultTypeForTranResultRetrieval() {
		return srcTableEndur.getColumnType(srcTableEndur.getColumnId(colName));
	}

	private EnumColType getColTypeForRetrievalFromComputationTable(JavaTable eventTable) {
		return eventTable.getColumnType(colName);
	}

	private EnumColType getColTypeForRetrievalFromTransactionField(
			Transactions transactions) {
		return EnumColType.String;
	}

	private EnumColType getColTypeForRetrievalFromParameterList(Map<String, String> parameters) {
		return EnumColType.String;		
	}

	private EnumColType getColTypeForRetrievalFromUdsrDefinition() {
		return EnumColType.String;
	}

	public String getColNameRuntimeTable() {
		return colNameRuntimeTable;
	}

	private void applyRetrievalFromUdsrDefinition(Table runtimeTable) {
		int colId = -1;
		if (!runtimeTable.isValidColumn(colNameRuntimeTable)) {
			throw new RuntimeException ("The column '" + colNameRuntimeTable 
					+ "' does not exist in the runtimeTable while applying \n" + rc.toString());
		} else {
			colId = runtimeTable.getColumnId(colNameRuntimeTable);
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
		if (retrievalLogic.trim().toLowerCase().startsWith("udsrdefinition")) {
			int bracketOpen = retrievalLogic.indexOf("(");
			int bracketClosed = retrievalLogic.indexOf(")");
			if (bracketOpen == -1 || bracketClosed == -1) {
				return false;
			}
			udsrDefField = retrievalLogic.substring(bracketOpen+1, bracketClosed).trim();
			return true;
		}
		return false;
	}

	private void applyRetrievalFromTransactionField(Table runtimeTable, Transactions transactions) {
		int tranFieldColId = -1;
		if (!runtimeTable.isValidColumn(colNameRuntimeTable)) {
			throw new RuntimeException ("The column '" + colNameRuntimeTable 
					+ "' does not exist in the runtimeTable while applying \n" + rc.toString());
		} else {
			tranFieldColId = runtimeTable.getColumnId(colNameRuntimeTable);
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

	private boolean isRetrievalFromTransactionField(Transactions transactions) {
		if (retrievalLogic.trim().toLowerCase().startsWith("tranfield")) {
			int bracketOpen = retrievalLogic.indexOf("(");
			int bracketClosed = retrievalLogic.indexOf(")");
			if (bracketOpen == -1 || bracketClosed == -1) {
				return false;
			}
			tranInfoFieldName = retrievalLogic.substring(bracketOpen+1, bracketClosed).trim();
			return true;
		}
		return false;
	}

	private void applyRetrievalFromParameterList(Table runtimeTable, Map<String, String> parameters) {
		String paramValue = parameters.get(parameterName);
		if (!runtimeTable.isValidColumn(colNameRuntimeTable)) {
			throw new RuntimeException ("The column '" + colNameRuntimeTable 
					+ "' does already exist in the runtimeTable while applying \n" + rc.toString());
		}
		runtimeTable.setColumnValues(colNameRuntimeTable, paramValue);
	}
	
	private void applyRetrievalFromPartyInfoField(Table runtimeTable,
			Table partyInfoTable) {
		String columnNames = "value->" + colNameRuntimeTable;
		String whereClause = "[In.party_id] == [Out." + partyInfoJoinColumn + "] " 
				+ " AND [In.type_name] == '" + partyInfoName + "'";
		runtimeTable.select(partyInfoTable, columnNames, whereClause);
	}

	private boolean isRetrievalFromParameterList(Map<String, String> parameters) {
		if (retrievalLogic.trim().toLowerCase().startsWith("parameter")) {
			int bracketOpen = retrievalLogic.indexOf("(");
			int bracketClosed = retrievalLogic.indexOf(")");
			if (bracketOpen == -1 || bracketClosed == -1) {
				return false;
			}
			parameterName = retrievalLogic.substring(bracketOpen+1, bracketClosed).trim();
			String[] tokens = parameterName.split("\\\\");
			if (tokens.length != 3) {
				throw new RuntimeException (getInvalidParameterExceptionText());
			}
			if (!parameters.containsKey(parameterName)) {
				throw new RuntimeException ("The parameter defined in '" + retrievalLogic 
						+ "' is not known. Known parameters: " + parameters.toString());
			}
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isRetrievalFromPartyInfoField() {
		if (retrievalLogic.trim().toLowerCase().startsWith("partyinfo")) {
			int bracketOpen = retrievalLogic.indexOf("(");
			int bracketClosed = retrievalLogic.indexOf(")");
			int comma = retrievalLogic.indexOf(",");
			if (bracketOpen == -1 || bracketClosed == -1 || comma == -1 || 
					comma < bracketOpen || comma > bracketClosed || bracketOpen >= bracketClosed) {
				return false;
			}
			partyInfoName = retrievalLogic.substring(bracketOpen+1, comma).trim();
			partyInfoJoinColumn = retrievalLogic.substring(comma+1, bracketClosed).trim();
			return true;
		} else {
			return false;
		}
	}

	private String getInvalidParameterExceptionText() {
		return "The parameter retrieval definition '" + retrievalLogic 
				+ "' is invalid. It should follow the syntax shown below: "
				+ "\n  Parameter(<Configuration Type>\\<Selection>\\<Field Name> "
				+ "\n  eg. Parameter (Result\\Accounting\\Mode)";
	}

	private void applyGenResultRetrieval(Table runtimeTable) {
		// if necessary apply special joins for individual sim results before the
		// heuristic join
		if (		resultTypeName.equals("JM General Ledger Data")
				||	resultTypeName.equals("JM Metal Ledger Data")
				||  resultTypeName.equals("JM Tran Data")) {
			srcTableEndur = srcTableEndur.getDistinctValues("tran_num", true);
			runtimeTable.select(srcTableEndur, colName + "->" + colNameRuntimeTable, "[In.tran_num] == [Out.tran_num]");// AND [In.deal_leg_phy] == -1 AND [In.deal_leg_fin] == -1");
			return;
		}
		
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
			runtimeTable.select(srcTableEndur, colName + "->" + colNameRuntimeTable, joinConditions.toString());
		} else {
			throw new RuntimeException ("Could not apply heuristic join between prerequisite sim result '" + resultTypeName + "'"
					+ " and the runtime computation table.");
		}
	}

	private void applyTranResultRetrieval(Table runtimeTable) {
		runtimeTable.select(srcTableEndur, colName + "->" + colNameRuntimeTable, "[In.deal_num] == [Out.deal_tracking_num] AND [In.deal_leg] == [Out.ins_para_seq_num]");
	}

	private boolean isRetrievalFromResultType(Session session, RevalResults prerequisites, Table runtimeTable) {
		int posFirstDot = retrievalLogic.indexOf(".");
		if (posFirstDot == -1) {
			showGeneralRetrievalSyntaxError ("");
		}
		int posSecondDot = retrievalLogic.indexOf(".", posFirstDot+1);
		if (posSecondDot == -1) {
			showGeneralRetrievalSyntaxError ("");
		}
		
		resultClassName = retrievalLogic.substring(0, posFirstDot).trim();
		resultTypeName = retrievalLogic.substring(posFirstDot+1, posSecondDot).trim();
		colName = retrievalLogic.substring(posSecondDot+1).trim();
		
		retrieveResultClass(resultClassName);
		retrieveResultType(session, resultTypeName);

		srcTableEndur = prerequisites.getResultTable(resultType);
		if (colName.equals(resultTypeName) && this.resultClass != EnumResultClass.Gen) {
			colName = Integer.toString(resultTypeId);
		}
		if (srcTableEndur.isValidColumn(colName)) {
			if (!runtimeTable.isValidColumn(colNameRuntimeTable)) {
				showGeneralRetrievalSyntaxError ("The column name '" + colNameRuntimeTable + "' for result type '" + resultTypeName + "' is already used in the runtime data table\n\n" );
			} else {
				return true;
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
		if (!retrievalLogic.contains(".")) {
			srcTableEndur = runtimeTable;
			if (!srcTableEndur.isValidColumn(retrievalLogic)) {
				showGeneralRetrievalSyntaxError("The provided column name '" + retrievalLogic + "' does not exist in the computation data table.\n\n");
			}
			colName = retrievalLogic;
			return true;
		} else {
			return false;
		}
	}

	private void applyRetrievalFromComputationTable(Table runtimeTable) {
		if (colNameRuntimeTable.equals(retrievalLogic)) {
			return;
		}
		runtimeTable.copyColumnData(runtimeTable.getColumnId(colName), runtimeTable.getColumnId(colNameRuntimeTable));
	}
	
	public static class RuntimeTableRetrievalApplicatorInput {
		private final Table runtimeTable;
		private final Session session;
		private final Scenario scenario;
		private final RevalResults prerequisites;
		private final Transactions transactions;
		private final Map<String, String> parameters;
		private final Table partyInfoTable;
		
		public RuntimeTableRetrievalApplicatorInput (Table runtimeTable, Session session,
				Scenario scenario, RevalResults prerequisites,
				Transactions transactions, Map<String, String> parameters,
				Table partyInfoTable) {
			this.runtimeTable = runtimeTable;
			this.session = session;
			this.scenario = scenario;
			this.prerequisites = prerequisites;
			this.transactions = transactions;
			this.parameters = parameters;
			this.partyInfoTable = partyInfoTable;
		}

		public Table getRuntimeTable() {
			return runtimeTable;
		}

		public Session getSession() {
			return session;
		}

		public Scenario getScenario() {
			return scenario;
		}

		public RevalResults getPrerequisites() {
			return prerequisites;
		}

		public Transactions getTransactions() {
			return transactions;
		}

		public Map<String, String> getParameters() {
			return parameters;
		}

		public Table getPartyInfoTable() {
			return partyInfoTable;
		}		
	}
}
