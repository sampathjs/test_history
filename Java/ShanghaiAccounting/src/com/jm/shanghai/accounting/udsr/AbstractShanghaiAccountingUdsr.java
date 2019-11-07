package com.jm.shanghai.accounting.udsr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.jm.shanghai.accounting.udsr.control.MappingTableFilterApplicator;
import com.jm.shanghai.accounting.udsr.control.OutputTableRetrievalApplicator;
import com.jm.shanghai.accounting.udsr.control.RuntimeTableRetrievalApplicator;
import com.jm.shanghai.accounting.udsr.model.fixed.ConfigurationItem;
import com.jm.shanghai.accounting.udsr.model.fixed.PartyInfoFields;
import com.jm.shanghai.accounting.udsr.model.mapping.MappingConfigurationColType;
import com.jm.shanghai.accounting.udsr.model.mapping.MappingTableCellConfiguration;
import com.jm.shanghai.accounting.udsr.model.mapping.MappingTableColumnConfiguration;
import com.jm.shanghai.accounting.udsr.model.mapping.MappingTableRowConfiguration;
import com.jm.shanghai.accounting.udsr.model.mapping.predicate.AbstractPredicate;
import com.jm.shanghai.accounting.udsr.model.mapping.predicate.PredicateParser;
import com.jm.shanghai.accounting.udsr.model.retrieval.JavaTable;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationTableCols;
import com.olf.embedded.simulation.AbstractSimulationResult2;
import com.olf.embedded.simulation.RevalResult;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.control.EnumSplitMethod;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.simulation.Configuration;
import com.olf.openrisk.simulation.ConfigurationField;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.ColumnFormatterAsRef;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableColumn;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2018-11-14		V1.0		jwaechter		- Initial Version
 * 2019-02-11		V1.1		jwaechter		- Moved company ID to ReportBuilder report
 *                                              - Removing unused code
 *                                              - Refactoring
 *                                              - Comments
 */

/**
 * Main Plugin to generate the raw data for the accounting postings 
 * in Shanghai.
 * @author jwaechter
 * @version 1.1
 */
public abstract class AbstractShanghaiAccountingUdsr extends AbstractSimulationResult2 {
	
	/**
	 * The configuration items designating the type of the result (GL vs SL) that is also used
	 * as a filter during rule mapping.
	 * */
	private final ConfigurationItem typePrefix;
		
	/**
	 * Column name of the an artificially created id column for mapping tables.
	 * This column name may not be used in any mapping table.	
	 */
	private static final String ROW_ID = "row_id";
	
	protected AbstractShanghaiAccountingUdsr (final ConfigurationItem typePrefix) {
		super();
		this.typePrefix = typePrefix;
	}

	@Override
	public void calculate(final Session session, final Scenario scenario,
			final RevalResult revalResult, final Transactions transactions,
			final RevalResults prerequisites) {
		init(session);
		try {
			long startTime = System.currentTimeMillis();
			PluginLog.info("Starting calculate of Raw Accounting Data UDSR");
			Map<String, String> parameters = generateSimParamTable(scenario);
			PluginLog.debug (parameters.toString());
			
			JavaTable eventDataTable = createEventDataTable(transactions, session.getTableFactory(), revalResult);
			PluginLog.info("Creation of event data table finished");
					
			PluginLog.info("Starting retrieval");
			 // using java table didn't bring an advantage over using an endur table
			long startRetrieval = System.currentTimeMillis();
			eventDataTable.mergeIntoEndurTable(revalResult.getTable());
			// apply all currently hard coded data retrieval by executing certain SQLs
			// and add the SQL results to the runtime table.
			retrieveAdditionalData (session, revalResult, transactions);
			List<RetrievalConfiguration> retrievalConfig = convertRetrievalConfigTableToList(session);
			// apply generic data retrieval according to the configuration in the retrieval table
			applyRetrievalToRuntimeTable(session, scenario, revalResult,
					transactions, prerequisites, parameters, retrievalConfig);
			long endRetrieval = System.currentTimeMillis();
			PluginLog.info("Finished retrieval. Computation time (ms): " + (endRetrieval-startRetrieval));
			// Apply hard wired formatting to certain columns to ensure the mapping takes names
			// not IDs
			formatColumns (revalResult);
			// generate unique row ids for each existing row in the runtime table before applying mapping
			// this allows later removal of rows from the runtime table that do no match.
			generateUniqueRowIdForTable(revalResult.getTable());
			showRuntimeDataTable(ConfigurationItem.VIEW_RUNTIME_DATA_TABLE_BEFORE_MAPPING, session, revalResult.getTable());
			// Apply mapping to account mapping table, tax code mapping table and material number mapping table
			applyAllMappings(session, revalResult, retrievalConfig);
			showRuntimeDataTable(ConfigurationItem.VIEW_RUNTIME_DATA_TABLE_AFTER_MAPPING, session, revalResult.getTable());
			// ensures updates based on changes in the const repo desktop are used
			ConfigurationItem.resetValues(); 
			long endTime = System.currentTimeMillis();
			PluginLog.info("Execution Time in ms: " + (endTime - startTime));
			PluginLog.info("Completed calculate of Raw Accounting Data UDSR");
		} catch (Throwable t) {
			PluginLog.info(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				PluginLog.info(ste.toString());
			}
			throw t;
		}
	}

	private void retrieveAdditionalData(Session session,
			RevalResult revalResult, Transactions transactions) {
		Table runtimeTable = revalResult.getTable();
		// add all new columns to the runtime table in advance in a single step
		runtimeTable.addColumns(
				Arrays.asList("party_code_cn_debtor", "party_code_cn_creditor", "ext_bu_jm_group", "endur_doc_num", "endur_doc_status").toArray(new String[5]), 
				Arrays.asList(EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.Int, EnumColType.Int).toArray(new EnumColType[5]));
		if (runtimeTable.getRowCount() == 0) {
			return;
		}
		Table partyInfoTable = createPartyInfoTable(session);		
		joinPartyCodes(session, revalResult, transactions, partyInfoTable);
		
		ConstTable documentInfoTable = createDocumentInfoTable (session, runtimeTable);
		joinDocumentInfo(session, revalResult, documentInfoTable);
	}

	private void joinDocumentInfo(Session session,
			RevalResult revalResult, ConstTable documentInfoTable) {
		Table runtimeTable = revalResult.getTable();
		runtimeTable.select(documentInfoTable, "endur_doc_num, endur_doc_status", 
				"[In.tran_num] == [Out.tran_num] AND [In.ins_para_seq_num] == [Out.ins_para_seq_num]");
	}

	private ConstTable createDocumentInfoTable(Session session, Table runtimeTable) {
		Set<Integer> tranNums = new HashSet<Integer>(runtimeTable.getRowCount());
		for (int tranNum : runtimeTable.getColumnValuesAsInt("tran_num")) {
			tranNums.add(tranNum);
		}
		StringBuilder allTranNums = new StringBuilder();
		boolean first = true;
		for (int tranNum : tranNums) {
			if (!first) {
				allTranNums.append(",");
			}
			allTranNums.append(tranNum);
			first = false;
		}
		// assumption: there is only one invoice per deal
		int docTypeInvoiceId = session.getStaticDataFactory().getId(EnumReferenceTable.StldocDocumentType, "Invoice");
		String sql = 
				"\nSELECT DISTINCT h.document_num AS endur_doc_num, h.doc_status AS endur_doc_status, d.tran_num, d.ins_para_seq_num"
			+	"\nFROM stldoc_details d"
			+	"\nINNER JOIN stldoc_header h"
			+	"\n ON d.document_num = h.document_num"
			+	"\n    AND d.doc_version = h.doc_version"
			+	"\nWHERE d.tran_num IN (" + allTranNums.toString() + ")"
			+	"\n AND h.doc_type = " + docTypeInvoiceId 
			;
		Table docData = session.getIOFactory().runSQL(sql);
		return docData;
		
	}

	private void joinPartyCodes(Session session,
			RevalResult revalResult, Transactions transactions, Table partyInfoTable) {
		Table runtimeTable = revalResult.getTable();
		runtimeTable.select(partyInfoTable, "party_code_cn_debtor, party_code_cn_creditor", "[In.party_id] == [Out.ext_bunit_id]");
		runtimeTable.select(partyInfoTable, "ext_bu_jm_group", "[In.party_id] == [Out.ext_lentity_id]");
	}

	private void applyAllMappings(final Session session,
			final RevalResult revalResult,
			List<RetrievalConfiguration> retrievalConfig) {
		// the following lines apply the same mapping algorithm to different mapping tables.
		// all mapping tables are taking into account the current runtime table excluding the output
		// columns of the previous mapping. They are also using instances of RetrievalConfiguration.
		// The RetrievalConfiguration contains different methods retrieving a string denoting the column
		// name of a certain column for each type of mapping table. To ensure that for each mapping table
		// the right get column name method is used, a different instance of "ColNameProvider" is
		// used in a Java 8 Lambda like manner but with the prev Java 8 clumsy way of instancing
		// an anonymous interface.
		
		// account mapping table
		PluginLog.info("Starting of mapping logic (" + ConfigurationItem.MAPPING_CONFIG_TABLE_NAME.getValue() + ")");
		long startMapping = System.currentTimeMillis();
		// In java 8 this should be just rc -> rc.getColNameMappingTable()
		ColNameProvider colNameProvider = new ColNameProvider() {				
			@Override
			public String getColName(RetrievalConfiguration rc) {
				return rc.getColNameMappingTable(); 
			}
		};
		Map<String, MappingTableColumnConfiguration> mappingTableColConfig = applyMapping(
				ConfigurationItem.MAPPING_CONFIG_TABLE_NAME, colNameProvider, 
				session, revalResult, retrievalConfig);
		long endMappingTime = System.currentTimeMillis();
		PluginLog.info("End of Mapping. computation time " + ConfigurationItem.MAPPING_CONFIG_TABLE_NAME.getValue() + " (ms):  " + (endMappingTime - startMapping));
		// tax config mapping table
		PluginLog.info("Starting of mapping logic (" + ConfigurationItem.TAX_CONFIG_TABLE_NAME.getValue() + ")");
		startMapping = System.currentTimeMillis();
		// In java 8 this should be just rc -> rc.getColNameTaxTable() using lambda expressions
		colNameProvider = new ColNameProvider() {				
			@Override
			public String getColName(RetrievalConfiguration rc) {
				return rc.getColNameTaxTable();
			}
		};
		Map<String, MappingTableColumnConfiguration> taxTableColConfig = applyMapping(
				ConfigurationItem.TAX_CONFIG_TABLE_NAME, colNameProvider, 
				session, revalResult, retrievalConfig);
		endMappingTime = System.currentTimeMillis();
		PluginLog.info("End of Mapping. computation time " + ConfigurationItem.TAX_CONFIG_TABLE_NAME.getValue() + " (ms):  " + (endMappingTime - startMapping));			
		// material number mapping
		PluginLog.info("Starting of mapping logic (" + ConfigurationItem.MATERIAL_NUMBER_CONFIG_TABLE_NAME.getValue() + ")");
		startMapping = System.currentTimeMillis();
		// In java 8 this should be just rc -> rc.getColNameMaterialNumberTable()
		colNameProvider = new ColNameProvider() {				
			@Override
			public String getColName(RetrievalConfiguration rc) {
				return rc.getColNameMaterialNumberTable();
			}
		};
		Map<String, MappingTableColumnConfiguration> materialNumberTableColConfig = applyMapping(
				ConfigurationItem.MATERIAL_NUMBER_CONFIG_TABLE_NAME, colNameProvider, 
				session, revalResult, retrievalConfig);
		endMappingTime = System.currentTimeMillis();
		PluginLog.info("End of Mapping. computation time " + ConfigurationItem.MATERIAL_NUMBER_CONFIG_TABLE_NAME.getValue() + " (ms):  " + (endMappingTime - startMapping));
		// now retrieve the output columns for all mapping tables in a single step
		// there is no need to separate the mapping table configuration from different tables
		// at this point, as we already have copied over the output columns of the different 
		// mapping tables
		Map<String, MappingTableColumnConfiguration> allColConfigs = new HashMap<>();
		allColConfigs.putAll(mappingTableColConfig);
		allColConfigs.putAll(taxTableColConfig);
		allColConfigs.putAll(materialNumberTableColConfig);
		createOutputTable(revalResult, revalResult.getTable(), retrievalConfig, allColConfigs);
	}

	/**
	 * Executes the mapping for the provided mapping table.
	 * @param mappingTableName Contains the name of the user table with the mappings to process
	 * @param colNameProvider The method used to retrieve the name of the column given an instance
	 * of RetrievalConfiguration. RetrievalConfiguration captures the column names for
	 * all mapping tables, so this parameter allows this method to be used in a generic
	 * way over all mapping tables.
	 * @param session
	 * @param revalResult Contains the runtime table.
	 * @param retrievalConfig The parsed retrieval table.
	 * @return
	 */
	private Map<String, MappingTableColumnConfiguration> applyMapping(
			ConfigurationItem mappingTableName, ColNameProvider colNameProvider,
			final Session session, final RevalResult revalResult,
			List<RetrievalConfiguration> retrievalConfig) {
		Table mappingTable = null;
		mappingTable = retrieveMappingTable(session, mappingTableName);
		Map<String, MappingTableColumnConfiguration> mappingTableColConfig = 
				confirmMappingTableStructure (mappingTableName, colNameProvider, 
						mappingTable, revalResult.getTable(), retrievalConfig);
		generateUniqueRowIdForTable(mappingTable);
		List<MappingTableRowConfiguration> mappingRows = 
				parseMappingTable (colNameProvider, mappingTable, revalResult.getTable(),
						retrievalConfig, mappingTableColConfig);
		createMappingOutputColumnsInRuntimeTable (revalResult.getTable(), mappingTable, mappingTableColConfig);
		executeMapping(colNameProvider, revalResult.getTable(), mappingTable, mappingTableColConfig,
				mappingRows, retrievalConfig);
		return mappingTableColConfig;
	}

	private void applyRetrievalToRuntimeTable(final Session session,
			final Scenario scenario, final RevalResult revalResult,
			final Transactions transactions, final RevalResults prerequisites,
			Map<String, String> parameters,
			List<RetrievalConfiguration> retrievalConfig) {
		Collections.sort(retrievalConfig); // ensure priority based execution
		for (RetrievalConfiguration rc : retrievalConfig) {
			RuntimeTableRetrievalApplicator retrievalApplicator = new RuntimeTableRetrievalApplicator(this, rc);
			retrievalApplicator.apply (revalResult.getTable(), session, scenario, prerequisites, transactions, parameters);
		}
	}

	/**
	 * Executes a SQL to retrieve party info fields. Primary key of the
	 * table returned is "party_id". 
	 * Other columns (all of type string):
	 * <ul> 
	 *   <li> ext_bu_jm_group </li>
	 *   <li> party_code_cn_debtor </li>
	 *   <li> party_code_cn_creditor </li>  
	 * </ul>
	 * @param session
	 * @return
	 */
	private Table createPartyInfoTable(Session session) {
		String sql = 				
				   "\nSELECT p.party_id"
				+ "\n  ,ISNULL(pi_jm_group.value, pit_jm_group.default_value) AS ext_bu_jm_group"
				+ "\n  ,ISNULL(pi_party_code_cn_debtor_e.value, ISNULL(pi_party_code_cn_debtor_i.value, '')) AS party_code_cn_debtor"
				+ "\n  ,ISNULL(pi_party_code_cn_creditor_e.value, ISNULL(pi_party_code_cn_creditor_i.value, '')) AS party_code_cn_creditor"
				+ "\nFROM party p"
				+ "\n  INNER JOIN party_info_types pit_jm_group"
				+ "\n    ON pit_jm_group.type_name = '" + PartyInfoFields.JM_GROUP.getName() + "'"
				+ "\n  LEFT OUTER JOIN party_info pi_jm_group"
				+ "\n    ON p.party_id = pi_jm_group.party_id AND pi_jm_group.type_id = pit_jm_group.type_id"
				+ "\n  LEFT OUTER JOIN party_info pi_party_code_cn_debtor_e"
				+ "\n    ON p.party_id = pi_party_code_cn_debtor_e.party_id AND pi_party_code_cn_debtor_e.type_id = "
					+ PartyInfoFields.PARTY_CODE_CN_DEBTOR_EXTERNAL.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_party_code_cn_creditor_e"
				+ "\n    ON p.party_id = pi_party_code_cn_creditor_e.party_id AND pi_party_code_cn_creditor_e.type_id = "
					+ PartyInfoFields.PARTY_CODE_CN_CREDITOR_EXTERNAL.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_party_code_cn_debtor_i"
				+ "\n    ON p.party_id = pi_party_code_cn_debtor_i.party_id AND pi_party_code_cn_debtor_i.type_id = "
					+ PartyInfoFields.PARTY_CODE_CN_DEBTOR_INTERNAL.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_party_code_cn_creditor_i"
				+ "\n    ON p.party_id = pi_party_code_cn_creditor_i.party_id AND pi_party_code_cn_creditor_i.type_id = "
					+ PartyInfoFields.PARTY_CODE_CN_CREDITOR_INTERNAL.retrieveId(session)
				;
		return session.getIOFactory().runSQL(sql);
	}

	/**
	 * Reads out the simulation parameters from this UDSR run. The keys
	 * of the resulting map are stored in a path like string having the pattern
	 * %configuration type%\\%selection%\\name
	 * @param scenario
	 * @return
	 */
	private Map<String, String> generateSimParamTable(final Scenario scenario) {
		Map<String, String> parameters = new HashMap<>();
		for (Configuration config : scenario.getConfigurations()) {
			String configType = config.getConfigurationType().getName();
			String selection = config.getSelection();
			for (ConfigurationField cf : config.getFields()) {
				String name = cf.getName();
				String displayString = cf.getDisplayString();
				String key = configType + "\\" + selection + "\\" + name;
				parameters.put(key, displayString);
			}
		}
		return parameters;
	}

	private void executeMapping(ColNameProvider colNameProvider, Table runtimeTable, Table mappingTable,
			Map<String, MappingTableColumnConfiguration> mappingTableColConfig,
			List<MappingTableRowConfiguration> mappingRows, List<RetrievalConfiguration> retrievalConfig) {
		PluginLog.info("Number of rows in runtime table before mapping: " + runtimeTable.getRowCount());

		Map<String, RetrievalConfiguration> rcByMappingColName = new HashMap<>(retrievalConfig.size()*3);
		for (RetrievalConfiguration rc : retrievalConfig) {
			if (colNameProvider.getColName(rc) != null && colNameProvider.getColName(rc).trim().length() > 0) {
				rcByMappingColName.put(colNameProvider.getColName(rc), rc);
			}
		}
		MappingTableFilterApplicator applicator = 
				new MappingTableFilterApplicator (rcByMappingColName, mappingTableColConfig);
		mappingTable.sort(ROW_ID);
		Set<Integer> rowsToIgnore = new TreeSet<>();
		for (int i=runtimeTable.getRowCount()-1; i >= 0; i--) {
			TableRow runtimeTableRow = runtimeTable.getRow(i);
			if (rowsToIgnore.contains(runtimeTableRow.getNumber())) {
				continue; // ignore rows that have been created during manual join
			}
			
			Collection<MappingTableRowConfiguration> matchingRows = applicator.apply(runtimeTable, runtimeTableRow);
			if (matchingRows.size() == 0) {
				runtimeTable.removeRow(runtimeTableRow.getNumber());
				continue;
			}
			boolean first = true;
			for (MappingTableRowConfiguration matchingRow : matchingRows) {
				int rowNum = -1;
				if (!first) {
					TableRow newRow = runtimeTable.addRow();
					newRow.copyData(runtimeTableRow);
					rowNum = newRow.getNumber();
					rowsToIgnore.add(rowNum);
				} else {
					
					rowNum = runtimeTableRow.getNumber();
				}
				int rowMappingTable = 
						mappingTable.findSorted(mappingTable.getColumnId(ROW_ID), matchingRow.getUniqueRowId(), 0);
				copyMappingDataOutputToRuntimeTable (runtimeTable, rowNum, mappingTable, rowMappingTable, mappingTableColConfig);
				first=false;
			}
		}
		PluginLog.info("Number of rows in runtime table after mapping: " + runtimeTable.getRowCount());
	}

	private void copyMappingDataOutputToRuntimeTable(Table runtimeTable,
			int rowNumRuntimeTable, Table mappingTable, int rowNumMappingTable,
			Map<String, MappingTableColumnConfiguration> mappingTableColConfig) {
		for (Entry<String, MappingTableColumnConfiguration>  mtcc : mappingTableColConfig.entrySet()) {
			Object value = mappingTable.getValue(mtcc.getKey(), rowNumMappingTable);
			if (mtcc.getValue().getMappingColType() == MappingConfigurationColType.OUTPUT) {
				if (mtcc.getValue().getColType() == EnumColType.String) {
					String stringValue = (String) value;
					if (stringValue.trim().startsWith("%") &&
						stringValue.trim().endsWith("%")) {
						String srcColName = stringValue.substring(1, stringValue.length()-1);
						value = runtimeTable.getDisplayString(runtimeTable.getColumnId(srcColName), rowNumRuntimeTable);
					}
				}
				runtimeTable.setValue(mtcc.getKey().substring(2), 
					rowNumRuntimeTable, value);
			}
		}
	}

	private void createMappingOutputColumnsInRuntimeTable(
			Table runtimeTable,
			Table mappingTable,
			Map<String, MappingTableColumnConfiguration> mappingTableColConfig) {
		List<String> newColNames = new ArrayList<>();
		List<EnumColType> newColTypes = new ArrayList<>();
		for (Entry<String, MappingTableColumnConfiguration>  mtcc : mappingTableColConfig.entrySet()) {
			if (mtcc.getValue().getMappingColType() == MappingConfigurationColType.OUTPUT) {
				EnumColType colType = mappingTable.getColumnType(mappingTable.getColumnId(mtcc.getKey()));
				newColNames.add(mtcc.getKey().substring(2));
				newColTypes.add(colType);
			}
		}
		runtimeTable.addColumns(newColNames.toArray(new String[newColNames.size()]), 
				newColTypes.toArray(new EnumColType[newColTypes.size()]));
	}

	private Table retrieveMappingTable(final Session session, ConfigurationItem mappingTableName) {
		Table mappingTable;
		UserTable mappingUserTable = session.getIOFactory().getUserTable(mappingTableName.getValue(), false);
		if (mappingUserTable == null) {
			String errorMessage = "The mandatory table " + mappingTableName.getValue() + " can't be retrieved. "
					+ ". Please check database access rights, confirm the existence of the table in the database "
					+ " and check the ConstantsRepository configuration for " + mappingTableName.getContext()
					+ "\\" + mappingTableName.getSubContext() + "\\"
					+ mappingTableName.getVarName();
			throw new RuntimeException (errorMessage);
		}
		mappingTable = mappingUserTable.retrieveTable();
		return mappingTable;
	}

	private List<MappingTableRowConfiguration> parseMappingTable(
			ColNameProvider colNameProvider, Table mappingTable,
			Table runtimeTable, // runtime table is used to determine col types
			List<RetrievalConfiguration> retrievalConfig,
			Map<String, MappingTableColumnConfiguration> mappingTableColConfig) {
		Map<String, RetrievalConfiguration> retrievalConfigByMappingColName = 
				new HashMap<>();
		for (RetrievalConfiguration rc : retrievalConfig) {
			retrievalConfigByMappingColName.put(colNameProvider.getColName(rc), rc);
		}
		List<MappingTableRowConfiguration> parsedRows = new ArrayList<>(mappingTable.getRowCount());
		for (TableRow mappingRow : mappingTable.getRows()) {
			int uniqueRowId = mappingTable.getInt(ROW_ID, mappingRow.getNumber());
			List<MappingTableCellConfiguration> cellConfigurations = new ArrayList<>(mappingTable.getColumnCount());
			for (MappingTableColumnConfiguration colConfig : mappingTableColConfig.values()) {
				// look at columns containing actual mapping logic only:
				if (colConfig.getMappingColType() == MappingConfigurationColType.MAPPING_LOGIC) {
					RetrievalConfiguration rc = retrievalConfigByMappingColName.get(colConfig.getColName());
					String colNameRuntimeTable = rc.getColNameRuntimeTable();
					EnumColType colType = runtimeTable.getColumnType(runtimeTable.getColumnId(colNameRuntimeTable));
					String unparsedPredicate = mappingTable.getString(colNameProvider.getColName(rc), mappingRow.getNumber());
					if (unparsedPredicate == null) {
						throw new RuntimeException ("The predicate in column '" + colConfig.getColName() + "' is "
								+ " null for row " + Helper.tableRowToString(mappingTable, mappingRow));
					}
					AbstractPredicate predicate = PredicateParser.parsePredicate (colType, unparsedPredicate);
					MappingTableCellConfiguration cellConfig = new MappingTableCellConfiguration(colConfig, predicate);
					cellConfigurations.add(cellConfig);
				}
			}
			MappingTableRowConfiguration rowConfig = new MappingTableRowConfiguration(colNameProvider,
					uniqueRowId, cellConfigurations, retrievalConfig);
			for (MappingTableCellConfiguration cellConfig : cellConfigurations) {
				cellConfig.setRowConfig(rowConfig);
			}
			parsedRows.add(rowConfig);
		}
		return parsedRows;
	}

	private Map<String, MappingTableColumnConfiguration> confirmMappingTableStructure(ConfigurationItem mappingTableName, ColNameProvider colNameProvider, Table mappingTable,
			Table runtimeDataTable,
			List<RetrievalConfiguration> retrievalConfig) {
		StringBuilder errorMessage = new StringBuilder();

		Map<String, RetrievalConfiguration> retrievalConfigByMappingColName = 
				new HashMap<>();
		for (RetrievalConfiguration rc : retrievalConfig) {
			if (colNameProvider.getColName(rc) != null && !colNameProvider.getColName(rc).trim().isEmpty()) {
				retrievalConfigByMappingColName.put(colNameProvider.getColName(rc), rc);				
			}
		}
		Map<String, MappingTableColumnConfiguration> columns = new HashMap<>();
		for (TableColumn tc : mappingTable.getColumns()) {
			boolean startsWith_ = tc.getName().startsWith("o_");
			boolean colNameDefinedInRetrievalTable = retrievalConfigByMappingColName.containsKey(tc.getName());
			RetrievalConfiguration rc = retrievalConfigByMappingColName.get(tc.getName());
			MappingConfigurationColType colType=null;
			if (startsWith_ && !colNameDefinedInRetrievalTable) {
				colType = MappingConfigurationColType.OUTPUT;
			} else if (colNameDefinedInRetrievalTable && tc.getType() != EnumColType.String) {
				errorMessage.append("\n\nThe column " + tc.getName() + " in the mapping table '" 
						+ mappingTableName.getValue() + "' is of type "
						+ tc.getType() + " but as this column is also defined in the retrieval table '" 
						+ mappingTableName.getValue() 
						+ "' so it has to be of type String");
			} else if (colNameDefinedInRetrievalTable) {
				colType = MappingConfigurationColType.MAPPING_LOGIC;
			} else {
				colType = MappingConfigurationColType.UNKNOWN;
			}
			EnumColType targetColType = colType == MappingConfigurationColType.MAPPING_LOGIC?runtimeDataTable.getColumn(rc.getColNameRuntimeTable()).getType():null;
			if (colType == MappingConfigurationColType.OUTPUT) {
				targetColType = mappingTable.getColumnType(mappingTable.getColumnId(tc.getName()));
			}
			columns.put(tc.getName(), new MappingTableColumnConfiguration(colType, tc.getName(), targetColType));
		}
		for (RetrievalConfiguration rc : retrievalConfig) {
			if (colNameProvider.getColName(rc) != null && !colNameProvider.getColName(rc).trim().isEmpty()) {
				if (!columns.containsKey(colNameProvider.getColName(rc))) {
					errorMessage.append("\n\nThe column " + colNameProvider.getColName(rc) + " as defined"
							+ " in the retrieval configuration table "
							+ mappingTableName.getValue()
							+ " does not exist in the mapping table "
							+ mappingTableName.getValue());
				}
			}
		}
		if (errorMessage.length() > 0) {
			throw new RuntimeException (errorMessage.toString());
		}
		return columns;
	}

	private void generateUniqueRowIdForTable(Table runtimeTable) {
		if (runtimeTable.isValidColumn(ROW_ID)) {
			String errorMessage = "The row '" + ROW_ID + " does already exist in the runtime data table."
					+ " This row is important to contain a unique row number. Can't proceed. "
					+ " Please ensure the runtime data table to not contain a row named '"
					+ ROW_ID + "'";
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		runtimeTable.addColumn(ROW_ID, EnumColType.Int);
		int counter = 1;
		for (TableRow runtimeTableRow : runtimeTable.getRows()) {
			runtimeTable.setValue(ROW_ID, runtimeTableRow.getNumber(), counter++);
		}
	}

	private void createOutputTable(final RevalResult revalResult,
			Table runtimeTable, 
			List<RetrievalConfiguration> retrievalConfig,
			Map<String, MappingTableColumnConfiguration> mappingTableColConfig) {
		Map<String, String> columnsToRetain = new TreeMap<>();
		applyMandatoryColumnsOutputTableRetrieval(revalResult.getTable(), runtimeTable, columnsToRetain);
		StringBuilder columnNames = new StringBuilder();
		for (RetrievalConfiguration rc : retrievalConfig) {
			OutputTableRetrievalApplicator retrievalApplicator = new OutputTableRetrievalApplicator(rc);
			retrievalApplicator.apply(revalResult.getTable(), runtimeTable, columnNames, columnsToRetain);
		}
		for (Entry<String, MappingTableColumnConfiguration>  mtcc : mappingTableColConfig.entrySet()) {
			if (mtcc.getValue().getMappingColType() == MappingConfigurationColType.OUTPUT) {
				columnsToRetain.put(mtcc.getKey().substring(2), mtcc.getKey().substring(2));
			}
		}
		for (int i=runtimeTable.getColumnCount()-1; i >= 0; i--) {
			TableColumn rc = runtimeTable.getColumn(i);
			if (columnsToRetain.containsKey(rc.getName())) {
				if (!columnsToRetain.get(rc.getName()).equals(rc.getName())) {
					runtimeTable.setColumnName(rc.getNumber(), columnsToRetain.get(rc.getName()));					
				}
			} else {
				runtimeTable.removeColumn(rc.getNumber());
			}
		}
		revalResult.setTable(runtimeTable);
	}

	private void applyMandatoryColumnsOutputTableRetrieval(Table resultTable,
			Table runtimeTable, Map<String, String> outputColNames) {
		outputColNames.put("event_num", "event_num");
		outputColNames.put("deal_tracking_num", "deal_tracking_num");
		outputColNames.put("tran_num", "tran_num");
		outputColNames.put("ins_num", "ins_num");
		outputColNames.put("ins_para_seq_num", "ins_para_seq_num");
		outputColNames.put("ins_seq_num", "ins_seq_num");
	}

	private List<RetrievalConfiguration> convertRetrievalConfigTableToList(
			final Session session) {
		Table configTable = session.getIOFactory().getUserTable(ConfigurationItem.RETRIEVAL_CONFIG_TABLE_NAME.getValue()).retrieveTable();
		List<RetrievalConfiguration> retrievalConfig=new ArrayList<>(configTable.getRowCount());
		for (TableRow configRow : configTable.getRows()) {
			int priority = configTable.getInt(RetrievalConfigurationTableCols.PRIORITY.getColName(), configRow.getNumber());
			String colNameReportOutput = configTable.getString(RetrievalConfigurationTableCols.COL_NAME_OUTPUT_TABLE.getColName(), configRow.getNumber()); // optional value, might be null
			String colNameRuntimeTable = configTable.getString(RetrievalConfigurationTableCols.COL_NAME_RUNTIME_TABLE.getColName(), configRow.getNumber());
			String colNameMappingTable = configTable.getString(RetrievalConfigurationTableCols.COL_NAME_MAPPING_TABLE.getColName(), configRow.getNumber()); // optional value, might be null
			String colNameTaxTable = configTable.getString(RetrievalConfigurationTableCols.COL_NAME_TAX_TABLE.getColName(), configRow.getNumber());  // optional value, might be null
			String colNameMaterialNumberTable = configTable.getString(RetrievalConfigurationTableCols.COL_NAME_MATERIAL_NUMBER_TABLE.getColName(), configRow.getNumber()); // optional value, might be null
			String retrievalLogic = configTable.getString(RetrievalConfigurationTableCols.RETRIEVAL_LOGIC.getColName(), configRow.getNumber());
			RetrievalConfiguration rc = new RetrievalConfiguration(priority, colNameRuntimeTable,
					colNameMappingTable, colNameReportOutput, colNameTaxTable,
					colNameMaterialNumberTable, retrievalLogic);
			retrievalConfig.add(rc);
		}
		return retrievalConfig;
	}

	private void showRuntimeDataTable(final ConfigurationItem trigger,
			final Session session, final Table runtimeTable) {
		String showRuntimeDataTable = trigger.getValue();
		if (showRuntimeDataTable != null) {
			try {
				boolean showTable = Boolean.parseBoolean(showRuntimeDataTable);
				if (showTable) {
					session.getDebug().viewTable(runtimeTable.cloneData());					
				}
			} catch (RuntimeException ex) { }
		}
	}

	private JavaTable createEventDataTable(final Transactions transactions, TableFactory tableFactory, RevalResult revalResult) {
		PluginLog.info("Start createEventDataTable");
		JavaTable resultTable = new JavaTable();
		Map<Integer, Integer> tranNumToDealTrackingNum = new TreeMap<>();
		long totalTimeAddRowsAndColumns = 0;
		long totalTimeCreateEventTable = 0;
		boolean first = true;
		for (Transaction tran : transactions) {
			tranNumToDealTrackingNum.put(tran.getTransactionId(), tran.getDealTrackingId());
			long start = System.currentTimeMillis();
			Table dealEventTable = tran.getDealEvents().asTable();
			long end = System.currentTimeMillis();;
			totalTimeCreateEventTable += end - start;
//			revalResult.setTable(dealEventTable.cloneStructure());
			start = System.currentTimeMillis();
			resultTable.mergeAddEndurTable(dealEventTable);
			end = System.currentTimeMillis();
			totalTimeAddRowsAndColumns += end - start;
//			if (first) {
//				revalResult.setTable(dealEventTable.cloneStructure());
//			}
			first = false;
		}
		PluginLog.info("Total time in creating event table(ms): " + totalTimeCreateEventTable);
		PluginLog.info("Total time in add rows and columns(ms): " + totalTimeAddRowsAndColumns);
		resultTable.addColumn("deal_tracking_num", EnumColType.Int);
		for (int row = resultTable.getRowCount()-1; row >= 0; row--) {
			int tranNum = resultTable.getInt("tran_num", row);
			resultTable.setValue ("deal_tracking_num", row, tranNumToDealTrackingNum.get(tranNum));
		}
		PluginLog.info("createEventDataTable finished successfully");
		return resultTable;
	}

	public void finalizeCalculation(final Session session, final Scenario scenario,
			final EnumSplitMethod splitMethod, final RevalResult masterResult) {
		init(session);
		// Empty default implementation
	}

	@Override
	public void format(final Session session, final RevalResult revalResult) {
		init(session);
		PluginLog.info("Starting format of Raw Accounting Data UDSR");
		PluginLog.info("Completed format of Raw Accounting Data UDSR");
	}

	private void formatColumns(final RevalResult revalResult) {
//		revalResult.applyDefaultFormat();
		Table result = revalResult.getTable();
		formatColumn(result, "uom", EnumReferenceTable.IdxUnit);
		formatColumn(result, "buy_sell", EnumReferenceTable.BuySell);
		formatColumn(result, "ins_type", EnumReferenceTable.InsType);
		formatColumn(result, "from_currency", EnumReferenceTable.Currency);
		formatColumn(result, "to_currency", EnumReferenceTable.Currency);
		formatColumn(result, "event_type", EnumReferenceTable.EventType);
		formatColumn(result, "settle_currency_id", EnumReferenceTable.Currency);
	}

	private void formatColumn(Table result, String colName, EnumReferenceTable refTable) {
		int colId = result.getColumnId(colName);
		if (result.isValidColumn(colId) && result.getColumnType(colId) == EnumColType.Int) {
			ColumnFormatterAsRef formatter = result.getFormatter().createColumnFormatterAsRef(refTable);
			if (result.getFormatter().getColumnFormatter(colId) != formatter) {
				result.getFormatter().setColumnFormatter(colId, formatter);				
			}
		}
	}

	public void init(Session session) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR");
		String logLevel = ConfigurationItem.LOG_LEVEL.getValue();
		String logFile = ConfigurationItem.LOG_FILE.getValue();
		String logDir = ConfigurationItem.LOG_DIRECTORY.getValue();
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

	public ConfigurationItem getTypePrefix() {
		return typePrefix;
	}
}
