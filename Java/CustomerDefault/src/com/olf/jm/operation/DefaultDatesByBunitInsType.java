
package com.olf.jm.operation;


import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractFieldListener;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranfField;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
//import com.openlink.util.logging.PluginLog;

@ScriptCategory({ EnumScriptCategory.OpsSvcTranfield })
public class DefaultDatesByBunitInsType extends AbstractFieldListener {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "OpsService";
	
	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "CustomerDefaulting";
	
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONTEXT, SUBCONTEXT);

		String logLevel = "Info";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}
	
	@Override
	public void postProcess(Session session, Field field, String oldValue, String newValue, Table clientData) {

		try{
			
			init();
			Logging.info("Start DefaultDatesByUnitInsType");

			
			
			Transaction tran = field.getTransaction();
			EnumToolset toolset = tran.getToolset();
			
			StaticDataFactory sdf = session.getStaticDataFactory();
			IOFactory iof = session.getIOFactory();

			
			if (field.getTranfId() == EnumTranfField.ProjIndex || 
	   			field.getTranfId() == EnumTranfField.Ticker    || 
	   			field.getTranfId() == EnumTranfField.CurrencyPair ||
	   			field.getTranfId() == EnumTranfField.ExternalBunit ||
	   			field.getTranfId() == EnumTranfField.StartDate ) {
				
				
				int extBU = tran.getValueAsInt(EnumTransactionFieldId.ExternalBusinessUnit);
				
				String strSql = "SELECT type_name, value \n";
				strSql += "FROM USER_bunit_ins_defaultdate \n";
				strSql += "WHERE internal_bunit = '" + Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE,extBU)  + "'\n";
				strSql += "AND toolset = '" + Ref.getName(SHM_USR_TABLES_ENUM.TOOLSET_ID_TABLE,toolset.getValue()) + "' \n";

				Table tblMapping = iof.runSQL(strSql);

				if(tblMapping.getRowCount() > 0){
					
					setDefaultSettlementDates(session, tran, tblMapping);	
				}
				else{
					Logging.info("No additional instype date mapping found for " + Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE,extBU));
				}
				
				tblMapping.dispose();
			}

			
		}catch (Exception e){
			String errorMessage = "Error " + e.getMessage();
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
			
			
		}
		Logging.info("End DefaultDatesByUnitInsType");
		Logging.close();
	}
		private void setDefaultSettlementDates(Session session, Transaction tran, Table tblMapping) {
			StaticDataFactory sdf = session.getStaticDataFactory();
			IOFactory iof = session.getIOFactory();
			EnumToolset toolset = tran.getToolset();
			
			if (toolset == EnumToolset.ComSwap){
				Currency ccy = sdf.getReferenceObject(Currency.class, tran.getLeg(0).getValueAsString(EnumLegFieldId.Currency));
				try {
					//setPymtOffsetComSwap(tran, ccy, temp, "Metal Payment Term");
					setPymtOffsetComSwap(tran, ccy, tblMapping, "Cash Payment Term");
				} catch (RuntimeException e) {
					return;
				}
			}
		}
		
		
		private void setPymtOffsetComSwap(Transaction tran, Currency ccy, Table temp, String infoValue) {
			
			String projIndex = tran.retrieveField(EnumTranfField.ProjIndex, 1).getValueAsString();
			
			String skipIndexs = "";
			try {
				skipIndexs = constRep.getStringValue("ComSwapSkipProjIndex", skipIndexs);
			} catch (Exception e1) {
				Logging.error("Error reading indexs to skip from the cons repo. \n");
				temp.dispose();
				throw new RuntimeException();
			}
			
			if(skipIndexs.contains(projIndex)) {
				Logging.info("Skipping defaulting for proj index " + projIndex);
				return;
			}
			 //int rowId = temp.find(0, infoValue, 0);
			int rowId = temp.find(temp.getColumnId("type_name"), infoValue, 0);
			if (rowId >= 0) {
				try {
					if (infoValue.equalsIgnoreCase("Metal Payment Term")) {
						if (ccy.isPreciousMetal()) {
							tran.retrieveField(EnumTranfField.PymtDateOffset, 0).setValue(temp.getString(1, rowId));
						}
					} 
					else if (infoValue.equalsIgnoreCase("Cash Payment Term")){
						if (ccy.isPreciousMetal()) {
							
							int intLegCount = tran.getLegCount();
							for(int i =0;i<intLegCount;i++){
								tran.retrieveField(EnumTranfField.PymtDateOffset, i).setValue(temp.getString(1, rowId));	
							}
						}
						else {
							tran.retrieveField(EnumTranfField.PymtDateOffset, 0).setValue(temp.getString(1, rowId));
						}
					}
				} catch (Exception e) {
					Logging.error("Symbolic date " + temp.getString(1, rowId) + " not Valid. \n");
					temp.dispose();
					throw new RuntimeException();
				}
			}
			
		}

}