package com.olf.jm.coverage.businessObjects.dataFactories;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;


/**
 * A factory for creating Data objects loaded from the database.
 */
public class CoverageDataFactory implements ICoverageDataFactory {

	/** The current OC context. */
	private Context context;

	/**  SQL statment used for loading internal / external BU and LE info.
	 * 
	 * The SQL expects the following Format Parameters to be populated before being called 		
	 * 	1 - Portfolio postfix 		
	 *  2 - SAP Internal LE 		
	 *  3 - SAP Internal LE 
	 *  4 - account number			
	 *  5 - SAP External BU	
	 *  6 - SAP External BU. 
	 */
	private static final String PARTY_SQL = 
			  " SELECT internal_id.*, internal_party.*, external_id.*, external_party.* \n" 
			+ " FROM (  \n"
					+ " SELECT p_le.short_name AS int_le, p_bu.short_name AS int_bu, po.name AS int_portfolio, value as int_sap_id \n "  
					+ "	    FROM 	party_info_view  piv \n " 
					+ "	    JOIN party p_bu ON p_bu.party_id = piv.party_id \n " 
					+ "	    JOIN party_relationship pr ON pr.business_unit_id = p_bu.party_id \n " 
					+ "	    JOIN party p_le ON p_le.party_id = pr.legal_entity_id \n " 
					+ "	    JOIN party_portfolio pp ON pp.party_id = p_bu.party_id \n " 
					+ "	    JOIN portfolio po ON po.id_number = pp.portfolio_id AND po.name like '%%%s' \n " 
					+ "	    WHERE type_name = 'SAP Desk Location'  \n " 
					+ "	    AND value = '%s'  ) internal_party 	 \n"		
					+ " RIGHT JOIN  (SELECT '%s'  AS input_int_id ) internal_id ON  int_sap_id = input_int_id, \n" 			
					+ " (SELECT p_le.short_name AS ext_le, p_bu.short_name AS ext_bu, value as ext_sap_id, settle_id, settle_name , account_number, account_name,  \n"
					+ "         ISNULL(si_list .info_value, (select default_value from account_info_type  where type_name =  'Auto SI Shortlist')) as use_auto_si_shortlist, \n"
					+ "         loco.info_value as loco,     form.info_value as form \n"
					+ " FROM party_info_view  piv \n"  
					+ " JOIN party p_bu ON p_bu.party_id = piv.party_id \n"  
					+ " JOIN party_relationship pr ON pr.business_unit_id = p_bu.party_id \n"  
					+ " JOIN party p_le ON p_le.party_id = pr.legal_entity_id \n"  
					+ " JOIN settle_instructions si on si.party_id = piv.party_id \n"
					+ " JOIN account a on si.account_id = a.account_id and account_number = '%s' \n"	
					+ " LEFT JOIN   (SELECT account_id, info_value \n"
					+ "              FROM account_info ai \n" 
					+ "              JOIN  account_info_type ait ON ai.info_type_id = ait.type_id and ait.type_name =  'Auto SI Shortlist' ) \n" 
					+ "               si_list on si_list.account_id = a.account_id \n"
					+ " LEFT JOIN ( SELECT account_id,   info_value   \n"
					+ "             FROM   ACCOUNT_INFO ai  \n"
					+ "             JOIN ACCOUNT_INFO_TYPE ait ON ai.info_type_id = ait.type_id AND  ait.type_name = 'Loco' ) \n"
					+ "             loco ON loco.account_id = a.account_id \n"
					+ " LEFT JOIN ( SELECT account_id,   info_value   \n"
					+ "             FROM   ACCOUNT_INFO ai  \n"
					+ "             JOIN ACCOUNT_INFO_TYPE ait   ON ai.info_type_id = ait.type_id AND  ait.type_name = 'Form' ) \n"
					+ "             form ON form.account_id = a.account_id \n"
					+ " WHERE type_name = 'Ext Business Unit Code' \n" 
					+ " AND value = '%s'			) external_party \n"			
					+ " RIGHT JOIN  (SELECT '%s'  AS input_ext_id ) external_id ON  ext_sap_id = Input_ext_id  \n";	
	
		   		   
	/** SQL statement used to load the template / index information needed to book a coverage trade.
	 * The SQL expects the following format parameters  to be set before being called.
	 * 1 - sap instrument id 
	 * 2 - sap company code 
	 * 3 - sap time code
	 * 4 - sap metal 
	 * 5 - sap currency. 
	 * */
	private static final String TEMPLATE_SQL = 
	//		  " SELECT inst_map.*, metal_map.*\n "
	//		+ " FROM \n "  
	//		+ "	(SELECT * FROM USER_jm_sap_inst_map WHERE sap_inst_id = '%s' AND sap_cmpy_code = '%s') inst_map, \n "  
	//		+ "	(SELECT * FROM USER_jm_sap_metal_map WHERE sap_metal = '%s' AND sap_currency = '%s') metal_map ";

	  " SELECT inst_map.sap_inst_id, inst_map.sap_cmpy_code, ins_type, template, name as ref_source, ticker, proj_index, portfolio_postfix, sap_metal, sap_currency, cflow_type \n "
	+ " FROM    (SELECT * FROM USER_jm_sap_inst_map WHERE sap_inst_id = '%s' AND sap_cmpy_code = '%s') inst_map \n "
	+ " LEFT JOIN (select  sap_inst_id, sap_time_code, name from USER_jm_sap_time_code_map map left join ref_source  ref  on ref.name = map.endur_time_code where  sap_time_code = '%s') time_code on time_code.sap_inst_id = inst_map.sap_inst_id, \n " 
	+ " (SELECT * FROM USER_jm_sap_metal_map WHERE sap_metal = '%s' AND sap_currency = '%s') metal_map \n ";

	
	/**
	 * Instantiates a new data factory.
	 *
	 * @param currentContext the current OC context
	 */
	public CoverageDataFactory(final Context currentContext) {
		this.context = currentContext;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.dataFactories.IDataFactory#
	 * getPartyData(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public final ISapPartyData getPartyData(final String portfolioPostfix,
			final String intSapId, final String extSapId,
			final String accountNumber) {

		String sql =  String.format(PARTY_SQL, portfolioPostfix, intSapId, intSapId, accountNumber, extSapId, extSapId);
		
		CoverageSapPartyData partyData = null;
		Table rawPartyData = null;
		try {
			rawPartyData = runSql(sql);
			
			partyData = new CoverageSapPartyData(rawPartyData);
		} catch (Exception e) {
			throw new RuntimeException(
					"Error loading party data for Int Id [" 
							+ intSapId + "] Ext Id [" + extSapId + "] portfolio [" 
							+ portfolioPostfix + "]");
		} finally {
			if (rawPartyData != null) {
				rawPartyData.dispose();
			}
		}
		
		return partyData;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.dataFactories.IDataFactory#
	 * getTemplateData(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public final ISapTemplateData getTemplateData(final String sapInsId, final String sapMetal,
			final String sapExtId, final String sapCurrency, final String sapTimeCode) {
		String sql =  String.format(TEMPLATE_SQL, sapInsId, sapExtId, sapTimeCode, sapMetal, sapCurrency);
		
		CoverageSapTemplateData templateData = null;
		Table rawTemplateData = null;
		try {
			rawTemplateData = runSql(sql);
			
			if (rawTemplateData.getRowCount() == 0) {
				
				// look for the default entry
				sql =  String.format(TEMPLATE_SQL, sapInsId, "*", sapTimeCode, sapMetal, sapCurrency);
				rawTemplateData = runSql(sql);
				if (rawTemplateData.getRowCount() == 0) {
					// No data create a empty object with just the input parameters.
					templateData = new CoverageSapTemplateData(sapInsId, sapMetal,
						sapExtId, sapCurrency);
				} else {
					templateData = new CoverageSapTemplateData(rawTemplateData);
				}
			} else {
				templateData = new CoverageSapTemplateData(rawTemplateData);
			}
		} catch (Exception e) {
			throw new RuntimeException(
					"Error loading template data for Instrument [" 
							+ sapInsId + "] Metal [" + sapMetal + "] currency [" 
							+ sapCurrency + "] External Id [" + sapExtId + "] Timecode [ " + sapTimeCode + "]. " + e.getMessage());
		} finally {
			if (rawTemplateData != null) {
				rawTemplateData.dispose();
			}
		}
		
		return templateData;
	}
	
	/**
	 * Helper method to run sql statements..
	 *
	 * @param sql the sql to execute
	 * @return the table containing the sql output
	 */
	private Table runSql(final String sql) {
		
		IOFactory iof = context.getIOFactory();
	   
		Logging.debug("About to run SQL. \n" + sql);
		
		
		Table t = null;
		try {
			t = iof.runSQL(sql);
		} catch (Exception e) {
			String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
				
		return t;
		
	}

}
