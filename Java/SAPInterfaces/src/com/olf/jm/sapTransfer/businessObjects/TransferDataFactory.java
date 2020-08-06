package com.olf.jm.sapTransfer.businessObjects;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;


/**
 * A factory for creating TransferData objects used in the booking on metal 
 * transfers.
 */
public class TransferDataFactory implements ITransferDataFactory {
	
	/** The current OC context. */
	private Context context;	
	
	
	/**  SQL statment used for loading internal party info.
	 * 
	 * The SQL expects the following Format Parameters to be populated before being called 		
	 * 	1 - Portfolio postfix 		
	 *  2 - SAP trading desk id 		
	 *  3 - SAP trading desk id 		
	 */	
	private static final String PARTY_SQL = 
			  " SELECT internal_id.*, internal_party.* \n"
			+ " FROM (  \n"
			+ " SELECT p_le.short_name AS int_le, p_bu.short_name AS int_bu, po.name AS int_portfolio, "
			+ "        piv.value as int_sap_id, loco.value as loco   \n"
			+ " FROM 	party_info_view  piv  \n"
			+ " JOIN party p_bu ON p_bu.party_id = piv.party_id  \n"
			+ " JOIN party_relationship pr ON pr.business_unit_id = p_bu.party_id  \n"
			+ " JOIN party p_le ON p_le.party_id = pr.legal_entity_id  \n"
			+ " JOIN party_portfolio pp ON pp.party_id = p_bu.party_id   \n"
			+ " JOIN portfolio po ON po.id_number = pp.portfolio_id AND po.name like '%%%s%%'  \n" 
			+ " LEFT JOIN 	party_info_view  loco on loco.party_id = p_bu.party_id  and loco.type_name = 'Loco' \n"
			+ " WHERE piv.type_name = 'SAP Desk Location'   \n"
			+ " AND piv.value = '%s' ) internal_party  		 \n"	
			+ " RIGHT JOIN  (SELECT '%s'  AS input_int_id ) internal_id ON  int_sap_id = input_int_id";
	
	
	/**  SQL statment used to load the account details for the to / from account  
	 * 
	 * The sql expects the following format parameters to be populated before being called  
	 * 1 - account_number  
	 * 2 - LE code   
	 * 3 - BU code  
	 * 4 - LE code   
	 * 5 - BU code  
	 * 6 - account_number  
	 * 7 - LE code   
	 * 8 - BU code  
	 * 9 - LE code   
	 * 10 - BU code. 
	 */
	private static final String ACCOUNT_SQL = 	
			  " SELECT 'external' as int_ext, sap.*, account.* FROM \n"
			+ " (SELECT p.short_name AS bu,  account_name, loco.info_value AS loco, form.info_value AS form, "
			+ "         piv.value AS bu_sap_id, piv_le.value AS le_sap_id, account_number \n"
			+ " FROM party_info_view piv \n"
			+ " JOIN party p ON p.party_id = piv.party_id \n" 
			+ " JOIN party_account pa ON pa.party_id = p.party_id \n" 
			+ " JOIN account a ON a.account_id = pa.account_id AND a.account_number = '%s' \n"
			+ " JOIN (SELECT account_id, info_value, type_name FROM account_info ai \n"
			+ " JOIN account_info_type ait ON ai.info_type_id = ait.type_id  AND type_name = 'Loco') loco  "
			+ "		ON loco.account_id = a.account_id  \n"
			+ " JOIN (SELECT account_id, info_value, type_name FROM account_info ai \n"
			+ " JOIN account_info_type ait ON ai.info_type_id = ait.type_id  AND type_name = 'Form') form  "
			+ "		ON form.account_id = a.account_id  \n"
			+ " JOIN party_relatiONship pr ON pr.business_unit_id = p.party_id \n"
			+ " JOIN party_info_view  piv_le ON pr.legal_entity_id = piv_le.party_id "
			+ "		AND piv_le.type_name = 'Ext Legal Entity Code' AND piv_le.value = '%s' \n"
			+ " WHERE piv.type_name = 'Ext Business Unit Code' AND piv.value = '%s') account \n"
			+ " RIGHT JOIN (SELECT '%s' AS input_le, '%s' AS input_bu ) sap ON input_le = le_sap_id AND input_bu = bu_sap_id \n"
			+ " UNION  \n"
			+ " SELECT 'internal' as int_ext, sap.*, account.* FROM \n"
			+ " (SELECT p.short_name AS bu,  account_name, loco.info_value AS loco, form.info_value AS form, "
			+ "			piv.value  AS bu_sap_id, piv_le.value AS le_sap_id, account_number \n"
			+ " FROM party_info_view piv \n"
			+ " JOIN party p ON p.party_id = piv.party_id \n"  
			+ " JOIN party_account pa ON pa.party_id = p.party_id \n"
			+ " JOIN account a ON a.account_id = pa.account_id AND a.account_number = '%s' \n"
			+ " JOIN (SELECT account_id, info_value, type_name FROM account_info ai \n"
			+ " JOIN account_info_type ait ON ai.info_type_id = ait.type_id  AND type_name = 'Loco') loco  "
			+ "		ON loco.account_id = a.account_id  \n"
			+ " JOIN (SELECT account_id, info_value, type_name FROM account_info ai \n"
			+ " JOIN account_info_type ait ON ai.info_type_id = ait.type_id  AND type_name = 'Form') form  "
			+ "		ON form.account_id = a.account_id  \n"
			+ " JOIN party_relatiONship pr ON pr.business_unit_id = p.party_id \n"
			+ " JOIN party_info_view  piv_le ON pr.legal_entity_id = piv_le.party_id AND piv_le.type_name = 'Int Legal Entity Code' "
			+ "	AND piv_le.value = '%s' \n"
			+ " WHERE piv.type_name = 'Int Business Unit Code' AND piv.value = '%s') account \n"
			+ " RIGHT JOIN (SELECT '%s' AS input_le, '%s' AS input_bu ) sap ON input_le = le_sap_id AND input_bu = bu_sap_id \n";
	
	
	private static final String TO_ACCOUNT_SQL = "SELECT 'external' as int_ext, a.account_number, a.account_name, p.short_name AS bu, \n"
												 + " loco.info_value AS loco, form.info_value AS form, '' AS input_le, '' AS input_bu, '' AS bu_sap_id, '' AS le_sap_id \n"
												 + " FROM party p JOIN party_account pa ON pa.party_id   = p.party_id \n"
												 + " JOIN account a ON a.account_id = pa.account_id \n"
												 + " AND a.account_number = '%s' \n"
												 + " JOIN (SELECT account_id, info_value, type_name FROM account_info ai \n"
												 + " JOIN account_info_type ait \n"
												 + " ON ai.info_type_id = ait.type_id  AND type_name = 'Loco') loco \n"
												 + " ON loco.account_id = a.account_id \n"
												 + " JOIN (SELECT account_id, info_value, type_name FROM account_info ai \n"
												 + " JOIN account_info_type ait \n"
												 + " ON ai.info_type_id = ait.type_id  AND type_name = 'Form') \n"
												 + " form ON form.account_id = a.account_id";
	/**
	 * Instantiates a new data factory.
	 *
	 * @param currentContext the current OC context
	 */
	public TransferDataFactory(final Context currentContext) {
		this.context = currentContext;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.sapTransfer.businessObjects.ITransferDataFactory#
	 * getPartyData(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, 
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public final ISapPartyData getPartyData(final String portfolioPostfix, final String tradingDeskId, final String toAccountNumber, 
			final String toCompanyCode, final String toSegment, final String fromAccountNumber, final String fromCompanyCode, 
			final String fromSegment) {
		
		Table partyInfo = loadInternalPartyData(portfolioPostfix, tradingDeskId);
			
		Table toAccount  = loadAccountData(toAccountNumber, toCompanyCode, toSegment);
		
		Table fromAccount = loadAccountData(fromAccountNumber, fromCompanyCode, fromSegment);
		
		return new TransferSAPPartyData(partyInfo, fromAccount, toAccount);
		
	}
	
	/**
	 * Load internal party data.
	 *
	 * @param portfolioPostfix the portfolio postfix
	 * @param tradingDeskId the trading desk id
	 * @return the table
	 */
	private Table loadInternalPartyData(final String portfolioPostfix, final String tradingDeskId) {
		String sql =  String.format(PARTY_SQL, portfolioPostfix, tradingDeskId, tradingDeskId);
		

		Table rawPartyData = null;
		try {
			rawPartyData = runSql(sql);
			
		} catch (Exception e) {
			throw new RuntimeException(
					"Error loading party data for Trading Desk Id [" 
							+ tradingDeskId + "]  portfolio [" 
							+ portfolioPostfix + "]");
		} 
		
		return rawPartyData;		
	}
	
	/**
	 * Load account data.
	 *
	 * @param accountNumber the accountNumber
	 * @param leCode the l  ecode
	 * @param buCode the b ucode
	 * @return the table
	 */
	private Table loadAccountData(final String accountNumber, final String leCode, final String buCode) {
		
		String sql = null;
		if ((leCode.isEmpty() && buCode.isEmpty()) && !accountNumber.isEmpty()){
			sql = String.format(TO_ACCOUNT_SQL, accountNumber);
		}else {
			sql = String.format(ACCOUNT_SQL, accountNumber, leCode, buCode,
					leCode, buCode, accountNumber, leCode, buCode, leCode, buCode);	
		}
			

		Table rawAccountData = null;
		try {
			rawAccountData = runSql(sql);
			
		} catch (Exception e) {
			throw new RuntimeException(
					"Error loading account " + e.getMessage()); 
			//data for Trading Desk Id [" 
			//				+ tradingDeskId + "]  portfolio [" 
			//				+ portfolioPostfix + "]");
		}
		return rawAccountData;		
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

	/* (non-Javadoc)
	 * @see com.olf.jm.sapTransfer.businessObjects.ITransferDataFactory#
	 * getTemplateData(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public final ISapTemplateData getTemplateData(final String sapMetal) {
		Table result = runSql("select description from currency where name = '" + sapMetal + "'");
		
		if (result == null || result.getRowCount() != 1) { 
			return new TransferSAPTemplateData("");
		}	
		
		String portfolioPostFix = result.getString("description", 0);

		
		return new TransferSAPTemplateData(portfolioPostFix);
	}	
	
}
